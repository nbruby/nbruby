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
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.DebuggerEngine;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.api.ruby.platform.RubyPlatformManager;
import org.netbeans.modules.ruby.debugger.breakpoints.RubyLineBreakpoint;
import org.netbeans.modules.ruby.debugger.breakpoints.RubyBreakpointManager;
import org.netbeans.modules.ruby.platform.execution.RubyExecutionDescriptor;
import org.netbeans.modules.ruby.platform.spi.RubyDebuggerImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.RequestProcessor;

public final class RubyDebuggerTest extends TestBase {
    
    private static final boolean VERBOSE = true;
    
    public RubyDebuggerTest(final String name) {
        super(name, VERBOSE);
    }
    
    @Override
    protected void setUp() throws Exception {
        clearWorkDir();
        super.setUp();
        watchStepping = false;
    }

    @Override
    protected boolean runInEQ() {
        return false;
    }

    public void testBasics() throws Exception {
        String[] testContent = {
            "puts 'aaa'",
            "puts 'bbb'",
            "puts 'ccc'",
            "puts 'ddd'",
            "puts 'eee'",
        };
        Process p = startDebugging(testContent, 2, 4);
        doContinue(); // 2 -> 4
        doAction(ActionsManager.ACTION_STEP_OVER); // 4 -> 5
        doContinue(); // finish
        waitFor(p);
    }

    public void testAttach() throws Exception {
        String[] testContent = {
            "sleep 0.01",
            "sleep 0.01",
            "sleep 0.01",
            "sleep 0.01"};
        File testF = createScript(testContent);
        FileObject testFO = FileUtil.toFileObject(testF);
        addBreakpoint(testFO, 2);
        addBreakpoint(testFO, 3);
        RubyPlatform platform = getTestConfiguredPlatform();
        int port = 12345;
        Process process = startDebuggerProcess(testF, port, platform);
        RubyDebugger debugger = new RubyDebugger();
        debugger.attach("localhost", port, 6);
        waitForSuspension();
        doContinue(); // 2 -> 3
        doContinue(); // 3 -> finish
        waitFor(process);
    }
    
    public void testStepInto() throws Exception {
        String[] testContent = {
            "def a",
            "  puts 'aaa'",
            "end",
            "a",
            "puts 'end'"
        };
        Process p = startDebugging(testContent, 4);
        doAction(ActionsManager.ACTION_STEP_INTO); // 4 -> 2
        doAction(ActionsManager.ACTION_STEP_OVER); // 2 -> 5
        doAction(ActionsManager.ACTION_STEP_OVER); // 5 -> finish
        waitFor(p);
    }
    
    public void testStepOut() throws Exception {
        String[] testContent = {
            "def a",
            "  puts 'a'",
            "  puts 'aa'",
            "  puts 'aaa'",
            "  puts 'aaaa'",
            "end",
            "a",
            "puts 'end'"
        };
        Process p = startDebugging(testContent, 2);
        doAction(ActionsManager.ACTION_STEP_OVER); // 2 -> 3
        doAction(ActionsManager.ACTION_STEP_OUT); // 3 -> 8
        doAction(ActionsManager.ACTION_STEP_OVER); // 8 -> finish
        waitFor(p);
    }
    
    public void testSimpleLoop() throws Exception {
        String[] testContent = {
            "1.upto(3) {",
            "  puts 'aaa'",
            "  puts 'bbb'",
            "  puts 'ccc'",
            "}",
        };
        File testF = createScript(testContent);
        FileObject testFO = FileUtil.toFileObject(testF);
        addBreakpoint(testFO, 2);
        RubyLineBreakpoint bp4 = addBreakpoint(testFO, 4);
        Process p = startDebugging(testF);
        doContinue(); // 2 -> 4
        doContinue(); // 4 -> 2
        RubyBreakpointManager.removeBreakpoint(bp4);
        doContinue(); // 2 -> 2
        doContinue(); // 2 -> finish
        waitFor(p);
    }
    
    public void testSpaceAndSemicolonsInPath() throws Exception {
        String[] testContent = {
            "1.upto(3) {",
            "  puts 'aaa'",
            "  puts 'bbb'",
            "  puts 'ccc'",
            "}",
        };
        File testF = createScript(testContent, "path spaces semi:colon.rb");
        FileObject testFO = FileUtil.toFileObject(testF);
        addBreakpoint(testFO, 2);
        RubyLineBreakpoint bp4 = addBreakpoint(testFO, 4);
        Process p = startDebugging(testF);
        doContinue(); // 2 -> 4
        doContinue(); // 4 -> 2
        RubyBreakpointManager.removeBreakpoint(bp4);
        doContinue(); // 2 -> 2
        doContinue(); // 2 -> finish
        waitFor(p);
    }
    
    //    public void testScriptArgumentsNoticed() throws Exception {
    //        String[] scriptArgs = { "--used-languages", "Ruby and Java" };
    //        String[] testContent = {
    //            "exit 1 if ARGV.size != 2",
    //            "puts 'OK'"
    //        };
    //        Process p = startDebugging(testContent, 2);
    //        Thread.sleep(3000); // TODO: do not depend on timing (use e.g. RubyDebugEventListener)
    //        doContinue(); // 2 -> finish
    //        waitFor(p);
    //    }
    
    public void testBreakpointsRemovingFirst() throws Exception {
        String[] testContent = {
            "3.times do", // 1
            "  b=10",     // 2
            "  b=11",     // 3
            "end"         // 4
        };
        File testF = createScript(testContent);
        FileObject testFO = FileUtil.toFileObject(testF);
        RubyLineBreakpoint bp2 = addBreakpoint(testFO, 2);
        addBreakpoint(testFO, 3);
        Process p = startDebugging(testF);
        doContinue(); // 2 -> 3
        doContinue(); // 3 -> 2
        RubyBreakpointManager.removeBreakpoint(bp2);
        doContinue(); // 2 -> 3
        doContinue(); // 3 -> 3
        doContinue(); // 3 -> finish
        waitFor(p);
    }
    
    public void testBreakpointsUpdating() throws Exception {
        String[] testContent = {
            "4.times do", // 1
            "  b=10",     // 2
            "  b=11",     // 3
            "end"         // 4
        };
        File testF = createScript(testContent);
        FileObject testFO = FileUtil.toFileObject(testF);
        RubyLineBreakpoint bp2 = addBreakpoint(testFO, 2);
        addBreakpoint(testFO, 3);
        Process p = startDebugging(testF);
        doContinue(); // 2 -> 3
        doContinue(); // 3 -> 2
        bp2.disable();
        doContinue(); // 2 -> 3
        doContinue(); // 3 -> 3
        bp2.enable();
        doContinue(); // 3 -> 2
        doContinue(); // 2 -> 3
        doContinue(); // 3 -> finish
        waitFor(p);
    }
    
    public void testFinish() throws Exception {
        String[] testContent = {
            "sleep 0.1", // 1
            "sleep 0.1", // 2
        };
        Process p = startDebugging(testContent, 2);
        Thread.sleep(3000); // TODO: rather wait for appropriate event
        doAction(ActionsManager.ACTION_KILL);
        waitFor(p);
    }

    public void testFinish2() throws Exception {
        String[] testContent = {
            "Thread.start() { puts 'hello from new thread' }",
            "puts 'main thread'"
        };
        Process p = startDebugging(testContent, 1);
        doAction(ActionsManager.ACTION_STEP_OVER);
        doAction(ActionsManager.ACTION_KILL);
        waitFor(p);
    }

    // XXX: check and enable
//    public void testFinishWhenSpawnedThreadIsSuspended() throws Exception {
//        String[] testContent = {
//            "Thread.start do",
//            "    puts '1'",
//            "end"
//        };
//        Process p = startDebugging(testContent, 2);
//        Thread.sleep(3000); // TODO: rather wait for appropriate event
//        doAction(ActionsManager.ACTION_KILL);
//        waitFor(p);
//    }
    
    public void testActionsFlood() throws Exception {
        // classic debugger only
        String[] testContent = {
            "20.times do",
            "    sleep 0.001",
            "end"
        };
        Process p = startDebugging(testContent, 2);
        while ((getEngineManager()) != null) {
            Thread.sleep(10);
            RequestProcessor.getDefault().post(new Runnable() {
                public void run() {
                    DebuggerEngine engine = getEngineManager();
                    if (engine != null) {
                        ActionsManager actionManager = engine.getActionsManager();
                        actionManager.doAction(ActionsManager.ACTION_STEP_OVER);
                    }
                }
            });
        }
        waitFor(p);
    }

    public void testDoNotStepIntoTheEval() throws Exception { // issue #106115
        String[] testContent = {
            "module A",
            "  module_eval(\"def A.a; sleep 0.01\\n sleep 0.01; end\")",
            "end",
            "A.a",
            "sleep 0.01",
            "sleep 0.01"
        };
        Process p = startDebugging(testContent, 4);
        doAction(ActionsManager.ACTION_STEP_INTO);
        doAction(ActionsManager.ACTION_STEP_INTO);
        doAction(ActionsManager.ACTION_STEP_INTO);
        waitFor(p);
    }
    
//    public void testDoNotStepIntoNonResolvedPath() throws Exception { // issue #106115
//        switchToJRuby();
//        String[] testContent = {
//            "require 'java'",
//            "import 'java.util.TreeSet'",
//            "t = TreeSet.new",
//            "t.add 1",
//            "t.add 2"
//        };
//        Process p = startDebugging(testContent, 3);
//        doAction(ActionsManager.ACTION_STEP_INTO);
//        doAction(ActionsManager.ACTION_STEP_INTO);
//        doAction(ActionsManager.ACTION_STEP_INTO);
//        waitFor(p);
//    }
    
    public void testCheckAndTuneSettings() throws IOException {
        RubyPlatform jruby = getSafeJRuby();
        RubyExecutionDescriptor descriptor = new RubyExecutionDescriptor(jruby);
        // DialogDisplayerImpl.createDialog() assertion would fail if dialog is shown
        assertTrue("default setting OK with JRuby", RubyDebugger.checkAndTuneSettings(descriptor));
        assertFalse("does not have fast debugger", jruby.hasFastDebuggerInstalled());
        installFakeFastRubyDebugger(jruby);
        assertTrue("succeed when fast debugger available", RubyDebugger.checkAndTuneSettings(descriptor));
    }

    public void testCheckAndTuneSettingsForJRubyAndRails() throws IOException {
        RubyPlatform jruby = RubyPlatformManager.getDefaultPlatform();
        RubyExecutionDescriptor descriptor = new RubyExecutionDescriptor(jruby);
        descriptor.fastDebugRequired(true); // simulate Rails
        assertTrue("default setting OK with JRuby and Rails", RubyDebugger.checkAndTuneSettings(descriptor));
    }

    public void testRubiniusDebugging() throws IOException {
        RubyPlatform rubinius = setUpRubinius();
        RubyExecutionDescriptor descriptor = new RubyExecutionDescriptor(rubinius);
        // DialogDisplayerImpl.createDialog() assertion would fail if dialog is shown
        RubyDebuggerImplementation rdi = new RubyDebugger();
        rdi.describeProcess(descriptor);
        assertFalse("Rubinius debuggin is not supported yet", rdi.prepare());
        assertFalse("Rubinius debuggin is not supported yet", RubyDebugger.checkAndTuneSettings(descriptor));
    }

    public void testSteppingThroughImportStatement() throws Exception {
        String[] testContent = {
            "require 'java'",
            "import 'java.lang.System'",
            "s = System",
        };
        Process p = startDebugging(testContent, 2);
        doAction(ActionsManager.ACTION_STEP_OVER);
        doAction(ActionsManager.ACTION_STEP_OVER);
        waitFor(p);
    }

    public void testJVMArguments() throws Exception {
        String[] testContent = {
            "require 'java'",
            "import 'java.lang.System'",
            "s = System",
        };
        setJVMArgs("-Xmx1024m");
        Process p = startDebugging(testContent, 2);
        doContinue();
        waitFor(p);
    }

    private DebuggerEngine getEngineManager() {
        return DebuggerManager.getDebuggerManager().getCurrentEngine();
    }
}

