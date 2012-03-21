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

package org.netbeans.modules.ruby;

import javax.swing.text.BadLocationException;

/**
 * Test for RubyBracesMatcher
 * 
 * @author Marek Slama
 */
public class RubyBracesMatcherTest extends RubyTestBase {
    
    public RubyBracesMatcherTest(String testName) {
        super(testName);
    }

    /** 
     * Test for BracesMatcher, first ^ gives current caret position,
     * second ^ gives matching caret position. Test is done in forward and backward direction.
     */
    private void match2(String original) throws BadLocationException {
        super.assertMatches2(original);
    }

    public void testFindMatching1() throws Exception {
        match2("^if true\n^end");
    }

    public void testFindMatching2() throws Exception {
        match2("x=^(true^)\ny=5");
    }

    public void testFindMatching3() throws Exception {
        match2("x=^(true || (false)^)\ny=5");
    }

    public void testFindMatching4() throws Exception {
        match2("^def foo\nif true\nend\n^end\nend");
    }

    public void testFindMatching5() throws Exception {
        // Test heredocs
        match2("x=f(^<<ABC,\"hello\")\nfoo\nbar\n^ABC\n");
    }

    public void testFindMatching6() throws Exception {
        // Test heredocs
        match2("x=f(^<<ABC,'hello',<<-DEF,'bye')\nfoo\nbar\n^ABC\nbaz\n  DEF\nwhatever");
    }

    public void testFindMatching7() throws Exception {
        // Test heredocs
        match2("x=f(<<ABC,'hello',^<<-DEF,'bye')\nfoo\nbar\nABC\nbaz\n  ^DEF\nwhatever");
    }
    
}
