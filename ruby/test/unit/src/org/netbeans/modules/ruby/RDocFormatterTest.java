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

package org.netbeans.modules.ruby;

import org.netbeans.api.lexer.Token;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.ruby.lexer.LexUtilities;
import org.netbeans.modules.ruby.lexer.RubyTokenId;
import org.openide.filesystems.FileObject;

/**
 *
 * @author tor
 */
public class RDocFormatterTest extends RubyTestBase {
    
    public RDocFormatterTest(String testName) {
        super(testName);
    }
    
    public void formatFile(String file) throws Exception {
        FileObject fileObj = getTestFile(file);
        BaseDocument doc = getDocument(fileObj);
        RDocFormatter formatter = new RDocFormatter();

        int offset = 0;
        int length = doc.getLength();
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body>\n");
        boolean started = false;
        boolean inDoc = false;
        for (; offset < length; offset = Utilities.getRowEnd(doc, offset) + 1) {
            if (inDoc && (Utilities.isRowEmpty(doc, offset) || Utilities.isRowWhite(doc, offset))) {
                String line = doc.getText(offset, Utilities.getRowEnd(doc, offset)-offset);
                formatter.appendLine(line);
                continue;
            }
            int start = Utilities.getRowFirstNonWhite(doc, offset);
            if (start != -1) {
                String line = doc.getText(offset, Utilities.getRowEnd(doc, offset)-offset);
                if (inDoc && line.startsWith("=end")) {
                    inDoc = false;
                    continue;
                }
                Token<?> token = LexUtilities.getToken(doc, start);
                if (token.id() == RubyTokenId.LINE_COMMENT) {
                    started = true;
                    line = doc.getText(start, Utilities.getRowEnd(doc, start)-start);
                    formatter.appendLine(line);
                    continue;
                } else if (token.id() == RubyTokenId.DOCUMENTATION) {
                    started = true;
                    if (line.startsWith("=begin")) {
                        inDoc = true;
                        continue;
                    }
                    formatter.appendLine(line);
                    continue;
                }
            }
            if (started) {
                sb.append("<hr>\n");
                if (start != -1) {
                    sb.append("<pre>");
                    String line = doc.getText(offset, Utilities.getRowEnd(doc, offset)-offset);
                    sb.append(line);
                    sb.append("</pre>");
                }
                sb.append("<br/>\n");
                sb.append(formatter.toHtml());
                // Peek ahead to find the definition
                formatter = new RDocFormatter();
                started = false;
            }
        }
        if (started) {
            sb.append("<h2 style=\"color: green\">Next Comment</h2>\n");
            sb.append(formatter.toHtml());
        }
        sb.append("</body></html>\n");
        
        assertDescriptionMatches(file, sb.toString(), false, ".html");
    }
    
    public void testFormatBig1() throws Exception {
        formatFile("testfiles/resolv.rb");
    }

    public void testFormatBig2() throws Exception {
        formatFile("testfiles/date.rb");
    }

    public void testFormatSeparators() throws Exception {
        formatFile("testfiles/rdocdashes.rb");
    }
    
    public void testToHtml() {
        RDocFormatter instance = new RDocFormatter();

        instance.appendLine("# Following does not work:<BR>\n");
        instance.appendLine("# test _italic_<BR>\n");
        instance.appendLine("# test *bold*<BR>\n");
        instance.appendLine("# <P>\n");
        instance.appendLine("# Following does:<BR>\n");
        instance.appendLine("# test +typewriter+<BR>\n");
        instance.appendLine("# test <em>italic_html</em><BR>\n");
        instance.appendLine("# test <b>bold_html</b><BR>\n");
        instance.appendLine("# test <tt>typewriter_html</tt><BR>\n");

        String expected = 
                
"Following does not work:<BR>\n" +
" test <i>italic</i><BR>\n" +
" test <b>bold</b><BR>\n" +
" <P>\n" +
" Following does:<BR>\n" +
" test <tt>typewriter</tt><BR>\n" +
" test <em>italic_html</em><BR>\n" +
" test <b>bold_html</b><BR>\n" +
" test <tt>typewriter_html</tt><BR>\n ";        
        String html = instance.toHtml();
                
        assertEquals(expected, html);
    }
    
    public void testEscape() {
        RDocFormatter instance = new RDocFormatter();
        
        instance.appendLine("#   class EliteGenerator < Rails::Generator::Base");
        
        assertEquals("<pre style=\"color:#000000;\">\n  <span style=\"color:#0000e6;\">class</span> <span" +
                " style=\"color:#000000;font-style:italic;\">EliteGenerator</span> <span style=\"\">&lt;</s" +
                "pan> <span style=\"color:#000000;font-style:italic;\">Rails</span><span style=\"\">::</s" +
                "pan><span style=\"color:#000000;font-style:italic;\">Generator</span><span style=\"\">::</" +
                "span><span style=\"color:#000000;font-style:italic;\">Base</span><br></pre>\n", instance.toHtml());
    }

    public void testNoEscape() {
        RDocFormatter instance = new RDocFormatter();
        
        instance.appendLine("# class EliteGenerator <b>Rails::Generator::Base</b>");

        assertEquals("class EliteGenerator <b>Rails::Generator::Base</b> ", instance.toHtml());
    }

    public void testBeginEnd() {
        RDocFormatter instance = new RDocFormatter();
        
        instance.appendLine("=begin");
        instance.appendLine("foo");
        instance.appendLine("=end");

        assertEquals("<h1>begin</h1>\nfoo <h1>end</h1>\n", instance.toHtml());
    }
    
    public void testNoLabelSpace() {
        RDocFormatter instance = new RDocFormatter();
        instance.appendLine("# [<tt>:id</tt>]");
        assertEquals("<table>\n<tr><td valign=\"top\"><tt>:id</tt> </td><td> </td></tr>\n</table>\n", instance.toHtml());
    }

    public void testNoNumberSpace() {
        RDocFormatter instance = new RDocFormatter();
        instance.appendLine("# 4.");
        assertEquals("<ol>\n<li value=\"4\"></ol>\n", instance.toHtml());
    }

    public void testNumber() {
        RDocFormatter instance = new RDocFormatter();
        instance.appendLine("# 4. Hello");
        assertEquals("<ol>\n<li value=\"4\">Hello </ol>\n", instance.toHtml());
    }

    public void testNoTableSpace() {
        RDocFormatter instance = new RDocFormatter();
        instance.appendLine("# Next::");
        assertEquals("<table>\n<tr><td valign=\"top\">Next </td><td> </td></tr>\n</table>\n", instance.toHtml());
    }
    
    public void testCodeFormatting() {
        RDocFormatter instance = new RDocFormatter();
        instance.setSeqName("basename");

        instance.appendLine("#     File.basename(file_name [, suffix] ) -> base_name\n");
        instance.appendLine("#\n");
        instance.appendLine("#\n");
        instance.appendLine("# Returns the last component of the filename given in <i>file_name</i>,\n");
        instance.appendLine("# which must be formed using forward slashes (``<code>/</code>'')\n");
        instance.appendLine("# regardless of the separator used on the local file system. If\n");
        instance.appendLine("# <i>suffix</i> is given and present at the end of <i>file_name</i>,\n");
        instance.appendLine("# it is removed.\n");
        instance.appendLine("#\n");
        instance.appendLine("#    File.basename(\"/home/gumby/work/ruby.rb\")          #=> \"ruby.rb\"\n");
        instance.appendLine("#    File.basename(\"/home/gumby/work/ruby.rb\", \".rb\")   #=> \"ruby\"\n");
        instance.appendLine("#\n");
        instance.appendLine("#\n");
        
        String expected =
"<pre>\n" +
"File.<b>basename</b>(file_name [, suffix] ) -> base_name\n" +
"<br></pre>\n" +
"<hr>\n" +
"#\n" +
" #\n" +
" Returns the last component of the filename given in <i>file_name</i>,\n" +
" which must be formed using forward slashes (``<code>/</code>'')\n" +
" regardless of the separator used on the local file system. If\n" +
" <i>suffix</i> is given and present at the end of <i>file_name</i>,\n" +
" it is removed.\n" +
" #\n" +
" <pre style=\"color:#000000;\">\n" +
"   <span style=\"color:#000000;font-style:italic;\">File</span><span style=\"\">.</span><span style=\"\">basename</span><span style=\"\">(</span><span style=\"color:#ce7b00;\">\"</span><span style=\"color:#ce7b00\"><span style=\"color:#ce7b00;\">/home/gumby/work/ruby.rb</span></span><span style=\"color:#ce7b00;\">\"</span><span style=\"\">)</span>          <span style=\"color:#969696\"><span style=\"color:#969696;\">#=> \"ruby.rb\"<br></span></span><br>   <span style=\"color:#000000;font-style:italic;\">File</span><span style=\"\">.</span><span style=\"\">basename</span><span style=\"\">(</span><span style=\"color:#ce7b00;\">\"</span><span style=\"color:#ce7b00\"><span style=\"color:#ce7b00;\">/home/gumby/work/ruby.rb</span></span><span style=\"color:#ce7b00;\">\"</span><span style=\"\">,</span> <span style=\"color:#ce7b00;\">\"</span><span style=\"color:#ce7b00\"><span style=\"color:#ce7b00;\">.rb</span></span><span style=\"color:#ce7b00;\">\"</span><span style=\"\">)</span>   <span style=\"color:#969696\"><span style=\"color:#969696;\">#=> \"ruby\"<br></span></span><br></pre>\n" +
"#\n" +
" #\n ";

        String html = instance.toHtml();
                
        assertEquals(expected, html);
    }
    
    public void testEndTag() {
        RDocFormatter instance = new RDocFormatter();
        
        instance.appendLine("# <b>whatever</b>");
        assertEquals("<b>whatever</b> ", instance.toHtml());
    }
    
    public void testLineBreaks() {
        RDocFormatter instance = new RDocFormatter();
        
        instance.appendLine("#");
        instance.appendLine("# Calling content_for stores the block of markup for later use.");
        instance.appendLine("# Subsequently, you can make calls to it by name with <tt>yield</tt> in");
        instance.appendLine("# another template or in the layout.");
        instance.appendLine("# ");
        instance.appendLine("# Example:");
        instance.appendLine("# ");
        instance.appendLine("#   <% content_for(\"header\") do %>");
        instance.appendLine("#     alert('hello world')");

        String html = instance.toHtml();
        assertEquals("Calling content_for stores the block of markup for later use. Subsequently," +
                " you can make calls to it by name with <tt>yield</tt> in another template or in " +
                "the layout. <br><br>Example: <br><pre>\n  &lt;% content_for(\"header\") do " +
                "%><br>    alert('hello world')<br></pre>\n", html);
    }

    public void testFormatLists() throws Exception {
        formatFile("testfiles/lists.comment");
    }

    public void testLinks() throws Exception {
        RDocFormatter instance = new RDocFormatter();

        instance.appendLine("#");
        instance.appendLine("#This should not be a link");
        instance.appendLine("# This#should be a link");
        instance.appendLine("# ");
        String html = instance.toHtml();
        assertEquals("This should not be a link <a href=\"This#should\">This#should</a> be a link <br><br>", html);

    }

    public void testStopDoc() throws Exception {
        RDocFormatter instance = new RDocFormatter();

        instance.appendLine("# This should be displayed");
        instance.appendLine("# :stopdoc:");
        instance.appendLine("# THIS SHOULD BE FILTERED OUT");
        instance.appendLine("# :startdoc:");
        instance.appendLine("# This should also be displayed");
        String html = instance.toHtml();
        assertEquals("This should be displayed  This should also be displayed ", html);

    }

    public void testCallSeq() {
        RDocFormatter instance = new RDocFormatter();
        instance.appendLine("# Here comes a call-seq");
        instance.appendLine("# :call-seq:");
        instance.appendLine("#   my_method(arg1, arg2) -> self");
        instance.appendLine("#");
        instance.appendLine("# and now it's over");

        String html = instance.toHtml();

        assertEquals("Here comes a call-seq \n" +
                "<hr>\n" +
                "<pre>\n" +
                "my_method(arg1, arg2) -> self<br></pre>\n" +
                "<hr>\n" +
                "and now it's over ", html);

    }
    
    // TODO test bullets, labels, preformat
}
