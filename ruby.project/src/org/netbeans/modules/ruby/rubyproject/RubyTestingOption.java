/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.netbeans.modules.ruby.rubyproject;

import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import org.netbeans.modules.ruby.rubyproject.spi.TestRunner.TestType;
import org.netbeans.spi.options.AdvancedOption;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;

/**
 * Options for the test runner.
 *
 * @author Erno Mononen
 */
public final class RubyTestingOption extends AdvancedOption {

    @Override
    public String getDisplayName() {
        return NbBundle.getMessage(RubyTestingOption.class, "RubyTestingOption.displayName.text");
    }

    @Override
    public String getTooltip() {
        return getDisplayName();
    }

    @Override
    public OptionsPanelController create() {
        return new Controller();
    }

    private static final class Controller extends OptionsPanelController {

        private final RubyTestingOptionsPanel component = new RubyTestingOptionsPanel();

        @Override
        public void update() {
            RubyTestingSettings settings = RubyTestingSettings.getDefault();
            component.setAutoTest(settings.useRunner(TestType.AUTOTEST));
            component.setAutoSpec(settings.useRunner(TestType.AUTOSPEC));
            component.setRspec(settings.useRunner(TestType.RSPEC));
            component.setTestUnit(settings.useRunner(TestType.TEST_UNIT));
        }

        @Override
        public void applyChanges() {
            RubyTestingSettings settings = RubyTestingSettings.getDefault();
            settings.setUseRunner(component.isAutoTestSelected(), TestType.AUTOTEST);
            settings.setUseRunner(component.isAutoSpecSelected(), TestType.AUTOSPEC);
            settings.setUseRunner(component.isRspecSelected(), TestType.RSPEC);
            settings.setUseRunner(component.isTestUnitSelected(), TestType.TEST_UNIT);
        }

        @Override
        public void cancel() {
        }

        @Override
        public boolean isValid() {
            return true;
        }

        @Override
        public boolean isChanged() {
            //XXX
            return false;
        }

        @Override
        public JComponent getComponent(Lookup masterLookup) {
            return component;
        }

        @Override
        public HelpCtx getHelpCtx() {
            return new HelpCtx(RubyTestingOption.class);
        }

        @Override
        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        @Override
        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

    }
}
