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
 * Portions Copyrighted 2009 Sun Microsystems, Inc.
 */
package org.netbeans.modules.ruby.rubyproject;

import java.awt.Dialog;
import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.project.Project;
import org.netbeans.api.ruby.platform.RubyInstallation;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.modules.ruby.platform.execution.RubyExecutionDescriptor;
import org.netbeans.modules.ruby.platform.execution.RubyProcessCreator;
import org.netbeans.spi.project.ActionProvider;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.ServiceProvider;

/**
 * Action provider for running ruby files that are not part of a project.
 *
 * @author Erno Mononen
 */
@ServiceProvider(service = ActionProvider.class)
public final class RunFileActionProvider implements ActionProvider {

    // store for one session
    private static final Map<File, RunFileArgs> ARGS_FOR_FILE = new HashMap<File, RunFileArgs>();

    public String[] getSupportedActions() {
        return new String[]{ActionProvider.COMMAND_RUN_SINGLE, ActionProvider.COMMAND_DEBUG_SINGLE};
    }

    public void invokeAction(String command, Lookup context) throws IllegalArgumentException {
        boolean debug = ActionProvider.COMMAND_DEBUG_SINGLE.equals(command);
        for (DataObject d : context.lookupAll(DataObject.class)) {
            File f = FileUtil.toFile(d.getPrimaryFile());
            if (f != null) {
                RubyBaseActionProvider.saveFile(d.getPrimaryFile());
                runFile(f, debug);
            }
        }
    }

    static RunFileArgs getRunArgs(File file) {
        return ARGS_FOR_FILE.get(file);
    }

    /**
     * Displays the Run/Debug File dialog for the given file.
     * @param presetArgs
     * @param file
     * @param debug
     * @param allowPlatformChange
     * @return the args from the dialog, or <code>null</code> if the user
     * pressed cancel.
     */
    static RunFileArgs showDialog(RunFileArgs presetArgs,
            File file,
            boolean debug,
            boolean allowPlatformChange) {

        Object[] options = new Object[]{
            DialogDescriptor.OK_OPTION,
            DialogDescriptor.CANCEL_OPTION
        };

        RunFilePanel panel = new RunFilePanel(presetArgs, allowPlatformChange);

        String key = debug ? "DebugFile" : "RunFile"; //NOI18N
        DialogDescriptor descriptor = new DialogDescriptor(panel,
                NbBundle.getMessage(RunFileActionProvider.class, key, file.getName()), true,
                options, DialogDescriptor.OK_OPTION, DialogDescriptor.DEFAULT_ALIGN, null, null);

        descriptor.setClosingOptions(new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION});
        Dialog dialog = DialogDisplayer.getDefault().createDialog(descriptor);
        dialog.setVisible(true);
        dialog.getAccessibleContext().setAccessibleDescription(
                NbBundle.getMessage(RunFileActionProvider.class,
                debug ? "ACSD_DebugFile" : "ACSD_RunFile",
                file.getName()));
        if (descriptor.getValue() == DialogDescriptor.OK_OPTION) {
            RunFileArgs runFileArgs = panel.getArgs();
            ARGS_FOR_FILE.put(file, runFileArgs);
            return runFileArgs;
        }

        return null;

    }

    private void runFile(File file, boolean debug) {
        RunFileArgs existing = ARGS_FOR_FILE.get(file);
        if (existing != null && !existing.displayDialog) {
            doRun(file, existing, debug);
            return;
        }
        if (existing == null) {
            // init work dir
            existing = new RunFileArgs(null, null, null, null, file.getParent(), true);
        }

        RunFileArgs runFileArgs = showDialog(existing, file, debug, true);

        if (runFileArgs == null) {
            return;
        }
        doRun(file, runFileArgs, debug);
    }

    private void doRun(File file, RunFileArgs runFileArgs, boolean debug) {
        File workDir = new File(runFileArgs.getWorkDir());
        RubyExecutionDescriptor desc = new RubyExecutionDescriptor(runFileArgs.getPlatform(), file.getName(), workDir);
        if (runFileArgs.getRunArgs() != null) {
            desc.additionalArgs(Utilities.parseParameters(runFileArgs.getRunArgs()));
        }
        desc.jvmArguments(runFileArgs.getJvmArgs());
        desc.initialArgs(runFileArgs.getRubyOpts());
        desc.debug(debug);
        desc.script(file.getAbsolutePath());
        RubyProcessCreator rpc = new RubyProcessCreator(desc);
        if (rpc.isAbleToCreateProcess()) {
            ExecutionService.newService(rpc, desc.toExecutionDescriptor(), file.getName()).run();
        }
    }

    public boolean isActionEnabled(String command, Lookup context) throws IllegalArgumentException {
        Project p = context.lookup(Project.class);
        if (p != null) {
            return false;
        }
        Collection<? extends DataObject> files = context.lookupAll(DataObject.class);
        if (files.isEmpty()) {
            return false;
        }
        for (DataObject d : files) {
            if (!d.getPrimaryFile().getMIMEType().equals(RubyInstallation.RUBY_MIME_TYPE)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Holds the args last given for running (file specific).
     */
    static final class RunFileArgs {

        private final RubyPlatform platform;
        private final String runArgs;
        private final String jvmArgs;
        private final String rubyOpts;
        private final String workDir;
        private final boolean displayDialog;

        public RunFileArgs(RubyPlatform platform, String runArgs, String jvmArgs, String rubyOpts, String workDir, boolean displayDialog) {
            this.platform = platform;
            this.runArgs = runArgs;
            this.jvmArgs = jvmArgs;
            this.rubyOpts = rubyOpts;
            this.workDir = workDir;
            this.displayDialog = displayDialog;
        }

        public String getJvmArgs() {
            return jvmArgs;
        }

        public RubyPlatform getPlatform() {
            return platform;
        }

        public String getRunArgs() {
            return runArgs;
        }

        public boolean displayDialog() {
            return displayDialog;
        }

        public String getRubyOpts() {
            return rubyOpts;
        }

        public String getWorkDir() {
            return workDir;
        }

    }
}
