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

import java.util.regex.Matcher;
import junit.framework.TestCase;

/**
 *
 * @author Erno Mononen
 */
public class TestUnitRecognizerTest extends TestCase {
    
    public void testTestStarted() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestStartedHandler();
        String output = "%TEST_STARTED% test_foo(TestFooBar)";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertEquals("test_foo", matcher.group(1));
        assertEquals("TestFooBar", matcher.group(2));

        output = "%TEST_STARTED% test_foo(Foo::Bar::TestFooBar)";
        matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertEquals("test_foo", matcher.group(1));
        assertEquals("Foo::Bar::TestFooBar", matcher.group(2));
    }

    public void testTestStartedIssue157577() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestStartedHandler();
        String output = "%TEST_STARTED% test_-with-a_dash(TestDash)";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertEquals("test_-with-a_dash", matcher.group(1));
        assertEquals("TestDash", matcher.group(2));
    }

    public void testShouldaTestStarted() {
        String output = "%TEST_STARTED% test: when index is called should respond with success. (MainControllerTest)";
        TestRecognizerHandler handler = new TestUnitHandlerFactory.ShouldaTestStartedHandler();
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertEquals("when index is called should respond with success", matcher.group(1));
        assertEquals("MainControllerTest", matcher.group(2));

    }

    public void testTestFinished() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestFinishedHandler();
        String output = "%TEST_FINISHED% time=0.008765 test_foo(TestFooBar)";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("0.008765", matcher.group(1));
        assertEquals("test_foo", matcher.group(2));
        assertEquals("TestFooBar", matcher.group(3));

        output = "%TEST_FINISHED% time=0.008765 test_foo(FooModule::TestFooBar)";
        matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("0.008765", matcher.group(1));
        assertEquals("test_foo", matcher.group(2));
        assertEquals("FooModule::TestFooBar", matcher.group(3));
    }

    public void testTestFinishedIssue157577() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestFinishedHandler();
        String output = "%TEST_FINISHED% time=0.123 test_with_a-dash(TestDash)";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("0.123", matcher.group(1));
        assertEquals("test_with_a-dash", matcher.group(2));
        assertEquals("TestDash", matcher.group(3));

        output = "%TEST_FINISHED% time=0.123 test_with_(parenthesis)(TestParenthesis)";
        matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("test_with_(parenthesis)", matcher.group(2));
        assertEquals("TestParenthesis", matcher.group(3));
    }

    public void testShouldaTestFinished() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.ShouldaTestFinishedHandler();
        String output = "%TEST_FINISHED% time=0.105651 test: when index is called should respond with success. (MainControllerTest)";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("0.105651", matcher.group(1));
        assertEquals("when index is called should respond with success", matcher.group(2));
        assertEquals("MainControllerTest", matcher.group(3));
    }

    public void testTestFinished2() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestFinishedHandler();
        String output = "%TEST_FINISHED% time=8.4e-05 test_foo(TestFooBar)";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("8.4e-05", matcher.group(1));
        assertEquals("test_foo", matcher.group(2));
        assertEquals("TestFooBar", matcher.group(3));
    }

    public void testTestFailed() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestFailedHandler();
        String output = "%TEST_FAILED% time=0.007233 testname=test_positive_price(ProductTest) message=<false> is not true. location=./test/unit/product_test.rb:69:in `test_positive_price'";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        
        assertEquals(5, matcher.groupCount());
        assertEquals("0.007233", matcher.group(1));
        assertEquals("test_positive_price", matcher.group(2));
        assertEquals("ProductTest", matcher.group(3));
        assertEquals("<false> is not true.", matcher.group(4));
        assertEquals("./test/unit/product_test.rb:69:in `test_positive_price'", matcher.group(5));
        
        String outputScientificNotation = "%TEST_FAILED% time=9.8e-07 testname=test_positive_price(ProductTest) message=<false> is not true. location=./test/unit/product_test.rb:69:in `test_positive_price'";
        matcher = handler.match(outputScientificNotation);
        assertTrue(matcher.matches());
        assertEquals("9.8e-07", matcher.group(1));

        // nested class name
        String outputNestedClass = "%TEST_FAILED% time=0.0060 testname=test_foo(TestSomething::TestNotExecuted) message=this test is not executed. location=/a/path/to/somewhere/test/test_something.rb:21:in `test_foo'";
        matcher = handler.match(outputNestedClass);
        assertTrue(matcher.matches());

        assertEquals(5, matcher.groupCount());
        assertEquals("0.0060", matcher.group(1));
        assertEquals("test_foo", matcher.group(2));
        assertEquals("TestSomething::TestNotExecuted", matcher.group(3));
        assertEquals("this test is not executed.", matcher.group(4));
        assertEquals("/a/path/to/somewhere/test/test_something.rb:21:in `test_foo'", matcher.group(5));
    }

    public void testShouldaTestFailed() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.ShouldaTestFailedHandler();
        String output = "%TEST_FAILED% time=0.041676 testname=test: when index is called should respond with forbidden. (MainControllerTest) message=Expected response to be a <:forbidden>, but was <200> location=/home/erno/NetBeansProjects/nb_shoulda/vendor/plugins/shoulda/lib/shoulda/controller/macros.rb:177";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());

        assertEquals(5, matcher.groupCount());
        assertEquals("0.041676", matcher.group(1));
        assertEquals("when index is called should respond with forbidden", matcher.group(2));
        assertEquals("MainControllerTest", matcher.group(3));
        assertEquals("Expected response to be a <:forbidden>, but was <200>", matcher.group(4));
    }

    public void testTestError() {
        TestUnitHandlerFactory.TestErrorHandler handler = new TestUnitHandlerFactory.TestErrorHandler();
        String output = "%TEST_ERROR% time=0.000883 testname=test_two_people_buying(DslUserStoriesTest) " +
                "message=StandardError: No fixture with name 'ruby_book' found for table 'products' " +
                "location=/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:894:in `products'%BR%" +
                "/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:888:in `map'%BR%" +
                "/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:888:in `products'%BR%" +
                "./test/integration/dsl_user_stories_test.rb:55:in `setup_without_fixtures'%BR%" +
                "/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:979:in `full_setup'%BR%" +
                "/usr/lib/ruby/1.8/test/unit/testcase.rb:77:in `setup'%BR%" +
                "/usr/lib/ruby/1.8/test/unit/testcase.rb:77:in `run'%BR%" +
                "/usr/lib/ruby/gems/1.8/gems/actionpack-2.0.2/lib/action_controller/integration.rb:547:in `run'%BR%" +
                "/usr/lib/ruby/1.8/test/unit/testsuite.rb:34:in `run'%BR%" +
                "/usr/lib/ruby/1.8/test/unit/testsuite.rb:33:in `each'%BR%" +
                "/usr/lib/ruby/1.8/test/unit/testsuite.rb:33:in `run'%BR%" +
                "/usr/lib/ruby/1.8/test/unit/ui/testrunnermediator.rb:46:in `run_suite'%BR%" +
                "/home/erno/work/elohopea/main-vara/ruby.testrunner/release/nb_test_mediator.rb:145:in `run_mediator'%BR%" +
                "/home/erno/work/elohopea/main-vara/ruby.testrunner/release/nb_test_mediator.rb:140:in `each'%BR%" +
                "/home/erno/work/elohopea/main-vara/ruby.testrunner/release/nb_test_mediator.rb:140:in `run_mediator'%BR%" +
                "/home/erno/work/elohopea/main-vara/ruby.testrunner/release/nb_test_mediator.rb:206";

        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        
        assertEquals(5, matcher.groupCount());
        assertEquals("0.000883", matcher.group(1));
        assertEquals("test_two_people_buying", matcher.group(2));
        assertEquals("DslUserStoriesTest", matcher.group(3));
        assertEquals("StandardError: No fixture with name 'ruby_book' found for table 'products'", matcher.group(4));
        assertEquals("StandardError: No fixture with name 'ruby_book' found for table 'products'", matcher.group(4));
        
        String[] stackTrace = TestUnitHandlerFactory.getStackTrace(matcher.group(4), matcher.group(5));
        assertEquals(13, stackTrace.length);
        assertEquals("StandardError: No fixture with name 'ruby_book' found for table 'products'", stackTrace[0]);
        assertEquals("/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:888:in `map'", stackTrace[2]);
        assertEquals("/usr/lib/ruby/gems/1.8/gems/actionpack-2.0.2/lib/action_controller/integration.rb:547:in `run'", stackTrace[8]);

        String outputScientificNotation = "%TEST_ERROR% time=1.2e-34 testname=test_two_people_buying(DslUserStoriesTest) " +
                "message=StandardError: No fixture with name 'ruby_book' found for table 'products' " +
                "location=/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:894:in `products'%BR%" +
                "/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:888:in `map'%BR%";

        matcher = handler.match(outputScientificNotation);
        assertTrue(matcher.matches());
        assertEquals("1.2e-34", matcher.group(1));

        String outputNestedClass = "%TEST_ERROR% time=1.2e-34 testname=test_two_people_buying(Some::Another::DslUserStoriesTest) " +
                "message=StandardError: No fixture with name 'ruby_book' found for table 'products' " +
                "location=/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:894:in `products'%BR%" +
                "/usr/lib/ruby/gems/1.8/gems/activerecord-2.0.2/lib/active_record/fixtures.rb:888:in `map'%BR%";

        matcher = handler.match(outputNestedClass);
        assertTrue(matcher.matches());
        assertEquals("Some::Another::DslUserStoriesTest", matcher.group(3));

    }

    public void testErrorMySqlError() {
        String mysqlError = "%TEST_ERROR% time=0.0050 testname=test_two_people_buying(DslUserStoriesTest) " +
                "message=Mysql::Error: #28000Access denied for user 'root'@'localhost' (using password: NO) " +
                "location=/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/vendor/mysql.rb:523:in `read'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/vendor/mysql.rb:153:in `real_connect'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/connection_adapters/mysql_adapter.rb:505:in `connect'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/connection_adapters/mysql_adapter.rb:183:in `initialize'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/connection_adapters/mysql_adapter.rb:88:in `new'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/connection_adapters/mysql_adapter.rb:88:in `mysql_connection'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/connection_adapters/abstract/connection_specification.rb:292:in `connection='%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/connection_adapters/abstract/connection_specification.rb:260:in `retrieve_connection'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/connection_adapters/abstract/connection_specification.rb:78:in `connection'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/fixtures.rb:503:in `create_fixtures'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/fixtures.rb:963:in `load_fixtures'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/fixtures.rb:929:in `setup_fixtures'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activesupport-2.1.0/lib/active_support/callbacks.rb:173:in `evaluate_method'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activesupport-2.1.0/lib/active_support/callbacks.rb:161:in `call'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activesupport-2.1.0/lib/active_support/callbacks.rb:90:in `run'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activesupport-2.1.0/lib/active_support/callbacks.rb:90:in `each'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activesupport-2.1.0/lib/active_support/callbacks.rb:90:in `run'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activesupport-2.1.0/lib/active_support/callbacks.rb:272:in `run_callbacks'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activesupport-2.1.0/lib/active_support/testing/setup_and_teardown.rb:31:in `run_with_callbacks'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/actionpack-2.1.0/lib/action_controller/integration.rb:600:in `run'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/1.8/test/unit/testsuite.rb:34:in `run'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/1.8/test/unit/testsuite.rb:33:in `each'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/1.8/test/unit/testsuite.rb:33:in `run'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/1.8/test/unit/ui/testrunnermediator.rb:46:in `run_suite'%BR%" +
                "/path/netbeans/ruby/nb_test_runner.rb:93:in `run_mediator'%BR%" +
                "/path/netbeans/ruby/nb_test_runner.rb:88:in `each'%BR%" +
                "/path/netbeans/ruby/nb_test_runner.rb:88:in `run_mediator'%BR%" +
                "/path/netbeans/ruby/nb_test_runner.rb:60:in `start'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/1.8/test/unit/ui/testrunnerutilities.rb:29:in `run'%BR%" +
                "/path/netbeans/ruby/nb_test_runner.rb:168:in `run'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/1.8/test/unit/autorunner.rb:12:in `run'%BR%" +
                "/path/netbeans/ruby/jruby-1.1.3/lib/ruby/1.8/test/unit.rb:278%BR%:1";

        TestUnitHandlerFactory.TestErrorHandler handler = new TestUnitHandlerFactory.TestErrorHandler();
        Matcher matcher = handler.match(mysqlError);
        assertTrue(matcher.matches());

        assertEquals(5, matcher.groupCount());
        assertEquals("0.0050", matcher.group(1));
        assertEquals("test_two_people_buying", matcher.group(2));
        assertEquals("DslUserStoriesTest", matcher.group(3));
        assertEquals("Mysql::Error: #28000Access denied for user 'root'@'localhost' (using password: NO)", matcher.group(4));
        assertTrue(matcher.group(5).startsWith("/path/netbeans/ruby/jruby-1.1.3/lib/ruby/gems/1.8/gems/activerecord-2.1.0/lib/active_record/vendor/mysql.rb:523:in `read'"));
    }

    public void testSuiteFinished() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.SuiteFinishedHandler();
        String output = "%SUITE_FINISHED% time=0.124";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        
        assertEquals(1, matcher.groupCount());
        assertEquals("0.124", matcher.group(1));
    }
    
    public void testSuiteFinished2() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.SuiteFinishedHandler();
        String output = "%SUITE_FINISHED% time=8.4e-05";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        
        assertEquals(1, matcher.groupCount());
        assertEquals("8.4e-05", matcher.group(1));
    }
    
    public void testSuiteStarted() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.SuiteStartedHandler();
        String output = "%SUITE_STARTED% 0 tests, 0 assertions, 0 failures, 0 errors";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
    }
    
    public void testSuiteStarting() throws InterruptedException {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.SuiteStartingHandler();
        String output = "%SUITE_STARTING% TestMe";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertEquals("TestMe", matcher.group(1));

        output = "%SUITE_STARTING% MyModule::TestMe";
        matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertEquals("MyModule::TestMe", matcher.group(1));
    }

    public void testSuiteErrorOutput() throws InterruptedException {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.SuiteErrorOutputHandler();
        String output = "%SUITE_ERROR_OUTPUT% error=undefined method `size' for UserHelperTest:Class";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(1, matcher.groupCount());
        assertEquals("undefined method `size' for UserHelperTest:Class", matcher.group(1));
    }

    public void testTestLogger() throws InterruptedException {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestLoggerHandler();
        String output = "%TEST_LOGGER% level=FINE msg=Loading 3 files took 12.345";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertEquals("FINE", matcher.group(1));
        assertEquals("Loading 3 files took 12.345", matcher.group(2));
    }
    
    public void testIssue143508TestStarted() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestStartedHandler();
        String output = "%TEST_STARTED% test_foo(FooTest)\\n";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(2, matcher.groupCount());
        assertEquals("test_foo", matcher.group(1));
        assertEquals("FooTest", matcher.group(2));
    } 

    public void testIssue143508TestFinished() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.TestFinishedHandler();
        String output = "%TEST_FINISHED% time=0.203 test_foo(FooTest)\\n";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("0.203", matcher.group(1));
        assertEquals("test_foo", matcher.group(2));
        assertEquals("FooTest", matcher.group(3));
    }

    public void testIssue164587ShouldaTestFinished() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.ShouldaTestFinishedHandler();
        String output = "%TEST_FINISHED% time=0.006234 test: Account should be valid. (AccountTest)";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(3, matcher.groupCount());
        assertEquals("0.006234", matcher.group(1));
        assertEquals("Account should be valid", matcher.group(2));
        assertEquals("AccountTest", matcher.group(3));
    }

    public void testIssue164587ShouldaTestError() {
        TestRecognizerHandler handler = new TestUnitHandlerFactory.ShouldaTestErrorHandler();
        String output = "%TEST_ERROR% time=0.00404 testname=test: Account should be valid. (AccountTest) " +
                "message=MissingSourceFile: no such file to load -- sqlite3 " +
                "location=/usr/local/lib/site_ruby/1.8/rubygems/custom_require.rb:31:in `gem_original_require'%BR%";
        Matcher matcher = handler.match(output);
        assertTrue(matcher.matches());
        assertEquals(5, matcher.groupCount());
        assertEquals("0.00404", matcher.group(1));
        assertEquals("Account should be valid", matcher.group(2));
        assertEquals("AccountTest", matcher.group(3));
    }

}
