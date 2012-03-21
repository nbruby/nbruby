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

package org.netbeans.modules.ruby.lexer;


import org.netbeans.modules.ruby.*;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.lib.lexer.test.LexerTestUtilities;
import org.netbeans.modules.ruby.lexer.RubyTokenId;


/**
 *
 * @author Tor Norbye
 */
public class RubyLexerTest extends RubyTestBase {
    public RubyLexerTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws java.lang.Exception {
        // Set-up testing environment
        LexerTestUtilities.setTesting(true);
    }

    @Override
    protected void tearDown() throws java.lang.Exception {
    }

    @SuppressWarnings("unchecked")
    public void testComments() {
        String text = "# This is my comment";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LINE_COMMENT, text);
    }

    @SuppressWarnings("unchecked")
    public void testRubyEmbedding() {
        String text = "%r{foo#{code}bar}";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_BEGIN, "%r{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_LITERAL, "foo");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "code");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_LITERAL, "}");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_LITERAL, "bar");
    }

    @SuppressWarnings("unchecked")
    public void testStatementModifiers() {
        String text = "foo if false}";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "foo");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        // Not RubyTokenId.IF - test that this if is just a statement modifier!
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ANY_KEYWORD, "if");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ANY_KEYWORD, "false");
        
        // Make sure if when used not as a statement modifier is recognized
        text = "if false foo}";
        hi = TokenHierarchy.create(text, RubyTokenId.language());
        ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IF, "if");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ANY_KEYWORD, "false");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "foo");
    }
    
    public void testStrings() {
        String[] strings =
            new String[] { 
            "\"Hello\"",
            "'Hello'",
            "%(Hello)",
            "%q(Hello)",
            "% Hello "};
        for (int i = 0; i < strings.length; i++) {
            TokenHierarchy hi = TokenHierarchy.create(strings[i], RubyTokenId.language());
            TokenSequence ts = hi.tokenSequence();
            assertTrue(ts.moveNext());
            assertTrue(ts.token().id() == RubyTokenId.STRING_BEGIN || ts.token().id() == RubyTokenId.QUOTED_STRING_BEGIN);
            assertTrue(ts.moveNext());
            assertTrue(ts.token().id() == RubyTokenId.STRING_LITERAL || ts.token().id() == RubyTokenId.QUOTED_STRING_LITERAL);
            assertTrue(ts.moveNext());
            assertTrue(ts.token().id() == RubyTokenId.STRING_END || ts.token().id() == RubyTokenId.QUOTED_STRING_END);
        }
    }

    @SuppressWarnings("unchecked")
    public void test96485() {
        String text = "\"foo#{\"";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#{");
        //LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ERROR, "\"");

        // Try related scenario for fields
        text = "\"foo#@\"";
        hi = TokenHierarchy.create(text, RubyTokenId.language());
        ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "@");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "\"");
    }
    
    @SuppressWarnings("unchecked")
    public void test101122() {
        String text = "\"\\n\\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ERROR, "\\n\\n");
    }

    
    
    @SuppressWarnings("unchecked")
    public void testUnterminatedString() {
        String text = "\"Line1\nLine2\nLine3";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ERROR, "Line1\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.CONSTANT, "Line2");
    }

    @SuppressWarnings("unchecked")
    public void testUnterminatedString2() {
        String text = "puts \"\n\n\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "puts");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ERROR, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n\n");
    }    

    @SuppressWarnings("unchecked")
    public void testUnterminatedString3() {
        String text = "x = \"";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "x");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.NONUNARY_OP, "=");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings({"unchecked", "empty-statement"})
    public void test93990() {
        String text = "f(<<EOT,\"abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz\")\n0123456789\nEOT\ny=5";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        // Just iterate through the sequence to make sure it's okay - this throws an exception because of bug 93990
        while (ts.moveNext()) {
            ;
        }
    }

    @SuppressWarnings({"unchecked", "empty-statement"})
    public void test93990b() {

        String text = "x = f(<<EOT,<<EOY, \"another string\", 50)  # Comment _here\n_xFoo bar\nEOT\nhello\nEOY\ndone\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        // Just iterate through the sequence to make sure it's okay - this throws an exception because of bug 93990
        while (ts.moveNext()) {
            ;
        }
    }
    
    @SuppressWarnings({"unchecked", "empty-statement"})
    public void test93990c() {
        // Multiline
        String text="    javax.swing.JOptionPane.showMessageDialog(nil, <<EOS)\n<html>Hello from <b><u>JRuby</u></b>.<br>\nButton '#{evt.getActionCommand()}' clicked.\nEOS\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        // Just iterate through the sequence to make sure it's okay - this throws an exception because of bug 93990
        while (ts.moveNext()) {
            ;
        }
    }

    @SuppressWarnings("unchecked")
    public void testHeredocInput0() {
        // Make sure I can handle input AFTER a heredoc marker and properly tokenize it
        String text = "f(<<EOT)\nfoo\nEOT\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOT\n");
        assertFalse(ts.moveNext());
    }
    
    @SuppressWarnings("unchecked")
    public void testHeredocInput() {
        // Make sure I can handle input AFTER a heredoc marker and properly tokenize it
        String text = "f(<<EOT,# Comment\nfoo\nEOT\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LINE_COMMENT, "# Comment\n");
       // LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOT\n");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings("unchecked")
    public void testHeredocInput2() {
        String text = "f(<<EOT,<<EOY)\nfoo\nEOT\nbar\nEOY\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOY");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOT\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "bar\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOY\n");
        assertFalse(ts.moveNext());
    }
    
    @SuppressWarnings("unchecked")
    public void testHeredocInput3a() { // Boiled down failure from postgresql_adapter.rb
        String text = "q(<<S,name)\nHELLO\nS\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "q");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<S");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "name");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "HELLO\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "S\n");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings("unchecked")
    public void testHeredocInput3b() { // Mutation of 3b
        String text = "q(<<S,t)\nHELLO\nS\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "q");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<S");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "t");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "HELLO\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "S\n");
        assertFalse(ts.moveNext());
    }
    
    @SuppressWarnings("unchecked")
    public void testHeredocEmbedded() {
        String text = "f(<<EOT,<<EOY)\nfoo#{hello}foo\n#{hello}\n\n\nEOT\nbar\nEOY\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOY");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "hello");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "}foo\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "hello");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "}\n\n\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOT\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "bar\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOY\n");
        assertFalse(ts.moveNext());
    }
    @SuppressWarnings("unchecked")
    public void testHeredocEmpty() {
        String text = "f(<<EOT\nEOT\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_END, "EOT\n");
        assertFalse(ts.moveNext());
    }
    
    @SuppressWarnings("unchecked")
    public void testHeredocError2() {
        String text = "f(<<EOT\nfoo";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        //LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.ERROR, "foo");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings("unchecked")
    public void testHeredocError3() {
        String text = "f(<<EOT";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        assertFalse(ts.moveNext());
    }
    
    @SuppressWarnings("unchecked")
    public void testHeredocsIndented() {
        String text = "f(<<-EOT,<<-EOY)\nfoo\n   EOT\nbar\n   EOY\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<-EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<-EOY");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        // XXX Is it correct that the string would include the indentation on the closing
        // delimiter line??
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo\n   ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOT\n");
        // XXX Is it correct that the string would include the indentation on the closing
        // delimiter line??
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "bar\n   ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOY\n");
        assertFalse(ts.moveNext());
    }
    
    @SuppressWarnings("unchecked")
    public void testHeredocsIndentedQuoted() {
        String text = "f(<<-\"EOT\",<<-\"EOY\")\nfoo\n   EOT\nbar\n   EOY\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<-\"EOT\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, ",");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<-\"EOY\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo\n   ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOT\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "bar\n   ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOY\n");
        assertFalse(ts.moveNext());
    }
    
    // 102082
    @SuppressWarnings("unchecked")
    public void testSymbol() {
        String text = ":\"foo\"";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, ":\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, "foo");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, "\"");
        assertFalse(ts.moveNext());
    }
    
    // #167952
    @SuppressWarnings("unchecked")
    public void testSymbolWithEmbeddedRuby() {
        String text = ":\"f#{0}o\"";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, ":\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "0");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, "}");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, "o");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.TYPE_SYMBOL, "\"");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings("unchecked")
    public void testQuotesInEmbeddedCode() {
        // Simplified from sqlserver_adapter's add_limit_offset! method which failed miserably:
        // total_rows = @connection.select_all("SELECT count(*) as TotalRows from (#{sql.gsub(/\bSELECT(\s+DISTINCT)?\b/i, "SELECT#{$1} TOP 1000000000")}) tally")[0][:TotalRows].to_i
        // Quotes are allowed inside a string embedded
        String text = "\"fo#{\"hello\"}\"";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "fo");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "\"hello\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "}");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "\"");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings("unchecked")
    public void testQuotesInEmbeddedCode2() {
        // Simplified from sqlserver_adapter's add_limit_offset! method which failed miserably:
        // total_rows = @connection.select_all("SELECT count(*) as TotalRows from (#{sql.gsub(/\bSELECT(\s+DISTINCT)?\b/i, "SELECT#{$1} TOP 1000000000")}) tally")[0][:TotalRows].to_i
        // Quotes are allowed inside a string embedded
        String text = "\"fo#{puts \"#notcomment\"}\"";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "fo");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "puts \"#notcomment\"");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "}");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "\"");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings("unchecked")
    public void testNestedEmbeddedCode() {
        // Simplified from sqlserver_adapter's add_limit_offset! method which failed miserably:
        // total_rows = @connection.select_all("SELECT count(*) as TotalRows from (#{sql.gsub(/\bSELECT(\s+DISTINCT)?\b/i, "SELECT#{$1} TOP 1000000000")}) tally")[0][:TotalRows].to_i
        String text = "x(%(#{y=#{z}}))";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "x");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "%(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "y=#{z}");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "}");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        assertFalse(ts.moveNext());
    }

    @SuppressWarnings("unchecked")
    public void testParensInEmbedded() {
        // Simplified from activerecord-1.15.3/test/base_test.rb in test_array_to_xml_including_methods
        //   assert xml.include?(%(<topic-id type="integer">#{topics(:first).topic_id}</topic-id>)), xml
        String text = "x(%(#{y(1)}))";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "x");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_BEGIN, "%(");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_LITERAL, "#{");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.EMBEDDED_RUBY, "y(1)");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "}");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, ")");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
        assertFalse(ts.moveNext());
    }
    
    @SuppressWarnings("unchecked")
    public void testSpaceEot() {
        // Make sure I can handle input AFTER a heredoc marker and properly tokenize it
        String text = "f <<EOT\nfoo\nEOT\ng <<EOM\nbar\nEOM\n";
        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOT");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "foo\n");
        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOT\n");

        // TODO!!
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "g");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.STRING_BEGIN, "<<EOM");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, "\n");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_LITERAL, "bar\n");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.QUOTED_STRING_END, "EOM\n");
//        
//        assertFalse(ts.moveNext());
    }
    

    // Not yet passing
//    @SuppressWarnings("unchecked")
//    public void testDefRegexp() {
//        //     def _make_regex(str) /([#{Regexp.escape(str)}])/n end
//        String text = "def f(s) /df/ end";
//        TokenHierarchy hi = TokenHierarchy.create(text, RubyTokenId.language());
//        TokenSequence<?extends RubyTokenId> ts = hi.tokenSequence();
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.DEF, "def");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "f");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.LPAREN, "(");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.IDENTIFIER, "s");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.RPAREN, ")");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_BEGIN, "/");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_LITERAL, "df");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.REGEXP_END, "/");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.WHITESPACE, " ");
//        LexerTestUtilities.assertNextTokenEquals(ts, RubyTokenId.END, "end");
//        assertFalse(ts.moveNext());
//    }
}    
