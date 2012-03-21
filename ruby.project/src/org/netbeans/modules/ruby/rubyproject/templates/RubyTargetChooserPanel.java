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

package org.netbeans.modules.ruby.rubyproject.templates;

import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.ruby.RubyUtils;
import org.netbeans.modules.ruby.rubyproject.templates.NewRubyFileWizardIterator.Type;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.ErrorManager;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;

/**
 * @author Petr Hrebejk
 */
public final class RubyTargetChooserPanel implements WizardDescriptor.Panel<WizardDescriptor>, ChangeListener {    

    private static final String FOLDER_TO_DELETE = "folderToDelete"; // NOI18N

    //private final SpecificationVersion JDK_14 = new SpecificationVersion ("1.4");   //NOI18N
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();
    private RubyTargetChooserPanelGUI gui;
    private final WizardDescriptor.Panel<WizardDescriptor> bottomPanel;
    private WizardDescriptor wizard;

    private final Project project;
    private final SourceGroup folders[];
    private final Type type;
    
    public RubyTargetChooserPanel( Project project, SourceGroup folders[], WizardDescriptor.Panel<WizardDescriptor> bottomPanel, Type type) {
        this.project = project;
        this.folders = folders;
        this.bottomPanel = bottomPanel;
        this.type = type;
        if ( bottomPanel != null ) {
            bottomPanel.addChangeListener( this );
        }
    }

    public Component getComponent() {
        if (gui == null) {
            gui = new RubyTargetChooserPanelGUI(project, folders, bottomPanel == null ? null : bottomPanel.getComponent(), type);
            gui.addChangeListener(this);
        }
        return gui;
    }

    public HelpCtx getHelp() {
        if ( bottomPanel != null ) {
            HelpCtx bottomHelp = bottomPanel.getHelp();
            if ( bottomHelp != null ) {
                return bottomHelp;
            }
        }
        
        //XXX
        return null;
        
    }

    public boolean isValid() {              
        if (gui == null || gui.getTargetName() == null) {
           setErrorMessage( null );
           return false;
        }        

        setLocalizedErrorMessage(null);
        
        if (type == Type.SPEC) {
            if (gui.getClassName() == null || !RubyUtils.isValidConstantName(gui.getClassName())) {
                setErrorMessage("ERR_RubyTargetChooser_InvalidClass"); // NOI18N
                return false;
            }
            String msg = RubyUtils.getIdentifierWarning(gui.getClassName(), 0);
            if (msg != null) {
                setLocalizedErrorMessage(msg); // warning only, don't return false
            }
        } else if (type == Type.CLASS || type == Type.MODULE ||
            type == Type.TEST) {
            if (type == Type.CLASS || type == Type.TEST) {
                if (gui.getClassName() == null || !RubyUtils.isValidConstantName(gui.getClassName())) {
                    setErrorMessage("ERR_RubyTargetChooser_InvalidClass"); // NOI18N
                    return false;
                }
                String msg = RubyUtils.getIdentifierWarning(gui.getClassName(), 0);
                if (msg != null) {
                    setLocalizedErrorMessage(msg); // warning only, don't return false
                }
                String superclass = gui.getExtends();
                if (superclass != null && superclass.length() > 0) {
                    String[] mods = superclass.split("::"); // NOI18N
                    for (String mod : mods) {
                        if (!RubyUtils.isValidConstantName(mod)) {
                            setErrorMessage("ERR_RubyTargetChooser_InvalidSuperclass"); // NOI18N
                            return false;
                        }
                        msg = RubyUtils.getIdentifierWarning(mod, 0);
                        if (msg != null) {
                            setLocalizedErrorMessage(msg); // warning only, don't return false
                        }
                    }
                }
            }
            if (type == Type.MODULE) {
                if (gui.getClassName() == null || !RubyUtils.isValidConstantName(gui.getClassName())) {
                    setErrorMessage("ERR_RubyTargetChooser_InvalidModule"); // NOI18N
                    return false;
                }
                String msg = RubyUtils.getIdentifierWarning(gui.getClassName(), 0);
                if (msg != null) {
                    setLocalizedErrorMessage(msg); // warning only, don't return false
                }
            }
            String in = gui.getModuleName();
            if (in != null && in.length() > 0) {
                String[] mods = in.split("::"); // NOI18N
                for (String mod : mods) {
                    if (!RubyUtils.isValidConstantName(mod)) {
                        setErrorMessage("ERR_RubyTargetChooser_InvalidInModule"); // NOI18N
                        return false;
                    }
                    String msg = RubyUtils.getIdentifierWarning(mod, 0);
                    if (msg != null) {
                        setLocalizedErrorMessage(msg); // warning only, don't return false
                    }
                }
            }
        }
        
        if (!isValidFileName(gui.getTargetName())) {
            setErrorMessage( "ERR_RubyTargetChooser_InvalidFilename" ); // NOI18N
            return false;
        }
        
        // check if the file name can be created
        FileObject template = Templates.getTemplate( wizard );

        boolean returnValue=true;
        String errorMessage = canUseFileName (gui.getTargetFolder(), gui.getTargetName(), template.getExt ());        
        if (gui != null && errorMessage != null) {
            setLocalizedErrorMessage (errorMessage);
        }
        if (errorMessage!=null) {
            returnValue = false;
        }                
        
        // this enables to display error messages from the bottom panel
        // Nevertheless, the previous error messages have bigger priorities 
        if (returnValue && bottomPanel != null && !bottomPanel.isValid()) {
            return false;
        }
        
        return returnValue;
    }

    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }

    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }

    private void fireChange() {
        ChangeEvent e = new ChangeEvent(this);
        Iterator it = listeners.iterator();
        while (it.hasNext()) {
            ((ChangeListener)it.next()).stateChanged(e);
        }
    }

    public void readSettings(WizardDescriptor settings ) {
        wizard = settings;
        
        if ( gui != null ) {
            // Try to preselect a folder
            FileObject preselectedFolder = Templates.getTargetFolder( wizard );            
            // Init values
            gui.initValues( Templates.getTemplate( wizard ), preselectedFolder );
        }
        
        if ( bottomPanel != null ) {
            bottomPanel.readSettings( settings );
        }        
        
        // XXX hack, TemplateWizard in final setTemplateImpl() forces new wizard's title
        // this name is used in NewFileWizard to modify the title
        if (gui != null) {
            Object substitute = gui.getClientProperty ("NewFileWizard_Title"); // NOI18N
            if (substitute != null) {
                wizard.putProperty ("NewFileWizard_Title", substitute); // NOI18N
            }
        }
    }

    private String pathToSpecHelper(WizardDescriptor wizard) {
        FileObject specHelper = project.getProjectDirectory().getFileObject("spec/spec_helper.rb");//NOI18N
        if (specHelper == null) {
            return null;
        }
        FileObject targetFolder = getTargetFolderFromGUI(wizard);
        if (!FileUtil.isParentOf(specHelper.getParent(), targetFolder)
                && !specHelper.getParent().equals(targetFolder)) {
            return null;
        }
        String path = "/"; //NOI18N
        FileObject parent = targetFolder;
        while (!parent.equals(specHelper.getParent())) {
            path += "../"; //NOI18N
            parent = parent.getParent();
        }
        return "File.expand_path(File.dirname(__FILE__) + '" + path + specHelper.getName() + "')"; //NOI18N
    }

    public void storeSettings(WizardDescriptor settings) { 
        Object value = settings.getValue();
        if (WizardDescriptor.PREVIOUS_OPTION.equals(value) || WizardDescriptor.CANCEL_OPTION.equals(value) ||
                WizardDescriptor.CLOSED_OPTION.equals(value)) {
            return;
        }
        if( isValid() ) {
            if ( bottomPanel != null ) {
                bottomPanel.storeSettings( settings );
            }
            Templates.setTargetFolder(settings, getTargetFolderFromGUI(settings));
            Templates.setTargetName(settings, gui.getTargetName());
            
            if (type == Type.SPEC) {
                wizard.putProperty("classname", gui.getClassName()); // NOI18N
                String name = RubyUtils.camelToUnderlinedName(gui.getClassName());
                String pathToRequire = pathToSpecHelper(wizard);
                if (pathToRequire == null) {
                    pathToRequire = "'" + name + "'";
                }
                wizard.putProperty("classfile", name); // NOI18N
                // file_to_require includes quoting, classfile not (storing it
                // for users to use).
                wizard.putProperty("file_to_require", pathToRequire); // NOI18N
                wizard.putProperty("classfield", name); // NOI18N
            } else if (type == Type.CLASS || 
                    type == Type.TEST) {
                wizard.putProperty("class", gui.getClassName()); // NOI18N
                String name = RubyUtils.camelToUnderlinedName(gui.getClassName());
                if (name.startsWith("test_")) {
                    name = name.substring("test_".length());
                } else if (name.endsWith("_test")) {
                    name = name.substring(0, name.length()-"_test".length());
                }
                wizard.putProperty("classfile", name); // NOI18N
                wizard.putProperty("module", gui.getModuleName()); // NOI18N
                wizard.putProperty("extend", gui.getExtends()); // NOI18N
            } else if (type == Type.MODULE) {
                // NOTE - even when adding a -module-, we will use the "class" textfield
                // to represent the name of the module, and the "module" text field to represent
                // modules surrounding the current module
                wizard.putProperty("module", gui.getClassName()); // NOI18N
                wizard.putProperty("outermodules", gui.getModuleName()); // NOI18N
            }
        }
        settings.putProperty ("NewFileWizard_Title", null); // NOI18N
        
        if (WizardDescriptor.FINISH_OPTION.equals(value)) {
            wizard.putProperty(FOLDER_TO_DELETE, null);
        }
    }

    public void stateChanged(ChangeEvent e) {
        fireChange();
    }
    
    // Private methods ---------------------------------------------------------
    
    private void setErrorMessage( String key ) {
        if ( key == null ) {
            setLocalizedErrorMessage ( "" ); // NOI18N
        }
        else {
            setLocalizedErrorMessage ( NbBundle.getMessage( RubyTargetChooserPanelGUI.class, key) ); // NOI18N
        }
    }
    
    private void setLocalizedErrorMessage (String message) {
        wizard.putProperty (WizardDescriptor.PROP_ERROR_MESSAGE, message); // NOI18N
    }
    
    private FileObject getTargetFolderFromGUI (WizardDescriptor wd) {
        assert gui != null;
        File file = new File(gui.getTargetFolder());
        FileObject folder = FileUtil.toFileObject(file);
        if ( folder == null ) {
            try {
                folder = FileUtil.createFolder(file);
//                folder = rootFolder;
//                StringTokenizer tk = new StringTokenizer (packageFileName,"/"); //NOI18N
//                String name = null;
//                while (tk.hasMoreTokens()) {
//                    name = tk.nextToken();
//                    FileObject fo = folder.getFileObject (name,"");   //NOI18N
//                    if (fo == null) {
//                        break;
//                    }
//                    folder = fo;
//                }
//                folder = folder.createFolder(name);
//                FileObject toDelete = (FileObject) wd.getProperty(FOLDER_TO_DELETE);
//                if (toDelete == null) {
//                    wd.putProperty(FOLDER_TO_DELETE,folder);
//                }
//                else if (!toDelete.equals(folder)) {
//                    toDelete.delete();
//                    wd.putProperty(FOLDER_TO_DELETE,folder);
//                }
//                while (tk.hasMoreTokens()) {
//                    name = tk.nextToken();
//                    folder = folder.createFolder(name);
//                }
            }
            catch( IOException e ) {
                ErrorManager.getDefault().notify( ErrorManager.INFORMATIONAL, e );
            }
        }
        return folder;
    }
    
    // Nice copy of useful methods (Taken from JavaModule)
    
    static boolean isValidPackageName(String str) {
        if (str.length() > 0 && str.charAt(0) == '.') {
            return false;
        }
        StringTokenizer tukac = new StringTokenizer(str, "."); // NOI18N
        while (tukac.hasMoreTokens()) {
            String token = tukac.nextToken();
            if ("".equals(token)) {
                return false;
            }
            if (!Utilities.isJavaIdentifier(token)) {
                return false;
            }
        }
        return true;
    }
    
    static boolean isValidTypeIdentifier(String ident) {
        return Utilities.isJavaIdentifier(ident);
    }

    static boolean isValidFileName(String ident) {
        if (ident == null || ident.length() == 0) {
            return false;
        }
        
        // TODO - do I want to filter filenames down to anything?
        
        return true;
    }
    
    // helper methods copied from project/ui/ProjectUtilities
    /** Checks if the given file name can be created in the target folder.
     *
     * @param targetFolder target folder (e.g. source group)
     * @param folderName name of the folder relative to target folder
     * @param newObjectName name of created file
     * @param extension extension of created file
     * @return localized error message or null if all right
     */    
    public static String canUseFileName(String folderName, String newObjectName, String extension) {
        String newObjectNameToDisplay = newObjectName;
        if (newObjectName != null) {
            newObjectName = newObjectName.replace ('.', '/'); // NOI18N
        }
        if (extension != null && extension.length () > 0) {
            StringBuffer sb = new StringBuffer ();
            sb.append (newObjectName);
            sb.append ('.'); // NOI18N
            sb.append (extension);
            newObjectName = sb.toString ();
        }
        
        if (extension != null && extension.length () > 0) {
            StringBuffer sb = new StringBuffer ();
            sb.append (newObjectNameToDisplay);
            sb.append ('.'); // NOI18N
            sb.append (extension);
            newObjectNameToDisplay = sb.toString ();
        }
        
        // test whether the selected folder on selected filesystem already exists
        if (folderName == null) {
            return NbBundle.getMessage (RubyTargetChooserPanel.class, "MSG_fs_or_folder_does_not_exist"); // NOI18N
        }
        
        // target filesystem should be writable
        FileObject targetFolder = FileUtil.toFileObject(new File(folderName));
        if (targetFolder == null) {
            return NbBundle.getMessage (RubyTargetChooserPanel.class, "MSG_fs_or_folder_does_not_exist"); // NOI18N
        }

        if (!targetFolder.canWrite()) {
            return NbBundle.getMessage (RubyTargetChooserPanel.class, "MSG_fs_is_readonly"); // NOI18N
        }
        
        
        if (existFileName(targetFolder, "/" + newObjectName)) {
            return NbBundle.getMessage (RubyTargetChooserPanel.class, "MSG_file_already_exist", newObjectNameToDisplay); // NOI18N
        }
        
        // all ok
        return null;
    }

    private static boolean existFileName(FileObject targetFolder, String relFileName) {
        boolean result = false;
        File fileForTargetFolder = FileUtil.toFile(targetFolder);
        if (fileForTargetFolder.exists()) {
            result = new File (fileForTargetFolder, relFileName).exists();
        } else {
            result = targetFolder.getFileObject (relFileName) != null;
        }
        
        return result;
    }
}
