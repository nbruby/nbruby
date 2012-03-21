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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2008 Sun
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

package org.netbeans.modules.ruby.railsprojects;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.netbeans.modules.ruby.spi.project.support.rake.RakeProjectHelper;
import org.netbeans.spi.project.CopyOperationImplementation;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.MoveOperationImplementation;
import org.openide.filesystems.FileObject;
import org.openide.util.EditableProperties;
import org.openide.util.NbBundle;

/**
 * @author Jan Lahoda
 */
public class RailsProjectOperations implements DeleteOperationImplementation, CopyOperationImplementation, MoveOperationImplementation {
    
    private final RailsProject project;
    // see RubyProjectOperations#origPrivateProperties
    private static EditableProperties origPrivateProperties;
    
    public RailsProjectOperations(RailsProject project) {
        this.project = project;
    }
    
    private static void addFile(FileObject projectDirectory, String fileName, Set<FileObject> result) {
        FileObject file = projectDirectory.getFileObject(fileName);
        if (file != null) {
            result.add(file);
        }
    }
    
    public List<FileObject> getMetadataFiles() {
        FileObject projectDirectory = project.getProjectDirectory();
        Set<FileObject> files = new LinkedHashSet<FileObject>();
        
        addFile(projectDirectory, "nbproject", files); // NOI18N
        addFile(projectDirectory, "build.xml", files); // NOI18N
        addFile(projectDirectory, "xml-resources", files); //NOI18N
        addFile(projectDirectory, "catalog.xml", files); //NOI18N

        return new ArrayList<FileObject>(files);
    }
    
    public List<FileObject> getDataFiles() {
        Set<FileObject> files = new LinkedHashSet<FileObject>();
        files.addAll(Arrays.asList(project.getSourceRoots().getRoots()));
        files.addAll(Arrays.asList(project.getTestSourceRoots().getRoots()));
        // additional dirs
        addFile(project.getProjectDirectory(), "app", files); // NOI18N
        addFile(project.getProjectDirectory(), "tmp", files); // NOI18N
        addFile(project.getProjectDirectory(), "test", files); // NOI18N
        // additional files
        addFile(project.getProjectDirectory(), "README", files); // NOI18N
        addFile(project.getProjectDirectory(), "Rakefile", files); // NOI18N
        addFile(project.getProjectDirectory(), "Capfile", files); // NOI18N
        return new ArrayList<FileObject>(files);
    }
    
    public void notifyDeleting() throws IOException {
        project.notifyDeleting();
    }
    
    public void notifyDeleted() throws IOException {
        project.getRakeProjectHelper().notifyDeleted();
    }
    
    public void notifyCopying() {
        origPrivateProperties = project.getRakeProjectHelper().getProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH);
    }
    
    public void notifyCopied(Project original, File originalPath, String nueName) {
        if (original == null) {
            //do nothing for the original project.
            return ;
        }
        
        project.getReferenceHelper().fixReferences(originalPath);
        project.setName(nueName);
        copyPrivateProps();
    }
    
    public void notifyMoving() throws IOException {
        origPrivateProperties = project.getRakeProjectHelper().getProperties(RakeProjectHelper.PRIVATE_PROPERTIES_PATH);
        if (!this.project.getUpdateHelper().requestSave()) {
            throw new IOException (NbBundle.getMessage(RailsProjectOperations.class,
                "MSG_OldProjectMetadata"));
        }
        notifyDeleting();
    }
    
    public void notifyMoved(Project original, File originalPath, String nueName) {
        if (original == null) {
            project.getRakeProjectHelper().notifyDeleted();
            return ;
        }                
        
        project.setName(nueName);        
        project.getReferenceHelper().fixReferences(originalPath);
        copyPrivateProps();
    }

    private void copyPrivateProps() {
        project.getReferenceHelper().copyToPrivateProperties(origPrivateProperties);
        origPrivateProperties = null;
    }
    
}
