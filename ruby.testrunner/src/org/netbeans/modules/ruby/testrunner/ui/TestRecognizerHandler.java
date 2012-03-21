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
package org.netbeans.modules.ruby.testrunner.ui;

import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.netbeans.modules.gsf.testrunner.api.Manager;
import org.netbeans.modules.gsf.testrunner.api.TestSession;
import org.netbeans.modules.gsf.testrunner.api.Trouble;

/**
 * Base class for test recognizer handlers. 
 * 
 * @author Erno Mononen
 */
abstract class TestRecognizerHandler {

    private static final Logger LOGGER = Logger.getLogger(TestRecognizerHandler.class.getName());

    /**
     * Patterns for recognizing comparison failures.
     */
    private static final Pattern[] STRING_COMPARISON = new Pattern[]{
        // test/unit
        Pattern.compile("<\"(.*)\"> expected but was <\"(.*)\">.*"),//NOI18N
        // rspec
        Pattern.compile(".*expected \"(.*)\", got \"(.*)\".*")//NOI18N
    };
    
    protected final Pattern pattern;
    protected Matcher matcher;

    TestRecognizerHandler(String regex) {
        // handle newline chars at the end -- see #143508
        if (!regex.endsWith(".*")) { //NOI18N
            regex += ".*";  //NOI18N
        }
        // see #151725
        if (!regex.startsWith(".*")) { //NOI18N
            regex = ".*" + regex; //NOI18N
        }
        this.pattern = Pattern.compile(regex, Pattern.DOTALL);
    }

    final boolean matches(String line) {
        return match(line).matches();
    }
    
    /**
     * <i>Package private for unit tests, otherwise don't use directly</i>.
     */
    final Matcher match(String line) {
        this.matcher = pattern.matcher(line);
        return matcher;
    }

    abstract void updateUI(Manager manager, TestSession session);

    /**
     * Gets the RecognizedOutput for output that should be passed on 
     * for printing to Output. Override in subclasses as needed, the default
     * implementation supresses all output (i.e. nothing is passed on 
     * for printing).
     * 
     * @return the RecognizedOutput for output that should be passed on 
     * for printing to Output. 
     */
    List<String> getRecognizedOutput() {
        return Collections.<String>emptyList();
    }

    protected static int toMillis(String timeInSeconds) {
        try {
            Double elapsedTimeMillis = Double.parseDouble(timeInSeconds) * 1000;
            return elapsedTimeMillis.intValue();
        } catch (NumberFormatException nfe) {
            LOGGER.log(Level.WARNING, "Could not parse time, returning 0", nfe);
        }
        return 0;
    }

    protected static Trouble.ComparisonFailure getComparisonFailure(String msg) {
        if (msg == null) {
            return null;
        }
        for (Pattern pattern : STRING_COMPARISON) {
            Matcher comparisonMatcher = pattern.matcher(msg);
            if (!comparisonMatcher.matches()) {
                continue;
            }
            return new Trouble.ComparisonFailure(toMultipleLines(comparisonMatcher.group(1)),
                    toMultipleLines(comparisonMatcher.group(2)));
        }
        return null;
    }

    private static String toMultipleLines(String toConvert) {
        return toConvert.replace("\\n", "\n"); //NOI18N
    }

    protected static boolean isEmpty(String str) {
        return str == null || "".equals(str.trim());
    }
}
