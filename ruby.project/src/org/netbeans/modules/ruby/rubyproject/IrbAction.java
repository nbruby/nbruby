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

package org.netbeans.modules.ruby.rubyproject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.netbeans.api.project.Project;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.api.ruby.platform.RubyPlatformManager;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.modules.ruby.platform.execution.RubyExecutionDescriptor;
import org.netbeans.modules.ruby.platform.execution.RubyProcessCreator;
import org.netbeans.modules.ruby.spi.project.support.rake.PropertyEvaluator;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.actions.CallableSystemAction;
import org.openide.windows.TopComponent;

/**
 * Action which shows IRB component.
 */
public final class IrbAction extends CallableSystemAction {
    
    private static final boolean USE_JRUBY_CONSOLE = Boolean.getBoolean("irb.jruby"); // NOI18N
    
    private boolean runIrbConsole(Project project) {
        PropertyEvaluator evaluator = project.getLookup().lookup(PropertyEvaluator.class);

        ActionProvider provider = project.getLookup().lookup(ActionProvider.class);
        if (!(provider instanceof ScriptDescProvider)) { // Lookup ScriptDescProvider directly?
            return false;
        }
        
        RubyPlatform platform = RubyPlatform.platformFor(project);
        if (platform == null) {
            platform = RubyPlatformManager.getDefaultPlatform();
        }
        String irbPath = platform.getIRB();
        if (irbPath == null) {
            return false;
        }

        ScriptDescProvider descProvider = (ScriptDescProvider)provider;
        List<String> additionalArgs = new ArrayList<String>(2);
        additionalArgs.add("--simple-prompt"); // NOI18N
        additionalArgs.add("--noreadline"); // NOI18N
        
        String displayName = NbBundle.getMessage(IrbAction.class, "CTL_IrbTopComponentWithPlatform", platform.getLabel());
        
        boolean debug = false;
        File pwd = FileUtil.toFile(project.getProjectDirectory());
        
        String charsetName = null;
        if (evaluator != null) {
            charsetName = evaluator.getProperty(SharedRubyProjectProperties.SOURCE_ENCODING);
        }
        String target = irbPath;
        RubyExecutionDescriptor desc = descProvider.getScriptDescriptor(pwd, null/*specFile?*/, target, displayName, project.getLookup(), debug, new TestNotifierLineConvertor(true, true));

        // Override args
        desc.additionalArgs(additionalArgs.toArray(new String[additionalArgs.size()]));
        desc.frontWindow(true);
        RubyProcessCreator rpc = new RubyProcessCreator(desc, charsetName);
        ExecutionService.newService(rpc, desc.toExecutionDescriptor(), displayName).run();
        
        return true;
    }

    @Override
    public void performAction() {
        if (USE_JRUBY_CONSOLE) {
            TopComponent win = IrbTopComponent.findInstance();
            win.open();
            win.requestActive();
            return;
        }
        
        RubyBaseProject project = Util.inferRubyProject();
        if (project != null) {
            runIrbConsole(project);
        } else {
            org.netbeans.modules.ruby.platform.Util.notifyLocalizedInfo(IrbAction.class, "IrbAction.not.project.in.context"); // NOI18N
        }
        
    }

    @Override
    public String getName() {
        return NbBundle.getMessage(IrbAction.class, "CTL_IrbAction");
    }

    @Override
    public HelpCtx getHelpCtx() {
        return HelpCtx.DEFAULT_HELP;
    }
    
    @Override
    protected boolean asynchronous() {
        return false;
    }
}
