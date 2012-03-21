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

package org.netbeans.modules.ruby.debugger;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.ruby.platform.DialogDisplayerImpl;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.api.ruby.platform.RubyPlatformManager;
import org.netbeans.api.ruby.platform.TestUtil;
import org.netbeans.junit.MockServices;
import org.netbeans.modules.ruby.RubyTestBase;
import org.netbeans.modules.ruby.debugger.breakpoints.RubyBreakpoint;
import org.netbeans.modules.ruby.debugger.breakpoints.RubyLineBreakpoint;
import org.netbeans.modules.ruby.debugger.breakpoints.RubyBreakpointManager;
import org.netbeans.modules.ruby.platform.execution.DirectoryFileLocator;
import org.netbeans.modules.ruby.platform.execution.RubyExecutionDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.InstalledFileLocator;
import org.openide.text.Line;
import org.openide.util.Exceptions;
import org.openide.util.lookup.Lookups;
import org.rubyforge.debugcommons.RubyDebugEvent;
import org.rubyforge.debugcommons.RubyDebugEventListener;
import org.rubyforge.debugcommons.RubyDebuggerException;
import org.rubyforge.debugcommons.RubyDebuggerProxy;

public abstract class TestBase extends RubyTestBase {

    private static final Logger LOGGER = Logger.getLogger(TestBase.class.getName());

    static {
        RubySession.TEST = true;
        EditorUtil.showLines = false;
    }
    private TestHandler testHandler;
    private boolean verbose;

    protected static boolean watchStepping = false;
    private RubyPlatform platform;
    private String jvmArgs;
    
    private String origGemHome;
    private String origGemPath;

    protected TestBase(final String name, final boolean verbose) {
        super(name);
        this.verbose = verbose;
    }

    @Override
    protected void setUp() throws Exception {
        if (verbose) {
            testHandler = new TestHandler(getName());
            Logger nbLogger = Logger.getLogger("org.netbeans.modules.ruby.debugger");
            nbLogger.setLevel(Level.ALL);
            nbLogger.addHandler(testHandler);
            Logger commonsLogger = Logger.getLogger("org.rubyforge.debugcommons");
            commonsLogger.setLevel(Level.ALL);
            commonsLogger.addHandler(testHandler);
        }
        MockServices.setServices(DialogDisplayerImpl.class, IFL.class);
        touch(getWorkDir(), "config/Services/org-netbeans-modules-debugger-Settings.properties");
        super.setUp();
        platform = getTestConfiguredPlatform();

        doCleanUp();
        assertTrue("no breakpoints set", RubyBreakpointManager.getBreakpoints().length == 0);

        RubyPlatform jruby = RubyPlatformManager.getDefaultPlatform();
        origGemHome = jruby.getInfo().getGemHome();
        origGemPath = jruby.getInfo().getGemPath();
    }

    @Override
    protected void tearDown() throws Exception {
        RubyPlatform jruby = RubyPlatformManager.getDefaultPlatform();
        jruby.getInfo().setGemHome(origGemHome);
        jruby.getInfo().setGemPath(origGemPath);

        super.tearDown();
        if (verbose) {
            Logger nbLogger = Logger.getLogger("org.netbeans.modules.ruby.debugger");
            nbLogger.removeHandler(testHandler);
            Logger logger = Logger.getLogger("org.rubyforge.debugcommons");
            logger.removeHandler(testHandler);
        }
    }

    private void doCleanUp() {
        for (RubyBreakpoint bp : RubyBreakpointManager.getBreakpoints()) {
            try {
                DebuggerManager.getDebuggerManager().removeBreakpoint(bp);
            } catch (Throwable t) {
                Exceptions.printStackTrace(t);
            }
        }
        DebuggerManager.getDebuggerManager().finishAllSessions();
    }

    protected TestBase(final String name) {
        this(name, false);
    }

    public void setJVMArgs(String jvmArgs) {
        this.jvmArgs = jvmArgs;
    }

    protected Process startDebugging(final String[] rubyCode, final int... breakpoints) throws RubyDebuggerException, IOException, InterruptedException {
        File testF = createScript(rubyCode);
        FileObject testFO = FileUtil.toFileObject(testF);
        for (int breakpoint : breakpoints) {
            addBreakpoint(testFO, breakpoint);
        }
        return startDebugging(testF);
    }

    protected Process startDebugging(final File f) throws RubyDebuggerException, IOException, InterruptedException {
        return startDebugging(f, true);
    }

    protected Process startDebugging(final File toTest, final RubyPlatform platform) throws RubyDebuggerException, IOException, InterruptedException {
        return startDebugging(toTest, true, platform);
    }

    protected Process startDebugging(final File toTest, final boolean waitForSuspension) throws RubyDebuggerException, IOException, InterruptedException {
        return startDebugging(toTest, waitForSuspension, platform);
    }

    private Process startDebugging(final File toTest, final boolean waitForSuspension, final RubyPlatform platform) throws RubyDebuggerException, IOException, InterruptedException {
        RubyExecutionDescriptor desc = new RubyExecutionDescriptor(platform,
                toTest.getName(), toTest.getParentFile(), toTest.getAbsolutePath());
        assertTrue(platform.hasFastDebuggerInstalled());
        desc.fileLocator(new DirectoryFileLocator(FileUtil.toFileObject(toTest.getParentFile())));
        if (this.jvmArgs != null) {
            desc.jvmArguments(this.jvmArgs);
        }
        RubySession session = RubyDebugger.startDebugging(desc);
        session.getProxy().attach(RubyBreakpointManager.getBreakpoints());
        Process process = session.getProxy().getDebugTarget().getProcess();
        if (waitForSuspension) {
            waitForSuspension();
        }
        return process;
    }

    /** Start debuggee process without attaching to it. */
    protected Process startDebuggerProcess(
            final File toTest,
            final int port,
            final RubyPlatform platform) throws IOException {
        String rdebugIDE = Util.findRDebugExecutable(platform);
        String versionToken = '_' + platform.getLatestAvailableValidRDebugIDEVersions() + '_';
        List<String> args = Arrays.asList(platform.getInterpreter(), rdebugIDE, versionToken, "-p",
                "" + port, "--xml-debug", "--", toTest.getAbsolutePath());
        ProcessBuilder pb = new ProcessBuilder(args);
        pb.directory(toTest.getParentFile());
        LOGGER.fine("Running [basedir: " + toTest.getParentFile().getPath() +
                "]: \"" + getProcessAsString(args) + "\"");
        return pb.start();
    }

    protected void waitForSuspension() throws InterruptedException {
        RubySession session = Util.getCurrentSession();
        //        while (session.getFrames() == null || session.getFrames().length == 0) {
        while (!session.isSessionSuspended()) {
            Thread.sleep(300);
        }
    }

    /**
     * Creates test.rb script in the {@link #getWorkDir} with the given content.
     */
    protected File createScript(final String[] scriptContent) throws IOException {
        return createScript(scriptContent, "test.rb");
    }

    /**
     * Creates script with the given name in the {@link #getWorkDir} with the
     * given content.
     */
    protected File createScript(final String[] scriptContent, final String name) throws IOException {
        FileObject script = FileUtil.createData(FileUtil.toFileObject(getWorkDir()), name);
        PrintWriter pw = new PrintWriter(script.getOutputStream());
        try {
            for (String line : scriptContent) {
                pw.println(line);
            }
        } finally {
            pw.close();
        }
        return FileUtil.toFile(script);
    }

    protected static RubyLineBreakpoint addBreakpoint(final FileObject fo, final int line) throws RubyDebuggerException {
        return RubyBreakpointManager.addLineBreakpoint(createDummyLine(fo, line - 1));
    }

    public static void doAction(final Object action) throws InterruptedException {
        if (watchStepping) {
            Thread.sleep(3000);
        }
        DebuggerManager manager = DebuggerManager.getDebuggerManager();
        DebuggerEngine engine = manager.getCurrentEngine();
        ActionsManager actionManager = engine.getActionsManager();
        actionManager.doAction(action);
    }

    protected void doContinue() throws InterruptedException {
        waitForEvents(Util.getCurrentSession().getProxy(), 1, new Runnable() {
            public void run() {
                try {
                    doAction(org.netbeans.api.debugger.ActionsManager.ACTION_CONTINUE);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    protected void waitFor(final Process p) throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        new Thread(new Runnable() {
            public void run() {
                try {
                    p.waitFor();
                    latch.countDown();
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }).start();
        latch.await(10, TimeUnit.SECONDS);
        if (latch.getCount() > 0) {
            fail("Process " + p + " was not finished.");
            p.destroy();
        }
    }

    protected void waitForEvents(RubyDebuggerProxy proxy, int n, Runnable block) throws InterruptedException {
        final CountDownLatch events = new CountDownLatch(n);
        RubyDebugEventListener listener = new RubyDebugEventListener() {
            public void onDebugEvent(RubyDebugEvent e) {
                LOGGER.finer("Received event: " + e);
                events.countDown();
            }
        };
        proxy.addRubyDebugEventListener(listener);
        block.run();
        events.await();
        proxy.removeRubyDebugEventListener(listener);
    }

    @SuppressWarnings("deprecation")
    static Line createDummyLine(final FileObject fo, final int editorLineNum) {
        return new Line(Lookups.singleton(fo)) {
            public int getLineNumber() { return editorLineNum; }
            public void show(int kind, int column) { throw new UnsupportedOperationException("Not supported."); }
            public void setBreakpoint(boolean b) { throw new UnsupportedOperationException("Not supported."); }
            public boolean isBreakpoint() { throw new UnsupportedOperationException("Not supported."); }
            public void markError() { throw new UnsupportedOperationException("Not supported."); }
            public void unmarkError() { throw new UnsupportedOperationException("Not supported."); }
            public void markCurrentLine() { throw new UnsupportedOperationException("Not supported."); }
            public void unmarkCurrentLine() { throw new UnsupportedOperationException("Not supported."); }
        };
    }

    public static final class IFL extends InstalledFileLocator {

        public IFL() {}
        @Override
        public File locate(String relativePath, String codeNameBase, boolean localized) {
            if (relativePath.equals("ruby/debug-commons-0.9.5/classic-debug.rb")) {
                File rubydebugDir = getDirectory("rubydebug.dir", true);
                File cd = new File(rubydebugDir, "classic-debug.rb");
                assertTrue("classic-debug found in " + rubydebugDir, cd.isFile());
                return cd;
            } else if (relativePath.equals("jruby")) {
                return TestUtil.getXTestJRubyHome();
            } else {
                return null;
            }
        }
    }

    private static File resolveFile(final String property, final boolean mandatory) {
        String path = System.getProperty(property);
        assertTrue("must set " + property, !mandatory || (path != null));
        return path == null ? null : new File(path);
    }

    static File getFile(final String property, boolean mandatory) {
        File file = resolveFile(property, mandatory);
        assertTrue(file + " is file", !mandatory || file.isFile());
        return file;

    }

    static File getDirectory(final String property, final boolean mandatory) {
        File directory = resolveFile(property, mandatory);
        assertTrue(directory + " is directory", !mandatory || directory.isDirectory());
        return directory;
    }

    /** Just helper method for logging. */
    private static String getProcessAsString(List<? extends String> process) {
        StringBuilder sb = new StringBuilder();
        for (String arg : process) {
            sb.append(arg).append(' ');
        }
        return sb.toString().trim();
    }

    protected static RubyPlatform getTestConfiguredPlatform() throws IOException {
        File alternative = TestBase.getFile("ruby.executable", false);
        RubyPlatform platform;
        if (alternative != null) {
            platform = RubyPlatformManager.addPlatform(alternative);
        } else {
            platform = RubyPlatformManager.getDefaultPlatform();
        }
        assertTrue(platform + " has RubyGems installed", platform.hasRubyGemsInstalled());
        assertTrue(platform + " has fast debugger installed", platform.hasFastDebuggerInstalled());
        String problems = platform.getFastDebuggerProblemsInHTML();
        assertNull("fast debugger installed: " + problems, problems);
        return platform;
    }

    private static class TestHandler extends Handler {

        private final String name;

        TestHandler(final String name) {
            this.name = name;
        }

        public void publish(LogRecord rec) {
            PrintStream os = rec.getLevel().intValue() >= Level.WARNING.intValue() ? System.err : System.out;
            os.println("[" + System.currentTimeMillis() + "::" + name + "::" + rec.getLevel().getName() + "]: " + rec.getMessage());
            Throwable th = rec.getThrown();
            if (th != null) {
                th.printStackTrace(os);
            }
        }

        public void flush() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public void close() throws SecurityException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

}
