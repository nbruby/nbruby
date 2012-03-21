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
package org.netbeans.modules.ruby.rubyproject.ui.customizer;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectUtils;
import org.netbeans.modules.ruby.rubyproject.SharedRubyProjectProperties;
import org.netbeans.modules.ruby.rubyproject.UpdateHelper;
import org.netbeans.modules.ruby.spi.project.support.rake.GeneratedFilesHelper;
import org.netbeans.modules.ruby.spi.project.support.rake.PropertyEvaluator;
import org.netbeans.modules.ruby.spi.project.support.rake.ReferenceHelper;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;
import org.openide.DialogDescriptor;
import org.openide.text.NbDocument;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;

/**
 * Base class for customization of Ruby projects.
 *
 * @author Petr Hrebejk, Erno Mononen
 */
public abstract class BaseRubyCustomizerProvider implements CustomizerProvider {

    private final Project project;
    private final UpdateHelper updateHelper;
    private final PropertyEvaluator evaluator;
    private final ReferenceHelper refHelper;
    private final GeneratedFilesHelper genFileHelper;
    private static Map<Project, Dialog> project2Dialog = new HashMap<Project, Dialog>();

    public BaseRubyCustomizerProvider(Project project, UpdateHelper updateHelper, PropertyEvaluator evaluator, ReferenceHelper refHelper, GeneratedFilesHelper genFileHelper) {
        this.project = project;
        this.updateHelper = updateHelper;
        this.evaluator = evaluator;
        this.refHelper = refHelper;
        this.genFileHelper = genFileHelper;
    }

    public void showCustomizer() {
        showCustomizer(null);
    }

    public void showCustomizer(String preselectedCategory) {
        showCustomizer(preselectedCategory, null);
    }

    protected abstract SharedRubyProjectProperties createUiProperties(Project project,
            UpdateHelper updateHelper, PropertyEvaluator evaluator, ReferenceHelper refHelper, GeneratedFilesHelper genFileHelper);

    protected abstract String getCustomizerFolderPath();

    public void showCustomizer(String preselectedCategory, String preselectedSubCategory) {

        Dialog dialog = project2Dialog.get(project);
        if (dialog != null) {
            dialog.setVisible(true);
            return;
        } else {
            SharedRubyProjectProperties uiProperties = createUiProperties(project, updateHelper, evaluator, refHelper, genFileHelper);
            Lookup context = Lookups.fixed(new Object[]{
                        project,
                        uiProperties,
                        new SubCategoryProvider(preselectedCategory, preselectedSubCategory)
                    });

            OptionListener listener = new OptionListener(project, uiProperties);
            dialog = ProjectCustomizer.createCustomizerDialog(getCustomizerFolderPath(), context, preselectedCategory, listener, null);
            dialog.addWindowListener(listener);
            dialog.setTitle(MessageFormat.format(
                    NbBundle.getMessage(BaseRubyCustomizerProvider.class, "LBL_Customizer_Title"), // NOI18N
                    new Object[]{ProjectUtils.getInformation(project).getDisplayName()}));

            project2Dialog.put(project, dialog);
            dialog.setVisible(true);
        }
    }

    /** Listens to the actions on the Customizer's option buttons */
    private class OptionListener extends WindowAdapter implements ActionListener {

        private Project project;
        private SharedRubyProjectProperties uiProperties;

        OptionListener(Project project, SharedRubyProjectProperties uiProperties) {
            this.project = project;
            this.uiProperties = uiProperties;
        }

        // Listening to OK button ----------------------------------------------------------
        public void actionPerformed(ActionEvent e) {
            // Store the properties into project 
            uiProperties.save();

            // Close & dispose the the dialog
            Dialog dialog = project2Dialog.get(project);
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        }

        // Listening to window events ------------------------------------------------------
        public 
        @Override
        void windowClosed(WindowEvent e) {
            project2Dialog.remove(project);
        }

        public 
        @Override
        void windowClosing(WindowEvent e) {
            //Dispose the dialog otherwsie the {@link WindowAdapter#windowClosed}
            //may not be called
            Dialog dialog = project2Dialog.get(project);
            if (dialog != null) {
                dialog.setVisible(false);
                dialog.dispose();
            }
        }
    }

    static final class SubCategoryProvider {

        private String subcategory;
        private String category;

        SubCategoryProvider(String category, String subcategory) {
            this.category = category;
            this.subcategory = subcategory;
        }

        public String getCategory() {
            return category;
        }

        public String getSubcategory() {
            return subcategory;
        }
    }
}
