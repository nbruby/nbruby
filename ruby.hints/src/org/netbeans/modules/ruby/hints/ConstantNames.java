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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.jrubyparser.ast.ConstDeclNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.ruby.AstUtilities;
import org.netbeans.modules.ruby.RubyUtils;
import org.netbeans.modules.ruby.hints.infrastructure.RubyAstRule;
import org.netbeans.modules.ruby.hints.infrastructure.RubyRuleContext;
import org.netbeans.modules.ruby.lexer.LexUtilities;
import org.openide.util.NbBundle;

/**
 * Check constant names to see if they conform to standard Ruby conventions
 * 
 * @author Tor Norbye
 */
public class ConstantNames extends RubyAstRule {
    public ConstantNames() {
    }

    public boolean appliesTo(RuleContext context) {
        return true;
    }

    public Set<NodeType> getKinds() {
        return Collections.singleton(NodeType.CONSTDECLNODE);
    }
    
    public void run(RubyRuleContext context, List<Hint> result) {
        Node node = context.node;
        ParserResult info = context.parserResult;

        assert (node.getNodeType() == NodeType.CONSTDECLNODE);
        String name = ((ConstDeclNode)node).getName();

        for (int i = 0; i < name.length(); i++) {
            if (Character.isLowerCase(name.charAt(i))) {
                String displayName = NbBundle.getMessage(ConstantNames.class, "InvalidConstantName");
                OffsetRange range = AstUtilities.getNameRange(node);
                range = LexUtilities.getLexerOffsets(info, range);
                if (range != OffsetRange.NONE) {
                    List<HintFix> fixList = Collections.emptyList();
                    Hint desc = new Hint(this, displayName, RubyUtils.getFileObject(info), range, fixList, 1600);
                    result.add(desc);
                }
                return;
            }
        }
    }
    
    public String getId() {
        return "Constant_Names"; // NOI18N
    }

    public String getDisplayName() {
        return NbBundle.getMessage(ConstantNames.class, "ConstantNameWarning");
    }

    public String getDescription() {
        return NbBundle.getMessage(ConstantNames.class, "ConstantNameWarningDesc");
    }

    public boolean getDefaultEnabled() {
        return false;
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

