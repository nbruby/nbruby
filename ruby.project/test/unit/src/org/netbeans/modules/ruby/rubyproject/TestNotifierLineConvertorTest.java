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

package org.netbeans.modules.ruby.rubyproject;

import org.netbeans.modules.ruby.RubyTestBase;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Tor Norbye
 */
public class TestNotifierLineConvertorTest extends RubyTestBase {
    
    public TestNotifierLineConvertorTest(String testName) {
        super(testName);
    }

    public void testUnit() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(false, false);
        
        assertTrue(notifier.recognizeLine("35 tests, 81 assertions, 0 failures, 1 errors"));
    }

    public void testRSpec() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(false, false);
        
        assertTrue(notifier.recognizeLine("5 examples, 3 failures, 5 not implemented")); // older rspec format
        assertTrue(notifier.recognizeLine("5 examples, 3 failures, 5 pending"));
        assertTrue(notifier.recognizeLine("1 example, 1 failure"));
    }

    public void testWindows() {        
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(false, false);

        assertTrue(notifier.recognizeLine("35 tests, 81 assertions, 0 failures, 1 errors\r"));
        assertTrue(notifier.recognizeLine("5 examples, 3 failures, 5 not implemented\r"));
        assertTrue(notifier.recognizeLine("5 examples, 3 failures, 5 pending\r"));
        assertTrue(notifier.recognizeLine("1 example, 1 failure\r"));
    }
    
    public void testNoFalseNegatives() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(false, false);
        
        assertFalse(notifier.recognizeLine("1 for example, 1 failure"));
        assertFalse(notifier.recognizeLine("hello world"));
        assertFalse(notifier.recognizeLine("1"));
        assertFalse(notifier.recognizeLine("NoMethodError: You have a nil object when you didn't expect it!"));
        assertFalse(notifier.recognizeLine(".......E..........................."));
        assertFalse(notifier.recognizeLine("   C:/InstantRails/rails_apps/rfs/test/unit/rest_phone/phone_action/phone_action_subtypes_test.rb:182:in `test_event_response'"));
    }
    
    public void testAccumulate1() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        notifier.convert("35 tests, 81 assertions, 0 failures, 0 errors");
        notifier.convert("10 tests, 1 assertions, 0 failures, 0 errors\r");
        notifier.convert("1 tests, 0 assertions, 0 failures, 0 errors");
        assertEquals("46 tests, 82 assertions, 0 failures, 0 errors", notifier.getSummary());
        assertTrue(!notifier.isError() && !notifier.isWarning());
    }

    public void testAccumulate2() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        notifier.convert("35 tests, 81 assertions, 0 failures, 1 errors");
        notifier.convert("10 tests, 1 assertions, 5 failures, 1 errors\r");
        notifier.convert("1 tests, 0 assertions, 0 failures, 0 errors");
        assertEquals("46 tests, 82 assertions, 5 failures, 2 errors", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRSpec1() {  
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("5 examples, 3 failures, 5 not implemented");
        notifier.convert("1 example, 1 failure\r");
        assertEquals("6 examples, 4 failures, 5 pending", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRSpec1b() {  
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("5 examples, 3 failures, 5 pending");
        notifier.convert("1 example, 1 failure\r");
        assertEquals("6 examples, 4 failures, 5 pending", notifier.getSummary());
        assertTrue(notifier.isError());
    }
    
    public void testRSpec2() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("0 examples, 0 failures, 5 not implemented");
        notifier.convert("1 example, 1 failure\r");
        assertEquals("1 example, 1 failure, 5 pending", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRSpec2b() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("0 examples, 0 failures, 5 pending");
        notifier.convert("1 example, 1 failure\r");
        assertEquals("1 example, 1 failure, 5 pending", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRSpec3() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("0 examples, 0 failures");
        notifier.convert("0 examples, 0 failures");
        assertEquals("0 examples, 0 failures", notifier.getSummary());
        assertTrue(!notifier.isError() && !notifier.isWarning());
    }

    public void testRSpec5() {  
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("5 examples, 0 failures, 5 not implemented");
        notifier.convert("1 example, 0 failures, 1 pending\r");
        assertEquals("6 examples, 0 failures, 6 pending", notifier.getSummary());
        assertTrue(notifier.isWarning() && !notifier.isError());
    }

    public void testCombined() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("0 examples, 0 failures, 5 not implemented");
        notifier.convert("1 example, 1 failure\r");
        assertEquals("1 example, 1 failure, 5 pending", notifier.getSummary());
        notifier.convert("1 tests, 1 assertions, 1 failures, 1 errors");
        assertEquals("1 test, 1 assertion, 1 example, 2 failures, 1 error, 5 pending", notifier.getSummary());
        assertTrue(notifier.isError());
    }
    
    public void testCombined2() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);

        notifier.convert("0 examples, 0 failures, 5 pending");
        notifier.convert("1 example, 1 failure\r");
        assertEquals("1 example, 1 failure, 5 pending", notifier.getSummary());
        notifier.convert("1 tests, 1 assertions, 1 failures, 1 errors");
        assertEquals("1 test, 1 assertion, 1 example, 2 failures, 1 error, 5 pending", notifier.getSummary());
        assertTrue(notifier.isError());
    }
    
    public void testNoAccumulate() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(false, false);
        notifier.convert("35 tests, 81 assertions, 0 failures, 0 errors");
        assertEquals("35 tests, 81 assertions, 0 failures, 0 errors", notifier.getSummary());
        assertTrue(!notifier.isError() && !notifier.isWarning());
        notifier.convert("10 tests, 1 assertions, 0 failures, 0 errors\r");
        assertEquals("10 tests, 1 assertion, 0 failures, 0 errors", notifier.getSummary());
        assertTrue(!notifier.isError() && !notifier.isWarning());
        notifier.convert("35 tests, 81 assertions, 0 failures, 1 errors");
        assertEquals("35 tests, 81 assertions, 0 failures, 1 error", notifier.getSummary());
        assertTrue(notifier.isError());
        notifier.convert("10 tests, 1 assertions, 0 failures, 0 errors\r");
        assertEquals("10 tests, 1 assertion, 0 failures, 0 errors", notifier.getSummary());
        assertTrue(!notifier.isError() && !notifier.isWarning());
    }
    
    public void testRake() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        notifier.convert("Test failures");
        assertEquals("1 error", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRake2() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        notifier.convert("1 tests, 1 assertions, 1 failures, 1 errors");
        notifier.convert("Test failures");
        assertEquals("1 test, 1 assertion, 1 failure, 2 errors", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRake2Windows() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        notifier.convert("1 tests, 1 assertions, 1 failures, 1 errors");
        notifier.convert("Test failures\r");
        assertEquals("1 test, 1 assertion, 1 failure, 2 errors", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRake3() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(false, false);
        notifier.convert("Test failures");
        assertEquals("1 error", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testRake4() {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        notifier.convert("No Test failures");
        // Make sure we don't pick up "Test failures" as just a substring
        assertEquals("0 failures", notifier.getSummary());
        assertTrue(!notifier.isError() && !notifier.isWarning());
    }

    public void testOutputLog1() throws Exception {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        FileObject fileObject = getTestFile("testfiles/testoutput.txt");
        String s = readFile(fileObject);
        String[] lines = s.split("\n");
        for (String line : lines) {
            notifier.convert(line);
        }
        assertEquals("313 tests, 504 assertions, 1 failure, 0 errors", notifier.getSummary());
        assertTrue(notifier.isError());
    }

    public void testOutputLog2() throws Exception {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        FileObject fileObject = getTestFile("testfiles/testoutput2.txt");
        String s = readFile(fileObject);
        String[] lines = s.split("\n");
        for (String line : lines) {
            notifier.convert(line);
        }
        assertEquals("25 tests, 46 assertions, 0 failures, 0 errors", notifier.getSummary());
        assertFalse(notifier.isError());
    }
    
    public void testRerun116386() throws Exception {
        TestNotifierLineConvertor notifier = new TestNotifierLineConvertor(true, false);
        FileObject fileObject = getTestFile("testfiles/testoutput2.txt");
        String s = readFile(fileObject);
        String[] lines = s.split("\n");
        notifier.start();
        for (String line : lines) {
            notifier.convert(line);
        }

        assertEquals("25 tests, 46 assertions, 0 failures, 0 errors", notifier.getSummary());
        assertFalse(notifier.isError());
        
        // Rerun!
        notifier.start();
        for (String line : lines) {
            notifier.convert(line);
        }

        assertEquals("25 tests, 46 assertions, 0 failures, 0 errors", notifier.getSummary());
        assertFalse(notifier.isError());

    }
}
