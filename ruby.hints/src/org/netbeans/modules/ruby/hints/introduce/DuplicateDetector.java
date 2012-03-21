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
package org.netbeans.modules.ruby.hints.introduce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jrubyparser.ast.BignumNode;
import org.jrubyparser.ast.FixnumNode;
import org.jrubyparser.ast.FloatNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.jrubyparser.ast.StrNode;
import org.jrubyparser.ast.SymbolNode;
import org.jrubyparser.ast.XStrNode;
import org.netbeans.editor.BaseDocument;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.ruby.AstUtilities;

/**
 * Detect duplicates of a given subtree in a large parse tree.
 * 
 * @author Tor Norbye
 */
public class DuplicateDetector {

    private ParserResult info;
    private BaseDocument doc;
    private List<Node> nodes;
    private Node startNode;
    private Node endNode;
    private Node root;
    private int currentStart;
    private int currentEnd;
    private List<OffsetRange> duplicates = new ArrayList<OffsetRange>();

    public DuplicateDetector(ParserResult info, BaseDocument doc,
            Node root, List<Node> nodes, Node startNode, Node endNode) {
        this.info = info;
        this.doc = doc;
        this.root = root;
        this.nodes = nodes;
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public static List<OffsetRange> findDuplicates(ParserResult info, BaseDocument doc,
            Node root, List<Node> nodes,
            Node startNode, Node endNode) {
        // I only support trivial duplicates now (single node constants like strings, numbers etc.
        if (nodes.size() == 0 || startNode != endNode) {
            return Collections.emptyList();
        }

        if (startNode.getNodeType() == NodeType.ARRAYNODE) {
            if (startNode.childNodes().size() == 1) {
                startNode = startNode.childNodes().get(0);
            } else {
                return Collections.emptyList();
            }
        }
        DuplicateDetector detector = new DuplicateDetector(info, doc, root, nodes, startNode,
                endNode);
        detector.visit(root, startNode);
        return detector.duplicates;
    }

    private void visit(Node node, Node target) {
        if (node.getNodeType() == target.getNodeType()) {
            //OffsetRange range = getEquivalentTree(child);
            //if (range != OffsetRange.NONE) {
            //    duplicates.add(range);
            //}
            // No useful equals implementations on these puppies yet
            //if (node.equals(target)) {
            //    duplicates.add(AstUtilities.getRange(node));
            //} else {
            boolean equal = false;
            switch (node.getNodeType()) {
            // TODO - compare HashNodes
            case FLOATNODE: {
                equal = (((FloatNode) node).getValue() == ((FloatNode) target).getValue());
                break;
            }
            case BIGNUMNODE: {
                equal = (((BignumNode) node).getValue() == ((BignumNode) target).getValue());
                break;
            }
            case FIXNUMNODE: {
                equal = (((FixnumNode) node).getValue() == ((FixnumNode) target).getValue());
                break;
            }
            case SYMBOLNODE: {
                equal = ((SymbolNode) node).getName().equals(((SymbolNode) target).getName());
                break;
            }
            case STRNODE: {
                equal = ((StrNode) node).getValue().equals(((StrNode) target).getValue());
                break;
            }
            case XSTRNODE: {
                equal = ((XStrNode) node).getValue().equals(((XStrNode) target).getValue());
                break;
            }
            //case DSTRNODE: {
            //    equal = ((DStrNode)node).getValue().equals(((DStrNode)target).getValue());
            //    break;
            //}
            //case DREGEXPNODE: {
            //    equal = ((DRegexpNode)node).getValue().equals(((DRegexpNode)target).getValue());
            //    break;
            //}
            }

            if (equal) {
                duplicates.add(AstUtilities.getRange(node));
            }
        //}
        }
        List<Node> list = node.childNodes();

        for (Node child : list) {
            if (child.isInvisible()) {
                continue;
            }
            visit(child, target);
        }
    }
    //private OffsetRange getEquivalentTree(Node top) {
    //    // Recursively check the given top and if the trees are equivalent return the range
    //    return OffsetRange.NONE;
    //}
}
