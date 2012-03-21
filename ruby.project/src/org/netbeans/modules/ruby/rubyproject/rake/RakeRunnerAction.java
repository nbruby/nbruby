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

package org.netbeans.modules.ruby.rubyproject.rake;

import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.modules.ruby.rubyproject.RubyBaseProject;
import org.netbeans.modules.ruby.rubyproject.Util;
import org.netbeans.modules.ruby.rubyproject.rake.RakeTaskChooser.TaskDescriptor;
import org.openide.NotifyDescriptor;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;

/**
 * Shows convenient runner for running or debugging Rake tasks, similar to e.g.
 * Go To File dialog.
 */
public final class RakeRunnerAction extends CallableSystemAction {

    public void performAction() {
        RubyBaseProject project = Util.inferRubyProject();
        if (project == null) {
            return;
        }

        RubyPlatform platform = RubyPlatform.platformFor(project);
        if (platform == null) {
            org.netbeans.modules.ruby.platform.Util.notifyLocalizedInfo(
                    RakeRunnerAction.class, "RakeRunnerAction.no.platform.for.project", // NOI18N
                    ProjectUtils.getInformation(project).getDisplayName(),
                    NotifyDescriptor.WARNING_MESSAGE);
            return;
        }
        if (!platform.showWarningIfInvalid()) {
            return;
        }

        TaskDescriptor taskDesc = RakeTaskChooser.select(project);
        if (taskDesc != null && taskDesc.getRakeTask() != null) {
            RakeRunner.runTask(project, taskDesc.getRakeTask(),
                    taskDesc.getTaskParams(), taskDesc.isDebug());
        }
    }

    public String getName() {
        return NbBundle.getMessage(RakeRunnerAction.class, "RakeRunnerAction.RunDebugRakeTask");
    }

    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() Javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE);
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return false;
    }
}