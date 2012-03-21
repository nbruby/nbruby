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

package org.netbeans.modules.ruby.rubyproject;

import java.io.File;
import org.netbeans.modules.csl.api.DeclarationFinder.DeclarationLocation;
import org.netbeans.spi.gototest.TestLocator;
import org.netbeans.spi.gototest.TestLocator.LocationResult;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 * @author Tor Norbye
 */
public class GotoTestTest extends RubyProjectTestBase {
    
    private RubyProject project;
    private GotoTest gotoTest;
    
    public GotoTestTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        
        project = createTestProject("GotoTest");
        assertNotNull(project);
        FileObject dir = project.getProjectDirectory();
        assertNotNull(dir);
        createFilesFromDesc(dir, "testfiles/gototestfiles");
        
        // Create some data files where the class -contents- are used to locate the test
        createFile(dir, "lib/hello.rb", "class Hello\ndef foo\nend\nend\n");
        createFile(dir, "test/world.rb", "class HelloTest\ndef foobar\nend\nend\n");

        createFile(dir, "whatever/donkey.rb", "#foo");
        createFile(dir, "whatever/donkey_spec.rb", "#foo");
        
        touch(dir, "app/controllers/donkey_controller.rb");
        touch(dir, "spec/controllers/donkey_controller_spec.rb");
        touch(dir, "test/functional/donkey_controller_test.rb");
        
        gotoTest = new GotoTest();
    }
    
    private FileObject getProjFile(String file) {
        return project.getProjectDirectory().getFileObject(file);
    }
    
    private void assertIsProjFile(String expectedRelPath, FileObject actualFO) {
        String relative = getRelative(actualFO);
        
        // slashify to the same format, so tests pass on all OSes
        relative = relative.replace(File.separatorChar, '/');
        expectedRelPath = expectedRelPath.replace(File.separatorChar, '/');
        
        assertEquals(expectedRelPath + " is a project file", relative, expectedRelPath);
    }

    private String getRelative(FileObject fo) {
        assertNotNull(fo);
        File path = FileUtil.toFile(fo);
        File projPath = FileUtil.toFile(project.getProjectDirectory());
        String relative = path.getAbsolutePath().substring(projPath.getAbsolutePath().length()+1);
        
        return relative;
    }
    
    public void testGotoTestUnit() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("lib/foo.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("test/test_foo.rb", loc.getFileObject());
    }

    public void testGotoTestUnit2() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("lib/bar.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("test/tc_bar.rb", loc.getFileObject());
    }
    
    public void testGotoTestUnit3() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("lib/main.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("test/main_test.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    // The ZenTest patterns are only checked if the ZenTest gem is installed
    //public void testGotoTestZenTest() {
    //    assertNotNull(project);
    //
    //    DeclarationLocation loc = gotoTest.findTest(getProjFile("app/controllers/my_controller.rb"), -1);
    //    assertNotSame(DeclarationLocation.NONE, loc);
    //    assertIsProjFile("test/controllers/my_controller_test.rb", loc.getFileObject());
    //}

    public void testGotoTestRspec() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("app/models/whatever.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("spec/models/whatever_spec.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGotoTestRspecRubyProject() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("lib/gibbon.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("spec/gibbon_spec.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGotoTestRails() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("app/models/mymodel.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("test/unit/mymodel_test.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }
    
    public void testEnsureDirection1() {
        // Make sure that looking for a test when we're in a test doesn't return the opposite
        // file
        assertNotNull(project);
        DeclarationLocation loc = gotoTest.findTest(getProjFile("test/test_foo.rb"), -1);
        assertSame(DeclarationLocation.NONE, loc);
    }

    public void testEnsureDirection2() {
        assertNotNull(project);
        DeclarationLocation loc = gotoTest.findTested(getProjFile("lib/foo.rb"), -1);
        assertSame(DeclarationLocation.NONE, loc);
    }

    public void testGotoTestedUnit() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("test/test_foo.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("lib/foo.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGotoTestedUnit2() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("test/tc_bar.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("lib/bar.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    // The ZenTest patterns are only checked if the ZenTest gem is installed
    //public void testGotoTestedZenTest() {
    //    assertNotNull(project);
    //
    //    DeclarationLocation loc = gotoTest.findTested(getProjFile("test/controllers/my_controller_test.rb"), -1);
    //    assertNotSame(DeclarationLocation.NONE, loc);
    //    assertIsProjFile("app/controllers/my_controller.rb", loc.getFileObject());
    //}

    public void testGotoTestedRspec() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("spec/models/whatever_spec.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("app/models/whatever.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGotoTestedRails() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("test/unit/mymodel_test.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("app/models/mymodel.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    // The code index doesn't yet work at test time so the index search for HelloTest won't work
    //public void testGotoTestedClass() {
    //    assertNotNull(project);
    //
    //    DeclarationLocation loc = gotoTest.findTested(getProjFile("lib/hello.rb"), -1);
    //    assertNotSame(DeclarationLocation.NONE, loc);
    //    assertIsProjFile("test/world.rb", loc.getFileObject());
    //}

    private final static String[] FILES = {
        "lib/foo.rb",
        "test/test_foo.rb",
        
        "lib/bar.rb",
        "test/tc_bar.rb",
        
        //"app/controllers/my_controller.rb",
        //"test/controllers/my_controller_test.rb",
        
        "app/models/whatever.rb",
        "spec/models/whatever_spec.rb",
        
        "app/models/mymodel.rb",
        "test/unit/mymodel_test.rb"
    };
    
    public void testFindOpposite() {
        int index = 0;
        for (; index < FILES.length; index += 2) {
            FileObject source = getProjFile(FILES[index]);
            FileObject test = getProjFile(FILES[index+1]);
            
            DeclarationLocation loc = gotoTest.findTest(source, -1);
            assertEquals(test, loc.getFileObject());

            loc = gotoTest.findTested(test, -1);
            assertEquals(source, loc.getFileObject());
            
            TestLocator.LocationResult res = gotoTest.findOpposite(test, -1);
            assertEquals(source, res.getFileObject());
            assertEquals(-1, res.getOffset());
            res = gotoTest.findOpposite(source, -1);
            assertEquals(test, res.getFileObject());
            assertEquals(-1, res.getOffset());
        }
    }
    
    public void testGoto112812() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("app/views/user/create.mab"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("spec/views/user/create_spec.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGoto112812b() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("app/views/user/_partial.mab"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("spec/views/user/_partial_spec.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGoto112812c() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("spec/views/user/create_spec.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("app/views/user/create.mab", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGoto112812d() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("spec/views/user/_partial_spec.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("app/views/user/_partial.mab", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testNegative() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("app/controllers/lonely_controller.rb"), -1);
        assertSame(DeclarationLocation.NONE, loc);
        assertEquals(-1, loc.getOffset());
    }

    public void testNegative2() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("test/unit/lonesometest.rb"), -1);
        assertSame(DeclarationLocation.NONE, loc);
        assertEquals(-1, loc.getOffset());
    }

    public void testNegative3() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("app/controllers/lonely_controller.rb"), -1);
        assertSame(DeclarationLocation.NONE, loc);
    }

    public void testNegative4() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTested(getProjFile("test/unit/lonesometest.rb"), -1);
        assertSame(DeclarationLocation.NONE, loc);
    }
    
    public void testGoto119106a() {
        assertNotNull(project);
        
        DeclarationLocation loc = gotoTest.findTest(getProjFile("app/models/rest_phone/phone_call.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("test/unit/rest_phone/phone_call_test.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGoto119106b() {
        assertNotNull(project);
        LocationResult loc = gotoTest.findOpposite(getProjFile("test/unit/rest_phone/phone_call_test.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("app/models/rest_phone/phone_call.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testGoto119106c() {
        assertNotNull(project);
        DeclarationLocation loc = gotoTest.findTest(getProjFile("test/unit/rest_phone/phone_call_test.rb"), -1);
        assertSame(DeclarationLocation.NONE, loc);
    }
    
    public void testGoto16588a() {
        assertNotNull(project);
        DeclarationLocation loc = gotoTest.findTest(getProjFile("lib/widget.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("spec/lib/widget_spec.rb", loc.getFileObject());
    }

    public void testGoto16588b() {
        assertNotNull(project);
        DeclarationLocation loc = gotoTest.findTested(getProjFile("spec/lib/widget_spec.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("lib/widget.rb", loc.getFileObject());
    }

    public void testGoto16588c() {
        assertNotNull(project);
        DeclarationLocation loc = gotoTest.findTest(getProjFile("lib/gadget.rb"), -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("test/lib/test_gadget.rb", loc.getFileObject());
    }

    public void testRSpecSingle() {
        assertNotNull(project);
        
        FileObject f = getProjFile("whatever/donkey_spec.rb");
        assertNotNull(f);
        DeclarationLocation loc = gotoTest.findTested(f, -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("whatever/donkey.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testRSpecSingle2() {
        assertNotNull(project);

        FileObject f = getProjFile("whatever/donkey.rb");
        assertNotNull(f);
        DeclarationLocation loc = gotoTest.findTest(f, -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("whatever/donkey_spec.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }

    public void testRSpecVsTest() {
        assertNotNull(project);

        FileObject f = getProjFile("app/controllers/donkey_controller.rb");
        assertNotNull(f);
        DeclarationLocation loc = gotoTest.findTest(f, -1);
        assertNotSame(DeclarationLocation.NONE, loc);
        assertIsProjFile("test/functional/donkey_controller_test.rb", loc.getFileObject());
        assertEquals(-1, loc.getOffset());
    }
}
