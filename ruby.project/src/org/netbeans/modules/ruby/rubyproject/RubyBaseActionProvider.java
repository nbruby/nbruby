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
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.ruby.platform.RubyInstallation;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.api.ruby.platform.RubyPlatformManager;
import org.netbeans.api.extexecution.ExecutionService;
import org.netbeans.api.extexecution.print.LineConvertor;
import org.netbeans.modules.ruby.platform.execution.RubyExecutionDescriptor;
import org.netbeans.modules.ruby.platform.execution.RubyProcessCreator;
import org.netbeans.modules.ruby.rubyproject.ui.customizer.RubyProjectProperties;
import org.netbeans.spi.project.ActionProvider;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Lookup;
import org.openide.util.Utilities;

/**
 * Base action provider of the Ruby projects.
 */
public abstract class RubyBaseActionProvider implements ActionProvider, ScriptDescProvider {

    private static final Logger LOGGER = Logger.getLogger(RubyBaseActionProvider.class.getName());

    /**
     * Standard command for running rdoc on a project.
     * @see org.netbeans.spi.project.ActionProvider
     */
    public static final String COMMAND_RDOC = "rdoc"; // NOI18N

    /**
     * Command for running auto test on this project (if installed)
     * @see org.netbeans.spi.project.ActionProvider
     */
    public static final String COMMAND_AUTOTEST = "autotest"; // NOI18N
    /**
     * Command for running autospec on this project (if installed)
     * @see org.netbeans.spi.project.ActionProvider
     */
    public static final String COMMAND_AUTOSPEC = "autospec"; // NOI18N

    /**
     * Command for running RSpec tests on this project (if installed)
     * @see org.netbeans.spi.project.ActionProvider
     */
    public static final String COMMAND_RSPEC = "rspec"; //NOI18N
    /**
     * Command for running all the rspec files in the project. Specifically
     * does not run the spec rake task (or any other rake task); runs
     * TestRunner#runAllTests always.
     */
    public static final String COMMAND_RSPEC_ALL = "rspec-all"; //NOI18N

    /**
     * The name of the test rake task.
     */
    protected static final String TEST_TASK_NAME = "test"; //NOI18N
    
    /**
     * The name of the spec rake task.
     */
    protected static final String RSPEC_TASK_NAME = "spec";//NOI18N


    private final RubyBaseProject project;
    private final UpdateHelper updateHelper;

    protected RubyBaseActionProvider(final RubyBaseProject project, final UpdateHelper updateHelper) {
        this.project = project;
        this.updateHelper = updateHelper;
    }

    protected abstract FileObject[] getSourceRoots();
    protected abstract FileObject[] getTestSourceRoots();
    
    protected abstract String[] getMimeTypes();

    protected RubyPlatform getPlatform() {
        RubyPlatform platform = RubyPlatform.platformFor(project);
        if (platform == null) {
            platform = RubyPlatformManager.getDefaultPlatform();
        }

        return platform;
    }

    protected UpdateHelper getUpdateHelper() {
        return updateHelper;
    }

    protected static void saveFile(final FileObject file) {
        // Save the file
        try {
            DataObject dobj = DataObject.find(file);
            if (dobj != null) {
                SaveCookie saveCookie = dobj.getCookie(SaveCookie.class);
                if (saveCookie != null) {
                    saveCookie.save();
                }
            }
        } catch (DataObjectNotFoundException donfe) {
            LOGGER.log(Level.SEVERE, donfe.getLocalizedMessage(), donfe);
        } catch (IOException ioe) {
            LOGGER.log(Level.SEVERE, ioe.getLocalizedMessage(), ioe);
        }
    }

    protected void runRubyScript(final FileObject fileObject, final String target,
            final String displayName, final Lookup context, final boolean debug,
            LineConvertor... extraConvertors) {
        if (!getPlatform().showWarningIfInvalid()) {
            return;
        }

        // bit messy here - we first get a desc preconfigured with project settings,
        // then modify it again with the args user may have provided in the run file dialog
        RubyExecutionDescriptor desc = getScriptDescriptor(null, fileObject, target, displayName, context, debug, extraConvertors);

        if (fileObject != null) { // null when running/debugging project, don't ask for parameters
            File file = FileUtil.toFile(fileObject);
            RunFileActionProvider.RunFileArgs args = RunFileActionProvider.getRunArgs(file);
            if (args == null) {
                String workDir = desc.getPwd() != null ? desc.getPwd().getAbsolutePath() : "";
                args = new RunFileActionProvider.RunFileArgs(getPlatform(),
                        asString(desc.getAdditionalArgs()),
                        asString(desc.getJVMArguments()),
                        desc.getInitialArgsPlain(),
                        workDir,
                        true);
            }
            if (args != null && args.displayDialog()) {
                args = RunFileActionProvider.showDialog(args, file, debug, false);
            }
            if (args == null) {
                // cancel pressed
                return;
            }
            if (args.getRunArgs() != null) {
                desc.additionalArgs(Utilities.parseParameters(args.getRunArgs()));
            }
            desc.jvmArguments(args.getJvmArgs());
            desc.initialArgs(args.getRubyOpts());
            desc.setPwd(new File(args.getWorkDir()));
        }

        RubyProcessCreator rpc = new RubyProcessCreator(desc, getSourceEncoding());
        if (rpc.isAbleToCreateProcess()) {
            ExecutionService service = ExecutionService.newService(rpc, desc.toExecutionDescriptor(), displayName);
            service.run();
        }
    }


    private static String asString(String[] array) {
        if (array == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            result.append(array[i]);
            if (i + 1 < array.length) {
                result.append(" "); //NOI18N
            }
        }
        return result.toString();
    }

    protected String getSourceEncoding() {
        return project.evaluator().getProperty(RubyProjectProperties.SOURCE_ENCODING);
    }

    /**
     * Find selected sources, the sources has to be under single source root,
     * 
     * @param context the lookup in which files should be found
     * @return The file objects in the sources folder
     */
    protected FileObject[] findSources(final Lookup context) {
        FileObject[] srcPath = getSourceRoots();
        for (int i = 0; i < srcPath.length; i++) {
            for (String mimeType : getMimeTypes()) {
                FileObject[] files = findSelectedFiles(context, srcPath[i], mimeType, true);
                if (files != null) {
                    return files;
                }
            }
        }
        return null;
    }

    /**
     * Find either selected tests or tests which belong to selected source
     * files.
     */
    protected FileObject[] findTestSources(final Lookup context) {
        //XXX: Ugly, should be rewritten
        FileObject[] testSrcPath = getTestSourceRoots();
        for (int i = 0; i < testSrcPath.length; i++) {
            FileObject[] files = findSelectedFiles(context, testSrcPath[i], RubyInstallation.RUBY_MIME_TYPE, true); // NOI18N
            if (files != null) {
                return files;
            }
        }
        return null;
    }

    protected FileObject getCurrentFile(final Lookup context) {
        FileObject[] fos = findSources(context);
        if (fos == null) {
            fos = findTestSources(context);
        }
        if (fos == null || fos.length == 0) {
            for (DataObject d : context.lookupAll(DataObject.class)) {
                FileObject fo = d.getPrimaryFile();
                if (fo.getMIMEType().equals(RubyInstallation.RUBY_MIME_TYPE)) {
                    return fo;
                }
            }
            return null;
        }

        return fos[0];
    }

    public static FileObject[] findSelectedFiles(
            final Lookup context, final FileObject dir,
            final String mimeType, final boolean strict) {
        if (dir != null && !dir.isFolder()) {
            throw new IllegalArgumentException("Not a folder: " + dir); // NOI18N
        }
        Collection<FileObject> files = new LinkedHashSet<FileObject>(); // #50644: remove dupes
        // XXX this should perhaps also check for FileObject's...
        for (DataObject d : context.lookupAll(DataObject.class)) {
            FileObject f = d.getPrimaryFile();
            boolean matches = FileUtil.toFile(f) != null;
            if (dir != null) {
                matches &= (FileUtil.isParentOf(dir, f) || dir == f);
            }
            if (mimeType != null) {
                matches &= f.getMIMEType().equals(mimeType);
            }
            // Generally only files from one project will make sense.
            // Currently the action UI infrastructure (PlaceHolderAction)
            // checks for that itself. Should there be another check here?
            if (matches) {
                files.add(f);
            } else if (strict) {
                return null;
            }
        }
        if (files.isEmpty()) {
            return null;
        }
        return files.toArray(new FileObject[files.size()]);
    }

    protected String[] getApplicationArguments() {
        String applicationArgs = project.evaluator().getProperty(SharedRubyProjectProperties.APPLICATION_ARGS);
        return (applicationArgs == null || applicationArgs.trim().length() == 0)
                ? null : Utilities.parseParameters(applicationArgs);
    }
}
