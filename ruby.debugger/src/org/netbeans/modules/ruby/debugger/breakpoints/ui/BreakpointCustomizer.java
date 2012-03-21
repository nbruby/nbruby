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
package org.netbeans.modules.ruby.debugger.breakpoints.ui;

import java.awt.Dialog;
import java.beans.Customizer;
import java.io.File;
import javax.swing.JPanel;
import org.netbeans.modules.ruby.debugger.EditorUtil;
import org.netbeans.modules.ruby.debugger.breakpoints.RubyLineBreakpoint;
import org.netbeans.modules.ruby.platform.Util;
import org.netbeans.spi.debugger.ui.Controller;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.text.Line;
import org.openide.util.NbBundle;

public final class BreakpointCustomizer extends JPanel implements Customizer, Controller {

    private RubyLineBreakpoint bp;
    
    public BreakpointCustomizer() {
        initComponents();
    }

    public static void customize(RubyLineBreakpoint bp) {
        BreakpointCustomizer customizer = new BreakpointCustomizer();
        customizer.setObject(bp);
        DialogDescriptor descriptor = new DialogDescriptor(customizer,
                NbBundle.getMessage(BreakpointCustomizer.class, "BreakpointCustomizer.title"));
        Dialog d = DialogDisplayer.getDefault().createDialog(descriptor);
        d.setVisible(true);
        if (descriptor.getValue() == NotifyDescriptor.OK_OPTION) {
            customizer.ok();
        }
    }

    public void setObject(Object bean) {
        if (!(bean instanceof RubyLineBreakpoint)) {
            throw new IllegalArgumentException(bean.toString());
        }
        this.bp = (RubyLineBreakpoint) bean;
        fileValue.setText(bp.getFilePath());
        lineValue.setText("" + bp.getLineNumber());
        String condition = bp.getCondition();
        conditionValue.setText(condition == null ? "" : condition);
    }

    public boolean ok() {
        try {
            int line = Integer.valueOf(lineValue.getText()) - 1; // need 0-based
            String file = fileValue.getText();
            if (!new File(file).isFile()) {
                Util.notifyLocalizedInfo(BreakpointCustomizer.class, "BreakpointCustomizer.file.not.found", file);
                return false;
            } else {
                Line eLine = EditorUtil.getLine(file, line);
                if (eLine == null) {
                    Util.notifyLocalizedInfo(BreakpointCustomizer.class, "BreakpointCustomizer.invalid.line.number", "" + (line + 1), file);
                    return false;
                }
                bp.setLine(eLine);
            }
            if (getCondition().length() > 0) {
                bp.setCondition(getCondition());
            }
            return true;
        } catch (NumberFormatException nfe) {
            Util.notifyLocalizedInfo(BreakpointCustomizer.class, "BreakpointCustomizer.invalid.number", lineValue.getText());
            return false;
        }
    }

    public boolean cancel() {
        return true;
    }
    
    private String getCondition() {
        return conditionValue.getText().trim();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        settingsPanel = new javax.swing.JPanel();
        lineValue = new javax.swing.JTextField();
        fileValue = new javax.swing.JTextField();
        fileLbl = new javax.swing.JLabel();
        lineLbl = new javax.swing.JLabel();
        conditionsPanel = new javax.swing.JPanel();
        conditionValue = new javax.swing.JTextField();
        conditionLbl = new javax.swing.JLabel();

        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(BreakpointCustomizer.class, "BreakpointCustomizer.settingsPanel.border.title"))); // NOI18N

        fileLbl.setLabelFor(fileValue);
        org.openide.awt.Mnemonics.setLocalizedText(fileLbl, org.openide.util.NbBundle.getMessage(BreakpointCustomizer.class, "BreakpointCustomizer.fileLbl.text")); // NOI18N

        lineLbl.setLabelFor(lineValue);
        org.openide.awt.Mnemonics.setLocalizedText(lineLbl, org.openide.util.NbBundle.getMessage(BreakpointCustomizer.class, "BreakpointCustomizer.lineLbl.text")); // NOI18N

        org.jdesktop.layout.GroupLayout settingsPanelLayout = new org.jdesktop.layout.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
            settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, settingsPanelLayout.createSequentialGroup()
                .add(fileLbl)
                .add(16, 16, 16)
                .add(fileValue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE))
            .add(org.jdesktop.layout.GroupLayout.TRAILING, settingsPanelLayout.createSequentialGroup()
                .add(lineLbl)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(lineValue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 360, Short.MAX_VALUE))
        );
        settingsPanelLayout.setVerticalGroup(
            settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(settingsPanelLayout.createSequentialGroup()
                .add(settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(fileLbl)
                    .add(fileValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(settingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(lineLbl)
                    .add(lineValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );

        conditionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(org.openide.util.NbBundle.getMessage(BreakpointCustomizer.class, "BreakpointCustomizer.conditionsPanel.border.title"))); // NOI18N

        conditionLbl.setLabelFor(conditionValue);
        org.openide.awt.Mnemonics.setLocalizedText(conditionLbl, org.openide.util.NbBundle.getMessage(BreakpointCustomizer.class, "BreakpointCustomizer.conditionLbl.text")); // NOI18N

        org.jdesktop.layout.GroupLayout conditionsPanelLayout = new org.jdesktop.layout.GroupLayout(conditionsPanel);
        conditionsPanel.setLayout(conditionsPanelLayout);
        conditionsPanelLayout.setHorizontalGroup(
            conditionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, conditionsPanelLayout.createSequentialGroup()
                .add(conditionLbl)
                .add(16, 16, 16)
                .add(conditionValue, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 323, Short.MAX_VALUE))
        );
        conditionsPanelLayout.setVerticalGroup(
            conditionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(conditionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                .add(conditionLbl)
                .add(conditionValue, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 19, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(conditionsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(settingsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(layout.createSequentialGroup()
                .add(settingsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(conditionsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel conditionLbl;
    private javax.swing.JTextField conditionValue;
    private javax.swing.JPanel conditionsPanel;
    private javax.swing.JLabel fileLbl;
    private javax.swing.JTextField fileValue;
    private javax.swing.JLabel lineLbl;
    private javax.swing.JTextField lineValue;
    private javax.swing.JPanel settingsPanel;
    // End of variables declaration//GEN-END:variables
}
