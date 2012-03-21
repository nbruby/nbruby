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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.modules.ruby.platform.execution.ExecutionUtils;
import org.netbeans.modules.ruby.platform.Util;
import org.netbeans.modules.ruby.railsprojects.RailsProject;
import org.netbeans.modules.ruby.platform.execution.RubyExecutionDescriptor;
import org.netbeans.modules.ruby.railsprojects.RailsProjectUtil;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.ErrorManager;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.util.Exceptions;
import org.openide.util.HelpCtx;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;

/**
 * Class which handles plugin interactions - executing plugin, installing, uninstalling, etc.
 *
 * @todo Use the new ExecutionService to do process manapluginent.
 * @todo Consolidate with the GemManager
 *
 * @author Tor Norbye
 */
public class PluginManager {
    private static final String PLUGIN_CUSTOMIZER = "plugin.rb";

    private RailsProject project;
    /** Share over invocations of the dialog since these are slow to compute */
    private List<Plugin> installed;
    
    /** Share over invocations of the dialog since these are ESPECIALLY slow to compute */
    private static List<Plugin> available;
    private static List<Plugin> cachedAvailable;
    
    public PluginManager(RailsProject project) {
        this.project = project;
    }

    private FileObject getPluginDir() {
        FileObject pluginDirPath = project.getProjectDirectory().getFileObject("vendor/plugins"); // NOI18N
        
        return pluginDirPath;
    }
    /**
     * Return null if there are no problems running plugin. Otherwise return
     * an error message which describes the problem.
     */
    public String getPluginProblem() {
        FileObject pluginDirPath = getPluginDir();
        
        if (pluginDirPath == null) {
            // edge case, misconfiguration? plugin tool is installed but repository is not found
            return NbBundle.getMessage(PluginAction.class, "CannotFindPluginRepository");
        }
        
        File pluginDir = FileUtil.toFile(pluginDirPath);
        
        if (!pluginDir.canWrite()) {
            return NbBundle.getMessage(PluginAction.class, "PluginNotWritable");
        }
        
        return null;
    }
    
    /**
     * Checks whether a plugin with the given name is installed in the plugin
     * repository used by the current Rails project
     *
     * @param pluginName name of a plugin to be checked
     * @return <tt>true</tt> if installed; <tt>false</tt> otherwise
     */
    public boolean isPluginInstalled(final String pluginName) {
        FileObject dir = getPluginDir();
        return dir != null && dir.getFileObject(pluginName) != null;
    }
    
    /** WARNING: slow call! Synchronous plugin execution (unless refresh==false)! */
    public List<Plugin> getInstalledPlugins(boolean refresh, String sourceRepository, List<String> lines) {
        if (refresh || (installed == null) || (installed.size() == 0)) {
            installed = new ArrayList<Plugin>(40);
            refreshList(installed, sourceRepository, true, lines);
        }
        
        return installed;
    }
    
    /** WARNING: slow call! Synchronous plugin execution! */
    public List<Plugin> getAvailablePlugins(boolean refresh, String sourceRepository, List<String> lines) {
        if (refresh || (available == null) || (available.size() == 0)) {
            available = new ArrayList<Plugin>(300);
            refreshList(available, sourceRepository, false, lines);
            
            if (available.size() > 1) {
                updateCachedList(lines);
            }
        }
        
        return available;
    }
    
    public boolean hasUptodateAvailableList() {
        // Turned off caching
        //return available != null;
        return false;
    }
    
    public List<Plugin> getCachedAvailablePlugins() {
        // Turned off
        if (true) {
            return null;
        }

        if (available != null) {
            return available;
        }
        
        if (cachedAvailable != null) {
            return cachedAvailable;
        }
        
        cachedAvailable = getCachedList();
        
        return cachedAvailable;
    }
    
    private void refreshList(final List<Plugin> list, String sourceRepository, final boolean local, final List<String> lines) {
        list.clear();
        
        // Install the given plugin
        List<String> argList = new ArrayList<String>();
        
        if (sourceRepository != null) {
            argList.add("-s"); // NOI18N
            argList.add(sourceRepository);
        }
        
        if (local) {
            argList.add("--local"); // NOI18N
        } else {
            argList.add("--remote"); // NOI18N
        }
        
        String[] args = argList.toArray(new String[argList.size()]);
        boolean ok = pluginRunner("list", null, null, lines, args); // NOI18N
        
        if (ok) {
            parsePluginList(lines, list, local);
            
            // Sort the list
            Collections.sort(list);
        }
    }
    
    private void parsePluginList(List<String> lines, List<Plugin> pluginList, boolean local) {
        Plugin plugin = null;
        boolean listStarted = false;
        
        for (String line : lines) {
            // Pretty simple format - lines simply have the name on the left, and optionally
            // a repository on the right.
            // However, with the verbose flag there's more output. Even though I'm not
            // using --verbose right now, prepare for it in case it's being picked up by
            // user configuration files etc.           
            if (line.trim().length() == 0 || line.startsWith("/") || line.startsWith("Discovering plugins in ")) { // NOI18N
                continue;
            }

            StringBuilder sb = new StringBuilder();
            int i = 0;
            int length = line.length();
            for (; i < length; i++) {
                char c = line.charAt(i);
                if (Character.isWhitespace(c)) {
                    break;
                }
                sb.append(c);
            }
            String name = sb.toString();
            for (; i < length; i++) {
                if (Character.isWhitespace(line.charAt(i))) {
                    break;
                }
            }
            // Skip whitespace
            while (i < length && Character.isWhitespace(line.charAt(i))) {
                i++;
            }
            String repository = null;
            if (i < length) {
                sb = new StringBuilder();
                for (; i < length; i++) {
                    char c = line.charAt(i);
                    if (Character.isWhitespace(c)) {
                        break;
                    }
                    sb.append(c);
                }
                if (sb.length() > 0) {
                    repository = sb.toString();
                }
            }
            
            plugin = new Plugin(name, repository);
            pluginList.add(plugin);
        }
    }
    
    /** Non-blocking plugin executor which also provides progress UI etc. */
    private void asynchPluginRunner(final JComponent parent, final String description,
            final String successMessage, final String failureMessage, final List<String> lines,
            final Runnable successCompletionTask, final String command, final String... commandArgs) {
        final Cursor originalCursor = parent.getCursor();
        Cursor busy = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
        parent.setCursor(busy);
        
        final JButton closeButton = new JButton(NbBundle.getMessage(PluginManager.class, "CTL_Close"));
        final JButton cancelButton =
                new JButton(NbBundle.getMessage(PluginManager.class, "CTL_Cancel"));
        closeButton.getAccessibleContext()
                .setAccessibleDescription(NbBundle.getMessage(PluginManager.class, "AD_Close"));
        
        Object[] options = new Object[] { closeButton, cancelButton };
        closeButton.setEnabled(false);
        
        final PluginProgressPanel progress =
                new PluginProgressPanel(NbBundle.getMessage(PluginManager.class, "PluginPleaseWait"));
        DialogDescriptor descriptor =
                new DialogDescriptor(progress, description, true, options, closeButton,
                DialogDescriptor.DEFAULT_ALIGN, new HelpCtx(PluginProgressPanel.class), null); // NOI18N
        descriptor.setModal(true);
        
        final Process[] processHolder = new Process[1];
        final Dialog dlg = DialogDisplayer.getDefault().createDialog(descriptor);
        
        closeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ev) {
                dlg.setVisible(false);
                dlg.dispose();
                parent.setCursor(originalCursor);
            }
        });
        
        Runnable runner =
                new Runnable() {
            public void run() {
                try {
                    boolean succeeded =
                            pluginRunner(command, progress, processHolder, lines, commandArgs);
                    
                    closeButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    
                    progress.done(succeeded ? successMessage : failureMessage);
                    
                    if (succeeded && (successCompletionTask != null)) {
                        successCompletionTask.run();
                    }
                } finally {
                    parent.setCursor(originalCursor);
                }
            }
        };
        
        RequestProcessor.getDefault().post(runner, 50);
        
        dlg.setVisible(true);
        
        if ((descriptor.getValue() == DialogDescriptor.CANCEL_OPTION) ||
                (descriptor.getValue() == cancelButton)) {
            parent.setCursor(originalCursor);
            cancelButton.setEnabled(false);
            
            Process process = processHolder[0];
            
            if (process != null) {
                process.destroy();
                dlg.setVisible(false);
                dlg.dispose();
            }
        }
    }
    
    private List<String> getPluginCmd() {
        // XXX: basically just placeholder code as the plugin manager 
        // is disabled for Rails 3 projects as in Rails 3 it is not 
        // possible to list installed/available plugins, rendering 
        // the plugin manager basically useless
        List<String> pluginCmd = new ArrayList<String>(2);
        if (RailsProjectUtil.getRailsVersion(project).isRails3OrHigher()) {
            pluginCmd.add("script" + File.separator + "rails");//NOI18N
            pluginCmd.add("plugin"); //NOI18N
        } else {
            pluginCmd.add("script" + File.separator + "plugin"); // NOI18N
        }
        return pluginCmd;
    }

    private boolean pluginRunner(String command, PluginProgressPanel progressPanel,
            Process[] processHolder, List<String> lines, String... commandArgs) {

        List<String> argList = new ArrayList<String>();
        
        RubyPlatform platform = RubyPlatform.platformFor(project);
        File cmd = new File(platform.getInterpreter());
        
        if (!cmd.getName().startsWith("jruby") || ExecutionUtils.launchJRubyScript()) { // NOI18N
            argList.add(cmd.getPath());
        }
        
        argList.addAll(ExecutionUtils.getRubyArgs(platform));
        // see #142698
        argList.add("-r" + getPluginCustomizer().getAbsolutePath()); //NOI18N
        argList.addAll(getPluginCmd());
        argList.add(command);
        
        for (String arg : commandArgs) {
            argList.add(arg);
        }
        
        String[] args = argList.toArray(new String[argList.size()]);
        ProcessBuilder pb = new ProcessBuilder(args);
        File pwd = FileUtil.toFile(project.getProjectDirectory());
        pb.directory(pwd);
        pb.redirectErrorStream(true);
        
        Util.adjustProxy(pb);
        // PATH additions for JRuby etc.
        RubyExecutionDescriptor descriptor = new RubyExecutionDescriptor(platform, "plugin", pb.directory()).cmd(cmd);
        ExecutionUtils.setupProcessEnvironment(pb.environment(), descriptor.getCmd().getParent(), descriptor.getAppendJdkToPath());
        
        if (lines == null) {
            lines = new ArrayList<String>(40);
        }
        
        int exitCode = -1;
        
        try {
            Process process = pb.start();
            
            if (processHolder != null) {
                processHolder[0] = process;
            }
            
            InputStream is = process.getInputStream();
            
            if (progressPanel != null) {
                progressPanel.setProcessInput(process.getOutputStream());
            }
            
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            
            try {
                while (true) {
                    line = br.readLine();
                    
                    if (line == null) {
                        break;
                    }
                    
                    if (progressPanel != null) {
                        // Add "\n" ?
                        progressPanel.appendOutput(line);
                    }
                    
                    lines.add(line);
                }
            } catch (IOException ioe) {
                // When we cancel we call Process.destroy which may quite possibly
                // raise an IO Exception in this thread reading text out of the
                // process. Silently ignore that.
                String message = "*** Plugin Process Killed ***\n"; // NOI18N
                lines.add(message);
                
                if (progressPanel != null) {
                    progressPanel.appendOutput(message);
                }
            }
            
            exitCode = process.waitFor();
            
            if (exitCode != 0) {
                try {
                    // This might not be necessary now that I'm
                    // calling ProcessBuilder.redirectErrorStream(true)
                    // but better safe than sorry
                    is = process.getErrorStream();
                    isr = new InputStreamReader(is);
                    br = new BufferedReader(isr);
                    
                    while ((line = br.readLine()) != null) {
                        if (progressPanel != null) {
                            // Add "\n" ?
                            progressPanel.appendOutput(line);
                        }
                        
                        lines.add(line);
                    }
                } catch (IOException ioe) {
                    // When we cancel we call Process.destroy which may quite possibly
                    // raise an IO Exception in this thread reading text out of the
                    // process. Silently ignore that.
                    String message = "*** Plugin Process Killed ***\n"; // NOI18N
                    lines.add(message);
                    
                    if (progressPanel != null) {
                        progressPanel.appendOutput(message);
                    }
                }
            }
        } catch (IOException ex) {
            ErrorManager.getDefault().notify(ex);
        } catch (InterruptedException ex) {
            ErrorManager.getDefault().notify(ex);
        }
        
        boolean succeeded = exitCode == 0;
        // see #142698
        if (succeeded && "remove".equals(command) && commandArgs != null && commandArgs[0] != null) { //NOI18N
            FileObject plugin = project.getProjectDirectory().getFileObject("vendor/plugins/" + commandArgs[0]); //NOI18N
            if (plugin != null) {
                try {
                    plugin.delete();
                } catch (IOException ex) {
                    succeeded = false;
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return succeeded;
    }
    
    /**
     * Install the given plugin.
     *
     * @param plugin Plugin description for the plugin to be installed. Only the name is relevant.
     * @param parent For asynchronous tasks, provide a parent JComponent that will have progress dialogs added,
     *   a possible cursor change, etc.
     * @param asynchronous If true, run the plugin task asynchronously - returning immediately and running the plugin task
     *    in a background thread. A progress bar and message will be displayed (along with the option to view the
     *    plugin output). If the exit code is normal, the completion task will be run at the end.
     * @param asyncCompletionTask If asynchronous is true and the plugin task completes normally, this task will be run at the end.
     * @param rdoc If true, generate rdoc as part of the installation
     * @param ri If true, generate ri data as part of the installation
     * @param version If non null, install the specified version rather than the latest available version
     */
    public boolean install(Plugin[] plugins, JComponent parent, String sourceRepository,
            boolean svnExternals, boolean svnCheckout, String revision, boolean asynchronous,
            Runnable asyncCompletionTask) {
        // Install the given plugin
        List<String> argList = new ArrayList<String>();
        
        if (sourceRepository != null) {
            argList.add("-s"); // NOI18N
            argList.add(sourceRepository);
        }
        
        //argList.add("--verbose"); // NOI18N

        for (Plugin plugin : plugins) {
            argList.add(plugin.getName());
        }
        
        if (svnExternals) {
            argList.add("--externals"); // NOI18N
        } else if (svnCheckout) {
            argList.add("--checkout"); // NOI18N
        }
        
        if (revision != null && (svnExternals || svnCheckout)) {
            argList.add("--revision"); // NOI18N
            argList.add(revision);
        }
        
        // see #123758
        argList.add("--force"); //NOI18N
        
        String[] args = argList.toArray(new String[argList.size()]);
        
        String title = NbBundle.getMessage(PluginManager.class, "Installation");
        String success = NbBundle.getMessage(PluginManager.class, "InstallationOk");
        String failure = NbBundle.getMessage(PluginManager.class, "InstallationFailed");
        String pluginCmd = "install"; // NOI18N
        
        if (asynchronous) {
            asynchPluginRunner(parent, title, success, failure, null, asyncCompletionTask, pluginCmd, args);
            
            return false;
        } else {
            boolean ok = pluginRunner(pluginCmd, null, null, null, args);
            return ok;
        }
    }
    
    /**
     * Uninstall the given plugin.
     *
     * @param plugin Plugin description for the plugin to be uninstalled. Only the name is relevant.
     * @param parent For asynchronous tasks, provide a parent JComponent that will have progress dialogs added,
     *   a possible cursor change, etc.
     * @param asynchronous If true, run the plugin task asynchronously - returning immediately and running the plugin task
     *    in a background thread. A progress bar and message will be displayed (along with the option to view the
     *    plugin output). If the exit code is normal, the completion task will be run at the end.
     * @param asyncCompletionTask If asynchronous is true and the plugin task completes normally, this task will be run at the end.
     */
    public boolean uninstall(Plugin[] plugins, String sourceRepository, JComponent parent,
            boolean asynchronous, Runnable asyncCompletionTask) {
        // Install the given plugin
        List<String> argList = new ArrayList<String>();
        
        if (sourceRepository != null) {
            argList.add("-s"); // NOI18N
            argList.add(sourceRepository);
        }
        
        // This string is replaced in the loop below, one gem at a time as we iterate over the
        // deletion results
        int nameIndex = argList.size();
        argList.add("placeholder"); // NOI18N

        //argList.add("--verbose"); // NOI18N
        
        String[] args = argList.toArray(new String[argList.size()]);
        String title = NbBundle.getMessage(PluginManager.class, "Uninstallation");
        String success = NbBundle.getMessage(PluginManager.class, "UninstallationOk");
        String failure = NbBundle.getMessage(PluginManager.class, "UninstallationFailed");
        String pluginCmd = "remove"; // NOI18N
        
        if (asynchronous) {
            for (Plugin plugin : plugins) {
                args[nameIndex] = plugin.getName();
                asynchPluginRunner(parent, title, success, failure, null, asyncCompletionTask, pluginCmd,
                        args);
            }
            
            return false;
        } else {
            boolean ok = true;
            
            for (Plugin plugin : plugins) {
                args[nameIndex] = plugin.getName();
                
                if (!pluginRunner(pluginCmd, null, null, null, args)) {
                    ok = false;
                }
            }
            
            return ok;
        }
    }
    
    /**
     * Update the given plugin, or all plugins if plugin == null
     *
     * @param plugin Plugin description for the plugin to be uninstalled. Only the name is relevant. If null, all installed plugins
     *    will be updated.
     * @param parent For asynchronous tasks, provide a parent JComponent that will have progress dialogs added,
     *   a possible cursor change, etc.
     * @param asynchronous If true, run the plugin task asynchronously - returning immediately and running the plugin task
     *    in a background thread. A progress bar and message will be displayed (along with the option to view the
     *    plugin output). If the exit code is normal, the completion task will be run at the end.
     * @param asyncCompletionTask If asynchronous is true and the plugin task completes normally, this task will be run at the end.
     */
    public boolean update(Plugin[] plugins, String revision, String sourceRepository, JComponent parent,
            boolean asynchronous, Runnable asyncCompletionTask) {
        // Install the given plugin
        List<String> argList = new ArrayList<String>();
        
        if (sourceRepository != null) {
            argList.add("-s"); // NOI18N
            argList.add(sourceRepository);
        }
        
        //argList.add("--verbose"); // NOI18N

        // If you specify a revision, only specify a single plugin
        assert revision == null || revision.length() == 0 || plugins.length == 1;
        
        if (plugins != null) {
            for (Plugin plugin : plugins) {
                argList.add(plugin.getName());
            }
        }
        
        if (revision != null) {
            argList.add("--revision"); // NOI18N
            argList.add(revision);
        }
        

        String[] args = argList.toArray(new String[argList.size()]);
        
        String title = NbBundle.getMessage(PluginManager.class, "Update");
        String success = NbBundle.getMessage(PluginManager.class, "UpdateOk");
        String failure = NbBundle.getMessage(PluginManager.class, "UpdateFailed");
        String pluginCmd = "update"; // NOI18N
        
        if (asynchronous) {
            asynchPluginRunner(parent, title, success, failure, null, asyncCompletionTask, pluginCmd, args);
            
            return false;
        } else {
            boolean ok = pluginRunner(pluginCmd, null, null, null, args);
            return ok;
        }
    }
    
    public boolean removeRepositories(String[] repositories, JComponent parent, ProgressHandle progressHandle,
            boolean asynchronous, Runnable asyncCompletionTask) {
        return modifyRepositories("unsource", repositories, parent, progressHandle, asynchronous, asyncCompletionTask); // NOI18N
    }

    public boolean addRepositories(String[] repositories, JComponent parent, ProgressHandle progressHandle,
            boolean asynchronous, Runnable asyncCompletionTask) {
        return modifyRepositories("source", repositories, parent, progressHandle, asynchronous, asyncCompletionTask); // NOI18N
    }
    
    public boolean modifyRepositories(String pluginCmd, String[] repositories, JComponent parent, ProgressHandle progressHandle,
            boolean asynchronous, Runnable asyncCompletionTask) {
        // Install the given plugin
        List<String> argList = new ArrayList<String>();
        
        //argList.add("--verbose"); // NOI18N

        // If you specify a revision, only specify a single plugin
        
        for (String repository : repositories) {
            argList.add(repository);
        }
        
        String[] args = argList.toArray(new String[argList.size()]);
        
        String title = NbBundle.getMessage(PluginManager.class, "ModifySource");
        String success = NbBundle.getMessage(PluginManager.class, "ModifySourceOk");
        String failure = NbBundle.getMessage(PluginManager.class, "ModifySourceFailed");
        
        if (asynchronous) {
            asynchPluginRunner(parent, title, success, failure, null, asyncCompletionTask, pluginCmd, args);
            
            return false;
        } else {
            boolean ok = pluginRunner(pluginCmd, null, null, null, args);
            return ok;
        }
    }
    
    public List<String> getRepositories(boolean local) {
        return local ? getLocalRepositories() : getRemoteRepositories();
    }

    private List<String> getRemoteRepositories() {
        List<String> lines = new ArrayList<String>(150);
        
        // Install the given plugin
        List<String> argList = new ArrayList<String>();
        
        argList.add("--list"); // NOI18N
        
        String[] args = argList.toArray(new String[argList.size()]);
        boolean ok = pluginRunner("discover", null, null, lines, args); // NOI18N

        if (ok) {
            return lines;
        } else {
            return Collections.emptyList();
        }
    }
    
    private List<String> getLocalRepositories() {
        List<String> lines = new ArrayList<String>(150);
        
        // Install the given plugin
        List<String> argList = new ArrayList<String>();
        
        //argList.add("--check"); // NOI18N
        
        String[] args = argList.toArray(new String[argList.size()]);
        boolean ok = pluginRunner("sources", null, null, lines, args); // NOI18N

        if (ok) {
            return lines;
        } else {
            return Collections.emptyList();
        }
    }
    
    private List<Plugin> getCachedList() {
        synchronized (PluginManager.class) {
            BufferedReader is = null;
            
            try {
                File cacheFile = getCacheFile();
                
                if (!cacheFile.exists()) {
                    return null;
                }
                
                List<String> lines = new ArrayList<String>(5000);
                is = new BufferedReader(new FileReader(getCacheFile()));
                
                while (true) {
                    String line = is.readLine();
                    
                    if (line == null) {
                        break;
                    }
                    
                    lines.add(line);
                }
                
                List<Plugin> list = new ArrayList<Plugin>();
                parsePluginList(lines, list, false);
                
                return list;
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
                
                return null;
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioe) {
                        Exceptions.printStackTrace(ioe);
                    }
                }
            }
        }
    }
    
    private void updateCachedList(List<String> lines) {
        // Disabled for now
        if (true) {
            return;
        }

        synchronized (PluginManager.class) {
            PrintWriter os = null;
            
            try {
                File cacheFile = getCacheFile();
                
                if (cacheFile.exists()) {
                    cacheFile.delete();
                }
                
                os = new PrintWriter(new BufferedWriter(new FileWriter(getCacheFile())));
                
                for (String line : lines) {
                    os.println(line);
                }
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            } finally {
                if (os != null) {
                    os.close();
                }
            }
        }
    }
    
    private static File getCacheFile() {
        return new File(getCacheFolder(), "remotePlugins.txt"); // NOI18N
    }
    
    private static File getCacheFolder() {
        final String nbUserProp = System.getProperty("netbeans.user"); // NOI18N
        assert nbUserProp != null;
        
        final File nbUserDir = new File(nbUserProp);
        File cacheFolder =
                FileUtil.normalizeFile(new File(nbUserDir,
                "var" + File.separator + "cache" + File.separatorChar)); // NOI18N
        
        if (!cacheFolder.exists()) {
            boolean created = cacheFolder.mkdirs();
            assert created : "Cannot create cache folder"; //NOI18N
        } else {
            assert cacheFolder.isDirectory() && cacheFolder.canRead() && cacheFolder.canWrite();
        }
        
        return cacheFolder;
    }
    
    private static synchronized File getPluginCustomizer() {
        File pluginScript = InstalledFileLocator.getDefault().locate(
                PLUGIN_CUSTOMIZER, "org.netbeans.modules.ruby.railsproject", false);  // NOI18N

        if (pluginScript == null) {
            throw new IllegalStateException("Could not locate " + PLUGIN_CUSTOMIZER); // NOI18N

        }
        return pluginScript;
    }

}
