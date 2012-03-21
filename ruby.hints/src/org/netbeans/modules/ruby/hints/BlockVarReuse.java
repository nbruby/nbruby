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
import org.jrubyparser.ast.IterNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.jrubyparser.ast.INameNode;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.EditList;
import org.netbeans.modules.csl.api.EditRegions;
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
import org.openide.util.NbBundle;

/**
 * A hint which looks for block variables that are reusing a variable
 * that already exists in scope when the block is initiated.
 * This will (-possibly- unintentionally) reassign the local variable
 * so might be something the user want to be alerted to
 * 
 * @todo Doesn't seem to work for params
 * @todo Don't warn on last lines?
 * @todo Doesn't seem to work for comma-separated lists of arguments, e.g. { |foo,bar| }
 *   (at least not the second arg)
 *
 * @author Tor Norbye
 */
public class BlockVarReuse extends RubyAstRule {

    public BlockVarReuse() {
    }

    public boolean appliesTo(RuleContext context) {
        return true;
    }

    public Set<NodeType> getKinds() {
        return Collections.singleton(NodeType.ITERNODE);
    }

    public void cancel() {
        // Does nothing
    }

    public String getId() {
        return "Block_Var_Reuse"; // NOI18N
    }

    public String getDisplayName() {
        return NbBundle.getMessage(BlockVarReuse.class, "UnintentionalSideEffect");
    }

    public String getDescription() {
        return NbBundle.getMessage(BlockVarReuse.class, "UnintentionalSideEffectDesc");
    }

    public void run(RubyRuleContext context, List<Hint> result) {
        Node node = context.node;
        ParserResult info = context.parserResult;

        if (node.getNodeType() == NodeType.ITERNODE) {
            // Check the children and see if we have a LocalAsgnNode; these are going
            // to be local variable reuses
            List<Node> list = node.childNodes();

            for (Node child : list) {
                if (child.getNodeType() == NodeType.LOCALASGNNODE) {

                    OffsetRange range = AstUtilities.getNameRange(child);
                    List<HintFix> fixList = new ArrayList<HintFix>(2);
                    Node root = AstUtilities.getRoot(info);
                    AstPath childPath = new AstPath(root, child);
                    fixList.add(new RenameVarFix(context, childPath, false));
                    fixList.add(new RenameVarFix(context, childPath, true));

                    range = LexUtilities.getLexerOffsets(info, range);
                    if (range != OffsetRange.NONE) {
                        Hint desc = new Hint(this, getDisplayName(), RubyUtils.getFileObject(info), range, fixList, 100);
                        result.add(desc);
                    }
                }
            }
        }
    }

    private static class RenameVarFix implements PreviewableFix {

        private final RubyRuleContext context;
        private final AstPath path;
        private final boolean renameLocal;

        RenameVarFix(RubyRuleContext context, AstPath path, boolean renameLocal) {
            this.context = context;
            this.path = path;
            this.renameLocal = renameLocal;
        }

        public String getDescription() {
            if (renameLocal) {
                return NbBundle.getMessage(BlockVarReuse.class, "ChangeLocalVarName");
            } else {
                return NbBundle.getMessage(BlockVarReuse.class, "ChangeBlockVarName");
            }
        }

        public void implement() throws Exception {
            // Refactoring isn't necessary here since local variables and block
            // variables are limited to the local scope, so we can accurately just
            // find their positions using the AST and let the user edit them synchronously.
            Set<OffsetRange> ranges = findRegionsToEdit();

            // Pick the first range as the caret offset
            int caretOffset = Integer.MAX_VALUE;
            for (OffsetRange range : ranges) {
                if (range.getStart() < caretOffset) {
                    caretOffset = range.getStart();
                }
            }

            // Initiate synchronous editing:
            EditRegions.getInstance().edit(RubyUtils.getFileObject(context.parserResult), ranges, caretOffset);
        }

        private void addNonBlockRefs(Node node, String name, Set<OffsetRange> ranges, boolean isParameter) {
            if ((node.getNodeType() == NodeType.LOCALASGNNODE || node.getNodeType() == NodeType.LOCALVARNODE) && name.equals(((INameNode)node).getName())) {
                OffsetRange range = AstUtilities.getNameRange(node);
                range = LexUtilities.getLexerOffsets(context.parserResult, range);
                if (range != OffsetRange.NONE) {
                    ranges.add(range);
                }
            } else if (isParameter && (node.getNodeType() == NodeType.ARGUMENTNODE && name.equals(((INameNode)node).getName()))) {
                OffsetRange range = AstUtilities.getNameRange(node);
                range = LexUtilities.getLexerOffsets(context.parserResult, range);
                if (range != OffsetRange.NONE) {
                    ranges.add(range);
                }
            } else if (node.getNodeType() == NodeType.ARGSNODE) {
                isParameter = true;
            }

            List<Node> list = node.childNodes();

            for (Node child : list) {
                if (child.isInvisible()) {
                    continue;
                }

                // Skip inline method defs
                if (child.getNodeType() == NodeType.DEFNNODE || child.getNodeType() == NodeType.DEFSNODE) {
                    continue;
                }

                // Skip SOME blocks:
                if (child.getNodeType() == NodeType.ITERNODE) {
                    // Skip only the block which the fix is applying to;
                    // the local variable could be aliased in other blocks as well
                    // and we need to keep the program correct!
                    if (child == path.leafParent()) {

                        continue;
                    }
                }

                addNonBlockRefs(child, name, ranges, isParameter);
            }
        }

        private Set<OffsetRange> findRegionsToEdit() {
            Set<OffsetRange> ranges = new HashSet<OffsetRange>();

            assert path.leaf() instanceof INameNode;
            String name = ((INameNode)path.leaf()).getName();

            if (renameLocal) {
                Node scope = AstUtilities.findLocalScope(path.leaf(), path);
                addNonBlockRefs(scope, name, ranges, false);
            } else {
                Node parent = path.leafParent();
                assert parent instanceof IterNode;
                addNonBlockRefs(parent, name, ranges, false);
            }

            return ranges;
        }

        public boolean isSafe() {
            return false;
        }

        public boolean isInteractive() {
            return true;
        }

        public EditList getEditList() throws Exception {
            BaseDocument doc = context.doc;
            EditList edits = new EditList(doc);
            Set<OffsetRange> ranges = findRegionsToEdit();
            String oldName = ((INameNode)path.leaf()).getName();
            int oldLen = oldName.length();
            String newName = "new_name";
            for (OffsetRange range : ranges) {
                edits.replace(range.getStart(), oldLen, newName, false, 0);
            }
            return edits;
        }

        public boolean canPreview() {
            return true;
        }
    }

    public boolean getDefaultEnabled() {
        return true;
    }

    public HintSeverity getDefaultSeverity() {
        return HintSeverity.WARNING;
    }

    public boolean showInTasklist() {
        return true;
    }

    public JComponent getCustomizer(Preferences node) {
        return null;
    }
}
