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
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.modules.ruby.RubyUtils;
import org.netbeans.modules.ruby.rubyproject.templates.NewRubyFileWizardIterator.Type;
import org.netbeans.modules.ruby.spi.project.support.rake.PropertyUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.awt.Mnemonics;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * Permits user to select a floder to place a Ruby file (or other resource) into.
 * @author Petr Hrebejk, Jesse Glick, Tor Norbye
 */
public class RubyTargetChooserPanelGUI extends JPanel implements ActionListener, DocumentListener {
  
    private static final String NEW_CLASS_PREFIX = 
        NbBundle.getMessage( RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_NewRubyClassPrefix" ); // NOI18N
    
    /** preferred dimension of the panel */
    private static final Dimension PREF_DIM = new Dimension(500, 340);
    
    private final Project project;
    
    /** File set except for when we're manually updating values in some of the
     * dependent text fields */
    private boolean userEdit;
    
    /** Flag used to keep track of whether the user has edited the file field manually */
    private boolean fileEdited;
    
    /** Flag used to keep track of whether the user has edited the class field manually */
    private boolean classEdited;
    
    private String expectedExtension;
    private final List<ChangeListener> listeners;
    private Type type;
    private final SourceGroup groups[];
    //private boolean ignoreRootCombo;
    
    public RubyTargetChooserPanelGUI(Project p, SourceGroup[] groups, Component bottomPanel, Type type) {
        this.type = type;
        this.project = p;
        this.groups = groups;
        this.listeners = new ArrayList<ChangeListener>();
        this.userEdit = true;

        initComponents();

        // NOTE - even when adding a -module-, we will use the "class" textfield
        // to represent the name of the module, and the "module" text field to represent
        // modules surrounding the current module
        if (type == Type.TEST) {
            extendsText.setText("Test::Unit::TestCase"); // NOI18N
            type = this.type = Type.CLASS;
        }

        if (type == Type.CLASS || type == Type.MODULE) {
            if (type == Type.MODULE) {
                extendsLabel.setVisible(false);
                extendsText.setVisible(false);
                Mnemonics.setLocalizedText(classLabel, NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_ModuleName_Label")); // NOI18N
            } else {
                extendsText.getDocument().addDocumentListener(this);
            }
            moduleText.getDocument().addDocumentListener(this);
            classText.getDocument().addDocumentListener(this);
        } else if (type == Type.SPEC) {
            Mnemonics.setLocalizedText(classLabel, NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_Spec_Class")); // NOI18N
            moduleLabel.setVisible(false);
            moduleText.setVisible(false);
            extendsLabel.setVisible(false);
            extendsText.setVisible(false);
            classText.getDocument().addDocumentListener(this);
        } else {
            // Type.FILE
            classLabel.setVisible(false);
            classText.setVisible(false);
            moduleLabel.setVisible(false);
            moduleText.setVisible(false);
            extendsLabel.setVisible(false);
            extendsText.setVisible(false);
        }
        
        documentNameTextField.getDocument().addDocumentListener( this );
                
        if ( bottomPanel != null ) {
            bottomPanelContainer.add( bottomPanel, java.awt.BorderLayout.CENTER );
        }
                
        browseButton.addActionListener( this );
        folderTextField.getDocument().addDocumentListener( this );
        
        //initValues( project, null, null );
        
        rootComboBox.setRenderer(new GroupListCellRenderer());        
        rootComboBox.addActionListener( this );
        
        setPreferredSize( PREF_DIM );
        setName( NbBundle.getBundle (RubyTargetChooserPanelGUI.class).getString ("LBL_RubyTargetChooserPanelGUI_Name") ); // NOI18N
    }
            
    @Override
    public void addNotify () {
        Dimension panel2Size = this.jPanel2.getPreferredSize();
        Dimension bottomPanelSize = this.bottomPanelContainer.getPreferredSize ();
        Dimension splitterSize = this.targetSeparator.getPreferredSize();        
        int vmax = panel2Size.height + bottomPanelSize.height + splitterSize.height + 12;   //Insets=12
        //Update only height, keep the wizard width
        if (vmax > PREF_DIM.height) {
            this.setPreferredSize (new Dimension (PREF_DIM.width,vmax));
        }
        super.addNotify();
    }
    
    public void initValues( FileObject template, FileObject preselectedFolder ) {
        assert project != null : "Project must be specified."; // NOI18N
        // Show name of the project
        projectTextField.setText( ProjectUtils.getInformation(project).getDisplayName() );
        assert template != null;
        
        String displayName = null;
        try {
            DataObject templateDo = DataObject.find (template);
            displayName = templateDo.getNodeDelegate ().getDisplayName ();
        } catch (DataObjectNotFoundException ex) {
            displayName = template.getName ();
        }
        
        putClientProperty ("NewFileWizard_Title", displayName);// NOI18N        
        // Setup comboboxes 
        rootComboBox.setModel(new DefaultComboBoxModel(groups));
        SourceGroup preselectedGroup = getPreselectedGroup(
                preselectedFolder, (Boolean) template.getAttribute("isTest")); // NOI18N
        //ignoreRootCombo = true;
        rootComboBox.setSelectedItem(preselectedGroup);
        if (preselectedFolder != null) {
            folderTextField.setText(FileUtil.toFile(preselectedFolder).getPath());
        }
        
        //ignoreRootCombo = false;

        if (template != null && documentNameTextField.getText().trim().length() == 0) { // To preserve the class name on back in the wiazard
            //Ordinary file
            String prefix = NEW_CLASS_PREFIX;
            // See 91580
            Object customPrefix = template.getAttribute("templateNamePrefix"); // NOI18N

            if (customPrefix != null) {
                prefix = customPrefix.toString();
            }

            if (type != Type.SPEC) {
                documentNameTextField.setText(prefix + template.getName());
            }
            documentNameTextField.selectAll();
        }

        // Determine the extension
        String ext = template == null ? "" : template.getExt(); // NOI18N
        expectedExtension = ext.length() == 0 ? "" : "." + ext; // NOI18N
        
        updateText();
        fileEdited = false;
        classEdited = false;
        if (type == Type.CLASS || type == Type.MODULE) {
            classText.selectAll();
        }
    }
        
    public FileObject getRootFolder() {
        return ((SourceGroup) rootComboBox.getSelectedItem()).getRootFolder();        
    }
    
    public String getTargetFolder() {
        
        String folderName = folderTextField.getText().trim();
        
        if ( folderName.length() == 0 ) {
            return null;
        }
        else {           
            return folderName.replace( File.separatorChar, '/' ); // NOI18N
        }
    }

    public String getTargetName() {
        String text = documentNameTextField.getText().trim();
        
        if ( text.length() == 0 ) {
            return null;
        }
        else {
            return text;
        }

    }

    public String getClassName() {
        String text = classText.getText().trim();
        
        if ( text.length() == 0 ) {
            return null;
        }
        else {
            return text;
        }
    }

    public String getModuleName() {
        String text = moduleText.getText().trim();
        
        if ( text.length() == 0 ) {
            return null;
        }
        else {
            return text;
        }
    }

    public String getExtends() {
        String text = extendsText.getText().trim();
        
        if ( text.length() == 0 ) {
            return null;
        }
        else {
            return text;
        }
    }
    
    public void addChangeListener(ChangeListener l) {
        listeners.add(l);
    }
    
    public void removeChangeListener(ChangeListener l) {
        listeners.remove(l);
    }
    
    private void fireChange() {
        ChangeEvent e = new ChangeEvent(this);
        for (ChangeListener l : listeners) {
            l.stateChanged(e);
        }
    }
        
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc=" Generated Code ">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        targetSeparator = new javax.swing.JSeparator();
        bottomPanelContainer = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        classLabel = new javax.swing.JLabel();
        classText = new javax.swing.JTextField();
        documentNameLabel = new javax.swing.JLabel();
        documentNameTextField = new javax.swing.JTextField();
        moduleLabel = new javax.swing.JLabel();
        moduleText = new javax.swing.JTextField();
        extendsLabel = new javax.swing.JLabel();
        extendsText = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        projectTextField = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        rootComboBox = new javax.swing.JComboBox();
        jLabel2 = new javax.swing.JLabel();
        folderTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        fileLabel = new javax.swing.JLabel();
        fileTextField = new javax.swing.JTextField();

        setLayout(new java.awt.GridBagLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        add(targetSeparator, gridBagConstraints);

        bottomPanelContainer.setLayout(new java.awt.BorderLayout());
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.gridheight = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        add(bottomPanelContainer, gridBagConstraints);

        jPanel2.setLayout(new java.awt.GridBagLayout());

        jPanel1.setLayout(new java.awt.GridBagLayout());

        classLabel.setLabelFor(classText);
        org.openide.awt.Mnemonics.setLocalizedText(classLabel, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_ClassName_Label")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanel1.add(classLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        jPanel1.add(classText, gridBagConstraints);

        documentNameLabel.setLabelFor(documentNameTextField);
        org.openide.awt.Mnemonics.setLocalizedText(documentNameLabel, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_FileName_Label")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanel1.add(documentNameLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        jPanel1.add(documentNameTextField, gridBagConstraints);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("org/netbeans/modules/ruby/rubyproject/templates/Bundle"); // NOI18N
        documentNameTextField.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_documentNameTextField")); // NOI18N

        moduleLabel.setLabelFor(moduleText);
        org.openide.awt.Mnemonics.setLocalizedText(moduleLabel, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_InModuleName_Label")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanel1.add(moduleLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        jPanel1.add(moduleText, gridBagConstraints);

        extendsLabel.setLabelFor(extendsText);
        org.openide.awt.Mnemonics.setLocalizedText(extendsLabel, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_Extends_Label")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanel1.add(extendsLabel, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        jPanel1.add(extendsText, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 24, 0);
        jPanel2.add(jPanel1, gridBagConstraints);

        jLabel5.setLabelFor(projectTextField);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel5, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_jLabel5")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanel2.add(jLabel5, gridBagConstraints);

        projectTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        jPanel2.add(projectTextField, gridBagConstraints);
        projectTextField.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_projectTextField")); // NOI18N

        jLabel1.setLabelFor(rootComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(jLabel1, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_jLabel1")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 6, 0);
        jPanel2.add(jLabel1, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 6, 0);
        jPanel2.add(rootComboBox, gridBagConstraints);
        rootComboBox.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_rootComboBox")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(jLabel2, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_Folder")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 0, 12, 0);
        jPanel2.add(jLabel2, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 12, 0);
        jPanel2.add(folderTextField, gridBagConstraints);

        org.openide.awt.Mnemonics.setLocalizedText(browseButton, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_Browse")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.insets = new java.awt.Insets(0, 6, 12, 0);
        jPanel2.add(browseButton, gridBagConstraints);

        fileLabel.setLabelFor(fileTextField);
        org.openide.awt.Mnemonics.setLocalizedText(fileLabel, org.openide.util.NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "LBL_RubyTargetChooserPanelGUI_CreatedFile_Label")); // NOI18N
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(6, 0, 12, 0);
        jPanel2.add(fileLabel, gridBagConstraints);

        fileTextField.setEditable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(6, 6, 12, 0);
        jPanel2.add(fileTextField, gridBagConstraints);
        fileTextField.getAccessibleContext().setAccessibleDescription(bundle.getString("AD_fileTextField")); // NOI18N

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = java.awt.GridBagConstraints.REMAINDER;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.weightx = 1.0;
        add(jPanel2, gridBagConstraints);

        getAccessibleContext().setAccessibleDescription(bundle.getString("AD_RubyTargetChooserPanelGUI")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel bottomPanelContainer;
    private javax.swing.JButton browseButton;
    private javax.swing.JLabel classLabel;
    private javax.swing.JTextField classText;
    private javax.swing.JLabel documentNameLabel;
    private javax.swing.JTextField documentNameTextField;
    private javax.swing.JLabel extendsLabel;
    private javax.swing.JTextField extendsText;
    private javax.swing.JLabel fileLabel;
    private javax.swing.JTextField fileTextField;
    private javax.swing.JTextField folderTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JLabel moduleLabel;
    private javax.swing.JTextField moduleText;
    private javax.swing.JTextField projectTextField;
    private javax.swing.JComboBox rootComboBox;
    private javax.swing.JSeparator targetSeparator;
    // End of variables declaration//GEN-END:variables

    // ActionListener implementation -------------------------------------------
        
    public void actionPerformed(java.awt.event.ActionEvent e) {
        if ( browseButton == e.getSource() ) {
            JFileChooser chooser = new JFileChooser();
            FileUtil.preventFileChooserSymlinkTraversal(chooser, null);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            chooser.setMultiSelectionEnabled(false);

            String workDir = folderTextField.getText();
            if (workDir.length() > 0) {
                File workdirFile = new File(workDir);
                chooser.setSelectedFile(workdirFile);
                chooser.setCurrentDirectory(workdirFile);
            }
            chooser.setDialogTitle(NbBundle.getMessage(RubyTargetChooserPanelGUI.class, "ChooseTargetFolder"));
            if (JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(this)) { //NOI18N
                File file = FileUtil.normalizeFile(chooser.getSelectedFile());
                folderTextField.setText(file.getAbsolutePath());
            }
            updateText();
            fireChange();
        }
        else if ( rootComboBox == e.getSource() )  {
            FileObject root = ((SourceGroup)rootComboBox.getSelectedItem()).getRootFolder();
            folderTextField.setText(FileUtil.toFile(root).getPath());
            updateText();
            fireChange();
        }
    }    
    
    // DocumentListener implementation -----------------------------------------
    
    public void changedUpdate(javax.swing.event.DocumentEvent e) {
        trackEdit(e);
        updateText();
        fireChange();        
    }    
    
    public void insertUpdate(javax.swing.event.DocumentEvent e) {
        changedUpdate( e );
    }
    
    public void removeUpdate(javax.swing.event.DocumentEvent e) {
        changedUpdate( e );
    }
    
    private void syncFields(JTextComponent textComponent, String value) {
        try {
            userEdit = false;
            textComponent.setText(value);
        } finally {
            userEdit = true;
        }
    }
    
    private void capitalizeFirstChar(final JTextComponent field, DocumentEvent e) {
        if (e.getType() == DocumentEvent.EventType.REMOVE) {
            // Don't change the first char when you're deleting it - it would
            // capitalize the second letter which is inconvenient when you're
            // backspacing up to change the word
            return;
        }
        
        String text = field.getText().trim();
        if (text.length() > 0 && Character.isLowerCase(text.charAt(0))) {
            // Force uppercase names to help lazy typists
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    String text = field.getText().trim();
                    if (text.length() > 0 && Character.isLowerCase(text.charAt(0))) {
                        boolean wasEditing = userEdit;
                        try {
                            userEdit = false;
                            ((AbstractDocument)field.getDocument()).replace(0, 1, ""+Character.toUpperCase(text.charAt(0)), null);
                        } catch (BadLocationException ble) {
                            Exceptions.printStackTrace(ble);
                        } finally {
                            userEdit = wasEditing;
                        }
                    }
                }
            });
        }
    }
    
    private void trackEdit(DocumentEvent e) {
        if (userEdit) {
            Document doc = e.getDocument();
            if (doc == documentNameTextField.getDocument()) {
                fileEdited = true;
                
                String text = documentNameTextField.getText().trim();
                if (text.length() == 0) {
                    fileEdited = false;
                }
                if (type == Type.SPEC && (!classEdited ||
                        classText.getText().length() == 0)) {
                    classEdited = false;
                    if (text.endsWith("_spec")) { // NOI18N
                        syncFields(classText, text.substring(0, text.length()-"_spec".length())); // NOI18N
                    }
                } else if ((type == Type.CLASS || type == Type.MODULE) && (!classEdited ||
                        classText.getText().length() == 0)) {
                    classEdited = false;
                    syncFields(classText, RubyUtils.underlinedNameToCamel(text));
                }
            } else if (doc == classText.getDocument()) {
                if (e.getType() != DocumentEvent.EventType.REMOVE) {
                    capitalizeFirstChar(classText, e);
                }
                classEdited = true;

                if (!fileEdited || documentNameTextField.getText().trim().length() == 0) {
                    fileEdited = false;
                    String text = classText.getText().trim();
                    if (type == Type.SPEC && text.length() > 0) {
                        syncFields(documentNameTextField, RubyUtils.camelToUnderlinedName(text) + "_spec"); // NOI18N
                    } else {
                        syncFields(documentNameTextField, RubyUtils.camelToUnderlinedName(text));
                    }
                }
            } else if (doc == extendsText.getDocument()) {
                capitalizeFirstChar(extendsText, e);
            } else if (doc == moduleText.getDocument()) {
                capitalizeFirstChar(moduleText, e);
            }
        }
    }
    
    // Private methods ---------------------------------------------------------
        
    private void updateText() {
        String folderName = folderTextField.getText().trim();
        
        String documentName = documentNameTextField.getText().trim();
        if ( documentName.length() > 0 ) {
            documentName = documentName + expectedExtension;
        }
        String createdFileName = 
            folderName + 
            ( folderName.endsWith("/") || folderName.endsWith( File.separator ) || folderName.length() == 0 ? "" : "/" ) + // NOI18N
            documentName;
        
        fileTextField.setText( createdFileName.replace( '/', File.separatorChar ) ); // NOI18N        
    }
    
    private SourceGroup getPreselectedGroup(final FileObject folder, final Boolean isTest) {
        for(int i = 0; folder != null && i < groups.length; i++) {
            FileObject root = groups[i].getRootFolder();
            if (root.equals(folder) || FileUtil.isParentOf(root, folder)) {
                return groups[i];
            }
        }
        if (isTest != null && isTest) { // #118440
            for (SourceGroup group : groups) {
                String relPath = PropertyUtils.relativizeFile(
                        FileUtil.toFile(project.getProjectDirectory()),
                        FileUtil.toFile(group.getRootFolder()));
                // standard project (test) and rails(test/unit)
                if ("test".equals(relPath) || "test/unit".equals(relPath)) { // NOI18N
                    return group;
                }
            }
        }
        return groups[0];
    }
    
    //    private String getRelativeNativeName( FileObject root, FileObject folder ) {
    //        if (root == null) {
    //            throw new NullPointerException("null root passed to getRelativeNativeName"); // NOI18N
    //        }
    //        
    //        String path;
    //        
    //        if (folder == null) {
    //            path = ""; // NOI18N
    //        }
    //        else {
    //            path = FileUtil.getRelativePath( root, folder );            
    //        }
    //        
    //        return path == null ? "" : path.replace( '/', File.separatorChar ); // NOI18N
    //    }
    
    // Private innerclasses ----------------------------------------------------

    /**
     * Displays a {@link SourceGroup} in {@link #rootComboBox}.
     */
    private static final class GroupListCellRenderer extends DefaultListCellRenderer/*<SourceGroup>*/ {
        
        public GroupListCellRenderer() {}
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (value != null) { // apple jdk bug
                SourceGroup g = (SourceGroup) value;
                super.getListCellRendererComponent(list, g.getDisplayName(), index, isSelected, cellHasFocus);
                setIcon(g.getIcon(false));
            }
            return this;
        }
        
    }
    
}
