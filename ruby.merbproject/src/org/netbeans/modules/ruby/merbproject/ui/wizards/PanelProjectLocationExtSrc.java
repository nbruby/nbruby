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

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import javax.swing.JFileChooser;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.NbBundle;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.modules.ruby.merbproject.ui.MerbProjectSettings;
import org.netbeans.spi.project.ui.support.ProjectChooser;


/**
 * Sets up source and test folders for new Java project from existing sources.
 * @author Tomas Zezula et al.
 */
public class PanelProjectLocationExtSrc extends SettingsPanel {

    private final PanelConfigureProject firer;
    private WizardDescriptor wizardDescriptor;
    private boolean calculatePF;

    public PanelProjectLocationExtSrc (PanelConfigureProject panel) {
        this.firer = panel;
        initComponents();
        this.projectName.getDocument().addDocumentListener (new DocumentListener (){
            public void changedUpdate(DocumentEvent e) {
                calculateProjectFolder ();
                dataChanged ();
            }

            public void insertUpdate(DocumentEvent e) {
                calculateProjectFolder ();
                dataChanged ();
            }

            public void removeUpdate(DocumentEvent e) {
                calculateProjectFolder ();
                dataChanged ();
            }
        });        
        this.projectLocation.getDocument().addDocumentListener(new DocumentListener () {
            public void changedUpdate(DocumentEvent e) {             
                setCalculateProjectFolder (false);
                dataChanged ();
            }

            public void insertUpdate(DocumentEvent e) {
                setCalculateProjectFolder (false);
                dataChanged ();
            }

            public void removeUpdate(DocumentEvent e) {
                setCalculateProjectFolder (false);
                dataChanged ();
            }
        });
    }

    private synchronized void calculateProjectFolder () {
        if (this.calculatePF) {                        
            File f = ProjectChooser.getProjectsFolder();
            this.projectLocation.setText (f.getAbsolutePath() + File.separator + this.projectName.getText());
            this.calculatePF = true;
        }
    }
    
    private synchronized void setCalculateProjectFolder (boolean value) {
        this.calculatePF = value;
    }

    private void dataChanged () {
        this.firer.fireChangeEvent();
    }


    void read (WizardDescriptor settings) {
        this.wizardDescriptor = settings;
        String projectName = null;
        File projectLocation = (File) settings.getProperty ("projdir");  //NOI18N
        if (projectLocation == null) {
            projectLocation = ProjectChooser.getProjectsFolder();                
            int index = MerbProjectSettings.getDefault().getNewProjectCount();
            String formater = NbBundle.getMessage(PanelProjectLocationExtSrc.class,"TXT_JavaProject");
            File file;
            do {
                index++;
                projectName = MessageFormat.format(formater, index);
                file = new File(projectLocation, projectName);
            } while (file.exists());
            settings.putProperty(NewMerbProjectWizardIterator.PROP_NAME_INDEX, index);
            this.projectLocation.setText(projectLocation.getAbsolutePath());
            this.setCalculateProjectFolder(true);
        }
        else {
            projectName = (String) settings.getProperty ("name"); //NOI18N
            boolean tmpFlag = this.calculatePF;
            this.projectLocation.setText (projectLocation.getAbsolutePath());
            this.setCalculateProjectFolder(tmpFlag);
        }
        this.projectName.setText (projectName);                
        this.projectName.selectAll();
    }

    void store (WizardDescriptor settings) {        
        settings.putProperty ("name",this.projectName.getText()); // NOI18N
        File projectsDir = new File(this.projectLocation.getText());
        settings.putProperty ("projdir", projectsDir); // NOI18N        
    }
    
    boolean valid (WizardDescriptor settings) {
        String result = checkValidity (this.projectName.getText(), this.projectLocation.getText());
        if (result == null) {
            wizardDescriptor.putProperty( WizardDescriptor.PROP_ERROR_MESSAGE,"");   //NOI18N
            
            // 103648: make sure users don't accidentally create Ruby projects from Rails sources
            // Like ZenTest, check for "config/environment.rb" to see if this looks like it
            // might be a Rails project
            File environment = new File(this.projectLocation.getText(), "config" + File.separator + "environment.rb");
            if (environment.exists()) {
                String message = NbBundle.getMessage (PanelProjectLocationExtSrc.class,"ProbablyRails");
                wizardDescriptor.putProperty( WizardDescriptor.PROP_ERROR_MESSAGE, message);       //NOI18N
            }
            return true;
        }
        else {
            wizardDescriptor.putProperty( WizardDescriptor.PROP_ERROR_MESSAGE,result);       //NOI18N
            return false;
        }
    }

    static String checkValidity (final String projectName, final String projectLocation) {
        if ( projectName.length() == 0 
             || projectName.indexOf('/')  > 0           //NOI18N
             || projectName.indexOf('\\') > 0           //NOI18N
             || projectName.indexOf(':')  > 0) {        //NOI18N
            // Display name not specified
            return NbBundle.getMessage(PanelProjectLocationExtSrc.class,"MSG_IllegalProjectName");
        }

        File projLoc = new File (projectLocation).getAbsoluteFile();

        if (PanelProjectLocationVisual.getCanonicalFile(projLoc) == null) {
            return NbBundle.getMessage (PanelProjectLocationExtSrc.class,"MSG_IllegalProjectLocation");
        }

        while (projLoc != null && !projLoc.exists()) {
            projLoc = projLoc.getParentFile();
        }
        if (projLoc == null || !projLoc.canWrite()) {
            return NbBundle.getMessage(PanelProjectLocationExtSrc.class,"MSG_ProjectFolderReadOnly");
        }

        File destFolder = FileUtil.normalizeFile(new File( projectLocation ));
        File[] kids = destFolder.listFiles();
        if ( destFolder.exists() && kids != null && kids.length > 0) {
            String file = null;
            for (int i=0; i< kids.length; i++) {
                String childName = kids[i].getName();
                if ("nbproject".equals(childName)) {   //NOI18N
                    file = NbBundle.getMessage (PanelProjectLocationExtSrc.class,"TXT_NetBeansProject");
                }
                if (file != null) {
                    String format = NbBundle.getMessage (PanelProjectLocationExtSrc.class,"MSG_ProjectFolderInvalid");
                    return MessageFormat.format(format, new Object[] {file});
                }
            }
        }

        // #47611: if there is a live project still residing here, forbid project creation.
        if (destFolder.isDirectory()) {
            FileObject destFO = FileUtil.toFileObject(destFolder);
            assert destFO != null : "No FileObject for " + destFolder;
            boolean clear = false;
            try {
                clear = ProjectManager.getDefault().findProject(destFO) == null;
            } catch (IOException e) {
                // need not report here; clear remains false -> error
            }
            if (!clear) {
                return NbBundle.getMessage(PanelProjectLocationExtSrc.class, "MSG_ProjectFolderHasDeletedProject");
            }
        }
        return null;
    }        

    void validate(WizardDescriptor settings) throws WizardValidationException {
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        projectInfoPanel = new javax.swing.JPanel();
        description = new javax.swing.JLabel();
        projectNameLabel = new javax.swing.JLabel();
        projectName = new javax.swing.JTextField();
        projectLocationLabel = new javax.swing.JLabel();
        projectLocation = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        projectInfoPanel.setLayout(new java.awt.GridBagLayout());

        description.setLabelFor(projectInfoPanel);
        org.openide.awt.Mnemonics.setLocalizedText(description, org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "LBL_ProjectNameAndLocationLabel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        projectInfoPanel.add(description, gridBagConstraints);
        description.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSN_jLabel4")); // NOI18N
        description.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSD_jLabel4")); // NOI18N

        projectNameLabel.setLabelFor(projectName);
        org.openide.awt.Mnemonics.setLocalizedText(projectNameLabel, org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "LBL_NWP1_ProjectName_Label")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        projectInfoPanel.add(projectNameLabel, gridBagConstraints);
        projectNameLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSN_projectNameLabel")); // NOI18N
        projectNameLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSD_projectNameLabel")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 6, 0, 0);
        projectInfoPanel.add(projectName, gridBagConstraints);

        projectLocationLabel.setLabelFor(projectLocation);
        org.openide.awt.Mnemonics.setLocalizedText(projectLocationLabel, org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "LBL_NWP1_CreatedProjectFolder_Lablel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        projectInfoPanel.add(projectLocationLabel, gridBagConstraints);
        projectLocationLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSN_projectLocationLabel")); // NOI18N
        projectLocationLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSD_projectLocationLabel")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(12, 6, 0, 0);
        projectInfoPanel.add(projectLocation, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "LBL_NWP1_BrowseLocation_Button3")); // NOI18N
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseProjectLocation(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 6, 0, 0);
        projectInfoPanel.add(browseButton, gridBagConstraints);
        browseButton.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSN_browseButton")); // NOI18N
        browseButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSD_browseButton")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(projectInfoPanel, gridBagConstraints);

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSN_PanelSourceFolders")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(PanelProjectLocationExtSrc.class, "ACSD_PanelSourceFolders")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void browseProjectLocation(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseProjectLocation
        // TODO add your handling code here:
        JFileChooser chooser = new JFileChooser();
        FileUtil.preventFileChooserSymlinkTraversal(chooser, null);
        chooser.setDialogTitle(NbBundle.getMessage(PanelProjectLocationExtSrc.class,"LBL_NWP1_SelectProjectLocation"));
        chooser.setFileSelectionMode (JFileChooser.DIRECTORIES_ONLY);
        String path = this.projectLocation.getText();
        if (path.length() > 0) {
            File f = new File (path);
            if (f.exists()) {
                chooser.setSelectedFile (f);
            }
        }
        if (chooser.showOpenDialog(this)== JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (file != null) {
                this.projectLocation.setText (FileUtil.normalizeFile(file).getAbsolutePath());
            }
        }
    }//GEN-LAST:event_browseProjectLocation

    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel description;
    private javax.swing.JPanel projectInfoPanel;
    private javax.swing.JTextField projectLocation;
    private javax.swing.JLabel projectLocationLabel;
    private javax.swing.JTextField projectName;
    private javax.swing.JLabel projectNameLabel;
    // End of variables declaration//GEN-END:variables


}
