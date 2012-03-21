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

package org.netbeans.modules.ruby.debugger.breakpoints;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.debugger.ActionsManager;
import org.netbeans.api.debugger.DebuggerManager;
import org.netbeans.modules.ruby.debugger.EditorUtil;
import org.netbeans.modules.ruby.debugger.Util;
import org.netbeans.spi.debugger.ActionsProvider.Registration;
import org.netbeans.spi.debugger.ActionsProviderSupport;
import org.netbeans.spi.debugger.ui.EditorContextDispatcher;
import org.openide.text.Line;
import org.openide.util.WeakListeners;
import org.rubyforge.debugcommons.RubyDebuggerException;

/**
 * Provides actions for adding and removing Ruby breakpoints.
 */
@Registration(actions={"toggleBreakpoint"}, activateForMIMETypes={"text/x-ruby", "application/x-httpd-eruby"})
public final class RubyBreakpointActionProvider extends ActionsProviderSupport
        implements PropertyChangeListener {
    
    private static final Logger LOGGER = Logger.getLogger(RubyBreakpointActionProvider.class.getName());

    private final static Set<Object> ACTIONS =
            Collections.singleton(ActionsManager.ACTION_TOGGLE_BREAKPOINT);
    
    public RubyBreakpointActionProvider() {
        setEnabled(ActionsManager.ACTION_TOGGLE_BREAKPOINT, false);
        PropertyChangeListener l = WeakListeners.propertyChange(this, EditorContextDispatcher.getDefault());
        EditorContextDispatcher.getDefault().addPropertyChangeListener(Util.RUBY_MIME_TYPE, l);
        EditorContextDispatcher.getDefault().addPropertyChangeListener(Util.ERB_MIME_TYPE, l);
    }
    
    @Override
    public Set<Object> getActions() {
        return ACTIONS;
    }
    
    @Override
    public void doAction(Object action) {
        RubyBreakpoint breakpoint = RubyBreakpointManager.getCurrentLineBreakpoint();
        if (breakpoint != null) {
            DebuggerManager.getDebuggerManager().removeBreakpoint(breakpoint);
        } else { // new breakpoint
            try {
                Line line = EditorUtil.getCurrentLine();
                if (line != null) {
                    RubyBreakpointManager.addLineBreakpoint(line);
                }
            } catch (RubyDebuggerException e) {
                LOGGER.log(Level.WARNING, "Unable to add breakpoint: " + e.getLocalizedMessage(), e);
            }
        }
    }
    
    public void propertyChange(PropertyChangeEvent evt) {
        boolean enabled = EditorUtil.getCurrentLine() != null;
        setEnabled(ActionsManager.ACTION_TOGGLE_BREAKPOINT, enabled);
    }
    
}
