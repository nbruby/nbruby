/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package org.netbeans.modules.ruby.spi.project.support.rake;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.ProjectManager;
import org.openide.ErrorManager;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileUtil;
import org.openide.util.EditableProperties;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.openide.util.NbCollections;
import org.openide.util.RequestProcessor;
import org.openide.util.TopologicalSortException;
import org.openide.util.Union2;
import org.openide.util.Utilities;
import org.openide.util.WeakListeners;

/**
 * Support for working with Ant properties and property files.
 * @author Jesse Glick
 */
public class PropertyUtils {
    
    private PropertyUtils() {}
    
    /**
     * Location in user directory of per-user global properties.
     * May be null if <code>netbeans.user</code> is not set.
     */
    static File userBuildProperties() {
        String nbuser = System.getProperty("netbeans.user"); // NOI18N
        if (nbuser != null) {
            return FileUtil.normalizeFile(new File(nbuser, "build.properties")); // NOI18N
        } else {
            return null;
        }
    }
    
    private static Map<File,Reference<PropertyProvider>> globalPropertyProviders = new HashMap<File,Reference<PropertyProvider>>();
    
    /**
     * Load global properties defined by the IDE in the user directory.
     * Currently loads ${netbeans.user}/build.properties if it exists.
     * <p>
     * Acquires read access.
     * <p>
     * To listen to changes use {@link #globalPropertyProvider}.
     * @return user properties (empty if missing or malformed)
     */
    public static EditableProperties getGlobalProperties() {
        return ProjectManager.mutex().readAccess(new Mutex.Action<EditableProperties>() {
            public EditableProperties run() {
                File ubp = userBuildProperties();
                if (ubp != null && ubp.isFile() && ubp.canRead()) {
                    try {
                        InputStream is = new FileInputStream(ubp);
                        try {
                            EditableProperties properties = new EditableProperties(true);
                            properties.load(is);
                            return properties;
                        } finally {
                            is.close();
                        }
                    } catch (IOException e) {
                        Logger.getLogger(PropertyUtils.class.getName()).log(Level.INFO, null, e);
                    }
                }
                // Missing or erroneous.
                return new EditableProperties(true);
            }
        });
    }
    
    /**
     * Edit global properties defined by the IDE in the user directory.
     * <p>
     * Acquires write access.
     * @param properties user properties to set
     * @throws IOException if they could not be stored
     * @see #getGlobalProperties
     */
    public static void putGlobalProperties(final EditableProperties properties) throws IOException {
        try {
            ProjectManager.mutex().writeAccess(new Mutex.ExceptionAction<Void>() {
                public Void run() throws IOException {
                    File ubp = userBuildProperties();
                    if (ubp != null) {
                        FileObject bp = FileUtil.toFileObject(ubp);
                        if (bp == null) {
                            if (!ubp.exists()) {
                                FileObject folder = FileUtil.createFolder(ubp.getParentFile());
                                folder.createData(ubp.getName());
                                assert ubp.isFile() : "Did not actually make " + ubp;
                            }
                            bp = FileUtil.toFileObject(ubp);
                            if (bp == null) {
                                // XXX ugly (and will not correctly notify changes) but better than nothing:
                                ErrorManager.getDefault().log(ErrorManager.WARNING, "Warning - cannot properly write to " + ubp + "; might be because your user directory is on a Windows UNC path (issue #46813)? If so, try using mapped drive letters.");
                                OutputStream os = new FileOutputStream(ubp);
                                try {
                                    properties.store(os);
                                } finally {
                                    os.close();
                                }
                                return null;
                            }
                        }
                        FileLock lock = bp.lock();
                        try {
                            OutputStream os = bp.getOutputStream(lock);
                            try {
                                properties.store(os);
                            } finally {
                                os.close();
                            }
                        } finally {
                            lock.releaseLock();
                        }
                    } else {
                        throw new IOException("Do not know where to store build.properties; must set netbeans.user!"); // NOI18N
                    }
                    return null;
                }
            });
        } catch (MutexException e) {
            throw (IOException)e.getException();
        }
    }
    
    /**
     * Create a property evaluator based on {@link #getGlobalProperties}
     * and {@link #putGlobalProperties}.
     * It will supply global properties and fire changes when this file
     * is changed.
     * @return a property producer
     */
    public static synchronized PropertyProvider globalPropertyProvider() {
        File ubp = userBuildProperties();
        if (ubp != null) {
            Reference<PropertyProvider> globalPropertyProvider = globalPropertyProviders.get(ubp);
            if (globalPropertyProvider != null) {
                PropertyProvider pp = globalPropertyProvider.get();
                if (pp != null) {
                    return pp;
                }
            }
            PropertyProvider gpp = propertiesFilePropertyProvider(ubp);
            globalPropertyProviders.put(ubp, new SoftReference<PropertyProvider>(gpp));
            return gpp;
        } else {
            return fixedPropertyProvider(Collections.<String,String>emptyMap());
        }
    }

    /**
     * Create a property provider based on a properties file.
     * The file need not exist at the moment; if it is created or deleted an appropriate
     * change will be fired. If its contents are changed on disk a change will also be fired.
     * @param propertiesFile a path to a (possibly nonexistent) *.properties file
     * @return a supplier of properties from such a file
     * @see Properties#load
     */
    public static PropertyProvider propertiesFilePropertyProvider(File propertiesFile) {
        assert propertiesFile != null;
        return new FilePropertyProvider(propertiesFile);
    }
    
    /**
     * Provider based on a named properties file.
     */
    private static final class FilePropertyProvider implements PropertyProvider, FileChangeListener {
        
        private static final RequestProcessor RP = new RequestProcessor("PropertyUtils.FilePropertyProvider.RP"); // NOI18N
        
        private final File properties;
        private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
        private Map<String,String> cached = null;
        private long cachedTime = 0L;
        
        public FilePropertyProvider(File properties) {
            this.properties = properties;
            FileUtil.addFileChangeListener(this, properties);
        }
        
        public Map<String,String> getProperties() {
            long currTime = properties.lastModified();
            if (cached == null || cachedTime != currTime) {
                cachedTime = currTime;
                cached = loadProperties();
            }
            return cached;
        }
        
        private Map<String,String> loadProperties() {
            // XXX does this need to run in PM.mutex.readAccess?
            if (properties.isFile() && properties.canRead()) {
                try {
                    InputStream is = new FileInputStream(properties);
                    try {
                        Properties props = new Properties();
                        props.load(is);
                        return NbCollections.checkedMapByFilter(props, String.class, String.class, true);
                    } finally {
                        is.close();
                    }
                } catch (IOException e) {
                    Logger.getLogger(PropertyUtils.class.getName()).log(Level.INFO, null, e);
                }
            }
            // Missing or erroneous.
            return Collections.emptyMap();
        }
        
        private void fireChange() {
            cachedTime = -1L; // force reload
            final ChangeListener[] ls;
            synchronized (this) {
                if (listeners.isEmpty()) {
                    return;
                }
                ls = listeners.toArray(new ChangeListener[listeners.size()]);
            }
            final ChangeEvent ev = new ChangeEvent(this);
            final Mutex.Action<Void> action = new Mutex.Action<Void>() {
                public Void run() {
                    for (ChangeListener l : ls) {
                        l.stateChanged(ev);
                    }
                    return null;
                }
            };
            if (ProjectManager.mutex().isWriteAccess()) {
                // Run it right now. postReadRequest would be too late.
                ProjectManager.mutex().readAccess(action);
            } else if (ProjectManager.mutex().isReadAccess()) {
                // Run immediately also. No need to switch to read access.
                action.run();
            } else {
                // Not safe to acquire a new lock, so run later in read access.
                RP.post(new Runnable() {
                    public void run() {
                        ProjectManager.mutex().readAccess(action);
                    }
                });
            }
        }
        
        public synchronized void addChangeListener(ChangeListener l) {
            listeners.add(l);
        }
        
        public synchronized void removeChangeListener(ChangeListener l) {
            listeners.remove(l);
        }

        public void fileFolderCreated(FileEvent fe) {
            fireChange();
        }

        public void fileDataCreated(FileEvent fe) {
            fireChange();
        }

        public void fileChanged(FileEvent fe) {
            fireChange();
        }

        public void fileDeleted(FileEvent fe) {
            fireChange();
        }

        public void fileRenamed(FileRenameEvent fe) {
            fireChange();
        }

        public void fileAttributeChanged(FileAttributeEvent fe) {
            fireChange();
        }
        
        public String toString() {
            return "FilePropertyProvider[" + properties + ":" + getProperties() + "]"; // NOI18N
        }
    }
    
    /**
     * Evaluate all properties in a list of property mappings.
     * <p>
     * If there are any cyclic definitions within a single mapping,
     * the evaluation will fail and return null.
     * @param defs an ordered list of property mappings, e.g. {@link EditableProperties} instances
     * @param predefs an unevaluated set of initial definitions
     * @return values for all defined properties, or null if a circularity error was detected
     */
    private static Map<String,String> evaluateAll(Map<String,String> predefs, List<Map<String,String>> defs) {
        Map<String,String> m = new HashMap<String,String>(predefs);
        for (Map<String,String> curr : defs) {
            // Set of properties which we are deferring because they subst sibling properties:
            Map<String,Set<String>> dependOnSiblings = new HashMap<String,Set<String>>();
            for (Map.Entry<String,String> entry : curr.entrySet()) {
                String prop = entry.getKey();
                if (!m.containsKey(prop)) {
                    String rawval = entry.getValue();
                    //System.err.println("subst " + prop + "=" + rawval + " with " + m);
                    Union2<String,Set<String>> o = substitute(rawval, m, curr.keySet());
                    if (o.hasFirst()) {
                        m.put(prop, o.first());
                    } else {
                        dependOnSiblings.put(prop, o.second());
                    }
                }
            }
            Set<String> toSort = new HashSet<String>(dependOnSiblings.keySet());
            for (Set<String> s : dependOnSiblings.values()) {
                toSort.addAll(s);
            }
            List<String> sorted;
            try {
                sorted = Utilities.topologicalSort(toSort, dependOnSiblings);
            } catch (TopologicalSortException e) {
                //System.err.println("Cyclic property refs: " + Arrays.asList(e.unsortableSets()));
                return null;
            }
            Collections.reverse(sorted);
            for (String prop : sorted) {
                if (!m.containsKey(prop)) {
                    String rawval = curr.get(prop);
                    m.put(prop, substitute(rawval, m, /*Collections.EMPTY_SET*/curr.keySet()).first());
                }
            }
        }
        return m;
    }
    
    /**
     * Try to substitute property references etc. in an Ant property value string.
     * @param rawval the raw value to be substituted
     * @param predefs a set of properties already defined
     * @param siblingProperties a set of property names that are yet to be defined
     * @return either a String, in case everything can be evaluated now;
     *         or a Set<String> of elements from siblingProperties in case those properties
     *         need to be defined in order to evaluate this one
     */
    private static Union2<String,Set<String>> substitute(String rawval, Map<String,String> predefs, Set<String> siblingProperties) {
        assert rawval != null : "null rawval passed in";
        if (rawval.indexOf('$') == -1) {
            // Shortcut:
            //System.err.println("shortcut");
            return Union2.createFirst(rawval);
        }
        // May need to subst something.
        int idx = 0;
        // Result in progress, if it is to be a String:
        StringBuffer val = new StringBuffer();
        // Or, result in progress, if it is to be a Set<String>:
        Set<String> needed = new HashSet<String>();
        while (true) {
            int shell = rawval.indexOf('$', idx);
            if (shell == -1 || shell == rawval.length() - 1) {
                // No more $, or only as last char -> copy all.
                //System.err.println("no more $");
                if (needed.isEmpty()) {
                    val.append(rawval.substring(idx));
                    return Union2.createFirst(val.toString());
                } else {
                    return Union2.createSecond(needed);
                }
            }
            char c = rawval.charAt(shell + 1);
            if (c == '$') {
                // $$ -> $
                //System.err.println("$$");
                if (needed.isEmpty()) {
                    val.append('$');
                }
                idx += 2;
            } else if (c == '{') {
                // Possibly a property ref.
                int end = rawval.indexOf('}', shell + 2);
                if (end != -1) {
                    // Definitely a property ref.
                    String otherprop = rawval.substring(shell + 2, end);
                    //System.err.println("prop ref to " + otherprop);
                    if (predefs.containsKey(otherprop)) {
                        // Well-defined.
                        if (needed.isEmpty()) {
                            val.append(rawval.substring(idx, shell));
                            val.append(predefs.get(otherprop));
                        }
                        idx = end + 1;
                    } else if (siblingProperties.contains(otherprop)) {
                        needed.add(otherprop);
                        // don't bother updating val, it will not be used anyway
                        idx = end + 1;
                    } else {
                        // No def, leave as is.
                        if (needed.isEmpty()) {
                            val.append(rawval.substring(idx, end + 1));
                        }
                        idx = end + 1;
                    }
                } else {
                    // Unclosed ${ sequence, leave as is.
                    if (needed.isEmpty()) {
                        val.append(rawval.substring(idx));
                        return Union2.createFirst(val.toString());
                    } else {
                        return Union2.createSecond(needed);
                    }
                }
            } else {
                // $ followed by some other char, leave as is.
                // XXX is this actually right?
                if (needed.isEmpty()) {
                    val.append(rawval.substring(idx, idx + 2));
                }
                idx += 2;
            }
        }
    }
    
    private static final Pattern RELATIVE_SLASH_SEPARATED_PATH = Pattern.compile("[^:/\\\\.][^:/\\\\]*(/[^:/\\\\.][^:/\\\\]*)*"); // NOI18N
    
    /**
     * Find an absolute file path from a possibly relative path.
     * @param basedir base file for relative filename resolving; must be an absolute path
     * @param filename a pathname which may be relative or absolute and may
     *                 use / or \ as the path separator
     * @return an absolute file corresponding to it
     * @throws IllegalArgumentException if basedir is not absolute
     */
    public static File resolveFile(File basedir, String filename) throws IllegalArgumentException {
        if (basedir == null) {
            throw new NullPointerException("null basedir passed to resolveFile"); // NOI18N
        }
        if (filename == null) {
            throw new NullPointerException("null filename passed to resolveFile"); // NOI18N
        }
        if (!basedir.isAbsolute()) {
            throw new IllegalArgumentException("nonabsolute basedir passed to resolveFile: " + basedir); // NOI18N
        }
        File f;
        if (RELATIVE_SLASH_SEPARATED_PATH.matcher(filename).matches()) {
            // Shortcut - simple relative path. Potentially faster.
            f = new File(basedir, filename.replace('/', File.separatorChar));
        } else {
            // All other cases.
            String machinePath = filename.replace('/', File.separatorChar).replace('\\', File.separatorChar);
            f = new File(machinePath);
            if (!f.isAbsolute()) {
                f = new File(basedir, machinePath);
            }
            assert f.isAbsolute();
        }
        return FileUtil.normalizeFile(f);
    }
    
    /**
     * Produce a machine-independent relativized version of a filename from a basedir.
     * Unlike {@link URI#relativize} this will produce "../" sequences as needed.
     * @param basedir a directory to resolve relative to (need not exist on disk)
     * @param file a file or directory to find a relative path for
     * @return a relativized path (slash-separated), or null if it is not possible (e.g. different DOS drives);
     *         just <samp>.</samp> in case the paths are the same
     * @throws IllegalArgumentException if the basedir is known to be a file and not a directory
     */
    public static String relativizeFile(File basedir, File file) {
        if (basedir.isFile()) {
            throw new IllegalArgumentException("Cannot relative w.r.t. a data file " + basedir); // NOI18N
        }
        if (basedir.equals(file)) {
            return "."; // NOI18N
        }
        StringBuffer b = new StringBuffer();
        File base = basedir;
        String filepath = file.getAbsolutePath();
        while (!filepath.startsWith(slashify(base.getAbsolutePath()))) {
            base = base.getParentFile();
            if (base == null) {
                return null;
            }
            if (base.equals(file)) {
                // #61687: file is a parent of basedir
                b.append(".."); // NOI18N
                return b.toString();
            }
            b.append("../"); // NOI18N
        }
        URI u = base.toURI().relativize(file.toURI());
        assert !u.isAbsolute() : u + " from " + basedir + " and " + file + " with common root " + base;
        b.append(u.getPath());
        if (b.charAt(b.length() - 1) == '/') {
            // file is an existing directory and file.toURI ends in /
            // we do not want the trailing slash
            b.setLength(b.length() - 1);
        }
        return b.toString();
    }
    
    private static String slashify(String path) {
        if (path.endsWith(File.separator)) {
            return path;
        } else {
            return path + File.separatorChar;
        }
    }
    
    /*public? */ static FileObject resolveFileObject(FileObject basedir, String filename) {
        if (RELATIVE_SLASH_SEPARATED_PATH.matcher(filename).matches()) {
            // Shortcut. Potentially much faster.
            return basedir.getFileObject(filename);
        } else {
            // Might be an absolute path, or \-separated, or . or .. components, etc.; use the safer method.
            return FileUtil.toFileObject(resolveFile(FileUtil.toFile(basedir), filename));
        }
    }
    
    /*public? */ static String resolvePath(File basedir, String path) {
        StringBuffer b = new StringBuffer();
        String[] toks = tokenizePath(path);
        for (int i = 0; i < toks.length; i++) {
            if (i > 0) {
                b.append(File.pathSeparatorChar);
            }
            b.append(resolveFile(basedir, toks[i]).getAbsolutePath());
        }
        return b.toString();
    }
    
    /**
     * Split an Ant-style path specification into components.
     * Tokenizes on <code>:</code> and <code>;</code>, paying
     * attention to DOS-style components such as <samp>C:\FOO</samp>.
     * Also removes any empty components.
     * @param path an Ant-style path (elements arbitrary) using DOS or Unix separators
     * @return a tokenization of that path into components
     */
    public static String[] tokenizePath(String path) {
        List<String> l = new ArrayList<String>();
        StringTokenizer tok = new StringTokenizer(path, ":;", true); // NOI18N
        char dosHack = '\0';
        char lastDelim = '\0';
        int delimCount = 0;
        while (tok.hasMoreTokens()) {
            String s = tok.nextToken();
            if (s.length() == 0) {
                // Strip empty components.
                continue;
            }
            if (s.length() == 1) {
                char c = s.charAt(0);
                if (c == ':' || c == ';') {
                    // Just a delimiter.
                    lastDelim = c;
                    delimCount++;
                    continue;
                }
            }
            if (dosHack != '\0') {
                // #50679 - "C:/something" is also accepted as DOS path
                if (lastDelim == ':' && delimCount == 1 && (s.charAt(0) == '\\' || s.charAt(0) == '/')) {
                    // We had a single letter followed by ':' now followed by \something or /something
                    s = "" + dosHack + ':' + s;
                    // and use the new token with the drive prefix...
                } else {
                    // Something else, leave alone.
                    l.add(Character.toString(dosHack));
                    // and continue with this token too...
                }
                dosHack = '\0';
            }
            // Reset count of # of delimiters in a row.
            delimCount = 0;
            if (s.length() == 1) {
                char c = s.charAt(0);
                if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                    // Probably a DOS drive letter. Leave it with the next component.
                    dosHack = c;
                    continue;
                }
            }
            l.add(s);
        }
        if (dosHack != '\0') {
            //the dosHack was the last letter in the input string (not followed by the ':')
            //so obviously not a drive letter.
            //Fix for issue #57304
            l.add(Character.toString(dosHack));
        }
        return l.toArray(new String[l.size()]);
    }

    private static final Pattern VALID_PROPERTY_NAME = Pattern.compile("[-._a-zA-Z0-9]"); // NOI18N

    /**
     * Checks whether the name is usable as Ant property name.
     * @param name name to check for usability as Ant property
     * @return true if name is usable otherwise false
     */
    public static boolean isUsablePropertyName(String name) {
        return VALID_PROPERTY_NAME.matcher(name).matches();
    }

    /**
     * Returns name usable as Ant property which is based on the given
     * name. All forbidden characters are either removed or replaced with
     * suitable ones.
     * @param name name to use as base for Ant property name
     * @return name usable as Ant property name
     */
    public static String getUsablePropertyName(String name) {
        if (isUsablePropertyName(name)) {
            return name;
        }
        StringBuffer sb = new StringBuffer(name);
        for (int i=0; i<sb.length(); i++) {
            if (!isUsablePropertyName(sb.substring(i,i+1))) {
                sb.replace(i,i+1,"_");
            }
        }
        return sb.toString();
    }
    
    /**
     * Create a trivial property producer using only a fixed list of property definitions.
     * Its values are constant, and it never fires changes.
     * @param defs a map from property names to values (it is illegal to modify this map
     *             after passing it to this method)
     * @return a matching property producer
     */
    public static PropertyProvider fixedPropertyProvider(Map<String,String> defs) {
        return new FixedPropertyProvider(defs);
    }
    
    private static final class FixedPropertyProvider implements PropertyProvider {
        
        private final Map<String,String> defs;
        
        public FixedPropertyProvider(Map<String,String> defs) {
            this.defs = defs;
        }
        
        public Map<String,String> getProperties() {
            return defs;
        }
        
        public void addChangeListener(ChangeListener l) {}
        
        public void removeChangeListener(ChangeListener l) {}
        
    }
    
    /**
     * Create a property evaluator based on a series of definitions.
     * <p>
     * Each batch of definitions can refer to properties within itself
     * (so long as there is no cycle) or any previous batch.
     * However the special first provider cannot refer to properties within itself.
     * </p>
     * <p>
     * This implementation acquires {@link ProjectManager#mutex} for all operations, in read mode,
     * and fires changes synchronously. It also expects changes to be fired from property
     * providers in read (or write) access.
     * </p>
     * @param preprovider an initial context (may be null)
     * @param providers a sequential list of property groups
     * @return an evaluator
     */
    public static PropertyEvaluator sequentialPropertyEvaluator(PropertyProvider preprovider, PropertyProvider... providers) {
        return new SequentialPropertyEvaluator(preprovider, providers);
    }

    /**
     * Creates a property provider similar to {@link #globalPropertyProvider}
     * but which can use a different global properties file.
     * If a specific file is pointed to, that is loaded; otherwise behaves like {@link #globalPropertyProvider}.
     * Permits behavior similar to command-line Ant where not erroneous, but using the IDE's
     * default global properties for projects which do not yet have this property registered.
     * @param findUserPropertiesFile an evaluator in which to look up <code>propertyName</code>
     * @param propertyName a property pointing to the global properties file (typically <code>"user.properties.file"</code>)
     * @param basedir a base directory to use when resolving the path to the global properties file, if relative
     * @return a provider of global properties
     * @since org.netbeans.modules.ruby.modules.project.rake/1 1.14
     */
    public static PropertyProvider userPropertiesProvider(PropertyEvaluator findUserPropertiesFile, String propertyName, File basedir) {
        return new UserPropertiesProvider(findUserPropertiesFile, propertyName, basedir);
    }
    private static final class UserPropertiesProvider extends FilterPropertyProvider implements PropertyChangeListener {
        private final PropertyEvaluator findUserPropertiesFile;
        private final String propertyName;
        private final File basedir;
        public UserPropertiesProvider(PropertyEvaluator findUserPropertiesFile, String propertyName, File basedir) {
            super(computeDelegate(findUserPropertiesFile, propertyName, basedir));
            this.findUserPropertiesFile = findUserPropertiesFile;
            this.propertyName = propertyName;
            this.basedir = basedir;
            findUserPropertiesFile.addPropertyChangeListener(this);
        }
        public void propertyChange(PropertyChangeEvent ev) {
            if (propertyName.equals(ev.getPropertyName())) {
                setDelegate(computeDelegate(findUserPropertiesFile, propertyName, basedir));
            }
        }
        private static PropertyProvider computeDelegate(PropertyEvaluator findUserPropertiesFile, String propertyName, File basedir) {
            String userPropertiesFile = findUserPropertiesFile.getProperty(propertyName);
            if (userPropertiesFile != null) {
                // Have some defined global properties file, so read it and listen to changes in it.
                File f = PropertyUtils.resolveFile(basedir, userPropertiesFile);
                if (f.equals(PropertyUtils.userBuildProperties())) {
                    // Just to share the cache.
                    return PropertyUtils.globalPropertyProvider();
                } else {
                    return PropertyUtils.propertiesFilePropertyProvider(f);
                }
            } else {
                // Use the in-IDE default.
                return PropertyUtils.globalPropertyProvider();
            }
        }
    }
    
    private static final class SequentialPropertyEvaluator implements PropertyEvaluator, ChangeListener {
        
        private final PropertyProvider preprovider;
        private final PropertyProvider[] providers;
        private Map<String,String> defs;
        private final List<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();
        
        public SequentialPropertyEvaluator(final PropertyProvider preprovider, final PropertyProvider[] providers) {
            this.preprovider = preprovider;
            this.providers = providers;
            // XXX defer until someone asks for them
            defs = ProjectManager.mutex().readAccess(new Mutex.Action<Map<String,String>>() {
                public Map<String,String> run() {
                    return compose(preprovider, providers);
                }
            });
            // XXX defer until someone is listening?
            if (preprovider != null) {
                preprovider.addChangeListener(WeakListeners.change(this, preprovider));
            }
            for (PropertyProvider pp : providers) {
                pp.addChangeListener(WeakListeners.change(this, pp));
            }
        }
        
        public String getProperty(final String prop) {
            return ProjectManager.mutex().readAccess(new Mutex.Action<String>() {
                public String run() {
                    if (defs == null) {
                        return null;
                    }
                    return defs.get(prop);
                }
            });
        }
        
        public String evaluate(final String text) {
            if (text == null) {
                throw new NullPointerException("Attempted to pass null to PropertyEvaluator.evaluate"); // NOI18N
            }
            return ProjectManager.mutex().readAccess(new Mutex.Action<String>() {
                public String run() {
                    if (defs == null) {
                        return null;
                    }
                    Union2<String,Set<String>> result = substitute(text, defs, Collections.<String>emptySet());
                    assert result.hasFirst() : "Unexpected result " + result + " from " + text + " on " + defs;
                    return result.first();
                }
            });
        }
        
        public Map<String,String> getProperties() {
            return ProjectManager.mutex().readAccess(new Mutex.Action<Map<String,String>>() {
                public Map<String,String> run() {
                    return defs;
                }
            });
        }
        
        public void addPropertyChangeListener(PropertyChangeListener listener) {
            synchronized (listeners) {
                listeners.add(listener);
            }
        }
        
        public void removePropertyChangeListener(PropertyChangeListener listener) {
            synchronized (listeners) {
                listeners.remove(listener);
            }
        }
        
        public void stateChanged(ChangeEvent e) {
            assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
            Map<String,String> newdefs = compose(preprovider, providers);
            // compose() may return null upon circularity errors
            Map<String,String> _defs = defs != null ? defs : Collections.<String,String>emptyMap();
            Map<String,String> _newdefs = newdefs != null ? newdefs : Collections.<String,String>emptyMap();
            if (!_defs.equals(_newdefs)) {
                Set<String> props = new HashSet<String>(_defs.keySet());
                props.addAll(_newdefs.keySet());
                List<PropertyChangeEvent> events = new LinkedList<PropertyChangeEvent>();
                for (String prop : props) {
                    assert prop != null;
                    String oldval = _defs.get(prop);
                    String newval = _newdefs.get(prop);
                    if (newval != null) {
                        if (newval.equals(oldval)) {
                            continue;
                        }
                    } else {
                        assert oldval != null : "should not have had " + prop;
                    }
                    events.add(new PropertyChangeEvent(this, prop, oldval, newval));
                }
                assert !events.isEmpty();
                defs = newdefs;
                PropertyChangeListener[] _listeners;
                synchronized (listeners) {
                    _listeners = listeners.toArray(new PropertyChangeListener[listeners.size()]);
                }
                for (PropertyChangeListener l : _listeners) {
                    for (PropertyChangeEvent ev : events) {
                        l.propertyChange(ev);
                    }
                }
            }
        }
        
        private static Map<String,String> compose(PropertyProvider preprovider, PropertyProvider[] providers) {
            assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
            Map<String,String> predefs;
            if (preprovider != null) {
                predefs = preprovider.getProperties();
            } else {
                predefs = Collections.emptyMap();
            }
            List<Map<String,String>> defs = new ArrayList<Map<String,String>>(providers.length);
            for (PropertyProvider pp : providers) {
                defs.add(pp.getProperties());
            }
            return evaluateAll(predefs, defs);
        }

    }

    
}
