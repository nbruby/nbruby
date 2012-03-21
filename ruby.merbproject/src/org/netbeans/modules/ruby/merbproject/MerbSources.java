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

package org.netbeans.modules.ruby.merbproject;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.openide.util.Mutex;
import org.netbeans.api.project.Sources;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.modules.ruby.rubyproject.SharedRubyProjectProperties;
import org.netbeans.modules.ruby.spi.project.support.rake.SourcesHelper;
import org.netbeans.modules.ruby.spi.project.support.rake.RakeProjectHelper;
import org.netbeans.modules.ruby.spi.project.support.rake.PropertyEvaluator;


/**
 * Implementation of {@link Sources} interface for RubyProject.
 */
public class MerbSources implements Sources, PropertyChangeListener, ChangeListener  {
    
    private static final String BUILD_DIR_PROP = "${" + SharedRubyProjectProperties.BUILD_DIR + "}";    //NOI18N
    private static final String DIST_DIR_PROP = "${" + SharedRubyProjectProperties.DIST_DIR + "}";    //NOI18N

    private final Project project;
    private final RakeProjectHelper helper;
    private final PropertyEvaluator evaluator;
    private final MerbSourceRoots sourceRoots;
    private final MerbSourceRoots testRoots;
    private Sources delegate;
    private final List<ChangeListener> listeners = new ArrayList<ChangeListener>();

    public MerbSources(Project project, RakeProjectHelper helper, PropertyEvaluator evaluator,
                MerbSourceRoots sourceRoots, MerbSourceRoots testRoots) {
        this.project = project;
        this.helper = helper;
        this.evaluator = evaluator;
        this.sourceRoots = sourceRoots;
        this.testRoots = testRoots;
        this.sourceRoots.addPropertyChangeListener(this);
        this.testRoots.addPropertyChangeListener(this);
        this.evaluator.addPropertyChangeListener(this);
        initSources(); // have to register external build roots eagerly - XXX Why?
    }

    /**
     * Returns an array of SourceGroup of given type. It delegates to {@link SourcesHelper}.
     * This method firstly acquire the {@link ProjectManager#mutex} in read mode then it enters
     * into the synchronized block to ensure that just one instance of the {@link SourcesHelper}
     * is created. These instance is cleared also in the synchronized block by the
     * {@link RailsSources#fireChange} method.
     */
    public SourceGroup[] getSourceGroups(final String type) {
        return ProjectManager.mutex().readAccess(new Mutex.Action<SourceGroup[]>() {
            public SourceGroup[] run() {
                Sources _delegate;
                synchronized (MerbSources.this) {
                    if (delegate == null) {
                        delegate = initSources();
                        delegate.addChangeListener(MerbSources.this);
                    }
                    _delegate = delegate;
                }
                return _delegate.getSourceGroups(type);
            }
        });
    }

    void notifyDeleting() {
        delegate.removeChangeListener(this);
        sourceRoots.removePropertyChangeListener(this);
        testRoots.removePropertyChangeListener(this);
        evaluator.removePropertyChangeListener(this);
    }

    private Sources initSources() {
        SourcesHelper sourcesHelper = new SourcesHelper(project, helper, evaluator);   //Safe to pass APH
        String[] propNames = sourceRoots.getRootProperties();
        String[] rootNames = sourceRoots.getRootNames();
        for (int i = 0; i < propNames.length; i++) {
            String displayName = rootNames[i];
            displayName = sourceRoots.getRootDisplayName(displayName, propNames[i]);
            String prop = /*"${" +*/ propNames[i]/* + "}"*/; // NOI18N
            sourcesHelper.addPrincipalSourceRoot(prop, displayName, /*XXX*/null, null);
            sourcesHelper.addTypedSourceRoot(prop,  MerbProject.SOURCES_TYPE_RUBY, displayName, /*XXX*/null, null);
        }
        propNames = testRoots.getRootProperties();
        rootNames = testRoots.getRootNames();
        for (int i = 0; i < propNames.length; i++) {
            String displayName = rootNames[i];
            displayName = testRoots.getRootDisplayName(displayName, propNames[i]);
            String prop = "${" + propNames[i] + "}"; // NOI18N
            sourcesHelper.addPrincipalSourceRoot(prop, displayName, /*XXX*/null, null);
            sourcesHelper.addTypedSourceRoot(prop,  MerbProject.SOURCES_TYPE_RUBY, displayName, /*XXX*/null, null);
        }
        sourcesHelper.addNonSourceRoot (BUILD_DIR_PROP);
        sourcesHelper.addNonSourceRoot(DIST_DIR_PROP);
        sourcesHelper.registerExternalRoots(FileOwnerQuery.EXTERNAL_ALGORITHM_TRANSIENT);
        return sourcesHelper.createSources();
    }

    public void addChangeListener(ChangeListener changeListener) {
        synchronized (listeners) {
            listeners.add(changeListener);
        }
    }

    public void removeChangeListener(ChangeListener changeListener) {
        synchronized (listeners) {
            listeners.remove(changeListener);
        }
    }

    private void fireChange() {
        ChangeListener[] _listeners;
        synchronized (this) {
            if (delegate != null) {
                delegate.removeChangeListener(this);
                delegate = null;
            }
        }
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
            _listeners = listeners.toArray(new ChangeListener[listeners.size()]);
        }
        ChangeEvent ev = new ChangeEvent(this);
        for (ChangeListener l : _listeners) {
            l.stateChanged(ev);
        }
    }

    public void propertyChange(PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (MerbSourceRoots.PROP_ROOT_PROPERTIES.equals(propName) ||
            SharedRubyProjectProperties.BUILD_DIR.equals(propName)  ||
            SharedRubyProjectProperties.DIST_DIR.equals(propName)) {
            this.fireChange();
        }
    }

    public void stateChanged (ChangeEvent event) {
        this.fireChange();
    }

}
