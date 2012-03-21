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
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */

package org.netbeans.modules.ruby.hints;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import org.jrubyparser.ast.IfNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.netbeans.api.lexer.TokenHierarchy;
import org.netbeans.api.lexer.TokenSequence;
import org.netbeans.editor.BaseDocument;
import org.netbeans.editor.Utilities;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.PreviewableFix;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.ruby.AstUtilities;
import org.netbeans.modules.ruby.RubyUtils;
import org.netbeans.modules.ruby.hints.infrastructure.RubyAstRule;
import org.netbeans.modules.ruby.hints.infrastructure.RubyRuleContext;
import org.netbeans.modules.ruby.lexer.LexUtilities;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * Convert conditions of the form "if !foo" to "unless foo".
 * 
 * Inspired by the excellent blog entry
 *   http://langexplr.blogspot.com/2007/11/creating-netbeans-ruby-hints-with-scala_24.html
 * by Luis Diego Fallas.
 * 
 * @todo Check ?: nodes
 * 
 * @author Tor Norbye
 */
public class ConvertIfToUnless extends RubyAstRule {

    public Set<NodeType> getKinds() {
        return Collections.singleton(NodeType.IFNODE);
    }

    public void run(RubyRuleContext context, List<Hint> result) {
        Node node = context.node;
        ParserResult info = context.parserResult;

        IfNode ifNode = (IfNode) node;

        // Convert unless blocks?
        Node condition = ifNode.getCondition();
        if (condition == null) {
            // Can happen for this code:
            //   if ()
            //   end
            // (typically while editing)
            return;
        }
        
        // Can't convert if !x/elseif blocks
        if (ifNode.getElseBody() != null && ifNode.getElseBody().getNodeType() == NodeType.IFNODE) {
            return;
        }
        
        if (condition.getNodeType() == NodeType.NOTNODE ||
                (condition.getNodeType() == NodeType.NEWLINENODE &&
                condition.childNodes().size() == 1 &&
                condition.childNodes().get(0).getNodeType() == NodeType.NOTNODE)) {
            try {
                BaseDocument doc = context.doc;
                int keywordOffset = findKeywordOffset(context, ifNode);
                if (keywordOffset == -1 || keywordOffset > doc.getLength() - 1) {
                    return;
                }

                OffsetRange range = AstUtilities.getRange(node);

                if (RubyUtils.isRhtmlDocument(doc) || RubyUtils.isYamlDocument(doc)) {
                    // Make sure that we're in a single contiguous Ruby section; if not, this won't work
                    range = LexUtilities.getLexerOffsets(info, range);
                    if (range == OffsetRange.NONE) {
                        return;
                    }

                    try {
                        doc.readLock();
                        TokenHierarchy th = TokenHierarchy.get(doc);
                        TokenSequence ts = th.tokenSequence();
                        ts.move(range.getStart());
                        if (!ts.moveNext() && !ts.movePrevious()) {
                            return;
                        }

                        if (ts.offset()+ts.token().length() < range.getEnd()) {
                            return;
                        }
                    } finally {
                        if (doc != null) {
                            doc.readUnlock();
                        }
                    }
                }

                ConvertToUnlessFix fix = new ConvertToUnlessFix(context, ifNode);
                
                // Make sure we can actually perform the edit
                if (fix.getEditList() == null) {
                    return;
                }
                
                List<HintFix> fixes = Collections.<HintFix>singletonList(fix);

                String displayName = NbBundle.getMessage(ConvertIfToUnless.class,
                        "ConvertIfToUnless");
                Hint desc = new Hint(this, displayName, RubyUtils.getFileObject(info), range,
                        fixes, 500);
                result.add(desc);
            } catch (BadLocationException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    public String getId() {
        return "ConvertIfToUnless"; // NOI18N
    }

    public String getDescription() {
        return NbBundle.getMessage(ConvertIfToUnless.class, "ConvertIfToUnlessDesc");
    }

    public boolean getDefaultEnabled() {
        return true;
    }

    public JComponent getCustomizer(Preferences node) {
        return null;
    }

    public boolean appliesTo(RuleContext context) {
        return true;
    }

    public String getDisplayName() {
        return NbBundle.getMessage(ConvertIfToUnless.class, "ConvertIfToUnless");
    }

    public boolean showInTasklist() {
        return false;
    }

    public HintSeverity getDefaultSeverity() {
        return HintSeverity.CURRENT_LINE_WARNING;
    }
    
    static int findKeywordOffset(RubyRuleContext context, IfNode ifNode) throws BadLocationException {
        BaseDocument doc = context.doc;
        ParserResult info = context.parserResult;

        int astIfOffset = ifNode.getPosition().getStartOffset();
        int lexIfOffset = LexUtilities.getLexerOffset(info, astIfOffset);
        if (lexIfOffset == -1 || lexIfOffset > doc.getLength()) {
            return -1;
        }

        String statement = doc.getText(lexIfOffset, 2);
        if (statement.equals("if")) {
            // Make sure it's not "elsif"
            if (lexIfOffset > 3) {
                statement = doc.getText(lexIfOffset-3, 5);
                if ("elsif".equals(statement)) {
                    return -1;
                }
            }
            return lexIfOffset;
        } else if (statement.equals("un")) {
            return lexIfOffset;
        } else {
            // Probably a statement modifier - gotta adjust the if offset
            int conditionStart = LexUtilities.getLexerOffset(info, AstUtilities.getRange(ifNode.getCondition()).getStart());
            int lineStart = Utilities.getRowFirstNonWhite(doc, conditionStart);
            if (lineStart != -1 && lineStart < conditionStart) {
                String line = doc.getText(lineStart, conditionStart-lineStart).trim();
                if (line.endsWith("elsif")) { // NO!I8N
                    // Can't perform conversions on elsif!
                    return -1;
                }
                if (line.endsWith("if")) { // NOI18N
                    return lineStart + line.length() - 2;
                } else if (line.endsWith("unless")) { // NOI18N
                    return lineStart + line.length() - 6;
                }
            }
        }
        
        return -1;
    }
    
    private class ConvertToUnlessFix implements PreviewableFix {
        private final RubyRuleContext context;
        private final IfNode ifNode;

        public ConvertToUnlessFix(RubyRuleContext context, IfNode ifNode) {
            this.context = context;
            this.ifNode = ifNode;
        }

        public String getDescription() {
            String lif = "if"; // NOI18N
            String lunless = "unless"; // NOI18N
            String from;
            String to;
            if (ifNode.getThenBody() != null) {
                from = lif;
                to = lunless;
            } else {
                from = lunless;
                to = lif;
            }
            return NbBundle.getMessage(ConvertIfToUnless.class, "ConvertIfToUnlessFix", from, to);
        }

        public void implement() throws Exception {
            EditList edits = getEditList();
            if (edits != null) {
                edits.apply();
            }
        }

        public EditList getEditList() {
            BaseDocument doc = context.doc;

            try {
                Node notNode = ifNode.getCondition();
                if (notNode.getNodeType() != NodeType.NOTNODE) {
                    assert notNode.getNodeType() == NodeType.NEWLINENODE;
                    Node firstChild = notNode.childNodes().size() == 1 ?
                        notNode.childNodes().get(0) : null;
                    if (firstChild != null && firstChild.getNodeType() == NodeType.NOTNODE) {
                        notNode = firstChild;
                    } else {
                        // Unexpected!
                        assert false : firstChild;
                        return null;
                    }

                }

                int deleteSize = 1;

                ParserResult info = context.parserResult;
                int astNotOffset = AstUtilities.getRange(notNode).getStart();
                int lexNotOffset = LexUtilities.getLexerOffset(info, astNotOffset);
                if (lexNotOffset == -1 || lexNotOffset > doc.getLength()-1) {
                    return null;
                }

                int astIfOffset = ifNode.getPosition().getStartOffset();
                int lexIfOffset = LexUtilities.getLexerOffset(info, astIfOffset);
                if (lexIfOffset == -1 || lexIfOffset > doc.getLength()) {
                    return null;
                }

                boolean isEqualComparison = false;
                char c = doc.getText(lexNotOffset, 1).charAt(0);
                if (c != '!') {
                    // Probably something like "!=", where the not node range points to
                    // a call node calling method "=="
                    int lineEnd = Utilities.getRowEnd(doc, lexNotOffset);
                    String line = doc.getText(lexNotOffset, lineEnd-lexNotOffset);
                    int lineOffset = line.indexOf("!=");
                    if (lineOffset == -1) {
                        lineOffset = line.indexOf("!~");
                        if (lineOffset != -1) {
                            lexNotOffset += lineOffset;
                        } else {
                            boolean ok = false;
                            if (lexNotOffset < doc.getLength()-3) {
                                String not = doc.getText(lexNotOffset, 3);
                                if ("not".equals(not)) { // NOI18N
                                    deleteSize = 3;
                                    if (lexNotOffset < doc.getLength()-4) {
                                        not = doc.getText(lexNotOffset, 4);
                                        if ("not ".equals(not)) {
                                            deleteSize = 4;
                                        }
                                    }
                                    ok = true;
                                }
                            }
                            if (!ok) {
                                assert false : line;
                                return null;
                            }
                        }
                    } else {
                        lexNotOffset += lineOffset;
                        isEqualComparison = true;
                    }
                }

                int keywordOffset = findKeywordOffset(context, ifNode);
                if (keywordOffset == -1 || keywordOffset > doc.getLength()-1) {
                    return null;
                }

                assert keywordOffset < lexNotOffset;

                char k = doc.getText(keywordOffset, 1).charAt(0);
                boolean isIf = k == 'i';

                EditList edits = new EditList(doc);

                if (isEqualComparison) {
                    // Convert != into ==
                    edits.replace(lexNotOffset, 1, "=", false, 0);
                } else {
                    // Just remove ! from the expression (or "not ")
                    edits.replace(lexNotOffset, deleteSize, null, false, 0);
                }
                if (isIf) {
                    edits.replace(keywordOffset, 2, "unless", false, 1);
                } else {
                    edits.replace(keywordOffset, 6, "if", false, 1);
                }

                return edits;
            } catch (BadLocationException ble) {
                Exceptions.printStackTrace(ble);
                return null;
            }
        }

        public boolean isSafe() {
            return true;
        }

        public boolean isInteractive() {
            return false;
        }

        public boolean canPreview() {
            return true;
        }
    }
}
