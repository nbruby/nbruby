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

import javax.swing.JTextArea;
import javax.swing.text.Caret;
import org.netbeans.editor.BaseDocument;
import org.openide.filesystems.FileObject;

/**
 *
 * @author Tor Norbye
 */
public class ReflowParagraphActionTest extends RubyTestBase {
    
    public ReflowParagraphActionTest(String testName) {
        super(testName);
    }

    private void formatParagraph(String file, String caretLine) throws Exception {
        FileObject fo = getTestFile(file);
        assertNotNull(fo);
        BaseDocument doc = getDocument(fo);
        assertNotNull(doc);
        String before = doc.getText(0, doc.getLength());

        int caretDelta = caretLine.indexOf('^');
        assertTrue(caretDelta != -1);
        caretLine = caretLine.substring(0, caretDelta) + caretLine.substring(caretDelta + 1);
        int lineOffset = before.indexOf(caretLine);
        assertTrue(lineOffset != -1);
        int caretOffset = lineOffset+caretDelta;

        
        ReflowParagraphAction action = new ReflowParagraphAction();
        JTextArea ta = new JTextArea(doc);
        Caret caret = ta.getCaret();
        caret.setDot(caretOffset);
        action.actionPerformed(null, ta);
        
        String after = doc.getText(0, doc.getLength());
        assertEquals(before, after);
    }

    private void formatParagraphFile(String file, String caretLine) throws Exception {
        FileObject fo = getTestFile(file);
        assertNotNull(fo);
        BaseDocument doc = getDocument(fo);
        assertNotNull(doc);
        String before = doc.getText(0, doc.getLength());

        int caretDelta = caretLine.indexOf('^');
        assertTrue(caretDelta != -1);
        caretLine = caretLine.substring(0, caretDelta) + caretLine.substring(caretDelta + 1);
        int lineOffset = before.indexOf(caretLine);
        assertTrue(lineOffset != -1);
        int caretOffset = lineOffset+caretDelta;

        
        ReflowParagraphAction action = new ReflowParagraphAction();
        JTextArea ta = new JTextArea(doc);
        Caret caret = ta.getCaret();
        caret.setDot(caretOffset);
        action.actionPerformed(null, ta);
        
        String after = doc.getText(0, doc.getLength());
        assertDescriptionMatches(file, after, false, ".formatted");
    }

    public void testScanfFormatting() throws Exception {
        formatParagraph("testfiles/scanf.comment", "Matches an opti^onally signed decimal integer");
    }
    
    public void testHttpHeaderFormatting() throws Exception {
        formatParagraph("testfiles/http-header.comment", " under the sa^me terms of ruby");
    }

    public void testHttpFormatting() throws Exception {
        formatParagraph("testfiles/http.comment", "This lib^rary provides your program functions");
    }
    
    public void testHttpFormatting2() throws Exception {
        formatParagraph("testfiles/http.comment", "#^ Example #4: More generic GET+prin");
    }

    public void testHttpFormatting3() throws Exception {
        formatParagraphFile("testfiles/http2.comment", " ^# This library provides your program functions");
    }
    
    public void testParagraph() throws Exception {
        formatParagraphFile("testfiles/paragraph.comment", " ^  # foo");
    }

    public void testSeparator() throws Exception {
        formatParagraphFile("testfiles/separator.comment", "This is s^ome text");
    }

    public void testLineBreak() throws Exception {
        formatParagraphFile("testfiles/linebreak.comment", "next available num^ber");
    }

    public void testLineBreak2() throws Exception {
        formatParagraphFile("testfiles/linebreak2.comment", "next available num^ber");
    }

    public void testLineBreak3() throws Exception {
        formatParagraphFile("testfiles/linebreak2.comment", "next available num^ber");
    }

    public void testLists() throws Exception {
        formatParagraphFile("testfiles/lists.comment", "chang^e_menu");
    }
}
