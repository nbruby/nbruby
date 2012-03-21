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

package org.netbeans.modules.ruby;

import java.util.HashMap;
import java.util.Map;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.ruby.platform.RubyPlatform;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Erno Mononen
 */
public class ActiveRecordAssociationFinderTest extends RubyTestBase {

    public ActiveRecordAssociationFinderTest(String testName) {
        super(testName);
    }

    @Override
    protected Map<String, ClassPath> createClassPathsForTest() {
        Map<String, ClassPath> loadPath = new HashMap<String, ClassPath>();
        // rubystubs
        loadPath.put(RubyLanguage.BOOT, ClassPathSupport.createClassPath(RubyPlatform.getRubyStubs()));
        // golden files
        FileObject testFileFO = FileUtil.toFileObject(getDataFile("/testfiles/ar-associations"));
        loadPath.put(RubyLanguage.SOURCE, ClassPathSupport.createClassPath(testFileFO));
        return loadPath;
    }

    public void testHasMany() throws Exception {
        checkDeclaration("testfiles/ar-associations/user.rb", ":pro^jects", "project.rb", 0);
    }

    public void testBelongsTo() throws Exception {
        checkDeclaration("testfiles/ar-associations/project.rb", ":us^er", "user.rb", 0);
    }

    public void testHasManyPolymorphs() throws Exception {
        checkDeclaration("testfiles/ar-associations/user.rb", "[:pro^jects", "project.rb", 0);
    }

    public void testHasManyPolymorphs2() throws Exception {
        checkDeclaration("testfiles/ar-associations/user.rb", ":us^er_details]", "user_detail.rb", 0);
    }

    public void testHasManyWithClassName() throws Exception {
        checkDeclaration("testfiles/ar-associations/user.rb", ":det^ails", "user_detail.rb", 0);
    }

    public void testClassName() throws Exception {
        checkDeclaration("testfiles/ar-associations/user.rb", ":class_name => \"UserD^etail\"", "user_detail.rb", 0);
    }

    public void testToDeclaration() throws Exception {
        // tests @user.proje^cts
        checkDeclaration("testfiles/ar-associations/client.rb", "@user.proje^cts", "user.rb", 43);
    }

    public void testToDeclaration2() throws Exception {
        checkDeclaration("testfiles/ar-associations/client.rb", "@project.use^r", "project.rb", 48);
    }

}
