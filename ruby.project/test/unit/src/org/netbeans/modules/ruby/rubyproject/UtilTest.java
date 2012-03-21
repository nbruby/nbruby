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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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

import junit.framework.TestCase;
import org.netbeans.modules.ruby.platform.Util;

/**
 * @author Tor Norbye
 */
public class UtilTest extends TestCase {
    
    public UtilTest(String testName) {
        super(testName);
    }

    public void testContainsAnsiColors() {
        assertTrue(Util.containsAnsiColors("\033[32m3 examples, 0 failures\033[0m"));
        assertTrue(Util.containsAnsiColors("\033[1;35m3 examples, 0 failures\033[0m"));
    }

    public void testContainsMultiAnsiColors() {
        assertTrue(Util.containsAnsiColors("\033[4;36;1mSQL (0.000210)\033[0m    \033[0;1mSET SQL_AUTO_IS_NULL=0\033[0m"));
        assertTrue(Util.containsAnsiColors("\033[4;36;1mRadiant::ExtensionMeta Columns (0.001849)\033[0m    \033[0;1mSHOW FIELDS FROM extension_meta\033[0m"));
    }

    public void testStripAnsiColors() {
        assertEquals("3 examples, 0 failures", Util.stripAnsiColors("\033[32m3 examples, 0 failures\033[0m"));
        assertEquals("3 examples, 0 failures", Util.stripAnsiColors("\033[1;35m3 examples, 0 failures\033[0m"));
    }

    public void testStripAnsiMultiColors() {
        assertEquals("SQL (0.000210)    SET SQL_AUTO_IS_NULL=0", Util.stripAnsiColors("\033[4;36;1mSQL (0.000210)\033[0m    \033[0;1mSET SQL_AUTO_IS_NULL=0\033[0m"));
        assertEquals("Radiant::ExtensionMeta Columns (0.001849)    SHOW FIELDS FROM extension_meta", Util.stripAnsiColors("\033[4;36;1mRadiant::ExtensionMeta Columns (0.001849)\033[0m    \033[0;1mSHOW FIELDS FROM extension_meta\033[0m"));
    }
}
