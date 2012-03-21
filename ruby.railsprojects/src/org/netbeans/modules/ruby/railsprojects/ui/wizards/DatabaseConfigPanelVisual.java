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

import java.awt.BorderLayout;
import java.awt.Component;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.openide.WizardDescriptor;
import org.openide.util.NbBundle;

/**
 *
 * @author Erno Mononen
 */
public class DatabaseConfigPanelVisual extends javax.swing.JPanel {

    private JdbcConnectionsPanel jdbcPanel;
    private RailsAdaptersPanel adaptersPanel;
    private boolean initialized;

    /** Creates new form DatabaseConfigPanel */
    public DatabaseConfigPanelVisual() {
        initComponents();
        configureOptionsButtonGroup.add(useIDEConnections);
        configureOptionsButtonGroup.add(useRailsAdapter);
        jdbcPanel = new JdbcConnectionsPanel();
        adaptersPanel = new RailsAdaptersPanel(this);
        ideConnectionsPanel.add(jdbcPanel, BorderLayout.CENTER);
        adapterConfigurationPanel.add(adaptersPanel, BorderLayout.CENTER);
        setName(NbBundle.getMessage(DatabaseConfigPanelVisual.class, "LAB_ConfigureDatabase"));
        putClientProperty("NewProjectWizard_Title", NbBundle.getMessage(DatabaseConfigPanelVisual.class, "TXT_NewRoRApp")); // NOI18N

        initInnerPanels();
    }

    private boolean isJdbc() {
        return useJdbc.isSelected();
    }

    private boolean useIdeConnections() {
        return useIDEConnections.isSelected();
    }

    private SettingsPanel getActivePanel() {
        return useIdeConnections() ? jdbcPanel : adaptersPanel;
    }

    void read(WizardDescriptor descriptor) {
        RubyPlatform platform =
                (RubyPlatform) descriptor.getProperty(NewRailsProjectWizardIterator.PLATFORM);
        boolean jruby = platform.isJRuby();
        if (!initialized) {
            useIDEConnections.setSelected(jruby);
            useRailsAdapter.setSelected(!jruby);
            initialized = true;
        }
        if (!jruby) {
            useJdbc.setSelected(false);
        }
        useJdbc.setEnabled(jruby);
        jdbcPanel.read(descriptor);
        adaptersPanel.read(descriptor);
        initInnerPanels();
    }

    // need access from RailsAdaptersPanel
    JCheckBox getUseJdbc() {
        return useJdbc;
    }

    void store(WizardDescriptor descriptor) {
        descriptor.putProperty(NewRailsProjectWizardIterator.JDBC_WN, isJdbc());
        getActivePanel().store(descriptor);
    }

    private void initInnerPanels() {
        boolean useIdeConnections = useIdeConnections();
        setEnabled(jdbcPanel, useIdeConnections);
        setEnabled(adaptersPanel, !useIdeConnections);
    }
    
    private void setEnabled(JPanel panel, boolean enable) {
        panel.setEnabled(enable);
        for (Component component : panel.getComponents()) {
            component.setEnabled(enable);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        configureOptionsButtonGroup = new javax.swing.ButtonGroup();
        ideConnectionsPanel = new javax.swing.JPanel();
        adapterConfigurationPanel = new javax.swing.JPanel();
        useIDEConnections = new javax.swing.JRadioButton();
        useRailsAdapter = new javax.swing.JRadioButton();
        jSeparator1 = new javax.swing.JSeparator();
        useJdbc = new javax.swing.JCheckBox();
        databaseAccessConfLabel = new javax.swing.JLabel();

        setPreferredSize(null);

        ideConnectionsPanel.setLayout(new java.awt.BorderLayout());

        adapterConfigurationPanel.setLayout(new java.awt.BorderLayout());

        org.openide.awt.Mnemonics.setLocalizedText(useIDEConnections, org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "LBL_ConfigureUsingIDEConnections")); // NOI18N
        useIDEConnections.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useIDEConnectionsActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(useRailsAdapter, org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "LBL_ConfigureDirectly")); // NOI18N
        useRailsAdapter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                useRailsAdapterActionPerformed(evt);
            }
        });

        org.openide.awt.Mnemonics.setLocalizedText(useJdbc, org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "UseJdbc")); // NOI18N

        org.openide.awt.Mnemonics.setLocalizedText(databaseAccessConfLabel, org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "LBL_DatabaseAccessInformation")); // NOI18N

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(useRailsAdapter, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(adapterConfigurationPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 265, Short.MAX_VALUE)
                .addContainerGap())
            .add(org.jdesktop.layout.GroupLayout.TRAILING, layout.createSequentialGroup()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(jSeparator1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 289, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, layout.createSequentialGroup()
                        .add(databaseAccessConfLabel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 204, Short.MAX_VALUE)
                        .add(85, 85, 85))
                    .add(layout.createSequentialGroup()
                        .addContainerGap()
                        .add(ideConnectionsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)))
                .add(0, 0, 0))
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(useIDEConnections, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                .add(32, 32, 32))
            .add(layout.createSequentialGroup()
                .add(useJdbc, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(databaseAccessConfLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 17, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(useIDEConnections, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(ideConnectionsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 100, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(useRailsAdapter, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(adapterConfigurationPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 114, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jSeparator1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(useJdbc, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 22, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        useIDEConnections.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "ACSD_ConfigureUsingIDEConnections")); // NOI18N
        useRailsAdapter.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "ACSD_ConfigureDirectly")); // NOI18N
        useJdbc.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "ASCD_UseJdbc")); // NOI18N
        databaseAccessConfLabel.getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "ASCN_DatabaseAccessInformation")); // NOI18N
        databaseAccessConfLabel.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "ASCD_DatabaseAccessInformation")); // NOI18N

        getAccessibleContext().setAccessibleName(org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "ASCN_DatabaseConfigPanel")); // NOI18N
        getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(DatabaseConfigPanelVisual.class, "ASCN_DatabaseConfigPanel")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

private void useIDEConnectionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useIDEConnectionsActionPerformed
    initInnerPanels();
}//GEN-LAST:event_useIDEConnectionsActionPerformed

private void useRailsAdapterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_useRailsAdapterActionPerformed
    initInnerPanels();
}//GEN-LAST:event_useRailsAdapterActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel adapterConfigurationPanel;
    private javax.swing.ButtonGroup configureOptionsButtonGroup;
    private javax.swing.JLabel databaseAccessConfLabel;
    private javax.swing.JPanel ideConnectionsPanel;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JRadioButton useIDEConnections;
    private javax.swing.JCheckBox useJdbc;
    private javax.swing.JRadioButton useRailsAdapter;
    // End of variables declaration//GEN-END:variables

}
