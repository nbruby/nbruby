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

/**
 *
 * @author  Tor Norbye
 */
public class RailsOptionPanel extends javax.swing.JPanel {

    /** Creates new form RailsOptionPanel */
    public RailsOptionPanel() {
        initComponents();
    }
    
    public boolean getLogicalChosen() {
        return logicalCB.isSelected();
    }
    
    public void setLogicalChosen(boolean selected) {
        logicalCB.setSelected(selected);
    }

    public boolean getIndexVendorGemsOnly() {
        return vendorGemsCheckBox.isSelected();
    }

    public void setIndexVendorGemsOnly(boolean selected) {
        vendorGemsCheckBox.setSelected(selected);
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        logicalCB = new javax.swing.JCheckBox();
        railsLabel = new javax.swing.JLabel();
        upperSep = new javax.swing.JSeparator();
        vendorGemsCheckBox = new javax.swing.JCheckBox();

        logicalCB.setSelected(true);
        org.openide.awt.Mnemonics.setLocalizedText(logicalCB, org.openide.util.NbBundle.getMessage(RailsOptionPanel.class, "LogicalView")); // NOI18N

        railsLabel.setText(org.openide.util.NbBundle.getMessage(RailsOptionPanel.class, "RailsProjectLabel")); // NOI18N

        vendorGemsCheckBox.setText(org.openide.util.NbBundle.getMessage(RailsOptionPanel.class, "RailsOptionPanel.vendorGemsCheckBox.text")); // NOI18N
        vendorGemsCheckBox.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                vendorGemsCheckBoxItemStateChanged(evt);
            }
        });

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(vendorGemsCheckBox)
                    .add(layout.createSequentialGroup()
                        .add(railsLabel)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(upperSep, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 176, Short.MAX_VALUE))
                    .add(logicalCB))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .addContainerGap()
                .add(layout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(railsLabel)
                    .add(upperSep, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(vendorGemsCheckBox)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(logicalCB)
                .addContainerGap())
        );

        logicalCB.getAccessibleContext().setAccessibleDescription(org.openide.util.NbBundle.getMessage(RailsOptionPanel.class, "RailsOptionPanel.logicalCB.AccessibleContext.accessibleDescription")); // NOI18N
    }// </editor-fold>//GEN-END:initComponents

    private void vendorGemsCheckBoxItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_vendorGemsCheckBoxItemStateChanged
        // TODO add your handling code here:
    }//GEN-LAST:event_vendorGemsCheckBoxItemStateChanged
    
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox logicalCB;
    private javax.swing.JLabel railsLabel;
    private javax.swing.JSeparator upperSep;
    private javax.swing.JCheckBox vendorGemsCheckBox;
    // End of variables declaration//GEN-END:variables
    
}