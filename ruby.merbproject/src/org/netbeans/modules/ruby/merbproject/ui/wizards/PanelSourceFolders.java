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

package org.netbeans.modules.ruby.merbproject.ui.wizards;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.spi.project.ui.templates.support.Templates;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

//XXX There should be a way how to add nonexistent test dir

/**
 * Sets up name and location for new Java project from existing sources.
 * @author Tomas Zezula et al.
 */
public class PanelSourceFolders extends SettingsPanel implements PropertyChangeListener {

    private final Panel firer;
    private WizardDescriptor wizardDescriptor;

    // key to action value that influence folder list current directory
    public static final String INITIAL_SOURCE_ROOT = "EXISTING_SOURCES_CURRENT_DIRECTORY"; // NOI18N

    /** Creates new form PanelSourceFolders */
    PanelSourceFolders(Panel panel) {
        this.firer = panel;
        initComponents();
        this.setName(NbBundle.getMessage(PanelSourceFolders.class, "LAB_ConfigureSourceRoots"));
        this.putClientProperty("NewProjectWizard_Title", NbBundle.getMessage(PanelSourceFolders.class,"TXT_JavaExtSourcesProjectLocation")); // NOI18N
        this.getAccessibleContext().setAccessibleName(NbBundle.getMessage(PanelSourceFolders.class,"AN_PanelSourceFolders"));
        this.getAccessibleContext().setAccessibleDescription(NbBundle.getMessage(PanelSourceFolders.class,"AD_PanelSourceFolders"));
        this.sourcePanel.addPropertyChangeListener (this);
        this.testsPanel.addPropertyChangeListener(this);
        ((FolderList)this.sourcePanel).setRelatedFolderList((FolderList)this.testsPanel);
        ((FolderList)this.testsPanel).setRelatedFolderList((FolderList)this.sourcePanel);
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if (FolderList.PROP_FILES.equals(evt.getPropertyName())) {
            this.dataChanged();
        }
        else if (FolderList.PROP_LAST_USED_DIR.equals (evt.getPropertyName())) {
            if (evt.getSource() == this.sourcePanel) {                
                ((FolderList)this.testsPanel).setLastUsedDir 
                        ((File)evt.getNewValue());
            }
            else if (evt.getSource() == this.testsPanel) {
                ((FolderList)this.sourcePanel).setLastUsedDir 
                        ((File)evt.getNewValue());
            }
        }
    }

    private void dataChanged () {
        this.firer.fireChangeEvent();
    }


    void read (WizardDescriptor settings) {
        this.wizardDescriptor = settings;
        File projectLocation = (File) settings.getProperty ("projdir");         //NOI18N
        ((FolderList)this.sourcePanel).setProjectFolder(projectLocation);
        ((FolderList)this.testsPanel).setProjectFolder(projectLocation);
        File[] srcRoot = (File[]) settings.getProperty ("sourceRoot");          //NOI18N
        assert srcRoot != null : "sourceRoot property must be initialized!" ;   //NOI18N
        ((FolderList)this.sourcePanel).setFiles(srcRoot);
        File[] testRoot = (File[]) settings.getProperty ("testRoot");           //NOI18N
        assert testRoot != null : "testRoot property must be initialized!";     //NOI18N
        ((FolderList)this.testsPanel).setFiles (testRoot);

        // #58489 honor existing source folder
        File currentDirectory = null;
        FileObject folder = Templates.getExistingSourcesFolder(wizardDescriptor);
        if (folder != null) {
            currentDirectory = FileUtil.toFile(folder);
        }        
        if (currentDirectory != null && currentDirectory.isDirectory()) {       
            ((FolderList)sourcePanel).setLastUsedDir(currentDirectory);
            ((FolderList)testsPanel).setLastUsedDir(currentDirectory);
        }
    }

    void store (WizardDescriptor settings) {
        File[] sourceRoots = ((FolderList)this.sourcePanel).getFiles();
        File[] testRoots = ((FolderList)this.testsPanel).getFiles();
        settings.putProperty ("sourceRoot",sourceRoots);    //NOI18N
        settings.putProperty("testRoot",testRoots);      //NOI18N
    }
    
    boolean valid (WizardDescriptor settings) {
        File projectLocation = (File) settings.getProperty ("projdir");  //NOI18N
        File[] sourceRoots = ((FolderList)this.sourcePanel).getFiles();
        File[] testRoots = ((FolderList)this.testsPanel).getFiles();
        String result = checkValidity (projectLocation, sourceRoots, testRoots);
        if (result == null) {
            wizardDescriptor.putProperty( WizardDescriptor.PROP_ERROR_MESSAGE,"");   //NOI18N
            return true;
        }
        else {
            wizardDescriptor.putProperty( WizardDescriptor.PROP_ERROR_MESSAGE,result);       //NOI18N
            return false;
        }
    }

    static String checkValidity (final File projectLocation, final File[] sources, final File[] tests ) {
        String ploc = projectLocation.getAbsolutePath ();        
        for (int i=0; i<sources.length;i++) {
            if (!sources[i].isDirectory() || !sources[i].canRead()) {
                return MessageFormat.format(NbBundle.getMessage(PanelSourceFolders.class,"MSG_IllegalSources"),
                        new Object[] {sources[i].getAbsolutePath()});
            }
            String sloc = sources[i].getAbsolutePath ();
            if (ploc.equals (sloc) || ploc.startsWith (sloc + File.separatorChar)) {
                return NbBundle.getMessage(PanelSourceFolders.class,"MSG_IllegalProjectFolder");
            }
        }
        for (int i=0; i<tests.length; i++) {
            if (!tests[i].isDirectory() || !tests[i].canRead()) {
                return MessageFormat.format(NbBundle.getMessage(PanelSourceFolders.class,"MSG_IllegalTests"),
                        new Object[] {sources[i].getAbsolutePath()});
            }
            String tloc = tests[i].getAbsolutePath();
            if (ploc.equals(tloc) || ploc.startsWith(tloc + File.separatorChar)) {
                return NbBundle.getMessage(PanelSourceFolders.class,"MSG_IllegalProjectFolder");
            }            
        }
        return null;
    }
    
    void validate (WizardDescriptor d) throws WizardValidationException {
        // sources root
        searchClassFiles (((FolderList)this.sourcePanel).getFiles());
        // test root, not asked in issue 48198
        //searchClassFiles (FileUtil.toFileObject (FileUtil.normalizeFile(new File (tests.getText ()))));
    }
    
    private static void findClassFiles(File folder, List<File> files) {
        File[] kids = folder.listFiles();
        if (kids == null) {
            return;
        }
        for (File kid : kids) {
            if (kid.isFile() && kid.getName().endsWith(".class")) {
                files.add(kid);
            } else if (kid.isDirectory()) {
                findClassFiles(kid, files);
            }
        }
    }
    
    private void searchClassFiles (File[] folders) throws WizardValidationException {
        List<File> classFiles = new ArrayList<File>();
        for (File folder : folders) {
            findClassFiles(folder, classFiles);
        }
        if (!classFiles.isEmpty()) {
            JButton DELETE_OPTION = new JButton (NbBundle.getMessage (PanelSourceFolders.class, "TXT_DeleteOption")); // NOI18N
            JButton KEEP_OPTION = new JButton (NbBundle.getMessage (PanelSourceFolders.class, "TXT_KeepOption")); // NOI18N
            JButton CANCEL_OPTION = new JButton (NbBundle.getMessage (PanelSourceFolders.class, "TXT_CancelOption")); // NOI18N
            DELETE_OPTION.getAccessibleContext().setAccessibleDescription (NbBundle.getMessage (PanelSourceFolders.class, "AD_DeleteOption"));
            KEEP_OPTION.getAccessibleContext().setAccessibleDescription (NbBundle.getMessage (PanelSourceFolders.class, "AD_KeepOption"));
            CANCEL_OPTION.getAccessibleContext().setAccessibleDescription (NbBundle.getMessage (PanelSourceFolders.class, "AD_CancelOption"));
            NotifyDescriptor desc = new NotifyDescriptor (
                    NbBundle.getMessage (PanelSourceFolders.class, "MSG_FoundClassFiles"), // NOI18N
                    NbBundle.getMessage (PanelSourceFolders.class, "MSG_FoundClassFiles_Title"), // NOI18N
                    NotifyDescriptor.YES_NO_CANCEL_OPTION,
                    NotifyDescriptor.QUESTION_MESSAGE,
                    new Object[] {DELETE_OPTION, KEEP_OPTION, CANCEL_OPTION},
                    DELETE_OPTION
                    );
            Object result = DialogDisplayer.getDefault().notify(desc);
            if (DELETE_OPTION.equals (result)) {
                for (File f : classFiles) {
                    f.delete(); // ignore if fails
                }
            } else if (!KEEP_OPTION.equals (result)) {
                // cancel, back to wizard
                throw new WizardValidationException (this.sourcePanel, "", ""); // NOI18N
            }
        }
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        description = new javax.swing.JLabel();
        sourcePanel = createSourcePanel();
        testsPanel = createTestPanel();

        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(description, org.openide.util.NbBundle.getMessage(PanelSourceFolders.class, "LBL_SourceDirectoriesLabel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(description, gridBagConstraints);
        description.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getBundle(PanelSourceFolders.class).getString("ACSN_jLabel3")); // NOI18N
        description.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getBundle(PanelSourceFolders.class).getString("ACSD_jLabel3")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.45;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(sourcePanel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 0.45;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(testsPanel, gridBagConstraints);

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PanelSourceFolders.class, "ACSN_PanelSourceFolders")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelSourceFolders.class, "ACSD_PanelSourceFolders")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel description;
    private javax.swing.JPanel sourcePanel;
    private javax.swing.JPanel testsPanel;
    // End of variables declaration//GEN-END:variables

    
    static class Panel implements WizardDescriptor.ValidatingPanel {
        
        private ArrayList<ChangeListener> listeners;        
        private PanelSourceFolders component;
        private WizardDescriptor settings;
        
        public synchronized void removeChangeListener(ChangeListener l) {
            if (this.listeners == null) {
                return;
            }
            this.listeners.remove(l);
        }

        public void addChangeListener(ChangeListener l) {
            if (this.listeners == null) {
                this.listeners = new ArrayList<ChangeListener>();
            }
            this.listeners.add (l);
        }

        public void readSettings(Object settings) {
            this.settings = (WizardDescriptor) settings;
            this.component.read (this.settings);
            // XXX hack, TemplateWizard in final setTemplateImpl() forces new wizard's title
            // this name is used in NewProjectWizard to modify the title
            Object substitute = component.getClientProperty ("NewProjectWizard_Title"); // NOI18N
            if (substitute != null) {
                this.settings.putProperty ("NewProjectWizard_Title", substitute); // NOI18N
            }
        }

        public void storeSettings(Object settings) {
            this.component.store (this.settings);
        }
        
        public void validate() throws WizardValidationException {
            this.component.validate(this.settings);
        }
                
        public boolean isValid() {
            return this.component.valid (this.settings);
        }

        public synchronized java.awt.Component getComponent() {
            if (this.component == null) {
                this.component = new PanelSourceFolders (this);
            }
            return this.component;
        }

        public HelpCtx getHelp() {
            return new HelpCtx (PanelSourceFolders.class);
        }        
        
        private void fireChangeEvent () {
           Iterator it = null;
           synchronized (this) {
               if (this.listeners == null) {
                   return;
               }
               it = ((ArrayList) this.listeners.clone()).iterator();
           }
           ChangeEvent event = new ChangeEvent (this);
           while (it.hasNext()) {
               ((ChangeListener)it.next()).stateChanged(event);
           }
        }
                
    }
    
    private JPanel createSourcePanel() {
        return new FolderList(
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_SourceRoots"),
                NbBundle.getMessage(PanelSourceFolders.class, "AD_SourceRoots"),
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_AddSourceRoot"),
                NbBundle.getMessage(PanelSourceFolders.class, "AD_AddSourceFolder"),
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_AddSourceFolder"),
                NbBundle.getMessage(PanelSourceFolders.class, "AD_RemoveSourceFolder"),
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_RemoveSourceFolder"));
    }

    private JPanel createTestPanel() {
        return new FolderList(
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_TestRoots"),
                NbBundle.getMessage(PanelSourceFolders.class, "AD_TestRoots"),
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_AddTestRoot"),
                NbBundle.getMessage(PanelSourceFolders.class, "AD_AddTestFolder"),
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_AddTestFolder"),
                NbBundle.getMessage(PanelSourceFolders.class, "AD_RemoveTestFolder"),
                NbBundle.getMessage(PanelSourceFolders.class, "CTL_RemoveTestFolder"));
    }
}
