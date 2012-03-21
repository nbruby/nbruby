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
package org.netbeans.modules.ruby.railsprojects.plugins;

import java.awt.Dialog;
import org.netbeans.api.project.Project;

import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.modules.ruby.railsprojects.RailsProject;
import org.netbeans.modules.ruby.railsprojects.RailsProjectUtil;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.Node;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.actions.NodeAction;

public final class PluginAction extends NodeAction {

    protected void performAction(Node[] activatedNodes) {
        Lookup lookup = activatedNodes[0].getLookup();
        Project p = lookup.lookup(Project.class);

        if (!RubyPlatform.platformFor(p).showWarningIfInvalid()) {
            return;
        }
        
        RailsProject project = (RailsProject)p;

        PluginManager manager = new PluginManager(project);
        String pluginProblem = manager.getPluginProblem();

        if (pluginProblem != null) {
            NotifyDescriptor nd =
                new NotifyDescriptor.Message(pluginProblem, NotifyDescriptor.Message.ERROR_MESSAGE);
            DialogDisplayer.getDefault().notify(nd);

            return;
        }

        PluginPanel customizer = new PluginPanel(manager);
        javax.swing.JButton close =
            new javax.swing.JButton(NbBundle.getMessage(PluginAction.class, "CTL_Close"));
        close.getAccessibleContext()
             .setAccessibleDescription(NbBundle.getMessage(PluginAction.class, "AD_Close"));

        DialogDescriptor descriptor =
            new DialogDescriptor(customizer, NbBundle.getMessage(PluginAction.class, "CTL_PluginTitle"),
                true, new Object[] { close }, close, DialogDescriptor.DEFAULT_ALIGN,
                new HelpCtx(PluginPanel.class), null); // NOI18N
        Dialog dlg = null;

        try {
            dlg = DialogDisplayer.getDefault().createDialog(descriptor);
            dlg.setVisible(true);
        } finally {
            if (dlg != null) {
                dlg.dispose();
            }
        }

        // The roots don't include Rails plugins yet anyway
        //if (customizer.isModified()) {
        //    RubyInstallation.getInstance().recomputeRoots();
        //}
    }

    protected boolean enable(Node[] activatedNodes) {
        if ((activatedNodes == null) || (activatedNodes.length != 1)) {
            return false;
        }

        Lookup lookup = activatedNodes[0].getLookup();
        Project project = lookup.lookup(Project.class);

        // see #183538
        return project instanceof RailsProject
                && !RailsProjectUtil.getRailsVersion(project).isRails3OrHigher();
    }
    
    @Override
    public String getName() {
        return NbBundle.getMessage(PluginAction.class, "CTL_PluginAction");
    }

    @Override
    protected void initialize() {
        super.initialize();
        // see org.openide.util.actions.SystemAction.iconResource() javadoc for more details
        putValue("noIconInMenu", Boolean.TRUE); // NOI18N
    }

    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }

    @Override
    protected boolean asynchronous() {
        return true;
    }
}
