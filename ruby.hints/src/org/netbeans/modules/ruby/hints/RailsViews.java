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

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import javax.swing.JComponent;
import org.jrubyparser.ast.Node;
import org.jrubyparser.ast.NodeType;
import org.jrubyparser.ast.INameNode;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.csl.api.Hint;
import org.netbeans.modules.csl.api.HintFix;
import org.netbeans.modules.csl.api.HintSeverity;
import org.netbeans.modules.csl.api.Modifier;
import org.netbeans.modules.csl.api.OffsetRange;
import org.netbeans.modules.csl.api.RuleContext;
import org.netbeans.modules.csl.spi.ParserResult;
import org.netbeans.modules.ruby.Arity;
import org.netbeans.modules.ruby.AstUtilities;
import org.netbeans.modules.ruby.RubyParseResult;
import org.netbeans.modules.ruby.RubyUtils;
import org.netbeans.modules.ruby.elements.AstElement;
import org.netbeans.modules.ruby.hints.infrastructure.RubyAstRule;
import org.netbeans.modules.ruby.hints.infrastructure.RubyRuleContext;
import org.openide.filesystems.FileObject;
import org.openide.util.Exceptions;
import org.openide.util.NbBundle;
import org.openide.util.actions.SystemAction;

/**
 * Check existence of view files for actions (and offer to fix it)
 * 
 * @author Tor Norbye
 */
public class RailsViews extends RubyAstRule {
    public RailsViews() {
    }

    public boolean appliesTo(RuleContext context) {
        ParserResult info = context.parserResult;
        return RubyUtils.getFileObject(info).getName().endsWith("_controller"); // NOI18N
    }

    public Set<NodeType> getKinds() {
        return Collections.singleton(NodeType.DEFNNODE);
    }
    
    public void run(RubyRuleContext context, List<Hint> result) {
        Node node = context.node;
        ParserResult info = context.parserResult;
        
        // See if this ia an action method and see if it has a view
        FileObject file = RubyUtils.getFileObject(info);
        assert file.getName().endsWith("_controller"); // NOI18N

        // Methods with arguments aren't actions
        Arity arity = Arity.getDefArity(node);
        if (arity.getMinArgs() != 0 || arity.getMaxArgs() != 0) {
            return;
        }
        
        String name = ((INameNode)node).getName();

        FileObject view = RubyUtils.getRailsViewFor(file, name, "_controller", "controllers", true);

        if (view == null && shouldHaveView(info, node) && isPublic(context.parserResult, node)) {
            String displayName = NbBundle.getMessage(RailsViews.class, "MissingView");
            OffsetRange range = AstUtilities.getNameRange(node);
            List<HintFix> fixList = Collections.<HintFix>singletonList(new CreateViewFix(file, name));
            Hint desc = new Hint(this, displayName, file, range, fixList, 400);
            result.add(desc);
        }
    }

    private boolean isPublic(ParserResult result, Node node) {
        RubyParseResult rpr = (RubyParseResult)result;
        AstElement element = rpr.getStructure().getElementFor(node);
        if (element != null) {
            Set<Modifier> modifiers = element.getModifiers();
            if (modifiers != null) {
                return !(modifiers.contains(Modifier.PRIVATE) || modifiers.contains(Modifier.PROTECTED));
            }
        }

        return true;
    }
    
    /**
     * Determine whether an action method should have an associated view file.
     * For example, methods that contain a redirect method probably don't need one.
     */
    private boolean shouldHaveView(ParserResult info, Node node) {
        if (node.getNodeType() == NodeType.FCALLNODE) {
            String method = ((INameNode)node).getName();
            
            if (method.startsWith("redirect_") || method.equals("render")) { // NOI18N
                return false;
            }
        }
        
        List<Node> list = node.childNodes();

        for (Node child : list) {
            if (child.isInvisible()) {
                continue;
            }
            boolean result = shouldHaveView(info, child);
            
            if (!result) {
                return result;
            }
        }
        
        return true;
    }
    
    public String getId() {
        return "Rails_Views"; // NOI18N
    }

    public String getDisplayName() {
        return NbBundle.getMessage(RailsViews.class, "FindActionViews");
    }

    public String getDescription() {
        return NbBundle.getMessage(RailsViews.class, "FindActionViewsDesc");
    }

    public boolean getDefaultEnabled() {
        return false;
    }

    public HintSeverity getDefaultSeverity() {
        return HintSeverity.WARNING;
    }

    public boolean showInTasklist() {
        return false;
    }

    public JComponent getCustomizer(Preferences node) {
        return null;
    }
    
    private static class CreateViewFix implements HintFix {

        private FileObject controller;
        private String action;

        CreateViewFix(FileObject controller, String action) {
            this.controller = controller;
            this.action = action;
        }

        public String getDescription() {
            return NbBundle.getMessage(NestedLocal.class, "CreateView");
        }

        public void implement() throws Exception {
            String controllerName = RubyUtils.getControllerName(controller);
            Project project = FileOwnerQuery.getOwner(controller);
            if (project == null) {
                return;
            }
            try {
                Class c = Class.forName("org.netbeans.modules.ruby.railsprojects.GenerateAction", true, // NOI18N
                        Thread.currentThread().getContextClassLoader());
                if (c != null) {
                    //Object generateAction = c.newInstance();
                    @SuppressWarnings("unchecked")
                    SystemAction generateAction = SystemAction.get(c);
                    @SuppressWarnings("unchecked")
                    Method m = c.getMethod("generate", // NOI18N
                            new Class[] { Project.class, String.class, String.class, String.class });
                    m.invoke(generateAction, new Object[] { project, "controller", controllerName, action }); // NOI18N
                }
            } catch (Exception ex) {
                Exceptions.printStackTrace(ex);
            }
        }

        public boolean isSafe() {
            return false;
        }

        public boolean isInteractive() {
            return true;
        }
    }
}
