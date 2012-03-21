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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import javax.swing.text.BadLocationException;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.jrubyparser.SourcePosition;
import org.netbeans.api.lexer.Token;
import org.netbeans.api.lexer.TokenId;
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
import org.netbeans.modules.ruby.AstPath;
import org.netbeans.modules.ruby.AstUtilities;
import org.netbeans.modules.ruby.RubyUtils;
import org.netbeans.modules.ruby.hints.infrastructure.RubyAstRule;
import org.netbeans.modules.ruby.hints.infrastructure.RubyRuleContext;
import org.netbeans.modules.ruby.lexer.LexUtilities;
import org.netbeans.modules.ruby.lexer.RubyTokenId;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;

/**
 * Hint which adds a fix to lines containing a "single-line" definition
 * of a method or a class, and offers to expand it into a multi-line
 * definition, e.g. replacing
 * <pre>
 *    def foo; bar; end
 * </pre>
 * with
 * <pre>
 *    def foo
 *      bar
 *    end
 * </pre>
 * <p>
 * NOTE - this hint is only activated for the line under the caret!
 *
 * @todo Filter out the case where you have a def inside a class on the same line!
 * @todo Apply this tip to brace blocks as well - and offer both expand and collapse!
 * @todo Why doesn't this work on line begins? E.g. add "def foo; bar; end" and put the
 *   caret to the left of "def"; it doesn't activate
 * @todo See James Moore's comment about formatting multi-line statements
 * 
 * @author Tor Norbye
 */
public class ExpandSameLineDef extends RubyAstRule {
    public ExpandSameLineDef() {
    }

    public boolean appliesTo(RuleContext context) {
        ParserResult info = context.parserResult;
        // Skip for RHTML files for now - isn't implemented properly
        return RubyUtils.getFileObject(info).getMIMEType().equals("text/x-ruby");
    }

    public Set<NodeType> getKinds() {
        Set<NodeType> types = new HashSet<NodeType>();
        types.add(NodeType.CLASSNODE);
        types.add(NodeType.DEFNNODE);
        types.add(NodeType.DEFSNODE);
        return types;
    }

    public void run(RubyRuleContext context, List<Hint> result) {
        Node node = context.node;
        AstPath path = context.path;
        ParserResult info = context.parserResult;
        BaseDocument doc = context.doc;

        // Look for use of deprecated fields
        if (node.getNodeType() == NodeType.DEFNNODE || node.getNodeType() == NodeType.DEFSNODE || node.getNodeType() == NodeType.CLASSNODE) {
            SourcePosition pos = node.getPosition();
            try {
                if (doc == null) {
                    // Run on a file that was just closed
                    return;
                }
                
                int start = pos.getStartOffset();
                int end = pos.getEndOffset();
                int length = doc.getLength();
                if (Utilities.getRowEnd(doc, Math.min(start,length)) == Utilities.getRowEnd(doc, Math.min(end,length))) {
                    // Block is on a single line
                    // TODO - add a hint to turn off this hint?
                    // Should be a utility or infrastructure option!
                    Node root = AstUtilities.getRoot(info);
                    if (path.leaf() != node) {
                        path = new AstPath(root, node);
                    }
                    List<HintFix> fixList = Collections.<HintFix>singletonList(new ExpandLineFix(context, path));

                    OffsetRange range = new OffsetRange(pos.getStartOffset(), pos.getEndOffset());
                    Hint desc = new Hint(this, getDisplayName(), RubyUtils.getFileObject(info), range, fixList, 150);
                    result.add(desc);
                    
                    // Exit; don't process children such that a def inside a class all
                    // on the same line only produces a single suggestion for the outer block
                    return;
                }
            } catch (BadLocationException ex){
                Exceptions.printStackTrace(ex);
            }
        }
    }


    public void cancel() {
        // Does nothing
    }

    public String getId() {
        return "Expand_Same_Line_Def"; // NOI18N
    }

    public String getDisplayName() {
        return NbBundle.getMessage(ExpandSameLineDef.class, "ExpandLine");
    }

    public String getDescription() {
        return NbBundle.getMessage(ExpandSameLineDef.class, "ExpandLineDesc");
    }

    private static class ExpandLineFix implements PreviewableFix {

        private final RubyRuleContext context;
        private final AstPath path;

        ExpandLineFix(RubyRuleContext context, AstPath path) {
            this.context = context;
            this.path = path;
        }

        public String getDescription() {
            String code = path.leaf().getNodeType() == NodeType.DEFNNODE ? "def" : "class";
            return NbBundle.getMessage(ExpandSameLineDef.class, "ExpandLineFix", code);
        }
        
        private void findLineBreaks(Node node, Set<Integer> offsets) {
            if (node.getNodeType() == NodeType.NEWLINENODE) {
                offsets.add(node.getPosition().getStartOffset());
            }
            List<Node> list = node.childNodes();

            for (Node child : list) {
                if (child.isInvisible()) {
                    continue;
                }
                findLineBreaks(child, offsets);
            }
        }

        /** 
         * Try to split a line like
         *   class FooController; def rescue_action(e) raise e end; end 
         * into multiple lines. We can use lexical tokens like ";" as a clue
         * to where to put newlines, but we want to use the AST too such that
         * we see that we need a newline between the argument (e) and raise in the
         * above line.
         * <p>
         * By using both we'll get some offsets in the same area so we'll need
         * to be careful when applying our ;-to-\n replacements and our \n insertions
         * so we don't get multiple newlines for places where both the AST and
         * the semicolons suggest we need newlines.
         */
        public void implement() throws Exception {
            getEditList().apply();
        }

        public EditList getEditList() throws Exception {
            BaseDocument doc = context.doc;
            SourcePosition pos = path.leaf().getPosition();
            int startOffset = pos.getStartOffset();
            int endOffset = pos.getEndOffset();
            if (endOffset > doc.getLength()) {
                if (startOffset > doc.getLength()) {
                    startOffset = doc.getLength();
                }
                endOffset = doc.getLength();
            }
            
            // Look through the document and find the statement separators (;);
            // at these locations I'll replace the ; with a newline and then
            // apply a formatter
            Set<Integer> offsetSet = new HashSet<Integer>();
            findLineBreaks(path.leaf(), offsetSet);
            
            // Add in ; replacements
            TokenSequence<?extends RubyTokenId> ts = LexUtilities.getRubyTokenSequence(doc, endOffset);
            if (ts != null) {
                // Traverse sequence in reverse order such that my offset list is in decreasing order
                ts.move(endOffset);
                while (ts.movePrevious() && ts.offset() > startOffset) {
                    Token<?extends RubyTokenId> token = ts.token();
                    TokenId id = token.id();

                    if (id == RubyTokenId.IDENTIFIER && ";".equals(token.text().toString())) { // NOI18N
                        offsetSet.add(ts.offset());
                    } else if (id == RubyTokenId.CLASS || id == RubyTokenId.DEF || id == RubyTokenId.END) {
                        offsetSet.add(ts.offset());
                    }
                }
            }
            
            List<Integer> offsets = new ArrayList<Integer>(offsetSet);
            Collections.sort(offsets);
            // Ensure that we go in high to lower order such that I edit the
            // document from bottom to top (so offsets don't have to be adjusted
            // to account for our own edits along the way)
            Collections.reverse(offsets);

            EditList edits = new EditList(doc);
            
            if (offsets.size() > 0) {
                // TODO: Create a ModificationResult here and process it
                // The following is the WRONG way to do it...
                // I've gotta use a ModificationResult instead!

                List<Integer> newlines = new ArrayList<Integer>();
                
                try {
                    // Process offsets from back to front such that I can
                    // modify the document without worrying that the other offsets
                    // need to be adjusted
                    int prev = -1;
                    for (int offset : offsets) {
                        // We might get some dupes since we add offsets from both
                        // the AST newline nodes and semicolons discovered in the lexical token hierarchy
                        if (offset == prev) {
                            continue;
                        }
                        prev = offset;
                        if (";".equals(doc.getText(offset, 1))) { // NOI18N
                            edits.replace(offset, 1, null, false, 1);
                            if (newlines.contains(offset+2)) {
                                continue;
                            }
                        }
                        if (newlines.contains(offset+1) || newlines.contains(offset)) {
                            continue;
                        }
                        edits.replace(offset, 0, "\n", false, 2); // NOI18N
                        newlines.add(offset);
                    }
                    
                    edits.setFormatAll(true);
                } catch (BadLocationException ble) {
                    Exceptions.printStackTrace(ble);
                }
            }
            
            return edits;
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

    public boolean getDefaultEnabled() {
        return true;
    }

    public HintSeverity getDefaultSeverity() {
        return HintSeverity.CURRENT_LINE_WARNING;
    }

    public boolean showInTasklist() {
        return false;
    }

    public JComponent getCustomizer(Preferences node) {
        return null;
    }
}
