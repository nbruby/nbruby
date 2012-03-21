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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.ruby.api.project.rake.RakeArtifact;
import org.netbeans.modules.ruby.modules.project.rake.RakeBasedProjectFactorySingleton;
import org.netbeans.modules.ruby.modules.project.rake.UserQuestionHandler;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.CacheDirectoryProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.queries.SharabilityQueryImplementation;
import org.openide.ErrorManager;
import org.openide.filesystems.FileAttributeEvent;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileRenameEvent;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.EditableProperties;
import org.openide.util.Mutex;
import org.openide.util.MutexException;
import org.openide.util.RequestProcessor;
import org.openide.util.UserQuestionException;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Support class for implementing Ant-based projects.
 * @author Jesse Glick
 */
public final class RakeProjectHelper {
    
    /**
     * Relative path from project directory to the customary shared properties file.
     */
    public static final String PROJECT_PROPERTIES_PATH = "nbproject/project.properties"; // NOI18N
    
    /**
     * Relative path from project directory to the customary private properties file.
     */
    public static final String PRIVATE_PROPERTIES_PATH = "nbproject/private/private.properties"; // NOI18N
    
    /**
     * Relative path from project directory to the required shared project metadata file.
     */
    public static final String PROJECT_XML_PATH = RakeBasedProjectFactorySingleton.PROJECT_XML_PATH;
    
    /**
     * Relative path from project directory to the required private project metadata file.
     */
    public static final String PRIVATE_XML_PATH = "nbproject/private/private.xml"; // NOI18N
    
    /**
     * XML namespace of Ant projects.
     */
    static final String PROJECT_NS = RakeBasedProjectFactorySingleton.PROJECT_NS;
    
    /**
     * XML namespace of private component of Ant projects.
     */
    static final String PRIVATE_NS = "http://www.netbeans.org/ns/project-private/1"; // NOI18N
    
    static {
        RakeBasedProjectFactorySingleton.HELPER_CALLBACK = new RakeBasedProjectFactorySingleton.RakeProjectHelperCallback() {
            public RakeProjectHelper createHelper(FileObject dir, Document projectXml, ProjectState state, RakeBasedProjectType type) {
                return new RakeProjectHelper(dir, projectXml, state, type);
            }
            public void save(RakeProjectHelper helper) throws IOException {
                helper.save();
            }
        };
    }
    
    private static final RequestProcessor RP = new RequestProcessor("RakeProjectHelper.RP"); // NOI18N
    
    /**
     * Project base directory.
     */
    private final FileObject dir;
    
    /**
     * State object permitting modifications.
     */
    private final ProjectState state;
    
    /**
     * Ant-based project type factory.
     */
    private final RakeBasedProjectType type;
    
    /**
     * Cached project.xml parse (null if not loaded).
     * Access within {@link #modifiedMetadataPaths} monitor.
     */
    private Document projectXml;
    
    /**
     * Cached private.xml parse (null if not loaded).
     * Access within {@link #modifiedMetadataPaths} monitor.
     */
    private Document privateXml;
    
    /**
     * Set of relative paths to metadata files which have been modified
     * and which need to be saved.
     * Also server as a monitor for {@link #projectXml} and {@link #privateXml} accesses;
     * Xerces' DOM is not thread-safe <em>even for reading<em> (#50198).
     */
    private final Set<String> modifiedMetadataPaths = new HashSet<String>();
    
    /**
     * Registered listeners.
     * Access must be directly synchronized.
     */
    private final List<RakeProjectListener> listeners = new ArrayList<RakeProjectListener>();
    
    /**
     * List of loaded properties.
     */
    private final ProjectProperties properties;
    
    /** Listener to XML files; needs to be held as an instance field so it is not GC'd */
    private final FileChangeListener fileListener;
    
    /** True if currently saving XML files. */
    private boolean writingXML = false;
    
    /**
     * Hook waiting to be called. See issue #57794.
     */
    private ProjectXmlSavedHook pendingHook;
    /**
     * Number of metadata files remaining to be written before {@link #pendingHook} can be called.
     * Javadoc for {@link ProjectXmlSavedHook} only guarantees that project.xml will be written,
     * but best to be safe and make sure also private.xml and *.properties are too.
     */
    private int pendingHookCount;
    
    // XXX lock any loaded XML files while the project is modified, to prevent manual editing,
    // and reload any modified files if the project is unmodified
    
    private RakeProjectHelper(FileObject dir, Document projectXml, ProjectState state, RakeBasedProjectType type) {
        this.dir = dir;
        assert dir != null && FileUtil.toFile(dir) != null;
        this.state = state;
        assert state != null;
        this.type = type;
        assert type != null;
        this.projectXml = projectXml;
        assert projectXml != null;
        properties = new ProjectProperties(this);
        fileListener = new FileListener();
        FileUtil.addFileChangeListener(fileListener, resolveFile(PROJECT_XML_PATH));
        FileUtil.addFileChangeListener(fileListener, resolveFile(PRIVATE_XML_PATH));
    }
    
    /**
     * Get the corresponding Ant-based project type factory.
     */
    RakeBasedProjectType getType() {
        return type;
    }

    /**
     * Retrieve project.xml or private.xml, loading from disk as needed.
     * private.xml is created as a skeleton on demand.
     */
    private Document getConfigurationXml(boolean shared) {
        assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
        assert Thread.holdsLock(modifiedMetadataPaths);
        Document xml = shared ? projectXml : privateXml;
        if (xml == null) {
            String path = shared ? PROJECT_XML_PATH : PRIVATE_XML_PATH;
            xml = loadXml(path);
            if (xml == null) {
                // Missing or broken; create a skeleton.
                String element = shared ? "project" : "project-private"; // NOI18N
                String ns = shared ? PROJECT_NS : PRIVATE_NS;
                xml = XMLUtil.createDocument(element, ns, null, null);
                if (shared) {
                    // #46048: need to generate minimal compliant XML skeleton.
                    Element typeEl = xml.createElementNS(PROJECT_NS, "type"); // NOI18N
                    typeEl.appendChild(xml.createTextNode(getType().getType()));
                    xml.getDocumentElement().appendChild(typeEl);
                    xml.getDocumentElement().appendChild(xml.createElementNS(PROJECT_NS, "configuration")); // NOI18N
                }
            }
            if (shared) {
                projectXml = xml;
            } else {
                privateXml = xml;
            }
        }
        assert xml != null;
        return xml;
    }
    
    /**
     * If true, do not report XML load errors.
     * For use only by unit tests.
     */
    static boolean QUIETLY_SWALLOW_XML_LOAD_ERRORS = false;
    
    /**
     * Try to load a config XML file from a named path.
     * If the file does not exist, or there is any load error, return null.
     */
    private Document loadXml(String path) {
        assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
        assert Thread.holdsLock(modifiedMetadataPaths);
        FileObject xml = dir.getFileObject(path);
        if (xml == null || !xml.isData()) {
            return null;
        }
        File f = FileUtil.toFile(xml);
        assert f != null;
        try {
            return XMLUtil.parse(new InputSource(f.toURI().toString()), false, true, XMLUtil.defaultErrorHandler(), null);
        } catch (IOException e) {
            if (!QUIETLY_SWALLOW_XML_LOAD_ERRORS) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        } catch (SAXException e) {
            if (!QUIETLY_SWALLOW_XML_LOAD_ERRORS) {
                ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
            }
        }
        return null;
    }
    
    /**
     * Save an XML config file to a named path.
     * If the file does not yet exist, it is created.
     */
    private FileLock saveXml(final Document doc, final String path) throws IOException {
        assert ProjectManager.mutex().isWriteAccess();
        assert !writingXML;
        assert Thread.holdsLock(modifiedMetadataPaths);
        final FileLock[] _lock = new FileLock[1];
        writingXML = true;
        try {
            dir.getFileSystem().runAtomicAction(new FileSystem.AtomicAction() {
                public void run() throws IOException {
                    // Keep a copy of xml *while holding modifiedMetadataPaths monitor*.
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    XMLUtil.write(doc, baos, "UTF-8"); // NOI18N
                    final byte[] data = baos.toByteArray();
                    final FileObject xml = FileUtil.createData(dir, path);
                    try {
                        _lock[0] = xml.lock(); // unlocked by {@link #save}
                        OutputStream os = xml.getOutputStream(_lock[0]);
                        try {
                            os.write(data);
                        } finally {
                            os.close();
                        }
                    } catch (UserQuestionException uqe) { // #46089
                        needPendingHook();
                        UserQuestionHandler.handle(uqe, new UserQuestionHandler.Callback() {
                            public void accepted() {
                                // Try again.
                                assert !writingXML;
                                writingXML = true;
                                try {
                                    FileLock lock = xml.lock();
                                    try {
                                        OutputStream os = xml.getOutputStream(lock);
                                        try {
                                            os.write(data);
                                        } finally {
                                            os.close();
                                        }
                                    } finally {
                                        lock.releaseLock();
                                    }
                                    maybeCallPendingHook();
                                } catch (IOException e) {
                                    // Oh well.
                                    ErrorManager.getDefault().notify(e);
                                    reload();
                                } finally {
                                    writingXML = false;
                                }
                            }
                            public void denied() {
                                reload();
                            }
                            public void error(IOException e) {
                                ErrorManager.getDefault().notify(e);
                                reload();
                            }
                            private void reload() {
                                // Revert the save.
                                if (path.equals(PROJECT_XML_PATH)) {
                                    synchronized (modifiedMetadataPaths) {
                                        projectXml = null;
                                    }
                                } else {
                                    assert path.equals(PRIVATE_XML_PATH) : path;
                                    synchronized (modifiedMetadataPaths) {
                                        privateXml = null;
                                    }
                                }
                                fireExternalChange(path);
                                cancelPendingHook();
                            }
                        });
                    }
                }
            });
        } finally {
            writingXML = false;
        }
        return _lock[0];
    }
    
    /**
     * Get the <code>&lt;configuration&gt;</code> element of project.xml
     * or the document element of private.xml.
     * Beneath this point you can load and store configuration fragments.
     * @param shared if true, use project.xml, else private.xml
     * @return the data root
     */
    private Element getConfigurationDataRoot(boolean shared) {
        assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
        assert Thread.holdsLock(modifiedMetadataPaths);
        Document doc = getConfigurationXml(shared);
        if (shared) {
            Element project = doc.getDocumentElement();
            Element config = XMLUtil.findElement(project, "configuration", PROJECT_NS); // NOI18N
            assert config != null;
            return config;
        } else {
            return doc.getDocumentElement();
        }
    }

    /**
     * Add a listener to changes in the project configuration.
     * <p>Thread-safe.
     * @param listener a listener to add
     */
    public void addRakeProjectListener(RakeProjectListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    /**
     * Remove a listener to changes in the project configuration.
     * <p>Thread-safe.
     * @param listener a listener to remove
     */
    public void removeRakeProjectListener(RakeProjectListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    /**
     * Fire a change of external provenance to all listeners.
     * Acquires write access.
     * @param path path to the changed file (XML or properties)
     */
    void fireExternalChange(final String path) {
        final Mutex.Action<Void> action = new Mutex.Action<Void>() {
            public Void run() {
                fireChange(path, false);
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

    /**
     * Fire a change to all listeners.
     * Must be called from write access; enters read access while firing.
     * @param path path to the changed file (XML or properties)
     * @param expected true if the result of an API-initiated change, false if from external causes
     */
    private void fireChange(String path, boolean expected) {
        assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
        final RakeProjectListener[] _listeners;
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            _listeners = listeners.toArray(new RakeProjectListener[listeners.size()]);
        }
        final RakeProjectEvent ev = new RakeProjectEvent(this, path, expected);
        final boolean xml = path.equals(PROJECT_XML_PATH) || path.equals(PRIVATE_XML_PATH);
        ProjectManager.mutex().readAccess(new Mutex.Action<Void>() {
            public Void run() {
                for (int i = 0; i < _listeners.length; i++) {
                    try {
                        if (xml) {
                            _listeners[i].configurationXmlChanged(ev);
                        } else {
                            _listeners[i].propertiesChanged(ev);
                        }
                    } catch (RuntimeException e) {
                        // Don't prevent other listeners from being notified.
                        ErrorManager.getDefault().notify(e);
                    }
                }
                return null;
            }
        });
    }
    
    /**
     * Call when explicitly modifying some piece of metadata.
     */
    private void modifying(String path) {
        assert ProjectManager.mutex().isWriteAccess();
        state.markModified();
        synchronized (modifiedMetadataPaths) {
            modifiedMetadataPaths.add(path);
        }
        fireChange(path, true);
    }
    
    /**
     * Get the top-level project directory.
     * @return the project directory beneath which everything in the project lies
     */
    public FileObject getProjectDirectory() {
        return dir;
    }
    
    /**Notification that this project has been deleted.
     * @see org.netbeans.spi.project.ProjectState#notifyDeleted
     *
     * @since 1.8
     */
    public void notifyDeleted() {
        state.notifyDeleted();
    }
    
    
    /**
     * Mark this project as being modified without actually changing anything in it.
     * Should only be called from {@link ProjectGenerator#createProject}.
     */
    void markModified() {
        assert ProjectManager.mutex().isWriteAccess();
        state.markModified();
        // To make sure projectXmlSaved is called:
        synchronized (modifiedMetadataPaths) {
            modifiedMetadataPaths.add(PROJECT_XML_PATH);
        }
    }
    
    /**
     * Check whether this project is currently modified including modifications
     * to <code>project.xml</code>.
     * Access from GeneratedFilesHelper.
     */
    boolean isProjectXmlModified() {
        assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
        return modifiedMetadataPaths.contains(PROJECT_XML_PATH);
    }
    
    /**
     * Save all cached project metadata.
     * If <code>project.xml</code> was one of the modified files, then
     * {@link RakeBasedProjectType#projectXmlSaved} is called, presumably
     * creating <code>build-impl.xml</code> and/or <code>build.xml</code>.
     */
    private void save() throws IOException {
        assert ProjectManager.mutex().isWriteAccess();
        Set<FileLock> locks = new HashSet<FileLock>();
        try {
            synchronized (modifiedMetadataPaths) {
                assert !modifiedMetadataPaths.isEmpty();
                assert pendingHook == null;
                if (modifiedMetadataPaths.contains(PROJECT_XML_PATH)) {
                    // Saving project.xml so look for that hook.
                    Project p = RakeBasedProjectFactorySingleton.getProjectFor(this);
                    pendingHook = p.getLookup().lookup(ProjectXmlSavedHook.class);
                    // might still be null
                }
                Iterator it = modifiedMetadataPaths.iterator();
                while (it.hasNext()) {
                    String path = (String)it.next();
                    if (path.equals(PROJECT_XML_PATH)) {
                        assert projectXml != null;
                        locks.add(saveXml(projectXml, path));
                    } else if (path.equals(PRIVATE_XML_PATH)) {
                        assert privateXml != null;
                        locks.add(saveXml(privateXml, path));
                    } else {
                        // XXX Rake projects should probably store everything in the XML file?
                        // All else is assumed to be a properties file.
                        locks.add(properties.write(path));
                    }
                    // As metadata files are saved, take them off the modified list.
                    it.remove();
                }
                if (pendingHook != null && pendingHookCount == 0) {
                    try {
                        pendingHook.projectXmlSaved();
                    } catch (IOException e) {
                        // Treat it as still modified.
                        modifiedMetadataPaths.add(PROJECT_XML_PATH);
                        throw e;
                    }
                }
            }
        } finally {
            // #57791: release locks outside synchronized block.
            locks.remove(null);
            for (FileLock lock : locks) {
                lock.releaseLock();
            }
            // More #57794.
            if (pendingHookCount == 0) {
                pendingHook = null;
            }
        }
    }
    
    /** See issue #57794. */
    void maybeCallPendingHook() {
        // XXX synchronization of this method?
        assert pendingHookCount > 0;
        pendingHookCount--;
        //#67465: the pendingHook may be null if project.xml is not being written
        //eg. only project.properties is being saved:
        if (pendingHookCount == 0 && pendingHook != null) {
            try {
                ProjectManager.mutex().writeAccess(new Mutex.ExceptionAction<Void>() {
                    public Void run() throws IOException {
                        pendingHook.projectXmlSaved();
                        return null;
                    }
                });
            } catch (MutexException e) {
                // XXX mark project modified again??
                ErrorManager.getDefault().notify(e);
            } finally {
                pendingHook = null;
            }
        }
    }
    void cancelPendingHook() {
        assert pendingHookCount > 0;
        pendingHookCount--;
        if (pendingHookCount == 0) {
            pendingHook = null;
        }
    }
    void needPendingHook() {
        pendingHookCount++;
    }
    
    /**
     * Load a property file from some location in the project.
     * The returned object may be edited but you must call {@link #putProperties}
     * to save any changes you make.
     * If the file does not (yet) exist or could not be loaded for whatever reason,
     * an empty properties list is returned instead.
     * @param path a relative URI in the project directory, e.g.
     *             {@link #PROJECT_PROPERTIES_PATH} or {@link #PRIVATE_PROPERTIES_PATH}
     * @return a set of properties
     */
    public EditableProperties getProperties(final String path) {
        if (path.equals(RakeProjectHelper.PROJECT_XML_PATH) || path.equals(RakeProjectHelper.PRIVATE_XML_PATH)) {
            throw new IllegalArgumentException("Attempt to load properties from a project XML file"); // NOI18N
        }
        return ProjectManager.mutex().readAccess(new Mutex.Action<EditableProperties>() {
            public EditableProperties run() {
                return properties.getProperties(path);
            }
        });
    }
    
    /**
     * Store a property file to some location in the project.
     * A clone will be made of the supplied properties file so as to snapshot it.
     * The new properties are not actually stored to disk immediately, but the project
     * is marked modified so that they will be later.
     * You can store to a path that does not yet exist and the file will be created
     * if and when the project is saved.
     * If the old value is the same as the new, nothing is done.
     * Otherwise an expected properties change event is fired.
     * <p>Acquires write access from {@link ProjectManager#mutex}. However, you are well
     * advised to explicitly enclose a <em>complete</em> operation within write access,
     * starting with {@link #getProperties}, to prevent race conditions.
     * @param path a relative URI in the project directory, e.g.
     *             {@link #PROJECT_PROPERTIES_PATH} or {@link #PRIVATE_PROPERTIES_PATH}
     * @param props a set of properties to store, or null to delete any existing properties file there
     */
    public void putProperties(final String path, final EditableProperties props) {
        if (path.equals(RakeProjectHelper.PROJECT_XML_PATH) || path.equals(RakeProjectHelper.PRIVATE_XML_PATH)) {
            throw new IllegalArgumentException("Attempt to store properties from a project XML file"); // NOI18N
        }
        ProjectManager.mutex().writeAccess(new Mutex.Action<Void>() {
            public Void run() {
                if (properties.putProperties(path, props)) {
                    modifying(path);
                }
                return null;
            }
        });
    }
    
    /**
     * Get a property provider that works with loadable project properties.
     * Its current values should match {@link #getProperties}, and calls to
     * {@link #putProperties} should cause it to fire changes.
     * @param path a relative URI in the project directory, e.g.
     *             {@link #PROJECT_PROPERTIES_PATH} or {@link #PRIVATE_PROPERTIES_PATH}
     * @return a property provider implementation
     */
    public PropertyProvider getPropertyProvider(final String path) {
        if (path.equals(RakeProjectHelper.PROJECT_XML_PATH) || path.equals(RakeProjectHelper.PRIVATE_XML_PATH)) {
            throw new IllegalArgumentException("Attempt to store properties from a project XML file"); // NOI18N
        }
        return ProjectManager.mutex().readAccess(new Mutex.Action<PropertyProvider>() {
            public PropertyProvider run() {
                return properties.getPropertyProvider(path);
            }
        });
    }
    
    /**
     * Get the primary configuration data for this project.
     * The returned element will be named according to
     * {@link RakeBasedProjectType#getPrimaryConfigurationDataElementName} and
     * {@link RakeBasedProjectType#getPrimaryConfigurationDataElementNamespace}.
     * The project may read this document fragment to get custom information
     * from <code>nbproject/project.xml</code> and <code>nbproject/private/private.xml</code>.
     * The fragment will have no parent node and while it may be modified, you must
     * use {@link #putPrimaryConfigurationData} to store any changes.
     * @param shared if true, refers to <code>project.xml</code>, else refers to
     *               <code>private.xml</code>
     * @return the configuration data that is available
     */
    public Element getPrimaryConfigurationData(final boolean shared) {
        final String name = type.getPrimaryConfigurationDataElementName(shared);
        assert name.indexOf(':') == -1;
        final String namespace = type.getPrimaryConfigurationDataElementNamespace(shared);
        assert namespace != null && namespace.length() > 0;
        return ProjectManager.mutex().readAccess(new Mutex.Action<Element>() {
            public Element run() {
                synchronized (modifiedMetadataPaths) {
                    Element el = getConfigurationFragment(name, namespace, shared);
                    if (el != null) {
                        return el;
                    } else {
                        // No such data, corrupt file.
                        return cloneSafely(getConfigurationXml(shared).createElementNS(namespace, name));
                    }
                }
            }
        });
    }
    
    /**
     * Store the primary configuration data for this project.
     * The supplied element must be named according to
     * {@link RakeBasedProjectType#getPrimaryConfigurationDataElementName} and
     * {@link RakeBasedProjectType#getPrimaryConfigurationDataElementNamespace}.
     * The project may save this document fragment to set custom information
     * in <code>nbproject/project.xml</code> and <code>nbproject/private/private.xml</code>.
     * The fragment will be cloned and so further modifications will have no effect.
     * <p>Acquires write access from {@link ProjectManager#mutex}. However, you are well
     * advised to explicitly enclose a <em>complete</em> operation within write access,
     * starting with {@link #getPrimaryConfigurationData}, to prevent race conditions.
     * @param data the desired new configuration data
     * @param shared if true, refers to <code>project.xml</code>, else refers to
     *               <code>private.xml</code>
     * @throws IllegalArgumentException if the element is not correctly named
     */
    public void putPrimaryConfigurationData(Element data, boolean shared) throws IllegalArgumentException {
        String name = type.getPrimaryConfigurationDataElementName(shared);
        assert name.indexOf(':') == -1;
        String namespace = type.getPrimaryConfigurationDataElementNamespace(shared);
        assert namespace != null && namespace.length() > 0;
        if (!name.equals(data.getLocalName()) || !namespace.equals(data.getNamespaceURI())) {
            throw new IllegalArgumentException("Wrong name/namespace: expected {" + namespace + "}" + name + " but was {" + data.getNamespaceURI() + "}" + data.getLocalName()); // NOI18N
        }
        putConfigurationFragment(data, shared);
    }

    private final class FileListener implements FileChangeListener {
        
        public FileListener() {}
        
        private void change(FileEvent fe) {
            if (writingXML) {
                return;
            }
            String path;
            File f = FileUtil.toFile(fe.getFile());
            synchronized (modifiedMetadataPaths) {
                if (f.equals(resolveFile(PROJECT_XML_PATH))) {
                    if (modifiedMetadataPaths.contains(PROJECT_XML_PATH)) {
                        //#68872: don't do anything if the given file has non-saved changes:
                        return ;
                    }
                    path = PROJECT_XML_PATH;
                    projectXml = null;
                } else if (f.equals(resolveFile(PRIVATE_XML_PATH))) {
                    if (modifiedMetadataPaths.contains(PRIVATE_XML_PATH)) {
                        //#68872: don't do anything if the given file has non-saved changes:
                        return ;
                    }
                    path = PRIVATE_XML_PATH;
                    privateXml = null;
                } else {
                    throw new AssertionError("Unexpected file change in " + f); // NOI18N
                }
            }
            fireExternalChange(path);
        }
        
        public void fileFolderCreated(FileEvent fe) {
            change(fe);
        }

        public void fileDataCreated(FileEvent fe) {
            change(fe);
        }

        public void fileChanged(FileEvent fe) {
            change(fe);
        }

        public void fileDeleted(FileEvent fe) {
            change(fe);
        }

        public void fileRenamed(FileRenameEvent fe) {
            change(fe);
        }

        public void fileAttributeChanged(FileAttributeEvent fe) {
        }
        
    }
    
    /**
     * Get a piece of the configuration subtree by name.
     * @param elementName the simple XML element name expected
     * @param namespace the XML namespace expected
     * @param shared to use project.xml vs. private.xml
     * @return (a clone of) the named configuration fragment, or null if it does not exist
     */
    Element getConfigurationFragment(final String elementName, final String namespace, final boolean shared) {
        return ProjectManager.mutex().readAccess(new Mutex.Action<Element>() {
            public Element run() {
                synchronized (modifiedMetadataPaths) {
                    Element root = getConfigurationDataRoot(shared);
                    Element data = XMLUtil.findElement(root, elementName, namespace);
                    if (data != null) {
                        return cloneSafely(data);
                    } else {
                        return null;
                    }
                }
            }
        });
    }
    
    private static final DocumentBuilder db;
    static {
        try {
            db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new AssertionError(e);
        }
    }
    private static Element cloneSafely(Element el) {
        // #50198: for thread safety, use a separate document.
        // Using XMLUtil.createDocument is much too slow.
        synchronized (db) {
            Document dummy = db.newDocument();
            return (Element) dummy.importNode(el, true);
        }
    }
    
    /**
     * Store a piece of the configuration subtree by name.
     * @param fragment a piece of the subtree to store (overwrite or add)
     * @param shared to use project.xml vs. private.xml
     */
    void putConfigurationFragment(final Element fragment, final boolean shared) {
        ProjectManager.mutex().writeAccess(new Mutex.Action<Void>() {
            public Void run() {
                synchronized (modifiedMetadataPaths) {
                    Element root = getConfigurationDataRoot(shared);
                    Element existing = XMLUtil.findElement(root, fragment.getLocalName(), fragment.getNamespaceURI());
                    // XXX first compare to existing and return if the same
                    if (existing != null) {
                        root.removeChild(existing);
                    }
                    // the children are alphabetize: find correct place to insert new node
                    Node ref = null;
                    NodeList list = root.getChildNodes();
                    for (int i=0; i<list.getLength(); i++) {
                        Node node  = list.item(i);
                        if (node.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        int comparison = node.getNodeName().compareTo(fragment.getNodeName());
                        if (comparison == 0) {
                            comparison = node.getNamespaceURI().compareTo(fragment.getNamespaceURI());
                        }
                        if (comparison > 0) {
                            ref = node;
                            break;
                        }
                    }
                    root.insertBefore(root.getOwnerDocument().importNode(fragment, true), ref);
                    modifying(shared ? PROJECT_XML_PATH : PRIVATE_XML_PATH);
                }
                return null;
            }
        });
    }
    
    /**
     * Remove a piece of the configuration subtree by name.
     * @param elementName the simple XML element name expected
     * @param namespace the XML namespace expected
     * @param shared to use project.xml vs. private.xml
     * @return true if anything was actually removed
     */
    boolean removeConfigurationFragment(final String elementName, final String namespace, final boolean shared) {
        return ProjectManager.mutex().writeAccess(new Mutex.Action<Boolean>() {
            public Boolean run() {
                synchronized (modifiedMetadataPaths) {
                    Element root = getConfigurationDataRoot(shared);
                    Element data = XMLUtil.findElement(root, elementName, namespace);
                    if (data != null) {
                        root.removeChild(data);
                        modifying(shared ? PROJECT_XML_PATH : PRIVATE_XML_PATH);
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        });
    }
    
    /**
     * Create an object permitting this project to store auxiliary configuration.
     * Would be placed into the project's lookup.
     * @return an auxiliary configuration provider object suitable for the project lookup
     */
    public AuxiliaryConfiguration createAuxiliaryConfiguration() {
        return new ExtensibleMetadataProviderImpl(this);
    }

    /**
     * Create an object permitting this project to expose {@link AuxiliaryProperties}.
     * Would be placed into the project's lookup.
     *
     * This implementation places the properties into {@link #PROJECT_PROPERTIES_PATH}
     * or {@link #PRIVATE_PROPERTIES_PATH} (depending on shared value). The properties are
     * prefixed with "<code>auxiliary.</code>".
     *
     * @return an instance of {@link AuxiliaryProperties} suitable for the project lookup
     */
    public AuxiliaryProperties createAuxiliaryProperties() {
        return new AuxiliaryPropertiesImpl(this);
    }

    /**
     * Create an object permitting this project to expose a cache directory.
     * Would be placed into the project's lookup.
     * @return a cache directory provider object suitable for the project lookup
     */
    public CacheDirectoryProvider createCacheDirectoryProvider() {
        return new ExtensibleMetadataProviderImpl(this);
    }
    
    /**
     * Create a basic implementation of {@link RakeArtifact} which assumes everything of interest
     * is in a fixed location under a standard Ant-based project.
     * @param type the type of artifact, e.g. <a href="@org-netbeans-modules-java-project@/org/netbeans/modules/gsfpath/api/project/JavaProjectConstants.html#ARTIFACT_TYPE_JAR"><code>JavaProjectConstants.ARTIFACT_TYPE_JAR</code></a>
     * @param locationProperty an Ant property name giving the project-relative
     *                         location of the artifact, e.g. <samp>dist.jar</samp>
     * @param eval a way to evaluate the location property (e.g. {@link #getStandardPropertyEvaluator})
     * @param targetName the name of an Ant target which will build the artifact,
     *                   e.g. <samp>jar</samp>
     * @param cleanTargetName the name of an Ant target which will delete the artifact
     *                        (and maybe other build products), e.g. <samp>clean</samp>
     * @return an artifact
     */
    public RakeArtifact createSimpleRakeArtifact(String type, String locationProperty, PropertyEvaluator eval, String targetName, String cleanTargetName) {
        return new SimpleRakeArtifact(this, type, locationProperty, eval, targetName, cleanTargetName);
    }
    
    /**
     * Create an implementation of the file sharability query.
     * You may specify a list of source roots to include that should be considered sharable,
     * as well as a list of build directories that should not be considered sharable.
     * <p>
     * The project directory itself is automatically included in the list of sharable directories
     * so you need not explicitly specify it.
     * Similarly, the <code>nbproject/private</code> subdirectory is automatically excluded
     * from VCS, so you do not need to explicitly specify it.
     * </p>
     * <p>
     * Any file (or directory) mentioned (explicitly or implicity) in the source
     * directory list but not in any of the build directory lists, and not containing
     * any build directories inside it, will be given as sharable. If a directory itself
     * is sharable but some directory inside it is not, it will be given as mixed.
     * A file or directory inside some build directory will be listed as not sharable.
     * A file or directory matching neither the source list nor the build directory list
     * will be treated as of unknown status, but in practice such a file should never
     * have been passed to this implementation anyway - {@link org.netbeans.api.queries.SharabilityQuery} will
     * normally only call an implementation in project lookup if the file is owned by
     * that project.
     * </p>
     * <p>
     * Each entry in either list should be a string evaluated first for Ant property
     * escapes (if any), then treated as a file path relative to the project directory
     * (or it may be absolute).
     * </p>
     * <p>
     * It is permitted, and harmless, to include items that overlap others. For example,
     * you can have both a directory and one of its children in the include list.
     * </p>
     * <div class="nonnormative">
     * <p>
     * Typical usage would be:
     * </p>
     * <pre>
     * helper.createSharabilityQuery(helper.getStandardPropertyEvaluator(),
     *                               new String[] {"${src.dir}", "${test.src.dir}"},
     *                               new String[] {"${build.dir}", "${dist.dir}"})
     * </pre>
     * <p>
     * A quick rule of thumb is that the include list should contain any
     * source directories which <em>might</em> reside outside the project directory;
     * and the exclude list should contain any directories which you would want
     * to add to a <samp>.cvsignore</samp> file if using CVS (for example).
     * </p>
     * <p>
     * Note that in this case <samp>${src.dir}</samp> and <samp>${test.src.dir}</samp>
     * may be relative paths inside the project directory; relative paths pointing
     * outside of the project directory; or absolute paths (generally outside of the
     * project directory). If they refer to locations inside the project directory,
     * including them does nothing but is harmless - since the project directory itself
     * is always treated as sharable. If they refer to external locations, you will
     * need to also make sure that {@link org.netbeans.api.queries.FileOwnerQuery} actually maps files in those
     * directories to this project, or else {@link org.netbeans.api.queries.SharabilityQuery} will never find
     * this implementation in your project lookup and may return <code>UNKNOWN</code>.
     * </p>
     * </div>
     * @param eval a property evaluator to interpret paths with
     * @param sourceRoots a list of additional paths to treat as sharable
     * @param buildDirectories a list of paths to treat as not sharable
     * @return a sharability query implementation suitable for the project lookup
     * @see Project#getLookup
     */
    public SharabilityQueryImplementation createSharabilityQuery(PropertyEvaluator eval, String[] sourceRoots, String[] buildDirectories) {
        String[] includes = new String[sourceRoots.length + 1];
        System.arraycopy(sourceRoots, 0, includes, 0, sourceRoots.length);
        includes[sourceRoots.length] = ""; // NOI18N
        String[] excludes = new String[buildDirectories.length + 1];
        System.arraycopy(buildDirectories, 0, excludes, 0, buildDirectories.length);
        excludes[buildDirectories.length] = "nbproject/private"; // NOI18N
        return new SharabilityQueryImpl(this, eval, includes, excludes);
    }
    
    /**
     * Get a property provider which defines <code>basedir</code> according to
     * the project directory and also copies all system properties in the current VM.
     * It may also define <code>ant.home</code> if it is able.
     * @return a stock property provider for initial Ant-related definitions
     * @see PropertyUtils#sequentialPropertyEvaluator
     */
    public PropertyProvider getStockPropertyPreprovider() {
        return properties.getStockPropertyPreprovider();
    }
    
    /**
     * Get a property evaluator that can evaluate properties according to the default
     * file layout for Ant-based projects.
     * First, {@link #getStockPropertyPreprovider stock properties} are predefined.
     * Then {@link #PRIVATE_PROPERTIES_PATH} is loaded via {@link #getPropertyProvider},
     * then global definitions from {@link PropertyUtils#globalPropertyProvider}
     * (though these may be overridden using the property <code>user.properties.file</code>
     * in <code>private.properties</code>), then {@link #PROJECT_PROPERTIES_PATH}.
     * @return a standard property evaluator
     */
    public PropertyEvaluator getStandardPropertyEvaluator() {
        return properties.getStandardPropertyEvaluator();
    }
    
    /**
     * Find an absolute file path from a possibly project-relative path.
     * @param filename a pathname which may be project-relative or absolute and may
     *                 use / or \ as the path separator
     * @return an absolute file corresponding to it
     */
    public File resolveFile(String filename) {
        if (filename == null) {
            throw new NullPointerException("Attempted to pass a null filename to resolveFile"); // NOI18N
        }
        return PropertyUtils.resolveFile(FileUtil.toFile(dir), filename);
    }
    
    /**
     * Same as {@link #resolveFile}, but produce a <code>FileObject</code> if possible.
     * @param filename a pathname according to Ant conventions
     * @return a file object it represents, or null if there is no such file object in known filesystems
     */
    public FileObject resolveFileObject(String filename) {
        if (filename == null) {
            throw new NullPointerException("Must pass a non-null filename"); // NOI18N
        }
        return PropertyUtils.resolveFileObject(dir, filename);
    }
    
    /**
     * Take an Ant-style path specification and convert it to a platform-specific absolute path.
     * The path separator characters are converted to the local convention, and individual
     * path components are resolved and cleaned up as for {@link #resolveFile}.
     * @param path an Ant-style abstract path
     * @return an absolute, locally usable path
     */
    public String resolvePath(String path) {
        if (path == null) {
            throw new NullPointerException("Must pass a non-null path"); // NOI18N
        }
        // XXX consider memoizing results since this is probably called a lot
        return PropertyUtils.resolvePath(FileUtil.toFile(dir), path);
    }
    
    public String toString() {
        return "RakeProjectHelper[" + getProjectDirectory() + "]"; // NOI18N
    }

}
