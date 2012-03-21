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
package org.netbeans.modules.ruby.testrunner.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.modules.gsf.testrunner.api.Manager;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.TestSuite;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.Trouble;
import org.netbeans.modules.ruby.rubyproject.spi.TestRunner.TestType;
import org.netbeans.modules.ruby.testrunner.TestRunnerUtilities;
import org.openide.util.NbBundle;

/**
 * An output recognizer for parsing output of the test/unit runner script,
 * <code>nb_test_mediator.rb</code>. Updates the test result UI.
 *
 * @author Erno Mononen
 */
public class TestUnitHandlerFactory implements TestHandlerFactory {

    private static final Logger LOGGER = Logger.getLogger(TestUnitHandlerFactory.class.getName());

    public boolean printSummary() {
        return true;
    }

    public List<TestRecognizerHandler> createHandlers() {
        List<TestRecognizerHandler> result = new ArrayList<TestRecognizerHandler>();
        result.add(new SuiteStartingHandler());
        result.add(new SuiteStartedHandler());
        result.add(new SuiteFinishedHandler());
        result.add(new SuiteErrorOutputHandler());
        result.add(new ShouldaTestStartedHandler());
        result.add(new ShouldaTestFailedHandler());
        result.add(new ShouldaTestErrorHandler());
        result.add(new TestStartedHandler());
        result.add(new TestFailedHandler());
        result.add(new TestErrorHandler());
        result.add(new ShouldaTestFinishedHandler());
        result.add(new TestFinishedHandler());
        result.add(new TestLoggerHandler());
        result.add(new TestMiscHandler());
        result.add(new SuiteMiscHandler());
        return result;
    }

    private static String errorMsg(long failureCount) {
        return NbBundle.getMessage(TestUnitHandlerFactory.class, "MSG_Error", failureCount);
    }

    private static String failureMsg(long failureCount) {
        return NbBundle.getMessage(TestUnitHandlerFactory.class, "MSG_Failure", failureCount);
    }

    static String[] getStackTrace(String message, String stackTrace) {
        List<String> stackTraceList = new ArrayList<String>();
        stackTraceList.add(message);
        for (String location : stackTrace.split("%BR%")) { //NOI18N
            if (!TestRunnerUtilities.filterOutFromStacktrace(location)) {
                stackTraceList.add(location);
            }
        }
        return stackTraceList.toArray(new String[stackTraceList.size()]);
    }

    static class TestFailedHandler extends TestRecognizerHandler {

        private List<String> output;

        public TestFailedHandler(String regex) {
            super(regex);
        }

        public TestFailedHandler() {
            super("%TEST_FAILED%\\stime=(.+)\\stestname=(.+)\\((.+)\\)\\smessage=(.*)\\slocation=(.*)"); //NOI18N
        }


        @Override
        void updateUI( Manager manager, TestSession session) {
            Testcase testcase = new Testcase(matcher.group(2), TestType.TEST_UNIT.name(), session);
            testcase.setTimeMillis(toMillis(matcher.group(1)));
            testcase.setClassName(matcher.group(3));
            testcase.setTrouble(new Trouble(false));

            String message = matcher.group(4);
            testcase.getTrouble().setComparisonFailure(getComparisonFailure(message));

            String location = matcher.group(5);
            testcase.getTrouble().setStackTrace(getStackTrace(message, location));
            session.addTestCase(testcase);

            String failureMsg = failureMsg(session.incrementFailuresCount());
            String testCase = testcase.getName() + "(" + testcase.getClassName() + "):";
            output = new ArrayList<String>();
            output.add("");
            output.add(failureMsg);
            output.add(testCase);
            output.addAll(Arrays.asList(testcase.getTrouble().getStackTrace()));
            output.add("");
            
            manager.displayOutput(session, "", false);
            manager.displayOutput(session, failureMsg, false);
            manager.displayOutput(session, testCase, false); //NOI18N
            for (String line : testcase.getTrouble().getStackTrace()) {
                manager.displayOutput(session, line, false);
            }
            manager.displayOutput(session, "", false);
            testcase.addOutputLines(output);
        }

        @Override
        List<String> getRecognizedOutput() {
            return new ArrayList<String>(output);
        }
    }

    /**
     * Support for Shoulda tests -- see #150613
     */
    static class ShouldaTestFailedHandler extends TestFailedHandler {

        public ShouldaTestFailedHandler() {
            super("%TEST_FAILED%\\stime=(.+)\\stestname=test:\\s(.*)\\.\\s\\((.+)\\)\\smessage=(.*)\\slocation=(.*)"); //NOI18N
        }
    }

    static class TestErrorHandler extends TestRecognizerHandler {

        private List<String> output;


        public TestErrorHandler() {
            this("%TEST_ERROR%\\stime=(.+)\\stestname=(.+)\\((.+)\\)\\smessage=(.*)\\slocation=(.*)"); //NOI18N
        }

        public TestErrorHandler(String regex) {
            super(regex); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
            Testcase testcase = new Testcase(matcher.group(2), TestType.TEST_UNIT.name(), session);
            testcase.setTimeMillis(toMillis(matcher.group(1)));
            testcase.setClassName(matcher.group(3));
            testcase.setTrouble(new Trouble(true));
            testcase.getTrouble().setStackTrace(getStackTrace(matcher.group(4), matcher.group(5)));
            session.addTestCase(testcase);

            String errorMsg = errorMsg(session.incrementFailuresCount());
            String testCase = testcase.getName() + "(" + testcase.getClassName() + "):";
            output = new ArrayList<String>();
            output.add("");
            output.add(errorMsg);
            output.add(testCase);
            output.addAll(Arrays.asList(testcase.getTrouble().getStackTrace()));
            output.add("");

            manager.displayOutput(session, "", false);
            manager.displayOutput(session, errorMsg, false);
            manager.displayOutput(session, testCase, false); //NOI18N
            for (String line : testcase.getTrouble().getStackTrace()) {
                manager.displayOutput(session, line, true);
            }
            manager.displayOutput(session, "", false);
            testcase.addOutputLines(output);
        }

        @Override
        List<String> getRecognizedOutput() {
            return new ArrayList<String>(output);
        }
    }

    static class TestStartedHandler extends TestRecognizerHandler {

        public TestStartedHandler() {
            super("%TEST_STARTED%\\s(.+)\\((.+)\\)"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
        }
    }

    /**
     * Support for Shoulda tests -- see #150613
     */
    static class ShouldaTestStartedHandler extends TestRecognizerHandler {

        public ShouldaTestStartedHandler() {
            super("%TEST_STARTED%\\stest:\\s(.*)\\.\\s\\((.+)\\)"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
        }
    }

    static class TestFinishedHandler extends TestRecognizerHandler {

        public TestFinishedHandler(String regex) {
            super(regex);
        }

        public TestFinishedHandler() {
            super("%TEST_FINISHED%\\stime=(.+)\\s(.+)\\((.+)\\)"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
            Testcase testcase = new Testcase(matcher.group(2), TestType.TEST_UNIT.name(), session);
            testcase.setTimeMillis(toMillis(matcher.group(1)));
            testcase.setClassName(matcher.group(3));
            session.addTestCase(testcase);
        }
    }

    /**
     * Support for Shoulda tests -- see #150613
     */
    static class ShouldaTestFinishedHandler extends TestFinishedHandler {

        public ShouldaTestFinishedHandler() {
            super("%TEST_FINISHED%\\stime=(.+)\\stest:\\s(.*)\\.\\s\\((.+)\\)"); //NOI18N
        }

    }

    /**
     * Support for Shoulda tests -- see #150613
     */
    static class ShouldaTestErrorHandler extends TestErrorHandler {

        public ShouldaTestErrorHandler() {
            super("%TEST_ERROR%\\stime=(.+)\\stestname=test:\\s(.+)\\.\\s\\((.+)\\)\\smessage=(.*)\\slocation=(.*)"); //NOI18N
        }

    }

    /**
     * Captures the rest of %TEST_* patterns that are not handled
     * otherwise (yet).
     */
    static class TestMiscHandler extends TestRecognizerHandler {

        public TestMiscHandler() {
            super("%TEST_.*"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
        }
    }

    static class SuiteFinishedHandler extends TestRecognizerHandler {

        public SuiteFinishedHandler() {
            super("%SUITE_FINISHED%\\stime=(.+)"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
            manager.displayReport(session, session.getReport(toMillis(matcher.group(1))));
        }
    }

    static class SuiteStartedHandler extends TestRecognizerHandler {

        public SuiteStartedHandler() {
            super("%SUITE_STARTED%\\s.*"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
        }
    }

    static class SuiteErrorOutputHandler extends TestRecognizerHandler {

        public SuiteErrorOutputHandler() {
            super("%SUITE_ERROR_OUTPUT%\\serror=(.*)"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
            manager.displayOutput(session, matcher.group(1), true);
            manager.displayOutput(session, "", false);
        }

        @Override
        List<String> getRecognizedOutput() {
            return Collections.<String>singletonList(matcher.group(1));
        }

    }

    static class SuiteStartingHandler extends TestRecognizerHandler {

        private boolean firstSuite = true;

        public SuiteStartingHandler() {
            super("%SUITE_STARTING%\\s(.+)"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
            if (firstSuite) {
                firstSuite = false;
                manager.testStarted(session);
            }
            String suiteName = matcher.group(1);
            session.addSuite(new TestSuite(suiteName));
            manager.displaySuiteRunning(session, suiteName);
        }
    }

    /**
     * Captures the rest of %SUITE_* patterns that are not handled
     * otherwise (yet).
     */
    static class SuiteMiscHandler extends TestRecognizerHandler {

        public SuiteMiscHandler() {
            super("%SUITE_.*"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
        }
    }

    /**
     * Captures output meant for logging.
     */
    static class TestLoggerHandler extends TestRecognizerHandler {

        public TestLoggerHandler() {
            super("%TEST_LOGGER%\\slevel=(.+)\\smsg=(.*)"); //NOI18N
        }

        @Override
        void updateUI( Manager manager, TestSession session) {
            Level level = Level.parse(matcher.group(1));
            if (LOGGER.isLoggable(level))
                LOGGER.log(level, matcher.group(2));
        }
    }

}

