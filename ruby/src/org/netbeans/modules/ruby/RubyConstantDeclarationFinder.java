/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2010 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.ruby;

import java.util.Set;
import org.jrubyparser.ast.Colon2Node;
import org.jrubyparser.ast.ConstDeclNode;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.INameNode;
import org.netbeans.modules.csl.api.DeclarationFinder.DeclarationLocation;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.ruby.elements.IndexedConstant;

final class RubyConstantDeclarationFinder extends RubyBaseDeclarationFinder<IndexedConstant> {

    private final Node constantNode;

    RubyConstantDeclarationFinder(
            final ParserResult parserResult,
            final Node root,
            final AstPath path,
            final RubyIndex index,
            final Node constantNode) {
        super(parserResult, root, path, index);
        this.constantNode = constantNode;
    }

    DeclarationLocation findConstantDeclaration() {
        Set<? extends IndexedConstant> constants;
        if (constantNode instanceof Colon2Node) {
            String constantFqn = AstUtilities.getFqn((Colon2Node) constantNode);
            constants = index.getConstants(constantFqn);
        } else {
            // inside of class or module?
            String className = AstUtilities.getFqnName(path);
            constants = index.getConstants(className, getConstantName());
        }
        DeclarationLocation decl = getElementDeclaration(constants, constantNode);
        if (decl != DeclarationLocation.NONE) {
            return decl;
        }

        return fix(findLocal(root), info);
    }

    @Override
    IndexedConstant findBestMatchHelper(final Set<? extends IndexedConstant> constants) {
        // trivial implementation - seems to be enough for now
        return constants.isEmpty() ? null : constants.iterator().next();
    }

    private String getConstantName() {
        return ((INameNode) constantNode).getName();
    }

    /**
     * Recursively search for constants declaration that matches the given name.
     */
    private DeclarationLocation findLocal(final Node node) {
        if (node instanceof ConstDeclNode) {
            if (((ConstDeclNode) node).getName().equals(getConstantName())) {
                return getLocation(info, node);
            }
        }

        for (Node child : node.childNodes()) {
            if (child.isInvisible()) {
                continue;
            }
            DeclarationLocation location = findLocal(child);
            if (location != DeclarationLocation.NONE) {
                return location;
            }
        }

        return DeclarationLocation.NONE;
    }
}
