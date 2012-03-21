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
 * Portions Copyrighted 2008 Sun Microsystems, Inc.
 */
package org.netbeans.modules.ruby.platform.gems;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.netbeans.junit.NbTestCase;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

/**
 *
 * @author Erno Mononen
 */
public class GemFilesParserTest extends NbTestCase {

    public GemFilesParserTest(String testName) {
        super(testName);
    }

    public void testParseInfo() {

        String gem1 = "mongrel-1.1.3-i386-mswin32";
        assertEquals("mongrel", GemFilesParser.parseNameAndVersion(gem1)[0]);
        assertEquals("1.1.3", GemFilesParser.parseNameAndVersion(gem1)[1]);

        String gem2 = "activeresource-2.0.2";
        assertEquals("activeresource", GemFilesParser.parseNameAndVersion(gem2)[0]);
        assertEquals("2.0.2", GemFilesParser.parseNameAndVersion(gem2)[1]);

        String gem3 = "win32-sapi-0.1.4";
        assertEquals("win32-sapi", GemFilesParser.parseNameAndVersion(gem3)[0]);
        assertEquals("0.1.4", GemFilesParser.parseNameAndVersion(gem3)[1]);

        String gem4 = "cgi_multipart_eof_fix-2.5.0";
        assertEquals("cgi_multipart_eof_fix", GemFilesParser.parseNameAndVersion(gem4)[0]);
        assertEquals("2.5.0", GemFilesParser.parseNameAndVersion(gem4)[1]);

        String gem5 = "win32-api-1.0.5-x86-mswin32-60";
        assertEquals("win32-api", GemFilesParser.parseNameAndVersion(gem5)[0]);
        assertEquals("1.0.5", GemFilesParser.parseNameAndVersion(gem5)[1]);

        String gem6 = "rails-3.0.0.beta";
        assertEquals("rails", GemFilesParser.parseNameAndVersion(gem6)[0]);
        assertEquals("3.0.0.beta", GemFilesParser.parseNameAndVersion(gem6)[1]);

        String gem7 = "rails-3.0.0.beta4";
        assertEquals("rails", GemFilesParser.parseNameAndVersion(gem7)[0]);
        assertEquals("3.0.0.beta4", GemFilesParser.parseNameAndVersion(gem7)[1]);

    }

    public void testVersionSorting() throws IOException {

        Collection<File> gemFiles = new ArrayList<File>();
        gemFiles.add(createGemFile("rails", "1.2.5"));
        gemFiles.add(createGemFile("rails", "2.0.2"));
        gemFiles.add(createGemFile("rails", "3.0.0.beta"));
        gemFiles.add(createGemFile("rails", "1.2.6"));
        gemFiles.add(createGemFile("rails", "3.0.0.beta4"));
        gemFiles.add(createGemFile("rails", "1.2"));
        gemFiles.add(createGemFile("rails", "3.0.0"));
        gemFiles.add(createGemFile("rails", "2.0"));
        
        Map<String, List<GemInfo>> result = GemFilesParser.getGemInfos(gemFiles);
        
        List<GemInfo> versions = result.get("rails");
        assertEquals(8, versions.size());
        assertEquals("3.0.0", versions.get(0).getVersion());
        assertEquals("3.0.0.beta4", versions.get(1).getVersion());
        assertEquals("3.0.0.beta", versions.get(2).getVersion());
        assertEquals("2.0.2", versions.get(3).getVersion());
        assertEquals("2.0", versions.get(4).getVersion());
        assertEquals("1.2.6", versions.get(5).getVersion());
        assertEquals("1.2.5", versions.get(6).getVersion());
        assertEquals("1.2", versions.get(7).getVersion());
    }
    
    private File createGemFile(String name, String version) throws IOException {
        FileObject result = 
                FileUtil.createData(FileUtil.toFileObject(getWorkDir()), name + '-' + version + ".gemspec");
        return FileUtil.toFile(result);
        
    }
}
