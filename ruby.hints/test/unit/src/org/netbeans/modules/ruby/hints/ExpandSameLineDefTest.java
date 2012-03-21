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
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */
package org.netbeans.modules.ruby.hints;

import org.netbeans.modules.ruby.hints.infrastructure.RubyAstRule;

/**
 * Test the ExpandSameLineDef hint
 *
 * @author Tor Norbye
 */
public class ExpandSameLineDefTest extends HintTestBase {

    public ExpandSameLineDefTest(String testName) {
        super(testName);
    }

    private RubyAstRule createRule() {
        return new ExpandSameLineDef();
    }

    public void testRegistered() throws Exception {
        ensureRegistered(createRule());
    }

    public void testWrongLine() throws Exception {
        String caretLine = "cl^ass FooControllerTest < Test::Unit::TestCase";
        checkHints(this, createRule(), "testfiles/sameline.rb", caretLine);
    }

    public void testExpandableLine() throws Exception {
        String caretLine = "cla^ss FooController; def rescue_action(e) raise e end; end";
        checkHints(this, createRule(), "testfiles/sameline.rb", caretLine);
    }
    
    public void testApplyFix() throws Exception {
        String caretLine = "cla^ss FooController; def rescue_action(e) raise e end; end";
        applyHint(this, createRule(), "testfiles/sameline.rb", caretLine, "class");
    }
}
