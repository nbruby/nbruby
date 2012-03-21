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
 * 
 * Contributor(s):
 * 
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.ruby.railsprojects.ui.wizards;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.modules.ruby.railsprojects.database.ConfigurableRailsAdapter;
import org.netbeans.modules.ruby.railsprojects.database.RailsAdapterFactory;
import org.netbeans.modules.ruby.railsprojects.database.RailsDatabaseConfiguration;
import org.openide.WizardDescriptor;
import org.openide.WizardValidationException;

/**
 * A panel for Rails database adapters.
 * 
 * TODO: currently only one (development) combo is displayed, test and production
 * adapters need to be inserted to database.yml after initial generation 
 * (AFAIK the rails generator doesn't let you specify those databases separately).
 * 
 * @author  Erno Mononen
 */
class RailsAdaptersPanel extends SettingsPanel {

    private static final Logger LOGGER = Logger.getLogger(RailsAdaptersPanel.class.getName());

    private String projectName;
    private boolean manuallyEdited;
    private DocumentListener databaseNameListener;
    private final DatabaseConfigPanelVisual parent;

    // for storing the original state of the jdbc check box (this panel 
    // may change it's state)
    private boolean useJdbcOriginallySelected;
    private boolean useJdbcOriginallyEnabled;

    /** Creates new form RailsAdaptersPanel */
    public RailsAdaptersPanel(DatabaseConfigPanelVisual parent) {
        initComponents();
        this.parent = parent;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            resetUseJdbc();
        } else {
            initJdbcCheckBox();
        }
    }

    private void resetUseJdbc() {
        parent.getUseJdbc().setSelected(useJdbcOriginallySelected);
        parent.getUseJdbc().setEnabled(useJdbcOriginallyEnabled);
    }

    private void initDatabaseNameField() {
        if (manuallyEdited) {
            return;
        }
        RailsDatabaseConfiguration configuration = (RailsDatabaseConfiguration) developmentComboBox.getSelectedItem();
        databaseNameField.getDocument().removeDocumentListener(databaseNameListener);
        if (configuration != null) {
            databaseNameField.setText(configuration.getDatabaseName(projectName)); //NOI18N
        }
        databaseNameField.getDocument().addDocumentListener(databaseNameListener);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        developmentLabel = new javax.swing.JLabel();
        developmentComboBox = new javax.swing.JComboBox();
        databaseNameLabel = new javax.swing.JLabel();
        databaseNameField = new javax.swing.JTextField();
        userNameLabel = new javax.swing.JLabel();
        userNameField = new javax.swing.JTextField();
        passwordLabel = new javax.swing.JLabel();
        passwordField = new javax.swing.JPasswordField();

        developmentLabel.setLabelFor(developmentComboBox);
        org.openide.awt.Mnemonics.setLocalizedText(developmentLabel, org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "LBL_DatabaseAdapter")); // NOI18N

        databaseNameLabel.setLabelFor(databaseNameField);
        org.openide.awt.Mnemonics.setLocalizedText(databaseNameLabel, org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "LBL_DatabaseName")); // NOI18N

        userNameLabel.setLabelFor(userNameField);
        org.openide.awt.Mnemonics.setLocalizedText(userNameLabel, org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "LBL_UserName")); // NOI18N

        userNameField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                userNameFieldActionPerformed(evt);
            }
        });

        passwordLabel.setLabelFor(passwordField);
        org.openide.awt.Mnemonics.setLocalizedText(passwordLabel, org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "LBL_Password")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(developmentLabel)
                    .add(databaseNameLabel)
                    .add(userNameLabel)
                    .add(passwordLabel))
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(developmentComboBox, 0, 374, Short.MAX_VALUE))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, passwordField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, userNameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)
                            .add(databaseNameField, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 374, Short.MAX_VALUE)))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(developmentLabel)
                    .add(developmentComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(databaseNameLabel)
                    .add(databaseNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(userNameLabel)
                    .add(userNameField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 21, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(passwordLabel)
                    .add(passwordField, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        developmentLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "ACSD_DatabaseAdapter")); // NOI18N
        databaseNameLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "ASCD_DatabaseName")); // NOI18N
        userNameLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "ASCD_UserName")); // NOI18N
        passwordLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "ASCD_Password")); // NOI18N

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "ASCN_RailsAdapterPanel")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RailsAdaptersPanel.class, "ASCD_RailsAdapterPanel")); // NOI18N
        getAccessibleContext().setAccessibleParent(this);
    }// </editor-fold>//GEN-END:initComponents

private void userNameFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_userNameFieldActionPerformed
// TODO add your handling code here:
}//GEN-LAST:event_userNameFieldActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextField databaseNameField;
    private javax.swing.JLabel databaseNameLabel;
    private javax.swing.JComboBox developmentComboBox;
    private javax.swing.JLabel developmentLabel;
    private javax.swing.JPasswordField passwordField;
    private javax.swing.JLabel passwordLabel;
    private javax.swing.JTextField userNameField;
    private javax.swing.JLabel userNameLabel;
    // End of variables declaration//GEN-END:variables
    @Override
    void store( WizardDescriptor settings) {
        boolean jdbc = settings.getProperty(NewRailsProjectWizardIterator.JDBC_WN) != null 
                ? ((Boolean) settings.getProperty(NewRailsProjectWizardIterator.JDBC_WN)).booleanValue()
                : false;
        RailsDatabaseConfiguration databaseConfiguration = (RailsDatabaseConfiguration) developmentComboBox.getSelectedItem();
        
        databaseConfiguration =
                new ConfigurableRailsAdapter((RailsDatabaseConfiguration) developmentComboBox.getSelectedItem(),
                userNameField.getText(),
                String.valueOf(passwordField.getPassword()),
                databaseNameField.getText(),
                jdbc);

        settings.putProperty(NewRailsProjectWizardIterator.RAILS_DEVELOPMENT_DB, databaseConfiguration);
//        settings.putProperty(NewRailsProjectWizardIterator.RAILS_PRODUCTION_DB, StandardRailsAdapter.get(pr));
//        settings.putProperty(NewRailsProjectWizardIterator.RAILS_DEVELOPMENT_DB, StandardRailsAdapter.get(devel));
    }

    void initJdbcCheckBox() {
        RailsDatabaseConfiguration selected = (RailsDatabaseConfiguration) developmentComboBox.getSelectedItem();
        if (selected != null && selected.requiresJdbc()) {
            parent.getUseJdbc().setSelected(true);
            parent.getUseJdbc().setEnabled(false);
        } else {
            resetUseJdbc();
        }
    }

    @Override
    void read( WizardDescriptor settings) {
        RubyPlatform platform = (RubyPlatform) settings.getProperty("platform"); //NOI8N
        List<RailsDatabaseConfiguration> adapters = RailsAdapterFactory.getAdapters(platform);
        // get the original state of the jdbc check box -- this panel might change it's state,
        // depending on the selected adapter, 
        // and we need to be able to reset it back to defaults
        this.useJdbcOriginallyEnabled = parent.getUseJdbc().isEnabled();
        this.useJdbcOriginallySelected = parent.getUseJdbc().isSelected();

        developmentComboBox.setModel(new AdapterListModel(adapters));
        developmentComboBox.setRenderer(new AdapterListCellRendered());
        developmentComboBox.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                initDatabaseNameField();
                initJdbcCheckBox();
            }
        });

        databaseNameListener = new DocumentListener() {

            public void insertUpdate(DocumentEvent e) {
                manuallyEdited = true;
            }

            public void removeUpdate(DocumentEvent e) {
                manuallyEdited = true;
            }

            public void changedUpdate(DocumentEvent e) {
                manuallyEdited = true;
            }
        };

        String name = (String) settings.getProperty("name"); //NOI18N
        if (!name.equals(projectName)) {
            projectName = name;
            initDatabaseNameField();
        }
        initJdbcCheckBox();

    }

    @Override
    boolean valid( WizardDescriptor settings) {
        return true;
    }

    @Override
    void validate( WizardDescriptor settings) throws WizardValidationException {
    }


    private static class AdapterListModel extends AbstractListModel implements ComboBoxModel {

        private final List<? extends RailsDatabaseConfiguration> adapters;
        private Object selected;

        public AdapterListModel(List<? extends RailsDatabaseConfiguration> adapters) {
            this.adapters = adapters;
            this.selected = adapters.get(0);
        }

        public int getSize() {
            return adapters.size();
        }

        public Object getElementAt(int index) {
            return adapters.get(index);
        }

        public void setSelectedItem(Object adapter) {
            if (selected != adapter) {
                this.selected = adapter;
                fireContentsChanged(this, -1, -1);
            }
        }

        public Object getSelectedItem() {
            return selected;
        }
    }

    private static class AdapterListCellRendered extends JLabel implements ListCellRenderer {

        public AdapterListCellRendered() {
            setOpaque(true);
        }

        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {

            if (value == null) { //apple JDK bug
                return this;
            }

            RailsDatabaseConfiguration dbConf = (RailsDatabaseConfiguration) value;

            setText(dbConf.getDisplayName());
            setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());

            return this;
        }
    }
}
