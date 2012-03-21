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
package org.netbeans.modules.ruby.testrunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.netbeans.api.extexecution.print.LineConvertors.FileLocator;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.modules.ruby.codecoverage.RubyCoverageProvider;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSession.SessionType;
import org.netbeans.modules.ruby.platform.execution.RubyExecutionDescriptor;
import org.netbeans.modules.ruby.rubyproject.spi.TestRunner;
import org.netbeans.modules.ruby.testrunner.ui.AutotestHandlerFactory;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.NbBundle;

/**
 * Handles running the autospec command, hooks its execution with the
 * UI test runner.
 *
 * @author Erno Mononen
 */
@org.openide.util.lookup.ServiceProvider(service = org.netbeans.modules.ruby.rubyproject.spi.TestRunner.class)
public class AutospecRunner implements TestRunner {

    private static final Logger LOGGER = Logger.getLogger(AutospecRunner.class.getName());
    private static final TestRunner INSTANCE = new AutospecRunner();
    private static final String NB_RSPEC_MEDIATOR = "NB_RSPEC_MEDIATOR"; //NOI18N
    private static final String AUTOSPEC_LOADER = "nb_autospec_loader.rb"; //NOI18N

    public AutospecRunner() {
    }

    public TestRunner getInstance() {
        return INSTANCE;
    }

    public boolean supports(TestType type) {
        return TestType.AUTOSPEC == type;
    }

    public void runTest(FileObject testFile, boolean debug) {
        throw new UnsupportedOperationException("Not supported."); //NOI18N
    }

    public void runSingleTest(FileObject testFile, String testMethod, boolean debug) {
        throw new UnsupportedOperationException("Not supported."); //NOI18N
    }

    public void runAllTests(Project project, boolean debug) {

        RubyPlatform platform = RubyPlatform.platformFor(project);
        if (!platform.hasValidAutoSpec(true)) {
            return;
        }

        String displayName = NbBundle.getMessage(AutospecRunner.class, "AutoSpec", ProjectUtils.getInformation(project).getDisplayName());
        FileLocator locator = project.getLookup().lookup(FileLocator.class);

        String autospec = platform.getAutoTest();
        RubyExecutionDescriptor desc = new RubyExecutionDescriptor(platform,
                displayName,
                FileUtil.toFile(project.getProjectDirectory()),
                autospec);

        TestRunnerUtilities.addProperties(desc, project);
        desc.addInitialArgs("-r \"" + getLoaderScript().getAbsolutePath() + "\""); //NOI18N
        Map<String, String> env = new HashMap<String, String>(2);
        addRspecMediatorOptionsToEnv(env);
        env.put("RSPEC", "true"); //NOI18N
        desc.addAdditionalEnv(env);
        desc.debug(debug);
        desc.allowInput();
        desc.fileLocator(locator);
        desc.addStandardRecognizers();

        RubyCoverageProvider coverageProvider = RubyCoverageProvider.get(project);
        if (coverageProvider != null && coverageProvider.isEnabled()) {
            desc = coverageProvider.wrapWithCoverage(desc, false, null);
        }

        TestSession session = new TestSession(displayName,
                project,
                debug ? SessionType.DEBUG : SessionType.TEST,
                new RubyTestRunnerNodeFactory());
        TestExecutionManager.getInstance().start(desc, new AutotestHandlerFactory(), session);
    }

    static void addRspecMediatorOptionsToEnv(Map<String, String> env) {
        // referenced from nb_autotest_loader.rb
        String options = "--require '" + RspecRunner.getMediatorScript().getAbsolutePath() + "' --runner NbRspecMediator";//NOI18N

        env.put(NB_RSPEC_MEDIATOR, options);
    }

    private static File getLoaderScript() {
        File mediatorScript = InstalledFileLocator.getDefault().locate(
                AUTOSPEC_LOADER, "org.netbeans.modules.ruby.testrunner", false);  // NOI18N

        if (mediatorScript == null) {
            throw new IllegalStateException("Could not locate " + AUTOSPEC_LOADER); // NOI18N

        }
        return mediatorScript;
    }
}
