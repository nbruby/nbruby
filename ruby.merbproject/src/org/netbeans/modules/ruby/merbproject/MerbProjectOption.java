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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.ruby.merbproject;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.netbeans.modules.ruby.merbproject.ui.MerbProjectSettings;
import org.netbeans.spi.options.AdvancedOption;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.Mutex;
import org.openide.util.NbBundle;

/**
 * Based on options panel in stripwhitespace module by Andrei Badea
 *
 * @author Tor Norbye
 */
public class MerbProjectOption extends AdvancedOption {
    
    public String getTooltip() {
        return getDisplayName();
    }

    public String getDisplayName() {
        return NbBundle.getMessage(MerbProjectOption.class, "MerbOption");
    }

    public OptionsPanelController create() {
        return new Controller();
    }

    private static final class Controller extends OptionsPanelController {
        private MerbOptionPanel component;

        public JComponent getComponent(Lookup masterLookup) {
            if (component == null) {
                component = new MerbOptionPanel();
            }

            return component;
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        public void update() {
            Mutex.EVENT.readAccess(new Runnable() {
                    public void run() {
                        boolean logical = MerbProjectSettings.getDefault().getLogicalView();
                        component.setLogicalChosen(logical);
                    }
                });
        }

        public boolean isValid() {
            return true;
        }

        public boolean isChanged() {
            return false;
        }

        public HelpCtx getHelpCtx() {
            return new HelpCtx(MerbProjectOption.class);
        }

        public void cancel() {
        }

        public void applyChanges() {
            Mutex.EVENT.readAccess(new Runnable() {
                    public void run() {
                        boolean logical = component.getLogicalChosen();
                        MerbProjectSettings.getDefault().setLogicalView(logical);
                    }
                });
        }
    }
}
