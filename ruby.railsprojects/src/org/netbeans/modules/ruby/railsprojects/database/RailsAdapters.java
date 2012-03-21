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
package org.netbeans.modules.ruby.railsprojects.database;

import java.io.IOException;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.openide.LifecycleManager;
import org.openide.cookies.EditorCookie;
import org.openide.cookies.SaveCookie;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.util.Exceptions;
import org.openide.util.Parameters;

/**
 * Utility methods for modifying database.yml.
 *
 * @author Erno Mononen
 */
final class RailsAdapters {

    /**
     * The default suffix for the development database name.
     */
    static final String DEVELOPMENT_DB_SUFFIX = "_development"; //NOI18N
    /**
     * The default suffix for the production database name.
     */
    private static final String PRODUCTION_DB_SUFFIX = "_production"; //NOI18N
    /**
     * The default suffix for the test database name.
     */
    private static final String TEST_DB_SUFFIX = "_test"; //NOI18N

    private RailsAdapters() {
    }

    /**
     * Tries to comment out the socket syntax from database.yml. Saves the file
     * after modifications.
     *
     * @param dir the project dir under which database.yml should be.
     * @param host the host to be used.
     */
    static void commentOutSocket(FileObject dir, String host) {
        Parameters.notNull("dir", dir);
        Parameters.notNull("host", host);

        FileObject fo = dir.getFileObject("config/database.yml"); // NOI18N
        if (fo != null) {
            try {
                DataObject dobj = DataObject.find(fo);
                EditorCookie ec = dobj.getCookie(EditorCookie.class);
                if (ec != null) {
                    javax.swing.text.Document doc = ec.openDocument();
                    commentOutSocket(doc, host, 0);
                    SaveCookie sc = dobj.getCookie(SaveCookie.class);
                    if (sc != null) {
                        sc.save();
                    } else {
                        LifecycleManager.getDefault().saveAll();
                    }
                }
            } catch (BadLocationException ble) {
                Exceptions.printStackTrace(ble);
            } catch (DataObjectNotFoundException dnfe) {
                Exceptions.printStackTrace(dnfe);
            } catch (IOException ioe) {
                Exceptions.printStackTrace(ioe);
            }
        }
    }

    private static void commentOutSocket(Document doc, String host, int fromIndex) throws BadLocationException {
        String text = doc.getText(0, doc.getLength());
        int offset = text.indexOf("socket:", fromIndex); // NOI18N
        if (offset == -1) {
            return;
        }
        // Determine indent
        int indent = 0;
        for (int i = offset - 1; i >= 0; i--) {
            if (text.charAt(i) == '\n') {
                break;
            } else {
                indent++;
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("# JRuby doesn't support socket:\n"); //NOI18N
        boolean addLocalHost = text.indexOf("host:", fromIndex) == -1; //NOI18N
        if (addLocalHost) {
            for (int i = 0; i < indent; i++) {
                sb.append(" ");
            }
            sb.append("host: " + host + "\n"); //NOI18N
        }
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append("#");
        doc.insertString(offset, sb.toString(), null);
        commentOutSocket(doc, host, offset + sb.length() + "socket:".length()); //NOI18N
    }

    static void addProperty(Document databaseYml, String propertyName,
            String propertyValue, String addAfter) throws BadLocationException {

        String text = databaseYml.getText(0, databaseYml.getLength());
        int offset = text.indexOf(addAfter); // NOI18N
        if (offset == -1) {
            // don't know where to add
            return;
        }
        int indent = determineIndent(text, offset);
        int indexForAdding = offset + addAfter.length();
        while (text.charAt(indexForAdding) != '\n' && indexForAdding <= text.length()) {
            indexForAdding++;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            sb.append(" ");
        }
        sb.append(propertyName + " " + propertyValue + '\n');
        databaseYml.insertString(indexForAdding + 1, sb.toString(), null);
    }

    static void removeProperty(Document databaseYml, String propertyName) throws BadLocationException {

        String text = databaseYml.getText(0, databaseYml.getLength());
        int offset = text.indexOf(propertyName); // NOI18N
        if (offset == -1) {
            // nothing to remove
            return;
        }

        int indent = determineIndent(text, offset);
        int valueLength = propertyName.length();
        for (int i = offset + propertyName.length(); i <= text.length(); i++) {
            valueLength++;
            if (text.charAt(i) == '\n') {
                break;
            }
        }
        databaseYml.remove(offset - indent, valueLength + indent);
    }

    static String getPropertyValue(Document databaseYml, String propertyName) throws BadLocationException {
        String text = databaseYml.getText(0, databaseYml.getLength());
        int propertyNameIndex = text.indexOf(propertyName);
        if (propertyNameIndex == -1) {
            return null;
        }

        int propertyNameEndIndex = propertyNameIndex + propertyName.length();
        int propertyValueLength = 0;
        for (int i = propertyNameEndIndex; i < text.length(); i++) {
            if ((text.charAt(i)) == '\n') {
                break;
            }
            propertyValueLength++;
        }
        return databaseYml.getText(propertyNameEndIndex, propertyValueLength).trim();

    }

    /**
     * Replaces the value of the given attribute with the given value. If the specified
     * attribute was not found and <code>addAfter</code> is specified, adds the attribute.
     * @param doc the doc representing a yaml file
     * @param attributeName the name of the attribute to replace
     * @param attributeValue the value for the attribute
     * @param addAfter the name of the attribute after which the specified <code>attributeName</code>
     * should be created. May be <code>null</code> in which case the attribute is not created
     * if it does not exist already. A non-null <code>addAfter</code> has no effect if
     * the attribute already exists.
     * @throws javax.swing.text.BadLocationException
     */
    static void changeAttribute(Document doc, String attributeName, String attributeValue, String addAfter) throws BadLocationException {
        Parameters.notWhitespace("attributeName", attributeName); //NOI18N
        if (attributeValue == null || "".equals(attributeValue.trim())) {
            // assume the default generated values are preferred
            return;
        }
        String text = doc.getText(0, doc.getLength());
        int attributeNameIndex = text.indexOf(attributeName);
        if (attributeNameIndex == -1) {
            if (addAfter != null) {
                addProperty(doc, attributeName, attributeValue, addAfter);
            } else {
                // can't do much
                return;
            }
        }
        int attributeNameEndIndex = attributeNameIndex + attributeName.length();
        int attributeValuelength = 0;
        for (int i = attributeNameEndIndex; i < text.length(); i++) {
            if ((text.charAt(i)) == '\n') { //NOI18N
                break;
            } else {
                attributeValuelength++;
            }
        }
        doc.remove(attributeNameEndIndex, attributeValuelength);
        doc.insertString(attributeNameEndIndex, attributeValue != null ? " " + attributeValue : "", null);

    }

    static Document getDatabaseYml(FileObject projectDir) {
        FileObject fo = projectDir.getFileObject("config/database.yml"); // NOI18N
        if (fo == null) {
            return null;
        }
        try {
            DataObject dobj = DataObject.find(fo);
            EditorCookie ec = dobj.getCookie(EditorCookie.class);
            if (ec != null) {
                try {
                    return ec.openDocument();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        } catch (DataObjectNotFoundException donfe) {
            Exceptions.printStackTrace(donfe);
        }
        return null;

    }

    private static int determineIndent(String text, int offset) {
        // Determine indent
        int indent = 0;
        for (int i = offset - 1; i >= 0; i--) {
            if (text.charAt(i) == '\n') {
                break;
            } else {
                indent++;
            }
        }
        return indent;
    }


    static String getTestDatabaseName(String develDb) {
        return buildDatabaseName(develDb, TEST_DB_SUFFIX);
    }

    static String getProductionDatabaseName(String develDb) {
        return buildDatabaseName(develDb, PRODUCTION_DB_SUFFIX);
    }

    /**
     * Builds a new database name based on the existing name and the given suffix.
     *
     * @param doc
     * @param suffix
     * @return
     * @throws javax.swing.text.BadLocationException
     */
    private static String buildDatabaseName(String develDb, String suffix) {
        if (develDb == null) {
            return "";
        }
        int i = develDb.indexOf(DEVELOPMENT_DB_SUFFIX); //NOI18N
        if (i == -1) {
            return develDb + suffix;
        } else if (i == 0) {
            return suffix;
        }
        return develDb.substring(0, i) + suffix;
    }

}
