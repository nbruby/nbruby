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

package org.netbeans.modules.ruby.javaint;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.netbeans.api.project.Project;
import org.netbeans.modules.ruby.rubyproject.SharedRubyProjectProperties;
import org.openide.awt.Mnemonics;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;

public class CustomizerLibraries extends JPanel implements HelpCtx.Provider, ListDataListener {

    public static final String COMPILE = "COMPILE";  //NOI18N
    public static final String RUN = "RUN";          //NOI18N
    public static final String COMPILE_TESTS = "COMPILE_TESTS"; //NOI18N
    public static final String RUN_TESTS = "RUN_TESTS";  //NOI18N

    public CustomizerLibraries(Project project, SharedRubyProjectProperties uiProperties,
            CustomizerProviderImpl.SubCategoryProvider subcat ) {
        initComponents();  

        // hidden until it is possible to use them (issue 134386)
        jButtonAddArtifactC.setVisible(false);
        jButtonAddLibraryC.setVisible(false);

        // the checkbox is not used anywhere for now (issue -TODO-)
        includeJavaCheckbox.setVisible(false);
        includeJavaSeparator.setVisible(false);

        this.putClientProperty( "HelpID", "J2SE_CustomizerGeneral" ); // NOI18N

        // Hide unused edit buttons
        jButtonEditC.setVisible( false );

        jListCpC.setModel(uiProperties.JAVAC_CLASSPATH_MODEL);
        jListCpC.setCellRenderer(uiProperties.CLASS_PATH_LIST_RENDERER );
        RubyClassPathUi.EditMediator.register( project,
                                               jListCpC,
                                               uiProperties.JAVAC_CLASSPATH_MODEL,
                                               jButtonAddJarC.getModel(),
                                               jButtonAddLibraryC.getModel(),
                                               jButtonAddArtifactC.getModel(),
                                               jButtonRemoveC.getModel(),
                                               jButtonMoveUpC.getModel(),
                                               jButtonMoveDownC.getModel() );

        //includeJavaCheckbox.setModel(uiProperties.INCLUDE_JAVA_MODEL);
        // XXX: the above call destroys mnemonic set in initComponents. Workarounding.
        Mnemonics.setLocalizedText(includeJavaCheckbox,
                NbBundle.getMessage(CustomizerLibraries.class, "IncludeJava")); // NOI18N

//        uiProperties.NO_DEPENDENCIES_MODEL.setMnemonic( jCheckBoxBuildSubprojects.getMnemonic() );
//        jCheckBoxBuildSubprojects.setModel( uiProperties.NO_DEPENDENCIES_MODEL );

//        if (uiProperties.PLATFORM_MODEL != null) {
//                jComboBoxTarget.setModel(uiProperties.PLATFORM_MODEL);
//                jComboBoxTarget.setRenderer(uiProperties.PLATFORM_LIST_RENDERER);
//        }

        platformCombo.putClientProperty ("JComboBox.isTableCellEditor", Boolean.TRUE);    //NOI18N
        platformCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e){
                JComboBox combo = (JComboBox) e.getSource();
                combo.setPopupVisible(false);
            }
        });
//        testBroken();
        if (subcat != null && RubyCompositePanelProvider.JAVA.equals(subcat.getCategory())) {
            showSubCategory(subcat.getSubcategory());
        }

        hidePlatformComponents();

//        uiProperties.JAVAC_CLASSPATH_MODEL.addListDataListener( this );
    }

    private void hidePlatformComponents() {
        platformButton.setVisible(false);
        platformCombo.setVisible(false);
        platformLabel.setVisible(false);
        jCheckBoxBuildSubprojects.setVisible(false);
    }

    // Implementation of HelpCtx.Provider --------------------------------------

    public HelpCtx getHelpCtx() {
        return new HelpCtx( CustomizerLibraries.class );
    }


    // Implementation of ListDataListener --------------------------------------


    public void intervalRemoved( ListDataEvent e ) {
//        testBroken();
    }

    public void intervalAdded( ListDataEvent e ) {
        // NOP
    }

    public void contentsChanged( ListDataEvent e ) {
        // NOP
    }


    private void showSubCategory (String name) {
        if (name.equals(COMPILE)) {
            jTabbedPane1.setSelectedIndex (0);
        }
        else if (name.equals(COMPILE_TESTS)) {
            jTabbedPane1.setSelectedIndex (2);
        }
        else if (name.equals(RUN)) {
            jTabbedPane1.setSelectedIndex (1);
        }
        else if (name.equals(RUN_TESTS)) {
            jTabbedPane1.setSelectedIndex (3);
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

        includeJavaCheckbox = new javax.swing.JCheckBox();
        includeJavaSeparator = new javax.swing.JSeparator();
        platformLabel = new javax.swing.JLabel();
        platformCombo = new javax.swing.JComboBox();
        platformButton = new javax.swing.JButton();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanelCompile = new javax.swing.JPanel();
        librariesJLabel1 = new javax.swing.JLabel();
        librariesJScrollPane = new javax.swing.JScrollPane();
        jListCpC = new javax.swing.JList();
        jButtonAddArtifactC = new javax.swing.JButton();
        jButtonAddLibraryC = new javax.swing.JButton();
        jButtonAddJarC = new javax.swing.JButton();
        jButtonEditC = new javax.swing.JButton();
        jButtonRemoveC = new javax.swing.JButton();
        jButtonMoveUpC = new javax.swing.JButton();
        jButtonMoveDownC = new javax.swing.JButton();
        jCheckBoxBuildSubprojects = new javax.swing.JCheckBox();
        jLabelErrorMessage = new javax.swing.JLabel();

        setLayout(new java.awt.GridBagLayout());

        org.openide.awt.Mnemonics.setLocalizedText(includeJavaCheckbox, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "IncludeJava")); // NOI18N
        includeJavaCheckbox.setToolTipText(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "IncludeJavaTip")); // NOI18N
        includeJavaCheckbox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        add(includeJavaCheckbox, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 11, 0);
        add(includeJavaSeparator, gridBagConstraints);

        platformLabel.setLabelFor(platformCombo);
        org.openide.awt.Mnemonics.setLocalizedText(platformLabel, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeGeneral_Platform_JLabel")); // NOI18N
        platformLabel.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 12);
        add(platformLabel, gridBagConstraints);
        platformLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerGeneral_jLabelTarget")); // NOI18N

        platformCombo.setEnabled(false);
        platformCombo.setMinimumSize(this.platformCombo.getPreferredSize());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        add(platformCombo, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(platformButton, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeGeneral_Platform_JButton")); // NOI18N
        platformButton.setEnabled(false);
        platformButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createNewPlatform(evt);
            }
        });
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 12, 0);
        add(platformButton, gridBagConstraints);
        platformButton.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerGeneral_jButton1")); // NOI18N

        jPanelCompile.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        jPanelCompile.setLayout(new java.awt.GridBagLayout());

        librariesJLabel1.setLabelFor(jListCpC);
        org.openide.awt.Mnemonics.setLocalizedText(librariesJLabel1, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_LibrariesC_JLabel")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanelCompile.add(librariesJLabel1, gridBagConstraints);

        librariesJScrollPane.setViewportView(jListCpC);
        jListCpC.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "AN_CustomizerLibraries_jListClasspathC")); // NOI18N
        jListCpC.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_jLabelClasspathC")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridheight = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 12);
        jPanelCompile.add(librariesJScrollPane, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jButtonAddArtifactC, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_AddProject_JButton")); // NOI18N
        jButtonAddArtifactC.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanelCompile.add(jButtonAddArtifactC, gridBagConstraints);
        jButtonAddArtifactC.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_jButtonAddArtifact")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonAddLibraryC, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_AddLibary_JButton")); // NOI18N
        jButtonAddLibraryC.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanelCompile.add(jButtonAddLibraryC, gridBagConstraints);
        jButtonAddLibraryC.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_jButtonAddLibrary")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonAddJarC, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_AddJar_JButton")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        jPanelCompile.add(jButtonAddJarC, gridBagConstraints);
        jButtonAddJarC.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_jButtonAddJar")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonEditC, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_Edit_JButton")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        jPanelCompile.add(jButtonEditC, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(jButtonRemoveC, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_Remove_JButton")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        jPanelCompile.add(jButtonRemoveC, gridBagConstraints);
        jButtonRemoveC.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_jButtonRemove")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveUpC, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_MoveUp_JButton")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 5, 0);
        jPanelCompile.add(jButtonMoveUpC, gridBagConstraints);
        jButtonMoveUpC.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_jButtonMoveUp")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jButtonMoveDownC, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_MoveDown_JButton")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        jPanelCompile.add(jButtonMoveDownC, gridBagConstraints);
        jButtonMoveDownC.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_jButtonMoveDown")); // NOI18N

        jTabbedPane1.addTab(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_LibrariesTab"), jPanelCompile); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        add(jTabbedPane1, gridBagConstraints);
        jTabbedPane1.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSN_CustomizerLibraries_JTabbedPane")); // NOI18N
        jTabbedPane1.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "ACSD_CustomizerLibraries_JTabbedPane")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jCheckBoxBuildSubprojects, org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "LBL_CustomizeLibraries_Build_Subprojects")); // NOI18N
        jCheckBoxBuildSubprojects.setEnabled(false);
        jCheckBoxBuildSubprojects.setMargin(new java.awt.Insets(0, 0, 0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 0, 0);
        add(jCheckBoxBuildSubprojects, gridBagConstraints);
        jCheckBoxBuildSubprojects.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(CustomizerLibraries.class, "AD_CheckBoxBuildSubprojects")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabelErrorMessage, " ");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(12, 0, 0, 0);
        add(jLabelErrorMessage, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void createNewPlatform(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createNewPlatform
//        Object selectedItem = this.platformCombo.getSelectedItem();
//        JavaPlatform jp = (selectedItem == null ? null : PlatformUiSupport.getPlatform(selectedItem));                                  
//        JavaPlatform jp = null;
//        PlatformsCustomizer.showCustomizer(jp);
    }


    // Variables declaration - do not modify//GEN-HEADEREND:event_createNewPlatform
    private javax.swing.JCheckBox includeJavaCheckbox;//GEN-LAST:event_createNewPlatform
    private javax.swing.JSeparator includeJavaSeparator;//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAddArtifactC;
    private javax.swing.JButton jButtonAddJarC;
    private javax.swing.JButton jButtonAddLibraryC;
    private javax.swing.JButton jButtonEditC;
    private javax.swing.JButton jButtonMoveDownC;
    private javax.swing.JButton jButtonMoveUpC;
    private javax.swing.JButton jButtonRemoveC;
    private javax.swing.JCheckBox jCheckBoxBuildSubprojects;
    private javax.swing.JLabel jLabelErrorMessage;
    private javax.swing.JList jListCpC;
    private javax.swing.JPanel jPanelCompile;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JLabel librariesJLabel1;
    private javax.swing.JScrollPane librariesJScrollPane;
    private javax.swing.JButton platformButton;
    private javax.swing.JComboBox platformCombo;
    private javax.swing.JLabel platformLabel;
    // End of variables declaration//GEN-END:variables


}
