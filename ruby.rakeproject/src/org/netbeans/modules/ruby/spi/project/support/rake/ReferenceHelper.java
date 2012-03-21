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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.ruby.api.project.rake.RakeArtifact;
import org.netbeans.modules.ruby.api.project.rake.RakeArtifactQuery;
import org.netbeans.api.queries.CollocationQuery;
import org.netbeans.modules.ruby.modules.project.rake.RakeBasedProjectFactorySingleton;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.SubprojectProvider;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.EditableProperties;
import org.openide.util.Mutex;
import org.openide.util.NbCollections;
import org.openide.xml.XMLUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

// XXX need a method to update non-key data in references e.g. during projectOpened()

/**
 * Helps manage inter-project references.
 * Normally you would create an instance of this object and keep it in your
 * project object in order to support {@link SubprojectProvider} and various
 * operations that change settings which might refer to build artifacts from
 * other projects: e.g. when changing the classpath for a Java-based project
 * you would want to use this helper to scan potential classpath entries for
 * JARs coming from other projects that you would like to be able to build
 * as dependencies before your project is built.
 * <p>
 * You probably only need the higher-level methods such as {@link #addReference}
 * and {@link #removeReference(String,String)}; the lower-level methods such as {@link #addRawReference}
 * are provided for completeness, but typical client code should not need them.
 * <p>
 * Only deals with references needed to support build artifacts coming from
 * foreign projects. If for some reason you wish to store other kinds of
 * references to foreign projects, you do not need this class; just store
 * them however you wish, and be sure to create an appropriate {@link SubprojectProvider}.
 * <p>
 * Modification methods (add, remove) mark the project as modified but do not save it.
 * @author Jesse Glick
 */
public final class ReferenceHelper {

    private static final Logger LOGGER = Logger.getLogger(ReferenceHelper.class.getName());

    /**
     * XML element name used to store references in <code>project.xml</code>.
     */
    static final String REFS_NAME = "references"; // NOI18N
    
    /**
     * XML element name used to store one reference in <code>project.xml</code>.
     */
    static final String REF_NAME = "reference"; // NOI18N
    
    /**
     * XML namespace used to store references in <code>project.xml</code>.
     */
    static final String REFS_NS = "http://www.netbeans.org/ns/rake-project-references/1"; // NOI18N
    
    /**
     * Newer version of {@link #REFS_NS} supporting Properties and with changed semantics of <script>.
     */
    static final String REFS_NS2 = "http://www.netbeans.org/ns/rake-project-references/2"; // NOI18N
    
    /** Set of property names which values can be used as additional base
     * directories. */
    private Set<String> extraBaseDirectories = new HashSet<String>();
    
    private final RakeProjectHelper h;
    final PropertyEvaluator eval;
    private final AuxiliaryConfiguration aux;

    /**
     * Create a new reference helper.
     * It needs an {@link RakeProjectHelper} object in order to update references
     * in <code>project.xml</code>,
     * as well as set project or private properties referring to the locations
     * of foreign projects on disk.
     * <p>
     * The property evaluator may be used in {@link #getForeignFileReferenceAsArtifact},
     * {@link ReferenceHelper.RawReference#toRakeArtifact}, or
     * {@link #createSubprojectProvider}. Typically this would
     * be {@link RakeProjectHelper#getStandardPropertyEvaluator}. You can substitute
     * a custom evaluator but be warned that this helper class assumes that
     * {@link RakeProjectHelper#PROJECT_PROPERTIES_PATH} and {@link RakeProjectHelper#PRIVATE_PROPERTIES_PATH}
     * have their customary meanings; specifically that they are both used when evaluating
     * properties (such as the location of a foreign project) and that private properties
     * can override public properties.
     * @param helper an Ant project helper object representing this project's configuration
     * @param aux an auxiliary configuration provider needed to store references
     * @param eval a property evaluator
     */
    public ReferenceHelper(RakeProjectHelper helper, AuxiliaryConfiguration aux, PropertyEvaluator eval) {
        h = helper;
        this.aux = aux;
        this.eval = eval;
    }

    /**
     * Load <references> from project.xml.
     * @return can return null if there are no references stored yet
     */
    private Element loadReferences() {
        assert ProjectManager.mutex().isReadAccess() || ProjectManager.mutex().isWriteAccess();
        Element references = aux.getConfigurationFragment(REFS_NAME, REFS_NS2, true);
        if (references == null) {
            references = aux.getConfigurationFragment(REFS_NAME, REFS_NS, true);
        }
        return references;
    }

    /**
     * Store <references> to project.xml (i.e. to memory and mark project modified).
     */
    private void storeReferences(Element references) {
        assert ProjectManager.mutex().isWriteAccess();
        assert references != null && references.getLocalName().equals(REFS_NAME) && 
            (REFS_NS.equals(references.getNamespaceURI()) || REFS_NS2.equals(references.getNamespaceURI()));
        aux.putConfigurationFragment(references, true);
    }
    
    private void removeOldReferences() {
        assert ProjectManager.mutex().isWriteAccess();
        aux.removeConfigurationFragment(REFS_NAME, REFS_NS, true);
    }
    
    /**
     * Add a reference to an artifact coming from a foreign project.
     * <p>
     * For more info see {@link #addReference(RakeArtifact, URI)}.
     * @param artifact the artifact to add
     * @return true if a reference or some property was actually added or modified,
     *         false if everything already existed and was not modified
     * @throws IllegalArgumentException if the artifact is not associated with a project
     * @deprecated to add reference use {@link #addReference(RakeArtifact, URI)};
     *   to check whether reference exist or not use {@link #isReferenced(RakeArtifact, URI)}.
     *   This method creates reference for the first artifact location only.
     */
    @Deprecated
    public boolean addReference(final RakeArtifact artifact) throws IllegalArgumentException {
        Object ret[] = addReference0(artifact, artifact.getArtifactLocations()[0]);
        return ((Boolean)ret[0]).booleanValue();
    }

    // @return array of two elements: [Boolean - any modification, String - reference]
    private Object[] addReference0(final RakeArtifact artifact, final URI location) throws IllegalArgumentException {
        return ProjectManager.mutex().writeAccess(new Mutex.Action<Object[]>() {
            public Object[] run() {
                int index = findLocationIndex(artifact, location);
                Project forProj = artifact.getProject();
                if (forProj == null) {
                    throw new IllegalArgumentException("No project associated with " + artifact); // NOI18N
                }
                // Set up the raw reference.
                File forProjDir = FileUtil.toFile(forProj.getProjectDirectory());
                assert forProjDir != null : forProj.getProjectDirectory();
                String projName = getUsableReferenceID(ProjectUtils.getInformation(forProj).getName());
                String forProjName = findReferenceID(projName, "project.", forProjDir.getAbsolutePath());
                if (forProjName == null) {
                    forProjName = generateUniqueID(projName, "project.", forProjDir.getAbsolutePath());
                }
                RawReference ref;
                File scriptFile = artifact.getScriptLocation();
                if (canUseVersion10(artifact, forProjDir)) {
                    String rel = PropertyUtils.relativizeFile(forProjDir, scriptFile);
                    URI scriptLocation;
                    try {
                        scriptLocation = new URI(null, null, rel, null);
                    } catch (URISyntaxException ex) {
                        scriptLocation = forProjDir.toURI().relativize(scriptFile.toURI());
                    }
                    ref = new RawReference(forProjName, artifact.getType(), scriptLocation, artifact.getTargetName(), artifact.getCleanTargetName(), artifact.getID());
                } else {
                    String scriptLocation;
                    if (scriptFile.getAbsolutePath().startsWith(forProjDir.getAbsolutePath())) {
                        String rel = PropertyUtils.relativizeFile(forProjDir, scriptFile);
                        assert rel != null : "Relativization must succeed for files: "+forProjDir+ " "+scriptFile;
                        scriptLocation = "${project."+forProjName+"}/"+rel;
                    } else {
                        scriptLocation = "build.script.reference." + forProjName;
                        setPathProperty(forProjDir, scriptFile, scriptLocation);
                        scriptLocation = "${"+scriptLocation+"}";
                    }
                    ref = new RawReference(forProjName, artifact.getType(), scriptLocation, 
                        artifact.getTargetName(), artifact.getCleanTargetName(), 
                        artifact.getID(), artifact.getProperties());
                }
                boolean success = addRawReference0(ref);
                // Set up ${project.whatever}.
                FileObject myProjDirFO = RakeBasedProjectFactorySingleton.getProjectFor(h).getProjectDirectory();
                File myProjDir = FileUtil.toFile(myProjDirFO);
                if (setPathProperty(myProjDir, forProjDir, "project." + forProjName)) {
                    success = true;
                }
                // Set up ${reference.whatever.whatever}.
                String propertiesFile;
                String forProjPathProp = "project." + forProjName; // NOI18N
                URI artFile = location;
                String refPath;
                if (artFile.isAbsolute()) {
                    refPath = new File(artFile).getAbsolutePath();
                    propertiesFile = RakeProjectHelper.PRIVATE_PROPERTIES_PATH;
                } else {
                    refPath = "${" + forProjPathProp + "}/" + artFile.getPath(); // NOI18N
                    propertiesFile = RakeProjectHelper.PROJECT_PROPERTIES_PATH;
                }
                EditableProperties props = h.getProperties(propertiesFile);
                String refPathProp = "reference." + forProjName + '.' + getUsableReferenceID(artifact.getID()); // NOI18N
                if (index > 0) {
                    refPathProp += "."+index;
                }
                if (!refPath.equals(props.getProperty(refPathProp))) {
                    props.put(refPathProp, refPath);
                    h.putProperties(propertiesFile, props);
                    success = true;
                }
                return new Object[] {success, "${" + refPathProp + "}"}; // NOI18N
            }
        });
    }
    
    private int findLocationIndex(final RakeArtifact artifact, final URI location) throws IllegalArgumentException {
        if (location == null) {
            throw new IllegalArgumentException("location cannot be null");
        }
        URI uris[] = artifact.getArtifactLocations();
        for (int i=0; i<uris.length; i++) {
            if (uris[i].equals(location)) {
                return i;
            }
        }
        throw new IllegalArgumentException("location ("+location+") must be in RakeArtifact's locations ("+artifact+")");
    }

    /**
     * Test whether the artifact can be stored as /1 artifact or not.
     */
    private static boolean canUseVersion10(RakeArtifact aa, File projectDirectory) {
        // is there multiple outputs?
        if (aa.getArtifactLocations().length > 1) {
            return false;
        }
        // has some properties?
        if (aa.getProperties().keySet().size() > 0) {
            return false;
        }
        // does Ant script lies under project directory?
        if (!aa.getScriptLocation().getAbsolutePath().startsWith(projectDirectory.getAbsolutePath())) {
            return false;
        }
        return true;
    }

    /**
     * Helper method which checks collocation status of two files and based on
     * that it will in private or project properties file set up property with
     * the given name and with absolute or relative path value.
     * @return was there any change or not
     */
    private boolean setPathProperty(File base, File path, String propertyName) {
        String[] values;
        String[] propertiesFiles;
        
        String relativePath = relativizeFileToExtraBaseFolders(path);
        // try relativize against external base dirs
        if (relativePath != null) {
            propertiesFiles = new String[] {
                RakeProjectHelper.PROJECT_PROPERTIES_PATH
            };
            values = new String[] {
                relativePath
            };
        }        
        else if (CollocationQuery.areCollocated(base, path)) {
            // Fine, using a relative path to subproject.
            relativePath = PropertyUtils.relativizeFile(base, path);
            assert relativePath != null : "These dirs are not really collocated: " + base + " & " + path;
            values = new String[] {
                relativePath,
                path.getAbsolutePath()
            };            
            propertiesFiles = new String[] {
                RakeProjectHelper.PROJECT_PROPERTIES_PATH,
                RakeProjectHelper.PRIVATE_PROPERTIES_PATH,
            };
        } else {                        
            // use an absolute path.
            // mkleint: when the AlwaysRelativeCollocationQueryImplementation gets removed
            // this code gets called more frequently
            // to get the previous behaviour replace CollocationQuery.areCollocated(base, path)
            // with PropertyUtils.relativizeFile(base, path) != null
            propertiesFiles = new String[] {
                RakeProjectHelper.PRIVATE_PROPERTIES_PATH
            };
            values = new String[] {
                path.getAbsolutePath()
            };            
        }
        
        boolean metadataChanged = false;
        for (int i=0; i<propertiesFiles.length; i++) {
            EditableProperties props = h.getProperties(propertiesFiles[i]);
            if (!values[i].equals(props.getProperty(propertyName))) {
                props.put(propertyName, values[i]);
                h.putProperties(propertiesFiles[i], props);
                metadataChanged = true;
            }
        }
        
        if (propertiesFiles.length == 1) {                    
            // check presence of this property in opposite property file and
            // remove it if necessary
            String propertiesFile = (propertiesFiles[0] == RakeProjectHelper.PROJECT_PROPERTIES_PATH ? 
                RakeProjectHelper.PRIVATE_PROPERTIES_PATH : RakeProjectHelper.PROJECT_PROPERTIES_PATH);
            EditableProperties props = h.getProperties(propertiesFile);
            if (props.remove(propertyName) != null) {
                h.putProperties(propertiesFile, props);
            }
        }
        return metadataChanged;
    }
    
    /**
     * Add a reference to an artifact's location coming from a foreign project.
     * <p>
     * Records the name of the foreign project.
     * Normally the foreign project name is that project's code name,
     * but it may be uniquified if that name is already taken to refer
     * to a different project with the same code name.
     * <p>
     * Adds a project property if necessary to refer to its location of the foreign
     * project - a shared property if the foreign project
     * is {@link CollocationQuery collocated} with this one, else a private property.
     * This property is named <samp>project.<i>foreignProjectName</i></samp>.
     * Example: <samp>project.mylib=../mylib</samp>
     * <p>
     * Adds a project property to refer to the artifact's location.
     * This property is named <samp>reference.<i>foreignProjectName</i>.<i>targetName</i></samp>
     * and will use <samp>${project.<i>foreignProjectName</i>}</samp> and be a shared
     * property - unless the artifact location is an absolute URI, in which case the property
     * will also be private.
     * Example: <samp>reference.mylib.jar=${project.mylib}/dist/mylib.jar</samp>
     * <p>
     * Also records the artifact type, (relative) script path, and build and
     * clean target names.
     * <p>
     * If the reference already exists (keyed by foreign project object
     * and target name), nothing is done, unless some other field (script location,
     * clean target name, or artifact type) needed to be updated, in which case
     * the new information replaces the old. Similarly, the artifact location
     * property is updated if necessary.
     * <p>
     * Acquires write access.
     * @param artifact the artifact to add
     * @param location the artifact's location to create reference to
     * @return name of reference which was created or already existed
     * @throws IllegalArgumentException if the artifact is not associated with a project
     *   or if the location is not artifact's location
     * @since 1.5
     */
    public String addReference(final RakeArtifact artifact, URI location) throws IllegalArgumentException {
        Object ret[] = addReference0(artifact, location);
        return (String)ret[1];
    }
    
    /**
     * Tests whether reference for artifact's location was already created by
     * {@link #addReference(RakeArtifact, URI)} for this project or not. This
     * method returns false also in case when reference exist but needs to be
     * updated.
     * <p>
     * Acquires read access.
     * @param artifact the artifact to add
     * @param location the artifact's location to create reference to
     * @return true if already referenced
     * @throws IllegalArgumentException if the artifact is not associated with a project
     *   or if the location is not artifact's location
     * @since 1.5
     */
    public boolean isReferenced(final RakeArtifact artifact, final URI location) throws IllegalArgumentException {
        return ProjectManager.mutex().readAccess(new Mutex.Action<Boolean>() {
            public Boolean run() {
                int index = findLocationIndex(artifact, location);
                Project forProj = artifact.getProject();
                if (forProj == null) {
                    throw new IllegalArgumentException("No project associated with " + artifact); // NOI18N
                }
                File forProjDir = FileUtil.toFile(forProj.getProjectDirectory());
                assert forProjDir != null : forProj.getProjectDirectory();
                String projName = getUsableReferenceID(ProjectUtils.getInformation(forProj).getName());
                String forProjName = findReferenceID(projName, "project.", forProjDir.getAbsolutePath());
                if (forProjName == null) {
                    return false;
                }
                RawReference ref = getRawReference(forProjName, getUsableReferenceID(artifact.getID()));
                if (ref == null) {
                    return false;
                }
                File script = h.resolveFile(eval.evaluate(ref.getScriptLocationValue()));
                if (!artifact.getType().equals(ref.getArtifactType()) ||
                        !artifact.getID().equals(ref.getID()) ||
                        !artifact.getScriptLocation().equals(script) ||
                        !artifact.getProperties().equals(ref.getProperties()) ||
                        !artifact.getTargetName().equals(ref.getTargetName()) ||
                        !artifact.getCleanTargetName().equals(ref.getCleanTargetName())) {
                    return false;
                }
                
                String reference = "reference." + forProjName + '.' + getUsableReferenceID(artifact.getID()); // NOI18N
                if (index > 0) {
                    reference += "."+index;
                }
                return eval.getProperty(reference) != null;
            }
        });
    }
    
    /**
     * Add a raw reference to a foreign project artifact.
     * Does not check if such a project already exists; does not create a project
     * property to refer to it; does not do any backreference usage notifications.
     * <p>
     * If the reference already exists (keyed by foreign project name and target name),
     * nothing is done, unless some other field (script location, clean target name,
     * or artifact type) needed to be updated, in which case the new information
     * replaces the old.
     * <p>
     * Note that since {@link RawReference} is just a descriptor, it is not guaranteed
     * that after adding one {@link #getRawReferences} or {@link #getRawReference}
     * would return the identical object.
     * <p>
     * Acquires write access.
     * @param ref a raw reference descriptor
     * @return true if a reference was actually added or modified,
     *         false if it already existed and was not modified
     */
    public boolean addRawReference(final RawReference ref) {
        return ProjectManager.mutex().writeAccess(new Mutex.Action<Boolean>() {
            public Boolean run() {
                try {
                    return addRawReference0(ref);
                } catch (IllegalArgumentException e) {
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    return false;
                }
            }
        });
    }
    
    private boolean addRawReference0(final RawReference ref) throws IllegalArgumentException {
        Element references = loadReferences();
        if (references == null) {
            references = XMLUtil.createDocument("ignore", null, null, null).createElementNS(ref.getNS(), REFS_NAME); // NOI18N
        }
        boolean modified = false;
        if (references.getNamespaceURI().equals(REFS_NS) && ref.getNS().equals(REFS_NS2)) {
            // upgrade all references to version /2 here:
            references = upgradeTo20(references);
            removeOldReferences();
            modified = true;
        }
        modified = updateRawReferenceElement(ref, references);
        if (modified) {
            storeReferences(references);
        }
        return modified;
    }
    
    private Element upgradeTo20(Element references) {
        Element references20 = XMLUtil.createDocument("ignore", null, null, null).createElementNS(REFS_NS2, REFS_NAME); // NOI18N
        RawReference rr[] = getRawReferences(references);
        for (int i=0; i<rr.length; i++) {
            rr[i].upgrade();
            updateRawReferenceElement(rr[i], references20);
        }
        return references20;
    }
    
    private static boolean updateRawReferenceElement(RawReference ref, Element references) throws IllegalArgumentException {
        // Linear search; always keeping references sorted first by foreign project
        // name, then by target name.
        Element nextRefEl = null;
        Iterator<Element> it = XMLUtil.findSubElements(references).iterator();
        while (it.hasNext()) {
            Element testRefEl = it.next();
            RawReference testRef = RawReference.create(testRefEl);
            if (testRef.getForeignProjectName().compareTo(ref.getForeignProjectName()) > 0) {
                // gone too far, go back
                nextRefEl = testRefEl;
                break;
            }
            if (testRef.getForeignProjectName().equals(ref.getForeignProjectName())) {
                if (testRef.getID().compareTo(ref.getID()) > 0) {
                    // again, gone too far, go back
                    nextRefEl = testRefEl;
                    break;
                }
                if (testRef.getID().equals(ref.getID())) {
                    // Key match, check if it needs to be updated.
                    if (testRef.getArtifactType().equals(ref.getArtifactType()) &&
                            testRef.getScriptLocationValue().equals(ref.getScriptLocationValue()) &&
                            testRef.getProperties().equals(ref.getProperties()) &&
                            testRef.getTargetName().equals(ref.getTargetName()) &&
                            testRef.getCleanTargetName().equals(ref.getCleanTargetName())) {
                        // Match on other fields. Return without changing anything.
                        return false;
                    }
                    // Something needs updating.
                    // Delete the old ref and set nextRef to the next item in line.
                    references.removeChild(testRefEl);
                    if (it.hasNext()) {
                        nextRefEl = it.next();
                    } else {
                        nextRefEl = null;
                    }
                    break;
                }
            }
        }
        // Need to insert a new record before nextRef.
        Element newRefEl = ref.toXml(references.getNamespaceURI(), references.getOwnerDocument());
        // Note: OK if nextRefEl == null, that means insert as last child.
        references.insertBefore(newRefEl, nextRefEl);
        return true;
    }
    
    /**
     * Remove a reference to an artifact coming from a foreign project.
     * <p>
     * The property giving the location of the artifact is removed if it existed.
     * <p>
     * If this was the last reference to the foreign project, its location
     * property is removed as well.
     * <p>
     * If the reference does not exist, nothing is done.
     * <p>
     * Acquires write access.
     * @param foreignProjectName the local name of the foreign project
     *                           (usually its code name)
     * @param id the ID of the build artifact (usually build target name)
     * @return true if a reference or some property was actually removed,
     *         false if the reference was not there and no property was removed
     * @deprecated use {@link #destroyReference} instead; was unused anyway
     */
    @Deprecated
    public boolean removeReference(final String foreignProjectName, final String id) {
        return removeReference(foreignProjectName, id, false, null);
    }
    
    /**
     * Checks whether this is last reference and therefore the artifact can
     * be removed from project.xml or not
     */
    private boolean isLastReference(String ref) {
       Object ret[] = findArtifactAndLocation(ref);
       if (ret[0] == null || ret[1] == null) {
           return true;
       }
       RakeArtifact aa = (RakeArtifact)ret[0];
       URI uri = (URI)ret[1];
       URI uris[] = aa.getArtifactLocations();
       boolean lastReference = true;
       // are there any other referenced jars or not:
       for (int i=0; i<uris.length; i++) {
           if (uris[i].equals(uri)) {
               continue;
           }
           if (isReferenced(aa, uris[i])) {
               lastReference = false;
               break;
           }
       }
       return lastReference;
    }
    
    private boolean removeReference(final String foreignProjectName, final String id, final boolean escaped, final String reference) {
        return ProjectManager.mutex().writeAccess(new Mutex.Action<Boolean>() {
            public Boolean run() {
                boolean success = false;
                try {
                    if (isLastReference("${"+reference+"}")) {
                        success = removeRawReference0(foreignProjectName, id, escaped);
                    }
                } catch (IllegalArgumentException e) {
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    return false;
                }
                // Note: try to delete obsoleted properties from both project.properties
                // and private.properties, just in case.
                String[] PROPS_PATHS = {
                    RakeProjectHelper.PROJECT_PROPERTIES_PATH,
                    RakeProjectHelper.PRIVATE_PROPERTIES_PATH,
                };
                // if raw reference was removed then try to clean also project reference property:
                if (success) {
                    // Check whether there are any other references using foreignProjectName.
                    // If not, we can delete ${project.foreignProjectName}.
                    RawReference[] refs = new RawReference[0];
                    Element references = loadReferences();
                    if (references != null) {
                        refs = getRawReferences(references);
                    }
                    boolean deleteProjProp = true;
                    for (int i = 0; i < refs.length; i++) {
                        if (refs[i].getForeignProjectName().equals(foreignProjectName)) {
                            deleteProjProp = false;
                            break;
                        }
                    }
                    if (deleteProjProp) {
                        String projProp = "project." + foreignProjectName; // NOI18N
                        for (int i = 0; i < PROPS_PATHS.length; i++) {
                            EditableProperties props = h.getProperties(PROPS_PATHS[i]);
                            if (props.containsKey(projProp)) {
                                props.remove(projProp);
                                h.putProperties(PROPS_PATHS[i], props);
                                success = true;
                            }
                        }
                    }
                }
                
                String refProp = reference;
                if (refProp == null) {
                    refProp = "reference." + foreignProjectName + '.' + getUsableReferenceID(id); // NOI18N
                }
                // remove also build script property if exist any:
                String buildScriptProperty = "build.script.reference." + foreignProjectName;
                for (String path : PROPS_PATHS) {
                    EditableProperties props = h.getProperties(path);
                    if (props.containsKey(refProp)) {
                        props.remove(refProp);
                        h.putProperties(path, props);
                        success = true;
                    }
                    if (props.containsKey(buildScriptProperty)) {
                        props.remove(buildScriptProperty);
                        h.putProperties(path, props);
                        success = true;
                    }
                }
                return success;
            }
        });
    }
    
    /**
     * Remove reference to a file.
     * <p>
     * If the reference does not exist, nothing is done.
     * <p>
     * Acquires write access.
     * @param fileReference file reference as created by 
     *    {@link #createForeignFileReference(File, String)}
     * @return true if the reference was actually removed; otherwise false
     * @deprecated use {@link #destroyReference} instead; was unused anyway
     */
    @Deprecated
    public boolean removeReference(final String fileReference) {
        return removeFileReference(fileReference);
    }
    
    private boolean removeFileReference(final String fileReference) {
        return ProjectManager.mutex().writeAccess(new Mutex.Action<Boolean>() {
            public Boolean run() {
                boolean success = false;
                // Note: try to delete obsoleted properties from both project.properties
                // and private.properties, just in case.
                String[] PROPS_PATHS = {
                    RakeProjectHelper.PROJECT_PROPERTIES_PATH,
                    RakeProjectHelper.PRIVATE_PROPERTIES_PATH,
                };
                String refProp = fileReference;
                if (refProp.startsWith("${") && refProp.endsWith("}")) {
                    refProp = refProp.substring(2, refProp.length()-1);
                }
                for (String path : PROPS_PATHS) {
                    EditableProperties props = h.getProperties(path);
                    if (props.containsKey(refProp)) {
                        props.remove(refProp);
                        h.putProperties(path, props);
                        success = true;
                    }
                }
                return success;
            }
        });
    }
    
    /**
     * Remove a raw reference to an artifact coming from a foreign project.
     * Does not attempt to manipulate backreferences in the foreign project
     * nor project properties.
     * <p>
     * If the reference does not exist, nothing is done.
     * <p>
     * Acquires write access.
     * @param foreignProjectName the local name of the foreign project
     *                           (usually its code name)
     * @param id the ID of the build artifact (usually build target name)
     * @return true if a reference was actually removed, false if it was not there
     */
    public boolean removeRawReference(final String foreignProjectName, final String id) {
        return ProjectManager.mutex().writeAccess(new Mutex.Action<Boolean>() {
            public Boolean run() {
                try {
                    return removeRawReference0(foreignProjectName, id, false);
                } catch (IllegalArgumentException e) {
                    ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    return false;
                }
            }
        });
    }
    
    private boolean removeRawReference0(final String foreignProjectName, final String id, boolean escaped) throws IllegalArgumentException {
        Element references = loadReferences();
        if (references == null) {
            return false;
        }
        boolean success = removeRawReferenceElement(foreignProjectName, id, references, escaped);
        if (success) {
            storeReferences(references);
        }
        return success;
    }
    
    private static boolean removeRawReferenceElement(String foreignProjectName, String id, Element references, boolean escaped) throws IllegalArgumentException {
        // As with addRawReference, do a linear search through.
        for (Element testRefEl : XMLUtil.findSubElements(references)) {
            RawReference testRef = RawReference.create(testRefEl);
            String refID = testRef.getID();
            String refName = testRef.getForeignProjectName();
            if (escaped) {
                refID = getUsableReferenceID(testRef.getID());
                refName = getUsableReferenceID(testRef.getForeignProjectName());
            }
            if (refName.compareTo(foreignProjectName) > 0) {
                // searched past it
                return false;
            }
            if (refName.equals(foreignProjectName)) {
                if (refID.compareTo(id) > 0) {
                    // again, searched past it
                    return false;
                }
                if (refID.equals(id)) {
                    // Key match, remove it.
                    references.removeChild(testRefEl);
                    return true;
                }
            }
        }
        // Searched through to the end and did not find it.
        return false;
    }

    /**
     * Get a list of raw references from this project to others.
     * If necessary, you may use {@link RawReference#toRakeArtifact} to get
     * live information from each reference, such as its associated project.
     * <p>
     * Acquires read access.
     * @return a (possibly empty) list of raw references from this project
     */
    public RawReference[] getRawReferences() {
        return ProjectManager.mutex().readAccess(new Mutex.Action<RawReference[]>() {
            public RawReference[] run() {
                Element references = loadReferences();
                if (references != null) {
                    try {
                        return getRawReferences(references);
                    } catch (IllegalArgumentException e) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    }
                }
                return new RawReference[0];
            }
        });
    }
    
    private static RawReference[] getRawReferences(Element references) throws IllegalArgumentException {
        List<Element> subEls = XMLUtil.findSubElements(references);
        List<RawReference> refs = new ArrayList<RawReference>(subEls.size());
        for (Element subEl : subEls) {
            refs.add(RawReference.create(subEl));
        }
        return refs.toArray(new RawReference[refs.size()]);
    }
    
    /**
     * Get a particular raw reference from this project to another.
     * If necessary, you may use {@link RawReference#toRakeArtifact} to get
     * live information from each reference, such as its associated project.
     * <p>
     * Acquires read access.
     * @param foreignProjectName the local name of the foreign project
     *                           (usually its code name)
     * @param id the ID of the build artifact (usually the build target name)
     * @return the specified raw reference from this project,
     *         or null if none such could be found
     */
    public RawReference getRawReference(final String foreignProjectName, final String id) {
        return getRawReference(foreignProjectName, id, false);
    }
    
    // not private only to allow unit testing
    RawReference getRawReference(final String foreignProjectName, final String id, final boolean escaped) {
        return ProjectManager.mutex().readAccess(new Mutex.Action<RawReference>() {
            public RawReference run() {
                Element references = loadReferences();
                if (references != null) {
                    try {
                        return getRawReference(foreignProjectName, id, references, escaped);
                    } catch (IllegalArgumentException e) {
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                    }
                }
                return null;
            }
        });
    }
    
    private static RawReference getRawReference(String foreignProjectName, String id, Element references, boolean escaped) throws IllegalArgumentException {
        for (Element subEl : XMLUtil.findSubElements(references)) {
            RawReference ref = RawReference.create(subEl);
            String refID = ref.getID();
            String refName = ref.getForeignProjectName();
            if (escaped) {
                refID = getUsableReferenceID(ref.getID());
                refName = getUsableReferenceID(ref.getForeignProjectName());
            }
            if (refName.equals(foreignProjectName) && refID.equals(id)) {
                return ref;
            }
        }
        return null;
    }
    
    /**
     * Create an Ant-interpretable string referring to a file on disk.
     * If the file refers to a known Ant artifact according to
     * {@link RakeArtifactQuery#findArtifactFromFile}, of the expected type
     * and associated with a particular project,
     * the behavior is identical to {@link #createForeignFileReference(RakeArtifact)}.
     * Otherwise, a reference for the file is created. The file path will
     * be relative in case {@link CollocationQuery#areCollocated} says that
     * the file is collocated with this project's main directory, else it
     * will be an absolute path.
     * <p>
     * Acquires write access.
     * @param file a file to refer to (need not currently exist)
     * @param expectedArtifactType the required {@link RakeArtifact#getType}
     * @return a string which can refer to that file somehow
     */
    public String createForeignFileReference(final File file, final String expectedArtifactType) {
        if (!file.equals(FileUtil.normalizeFile(file))) {
            throw new IllegalArgumentException("Parameter file was not "+  // NOI18N
                "normalized. Was "+file+" instead of "+FileUtil.normalizeFile(file));  // NOI18N
        }
        return ProjectManager.mutex().writeAccess(new Mutex.Action<String>() {
            public String run() {
                RakeArtifact art = RakeArtifactQuery.findArtifactFromFile(file);
                if (art != null && art.getType().equals(expectedArtifactType) && art.getProject() != null) {
                    try {
                        return createForeignFileReference(art);
                    } catch (IllegalArgumentException iae) {
                        throw new AssertionError(iae);
                    }
                } else {
                    String propertiesFile;
                    String path;
                    File myProjDir = FileUtil.toFile(RakeBasedProjectFactorySingleton.getProjectFor(h).getProjectDirectory());
                    String fileID = file.getName();
                    // if the file is folder then add to ID string also parent folder name,
                    // i.e. if external source folder name is "src" the ID will
                    // be a bit more selfdescribing, e.g. project-src in case
                    // of ID for ant/project/src directory.
                    if (file.isDirectory() && file.getParentFile() != null) {
                        fileID = file.getParentFile().getName()+"-"+file.getName();
                    }
                    fileID = PropertyUtils.getUsablePropertyName(fileID);
                    String prop = findReferenceID(fileID, "file.reference.", file.getAbsolutePath()); // NOI18N
                    if (prop == null) {
                        prop = generateUniqueID(fileID, "file.reference.", file.getAbsolutePath()); // NOI18N
                    }
                    setPathProperty(myProjDir, file, "file.reference." + prop);
                    return "${file.reference." + prop + '}'; // NOI18N
                }
            }
        });
    }
    
    /**
     * Test whether file does not lie under an extra base folder and if it does
     * then return string in form of "${extra.base}/remaining/path"; or null.
     */
    private String relativizeFileToExtraBaseFolders(File f) {
        File base = FileUtil.toFile(h.getProjectDirectory());
        String fileToRelativize = f.getAbsolutePath();
        for (String prop : extraBaseDirectories) {
            String path = eval.getProperty(prop);
            File extraBase = PropertyUtils.resolveFile(base, path);
            path = extraBase.getAbsolutePath();
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
            if (fileToRelativize.startsWith(path)) {
                return "${"+prop+"}/"+fileToRelativize.substring(path.length()).replace('\\', '/'); // NOI18N
            }
        }
        return null;
    }

    /**
     * Add extra folder which can be used as base directory (in addition to
     * project base folder) for creating references. Duplicate property names
     * are not allowed. Any newly created reference to a file lying under an
     * extra base directory will be based on that property and will be stored in
     * shared project properties.
     * <p>Acquires write access.
     * @param propertyName property name which value is path to folder which
     *  can be used as alternative project's base directory; cannot be null;
     *  property must exist
     * @throws IllegalArgumentException if propertyName is null or such a 
     *   property does not exist
     * @since 1.4
     */
    public void addExtraBaseDirectory(final String propertyName) {
        if (propertyName == null || eval.getProperty(propertyName) == null) {
            throw new IllegalArgumentException("propertyName is null or such a property does not exist: "+propertyName); // NOI18N
        }
        ProjectManager.mutex().writeAccess(new Runnable() {
                public void run() {
                    if (!extraBaseDirectories.add(propertyName)) {
                        throw new IllegalArgumentException("Already extra base directory property: "+propertyName); // NOI18N
                    }
                }
            });
    }
    
    /**
     * Remove extra base directory. The base directory property had to be added
     * by {@link #addExtraBaseDirectory} method call. At the time when this
     * method is called the property must still exist and must be valid. This
     * method will replace all references of the extra base directory property
     * with its current value and if needed it may move such a property from
     * shared project properties into the private properties.
     * <p>Acquires write access.
     * @param propertyName property name which was added by 
     * {@link #addExtraBaseDirectory} method.
     * @throws IllegalArgumentException if given property is not extra base 
     *   directory
     * @since 1.4
     */
    public void removeExtraBaseDirectory(final String propertyName) {
        ProjectManager.mutex().writeAccess(new Runnable() {
                public void run() {
                    if (!extraBaseDirectories.remove(propertyName)) {
                        throw new IllegalArgumentException("Non-existing extra base directory property: "+propertyName); // NOI18N
                    }
                    // substitute all references of removed extra base folder property with its value
                    String tag = "${"+propertyName+"}"; // NOI18N
                    // was extra base property defined in shared file or not:
                    boolean shared = h.getProperties(RakeProjectHelper.PROJECT_PROPERTIES_PATH).containsKey(propertyName);
                    String value = eval.getProperty(propertyName);
                    EditableProperties propProj = h.getProperties(RakeProjectHelper.PROJECT_PROPERTIES_PATH);
                    EditableProperties propPriv = h.getProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH);
                    boolean modifiedProj = false;
                    boolean modifiedPriv = false;
                    Iterator<Map.Entry<String,String>> it = propProj.entrySet().iterator();
                    while (it.hasNext()) {
                        Map.Entry<String,String> entry = it.next();
                        String val = entry.getValue();
                        int index;
                        if ((index = val.indexOf(tag)) != -1) {
                            val = val.substring(0, index) +value + val.substring(index+tag.length());
                            if (shared) {
                                // substitute extra base folder property with its value
                                entry.setValue(val);
                            } else {
                                // move property to private properties file
                                it.remove();
                                propPriv.put(entry.getKey(), val);
                                modifiedPriv = true;
                            }
                            modifiedProj = true;
                        }
                    }
                    if (modifiedProj) {
                        h.putProperties(RakeProjectHelper.PROJECT_PROPERTIES_PATH, propProj);
                    }
                    if (modifiedPriv) {
                        h.putProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH, propPriv);
                    }
                }
            });
    }
    
    /**
     * Find reference ID (e.g. something you can then pass to RawReference 
     * as foreignProjectName) for the given property base name, prefix and path.
     * @param property project name or jar filename
     * @param prefix prefix used for reference, i.e. "project." for project 
     *    reference or "file.reference." for file reference
     * @param path absolute filename the reference points to
     * @return found reference ID or null
     */
    private String findReferenceID(String property, String prefix, String path) {
        Map<String,String> m = h.getStandardPropertyEvaluator().getProperties();
        for (Map.Entry<String,String> e : m.entrySet()) {
            String key = e.getKey();
            if (key.startsWith(prefix+property)) {
                String v = h.resolvePath(e.getValue());
                if (path.equals(v)) {
                    return key.substring(prefix.length());
                }
            }
        }
        return null;
    }
    
    /**
     * Generate unique reference ID for the given property base name, prefix 
     * and path. See also {@link #findReferenceID(String, String, String)}.
     * @param property project name or jar filename
     * @param prefix prefix used for reference, i.e. "project." for project 
     *    reference or "file.reference." for file reference
     * @param path absolute filename the reference points to
     * @return generated unique reference ID
     */
    private String generateUniqueID(String property, String prefix, String value) {
        PropertyEvaluator pev = h.getStandardPropertyEvaluator();
        if (pev.getProperty(prefix+property) == null) {
            return property;
        }
        int i = 1;
        while (pev.getProperty(prefix+property+"-"+i) != null) {
            i++;
        }
        return property+"-"+i;
    }
    
    /**
     * Create an Ant-interpretable string referring to a known build artifact file.
     * Simply calls {@link #addReference} and returns an Ant string which will
     * refer to that artifact correctly.
     * <p>
     * Acquires write access.
     * @param artifact a known build artifact to refer to
     * @return a string which can refer to that artifact file somehow
     * @throws IllegalArgumentException if the artifact is not associated with a project
     * @deprecated use {@link #addReference(RakeArtifact, URI)} instead
     */
    @Deprecated
    public String createForeignFileReference(RakeArtifact artifact) throws IllegalArgumentException {
        Object ret[] = addReference0(artifact, artifact.getArtifactLocations()[0]);
        return (String)ret[1];
    }

    /**
     * Project reference ID cannot contain dot character.
     * File reference can.
     */
    private static String getUsableReferenceID(String ID) {
        return PropertyUtils.getUsablePropertyName(ID).replace('.', '_');
    }
    
    
    private static final Pattern FOREIGN_FILE_REFERENCE = Pattern.compile("\\$\\{reference\\.([^.${}]+)\\.([^.${}]+)\\.([\\d&&[^.${}]]+)\\}"); // NOI18N
    private static final Pattern FOREIGN_FILE_REFERENCE_OLD = Pattern.compile("\\$\\{reference\\.([^.${}]+)\\.([^.${}]+)\\}"); // NOI18N
    private static final Pattern FOREIGN_PLAIN_FILE_REFERENCE = Pattern.compile("\\$\\{file\\.reference\\.([^${}]+)\\}"); // NOI18N
    
    /**
     * Try to find an <code>RakeArtifact</code> object corresponding to a given
     * foreign file reference.
     * If the supplied string is not a recognized reference to a build
     * artifact, returns null.
     * <p>Acquires read access.
     * @param reference a reference string as present in an Ant property
     * @return a corresponding Ant artifact object if there is one, else null
     * @deprecated use {@link #findArtifactAndLocation} instead
     */
    @Deprecated
    public RakeArtifact getForeignFileReferenceAsArtifact(final String reference) {
        Object ret[] = findArtifactAndLocation(reference);
        return (RakeArtifact)ret[0];
    }
    
    /**
     * Try to find an <code>RakeArtifact</code> object and location corresponding
     * to a given reference. If the supplied string is not a recognized 
     * reference to a build artifact, returns null.
     * <p>
     * Acquires read access.
     * @param reference a reference string as present in an Ant property and as
     *   created by {@link #addReference(RakeArtifact, URI)}
     * @return always returns array of two items. The items may be null. First
     *   one is instance of RakeArtifact and second is instance of URI and is 
     *   RakeArtifact's location
     * @since 1.5
     */
    public Object[] findArtifactAndLocation(final String reference) {
        return ProjectManager.mutex().readAccess(new Mutex.Action<Object[]>() {
            public Object[] run() {
                RakeArtifact aa = null;
                Matcher m = FOREIGN_FILE_REFERENCE.matcher(reference);
                boolean matches = m.matches();
                int index = 0;
                if (!matches) {
                    m = FOREIGN_FILE_REFERENCE_OLD.matcher(reference);
                    matches = m.matches();
                } else {
                    try {
                        index = Integer.parseInt(m.group(3));
                    } catch (NumberFormatException ex) {
                        ErrorManager.getDefault().log(ErrorManager.INFORMATIONAL, 
                            "Could not parse reference ("+reference+") for the jar index. " + // NOI18N
                            "Expected number: "+m.group(3)); // NOI18N
                        matches = false;
                    }
                }
                if (matches) {
                    RawReference ref = getRawReference(m.group(1), m.group(2), true);
                    if (ref != null) {
                        aa = ref.toRakeArtifact(ReferenceHelper.this);
                    }
                }
                if (aa == null) {
                    return new Object[] {null, null};
                }
                if (index >= aa.getArtifactLocations().length) {
                    // #55413: we no longer have that many items...treat it as dead.
                    return new Object[] {null, null};
                }
                URI uri = aa.getArtifactLocations()[index];
                return new Object[] {aa, uri};
            }
        });
    }
    
    /**
     * Remove a reference to a foreign file from the project.
     * See {@link #destroyReference} for more information.
     * @param reference an Ant-interpretable foreign file reference as created e.g.
     *                  by {@link #createForeignFileReference(File,String)} or
     *                  by {@link #createForeignFileReference(RakeArtifact)}
     * @deprecated use {@link #destroyReference} instead which does exactly 
     *   the same but has more appropriate name
     */
    @Deprecated
    public void destroyForeignFileReference(String reference) {
        destroyReference(reference);
    }
    
    /**
     * Remove a reference to a foreign file from the project.
     * If the passed string consists of an Ant property reference corresponding to
     * a known inter-project reference created by 
     * {@link #addReference(RakeArtifact, URI)} or file reference created by
     * {@link #createForeignFileReference(File, String)}, that reference is removed.
     * Since this would break any other identical foreign
     * file references present in the project, you should first confirm that this
     * reference was the last one of its kind (by string match).
     * <p>
     * If the passed string is anything else (i.e. a plain file path, relative or
     * absolute), nothing is done.
     * <p>
     * Acquires write access.
     * @param reference an Ant-interpretable foreign file reference as created e.g.
     *                  by {@link #createForeignFileReference(File,String)} or
     *                  by {@link #createForeignFileReference(RakeArtifact)}
     * @return true if reference was really destroyed or not
     * @since 1.5
     */
    public boolean destroyReference(String reference) {
        Matcher m = FOREIGN_FILE_REFERENCE.matcher(reference);
        boolean matches = m.matches();
        if (!matches) {
            m = FOREIGN_FILE_REFERENCE_OLD.matcher(reference);
            matches = m.matches();
        }
        if (matches) {
            String forProjName = m.group(1);
            String id = m.group(2);
            return removeReference(forProjName, id, true, reference.substring(2, reference.length()-1));
        }
        m = FOREIGN_PLAIN_FILE_REFERENCE.matcher(reference);
        if (m.matches()) {
            return removeFileReference(reference);
        }
        return false;
    }
    
    /**
     * Create an object permitting this project to represent subprojects.
     * Would be placed into the project's lookup.
     * @return a subproject provider object suitable for the project lookup
     * @see Project#getLookup
     */
    public SubprojectProvider createSubprojectProvider() {
        return new SubprojectProviderImpl(this);
    }
    
    /**
     * Access from SubprojectProviderImpl.
     */
    RakeProjectHelper getRakeProjectHelper() {
        return h;
    }
    
    /**Tries to fix references after copy/rename/move operation on the project.
     * Handles relative/absolute paths.
     *
     * @param originalPath the project folder of the original project
     * @see org.netbeans.spi.project.CopyOperationImplementation
     * @see org.netbeans.spi.project.MoveOperationImplementation
     * @since 1.9
     */
    public void fixReferences(File originalPath) {
        LOGGER.fine("Fixing refs for: " + originalPath);

        String[] prefixesToFix = new String[] {"file.reference.", "project."};
        EditableProperties pub  = h.getProperties(RakeProjectHelper.PROJECT_PROPERTIES_PATH);
        EditableProperties priv = h.getProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH);
        
        File projectDir = FileUtil.toFile(h.getProjectDirectory());
        
        List<String> pubRemove = new ArrayList<String>();
        List<String> privRemove = new ArrayList<String>();
        Map<String,String> pubAdd = new HashMap<String,String>();
        Map<String,String> privAdd = new HashMap<String,String>();
        
        for (Map.Entry<String,String> e : pub.entrySet()) {
            String    key  = e.getKey();
            boolean   cont = false;
            
            for (String prefix : prefixesToFix) {
                if (key.startsWith(prefix)) {
                    cont = true;
                    break;
                }
            }
            if (!cont)
                continue;
            
            // #151648: do not try to fix references defined via property
            String value = e.getValue();
            if (value.startsWith("${")) { // NOI18N
                continue;
            }
            
            File absolutePath = FileUtil.normalizeFile(PropertyUtils.resolveFile(originalPath, value));
            
            //TODO: extra base dir relativization:

        //mkleint: removed CollocationQuery.areCollocated() reference
        // when AlwaysRelativeCQI gets removed the condition resolves to false more frequently.
        // that might not be desirable.
            String rel = PropertyUtils.relativizeFile(projectDir, absolutePath);
            if (rel == null) {
                pubRemove.add(key);
                privAdd.put(key, absolutePath.getAbsolutePath());
            }
        }
        
        for (Map.Entry<String,String> e : pub.entrySet()) {
            String    key  = e.getKey();
            boolean   cont = false;
            
            for (String prefix : prefixesToFix) {
                if (key.startsWith(prefix)) {
                    cont = true;
                    break;
                }
            }
            if (!cont)
                continue;
            
            // #151648: do not try to fix references defined via property
            String value = e.getValue();
            if (value.startsWith("${")) { // NOI18N
                continue;
            }
            
            File absolutePath = FileUtil.normalizeFile(PropertyUtils.resolveFile(originalPath, value));
            
            if (absolutePath.getAbsolutePath().startsWith(originalPath.getAbsolutePath())) {
                //#65141: in private.properties, a full path into originalPath may be given, fix:
                String relative = PropertyUtils.relativizeFile(originalPath, absolutePath);

                absolutePath = FileUtil.normalizeFile(new File(projectDir, relative));

                LOGGER.fine("Removing " + key + ", " +
                        "path: " + absolutePath.getAbsolutePath() + ", " +
                        "original path: " + originalPath.getAbsolutePath());
                privRemove.add(key);
                privAdd.put(key, absolutePath.getAbsolutePath());
            }
	    
            //TODO: extra base dir relativization:

        //mkleint: removed CollocationQuery.areCollocated() reference
        // when AlwaysRelativeCQI gets removed the condition resolves to false more frequently.
        // that might not be desirable.
            String rel = PropertyUtils.relativizeFile(projectDir, absolutePath);
            if (rel != null) {
                pubAdd.put(key, rel);
            }
        }
        
        for (String s : pubRemove) {
            pub.remove(s);
        }
        
        for (String s : privRemove) {
            priv.remove(s);
        }
        
        pub.putAll(pubAdd);
        priv.putAll(privAdd);
        
        h.putProperties(RakeProjectHelper.PROJECT_PROPERTIES_PATH, pub);
        h.putProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH, priv);
    }

    /**
     * Copies the given properties to private.properties.
     * @param toCopy the properties to copy; may be <code>null</code> in which
     * case nothing is copied.
     */
    public void copyToPrivateProperties(EditableProperties toCopy) {
        if (toCopy == null) {
            return;
        }
        EditableProperties priv = h.getProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH);
        if (priv == null) {
            return;
        }
        for (Entry<String, String> each : toCopy.entrySet()) {
            priv.put(each.getKey(), each.getValue());
        }
        h.putProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH, priv);
    }
    /**
     * A raw reference descriptor representing a link to a foreign project
     * and some build artifact used from it.
     * This class corresponds directly to what it stored in <code>project.xml</code>
     * to refer to a target in a foreign project.
     * See {@link RakeArtifact} for the precise meaning of several of the fields in this class.
     */
    public static final class RawReference {
        
        private final String foreignProjectName;
        private final String artifactType;
        private URI scriptLocation;
        // introduced in /2 version
        private String newScriptLocation;
        private final String targetName;
        private final String cleanTargetName;
        private final String artifactID;
        private final Properties props;
        
        /**
         * Create a raw reference descriptor.
         * As this is basically just a struct, does no real work.
         * @param foreignProjectName the name of the foreign project (usually its code name)
         * @param artifactType the {@link RakeArtifact#getType type} of the build artifact
         * @param scriptLocation the relative URI to the build script from the project directory
         * @param targetName the Ant target name
         * @param cleanTargetName the Ant clean target name
         * @param artifactID the {@link RakeArtifact#getID ID} of the build artifact
         * @throws IllegalArgumentException if the script location is given an absolute URI
         */
        public RawReference(String foreignProjectName, String artifactType, URI scriptLocation, String targetName, String cleanTargetName, String artifactID) throws IllegalArgumentException {
           this(foreignProjectName, artifactType, scriptLocation, null, targetName, cleanTargetName, artifactID, new Properties());
        }
        
        /**
         * Create a raw reference descriptor.
         * As this is basically just a struct, does no real work.
         * @param foreignProjectName the name of the foreign project (usually its code name)
         * @param artifactType the {@link RakeArtifact#getType type} of the build artifact
         * @param newScriptLocation absolute path to the build script; can contain Ant-like properties
         * @param targetName the Ant target name
         * @param cleanTargetName the Ant clean target name
         * @param artifactID the {@link RakeArtifact#getID ID} of the build artifact
         * @param props optional properties to be used for target execution; never null
         * @throws IllegalArgumentException if the script location is given an absolute URI
         * @since 1.5
         */
        public RawReference(String foreignProjectName, String artifactType, String newScriptLocation, String targetName, String cleanTargetName, String artifactID, Properties props) throws IllegalArgumentException {
           this(foreignProjectName, artifactType, null, newScriptLocation, targetName, cleanTargetName, artifactID, props);
        }
        
        private RawReference(String foreignProjectName, String artifactType, URI scriptLocation, String newScriptLocation, String targetName, String cleanTargetName, String artifactID, Properties props) throws IllegalArgumentException {
            this.foreignProjectName = foreignProjectName;
            this.artifactType = artifactType;
            if (scriptLocation != null && scriptLocation.isAbsolute()) {
                throw new IllegalArgumentException("Cannot use an absolute URI " + scriptLocation + " for script location"); // NOI18N
            }
            this.scriptLocation = scriptLocation;
            this.newScriptLocation = newScriptLocation;
            this.targetName = targetName;
            this.cleanTargetName = cleanTargetName;
            this.artifactID = artifactID;
            this.props = props;
        }
        
        private static final List/*<String>*/ SUB_ELEMENT_NAMES = Arrays.asList(new String[] {
            "foreign-project", // NOI18N
            "artifact-type", // NOI18N
            "script", // NOI18N
            "target", // NOI18N
            "clean-target", // NOI18N
            "id", // NOI18N
        });
        
        /**
         * Create a RawReference by parsing an XML &lt;reference&gt; fragment.
         * @throws IllegalArgumentException if anything is missing or duplicated or malformed etc.
         */
        static RawReference create(Element xml) throws IllegalArgumentException {
            if (REFS_NS.equals(xml.getNamespaceURI())) {
                return create1(xml);
            } else {
                return create2(xml);
            }
        }
        
        private static RawReference create1(Element xml) throws IllegalArgumentException {
            if (!REF_NAME.equals(xml.getLocalName()) || !REFS_NS.equals(xml.getNamespaceURI())) {
                throw new IllegalArgumentException("bad element name: " + xml); // NOI18N
            }
            NodeList nl = xml.getElementsByTagNameNS("*", "*"); // NOI18N
            if (nl.getLength() != 6) {
                throw new IllegalArgumentException("missing or extra data: " + xml); // NOI18N
            }
            String[] values = new String[nl.getLength()];
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element)nl.item(i);
                if (!REFS_NS.equals(el.getNamespaceURI())) {
                    throw new IllegalArgumentException("bad subelement ns: " + el); // NOI18N
                }
                String elName = el.getLocalName();
                int idx = SUB_ELEMENT_NAMES.indexOf(elName);
                if (idx == -1) {
                    throw new IllegalArgumentException("bad subelement name: " + elName); // NOI18N
                }
                String val = XMLUtil.findText(el);
                if (val == null) {
                    throw new IllegalArgumentException("empty subelement: " + el); // NOI18N
                }
                if (values[idx] != null) {
                    throw new IllegalArgumentException("duplicate " + elName + ": " + values[idx] + " and " + val); // NOI18N
                }
                values[idx] = val;
            }
            assert !Arrays.asList(values).contains(null);
            URI scriptLocation = URI.create(values[2]); // throws IllegalArgumentException
            return new RawReference(values[0], values[1], scriptLocation, values[3], values[4], values[5]);
        }
        
        private static RawReference create2(Element xml) throws IllegalArgumentException {
            if (!REF_NAME.equals(xml.getLocalName()) || !REFS_NS2.equals(xml.getNamespaceURI())) {
                throw new IllegalArgumentException("bad element name: " + xml); // NOI18N
            }
            List nl = XMLUtil.findSubElements(xml);
            if (nl.size() < 6) {
                throw new IllegalArgumentException("missing or extra data: " + xml); // NOI18N
            }
            String[] values = new String[6];
            for (int i = 0; i < 6; i++) {
                Element el = (Element)nl.get(i);
                if (!REFS_NS2.equals(el.getNamespaceURI())) {
                    throw new IllegalArgumentException("bad subelement ns: " + el); // NOI18N
                }
                String elName = el.getLocalName();
                int idx = SUB_ELEMENT_NAMES.indexOf(elName);
                if (idx == -1) {
                    throw new IllegalArgumentException("bad subelement name: " + elName); // NOI18N
                }
                String val = XMLUtil.findText(el);
                if (val == null) {
                    throw new IllegalArgumentException("empty subelement: " + el); // NOI18N
                }
                if (values[idx] != null) {
                    throw new IllegalArgumentException("duplicate " + elName + ": " + values[idx] + " and " + val); // NOI18N
                }
                values[idx] = val;
            }
            Properties props = new Properties();
            if (nl.size() == 7) {
                Element el = (Element)nl.get(6);
                if (!REFS_NS2.equals(el.getNamespaceURI())) {
                    throw new IllegalArgumentException("bad subelement ns: " + el); // NOI18N
                }
                if (!"properties".equals(el.getLocalName())) { // NOI18N
                    throw new IllegalArgumentException("bad subelement. expected 'properties': " + el); // NOI18N
                }
                for (Element el2 : XMLUtil.findSubElements(el)) {
                    String key = el2.getAttribute("name");
                    String value = XMLUtil.findText(el2);
                    // #53553: NPE
                    if (value == null) {
                        value = ""; // NOI18N
                    }
                    props.setProperty(key, value);
                }
            }
            assert !Arrays.asList(values).contains(null);
            return new RawReference(values[0], values[1], values[2], values[3], values[4], values[5], props);
        }
        
        /**
         * Write a RawReference as an XML &lt;reference&gt; fragment.
         */
        Element toXml(String namespace, Document ownerDocument) {
            Element el = ownerDocument.createElementNS(namespace, REF_NAME);
            String[] values = {
                foreignProjectName,
                artifactType,
                newScriptLocation != null ? newScriptLocation : scriptLocation.toString(),
                targetName,
                cleanTargetName,
                artifactID,
            };
            for (int i = 0; i < 6; i++) {
                Element subel = ownerDocument.createElementNS(namespace, (String)SUB_ELEMENT_NAMES.get(i));
                subel.appendChild(ownerDocument.createTextNode(values[i]));
                el.appendChild(subel);
            }
            if (props.keySet().size() > 0) {
                assert namespace.equals(REFS_NS2) : "can happen only in /2"; // NOI18N
                Element propEls = ownerDocument.createElementNS(namespace, "properties"); // NOI18N
                el.appendChild(propEls);
                for (String key : new TreeSet<String>(NbCollections.checkedSetByFilter(props.keySet(), String.class, true))) {
                    Element propEl = ownerDocument.createElementNS(namespace, "property"); // NOI18N
                    propEl.appendChild(ownerDocument.createTextNode(props.getProperty(key)));
                    propEl.setAttribute("name", key); // NOI18N
                    propEls.appendChild(propEl);
                }
            }
            return el;
        }
        
        private String getNS() {
            if (newScriptLocation != null) {
                return REFS_NS2;
            } else {
                return REFS_NS;
            }
        }
        
        /**
         * Get the name of the foreign project as referred to from this project.
         * Usually this will be the code name of the foreign project, but it may
         * instead be a uniquified name.
         * The name can be used in project properties and the build script to refer
         * to the foreign project from among subprojects.
         * @return the foreign project name
         */
        public String getForeignProjectName() {
            return foreignProjectName;
        }
        
        /**
         * Get the type of the foreign project's build artifact.
         * For example, <a href="@org-netbeans-modules-java-project@/org/netbeans/modules/gsfpath/api/project/JavaProjectConstants.html#ARTIFACT_TYPE_JAR"><code>JavaProjectConstants.ARTIFACT_TYPE_JAR</code></a>.
         * @return the artifact type
         */
        public String getArtifactType() {
            return artifactType;
        }
        
        /**
         * Get the location of the foreign project's build script relative to the
         * project directory.
         * This is the script which would be called to build the desired artifact.
         * @return the script location
         * @deprecated use {@link #getScriptLocationValue} instead; may return null now
         */
        @Deprecated
        public URI getScriptLocation() {
            return scriptLocation;
        }
        
        /**
         * Get absolute path location of the foreign project's build script.
         * This is the script which would be called to build the desired artifact.
         * @return absolute path possibly containing Ant properties
         */
        public String getScriptLocationValue() {
            if (newScriptLocation != null) {
                return newScriptLocation;
            } else {
                return "${project."+foreignProjectName+"}/"+scriptLocation.toString();
            }
        }
        
        /**
         * Get the Ant target name to build the artifact.
         * @return the target name
         */
        public String getTargetName() {
            return targetName;
        }
        
        /**
         * Get the Ant target name to clean the artifact.
         * @return the clean target name
         */
        public String getCleanTargetName() {
            return cleanTargetName;
        }
        
        /**
         * Get the ID of the foreign project's build artifact.
         * See also {@link RakeArtifact#getID}.
         * @return the artifact identifier
         */
        public String getID() {
            return artifactID;
        }
        
        public Properties getProperties() {
            return props;
        }
        
        /**
         * Attempt to convert this reference to a live artifact object.
         * This involves finding the referenced foreign project on disk
         * (among standard project and private properties) and asking it
         * for the artifact named by the given target.
         * Given that object, you can find important further information
         * such as the location of the actual artifact on disk.
         * <p>
         * Note that non-key attributes of the returned artifact (i.e.
         * type, script location, and clean target name) might not match
         * those in this raw reference.
         * <p>
         * Acquires read access.
         * @param helper an associated reference helper used to resolve the foreign
         *               project location
         * @return the actual Ant artifact object, or null if it could not be located
         */
        public RakeArtifact toRakeArtifact(final ReferenceHelper helper) {
            return ProjectManager.mutex().readAccess(new Mutex.Action<RakeArtifact>() {
                public RakeArtifact run() {
                    RakeProjectHelper h = helper.h;
                    String path = helper.eval.getProperty("project." + foreignProjectName); // NOI18N
                    if (path == null) {
                        // Undefined foreign project.
                        return null;
                    }
                    FileObject foreignProjectDir = h.resolveFileObject(path);
                    if (foreignProjectDir == null) {
                        // Nonexistent foreign project dir.
                        return null;
                    }
                    Project p;
                    try {
                        p = ProjectManager.getDefault().findProject(foreignProjectDir);
                    } catch (IOException e) {
                        // Could not load it.
                        ErrorManager.getDefault().notify(ErrorManager.INFORMATIONAL, e);
                        return null;
                    }
                    if (p == null) {
                        // Was not a project dir.
                        return null;
                    }
                    return RakeArtifactQuery.findArtifactByID(p, artifactID);
                }
            });
        }
        
        private void upgrade() {
            assert newScriptLocation == null && scriptLocation != null : "was already upgraded "+this;
            newScriptLocation = "${project."+foreignProjectName+"}/" +scriptLocation.toString(); // NOI18N
            scriptLocation = null;
        }
        
        public String toString() {
            return "ReferenceHelper.RawReference<" + foreignProjectName + "," + 
                artifactType + "," + newScriptLocation != null ? newScriptLocation : scriptLocation + 
                "," + targetName + "," + cleanTargetName + "," + artifactID + ">"; // NOI18N
        }
        
    }
    
}
