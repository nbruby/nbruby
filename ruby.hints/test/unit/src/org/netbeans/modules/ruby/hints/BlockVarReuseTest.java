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

import java.util.List;
import org.netbeans.modules.ruby.hints.infrastructure.RubyAstRule;
import org.openide.filesystems.FileObject;

/**
 * Test the block-var hint
 *
 * @author Tor Norbye
 */
public class BlockVarReuseTest extends HintTestBase {

    public BlockVarReuseTest(String testName) {
        super(testName);
    }

    private RubyAstRule createRule() {
        return new BlockVarReuse();
    }

    public void testRegistered() throws Exception {
        ensureRegistered(createRule());
    }
    
    public void testHint1() throws Exception {
        checkHints(this, createRule(), "testfiles/blockvars.rb", null);
    }

    public void testHint2() throws Exception {
        checkHints(this, createRule(), "testfiles/blockvars2.rb", null);
    }

    public void testFix1() throws Exception {
        String caretLine = "3.14.each { |loc^al|";
        applyHint(this, createRule(), "testfiles/blockvars2.rb", caretLine, "Rename the local variable");
    }

    public void testFix2() throws Exception {
        String caretLine = "3.14.each { |loc^al|";
        applyHint(this, createRule(), "testfiles/blockvars2.rb", caretLine, "Rename the block variable");
    }

    public void testBlockVarReuse() throws Exception {
        List<FileObject> files = getBigSourceFiles();
        for (FileObject f : files) {
            findHints(this, createRule(), f, null);
        }
    }
}
