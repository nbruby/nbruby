/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
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
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * Portions Copyrighted 2007 Sun Microsystems, Inc.
 */

package indexhelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Program which builds the index helper used during indexing to augment
 * information we cannot glean directly from the code or comments, such as
 * the valid hashkeys that certain APIs accept. While it's possible to use
 * some heuristics to figure these out by just looking for the hashkey or splat
 * argument name and then looking for bulleted lists with hashkeys under a
 * sentence mentioning the parameter name, it doesn't work in general; many
 * APIs just say things like "the same options apply here as for the url_for
 * method", and so on.
 * 
 * Maintaining this index is obviously painful. I'm -generating- the code
 * here in such a way that I can generate debug-indexers which tell me
 * whether there are errors in the data (e.g. certain class methods weren't
 * encountered during indexing) as well as an optimized indexer used in
 * production code.
 *
 * @todo This was written in Java because I was planning on hooking it up
 *   to other functionality (JRuby AST checks etc) but I see I haven't needed
 *   any of that so it should probably have been written in Ruby instead...
 * 
 * @author Tor Norbye
 */
public class Generator {
    /** When true, the emitted code will be diagnostic, tracking which files were
     * actually hit during indexing etc. such that I can ensure that there are
     * no typos etc.
     */
    private static final boolean GENERATE_DEBUG_VERSION = false;
    
    private static final String BEGINMARKER = "// BEGIN AUTOMATICALLY GENERATED CODE. SEE THE http://hg.netbeans.org/main/misc/ruby/indexhelper PROJECT FOR DETAILS.";
    private static final String ENDMARKER = "// END AUTOMATICALLY GENERATED CODE";
    
    private List<MethodDef> methods;
    
    public Generator() {
    }
    
    public void run(File file) {
        initializeData();
        checkData();
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        try {
            writer.write(BEGINMARKER);
            writer.write("\n");
            
            // Constants we'll need
            writer.write("public static final String HASH_KEY_BOOL = \"bool\";\n");
            writer.write("public static final String HASH_KEY_STRING = \"string\";\n");
            writer.write("public static final String HASH_KEY_INTEGER = \"string\";\n\n");
    
            if (GENERATE_DEBUG_VERSION) {
                generateDebugList(writer);
            }
            //generateNameMap(writer);
            generateAttributeIndexer(writer);
            writer.write(ENDMARKER);
            writer.flush();

            String newSection = sw.toString();
            // Indent
            //newSection = newSection.replace("\n", "\n    ");
            String[] lines = newSection.split("\n");
            StringBuilder sb = new StringBuilder();
            for (String line : lines) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append("    "); // indent
                sb.append(line);
                if (line.indexOf('\"') != -1) {
                    sb.append(" // NOI18N");
                }
            }
            newSection = sb.toString();
            
            sb = new StringBuilder();
            BufferedReader br = new BufferedReader(new FileReader(file));
            while (true) {
                String s = br.readLine();
                if (s == null) {
                    break;
                }
                sb.append(s);
                sb.append("\n");
            }
            br.close();

            // Attempt to replace the contents
            String oldContents = sb.toString();
            int start = oldContents.indexOf(BEGINMARKER);
            assert start != -1;
            int end = oldContents.indexOf(ENDMARKER);
            assert end != -1;
            String newContents = oldContents.substring(0, start) +
                    newSection + oldContents.substring(end+ENDMARKER.length());
            
            // Update the file
            BufferedWriter bw = new BufferedWriter(new FileWriter(file));
            bw.write(newContents);
            bw.flush();
            bw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        
        // Attempt to replace file contents...
    }

    private void generateDebugList(Writer writer) throws IOException {
        assert GENERATE_DEBUG_VERSION;
        writer.write("\nprivate static List<String> unused = new ArrayList<String>();\n");
        writer.write("static {\n");
        for (MethodDef m : methods) {
            String s = m.classname + "#" + m.methodPrefix;
            writer.write("    unused.add(\"" + s + "\");\n");
        }
        writer.write("    unused.add(\"Dummy item, should be only result in unused after indexing\");\n");

        writer.write("    Runtime.getRuntime().addShutdownHook(new Thread() {\n");
        writer.write("        public void run() {\n");
        writer.write("            if (unused.size() > 0) {\n                for (int i = 0; i < 5; i++) java.awt.Toolkit.getDefaultToolkit().beep();\n            }\n");
        writer.write("            System.err.println(\"***Unused index entries (\" + unused.size() + \")=\" + unused);\n");
        writer.write("        }\n");
        writer.write("    });\n");
        writer.write("}\n");
        writer.write("public static void showUnused() {\n");
        writer.write("    System.err.println(\"***Unused index entries (\" + unused.size() + \")=\" + unused);\n");
        writer.write("}\n");
        
    }
        
    private void generateNameMap(Writer writer) throws IOException {
        Set<String> fileSet = new HashSet<String>();
        for (MethodDef m : methods) {
            final String filename = m.filename;
            assert filename.length() > 2 : filename; // I will be keying by first two chars
            fileSet.add(filename);
        }

        List<String> filenames = new ArrayList<String>(fileSet);
        Collections.sort(filenames);
        // Ensure all filenames have at least two chars

        writer.write("private static Set<String> knownFiles = new HashSet<String>(" + 2*filenames.size() + ");\n");
        writer.write("static {\n");
        for (String s : filenames) {
            writer.write("    knownFiles.add(\"");
            writer.write(s);
            writer.write("\");\n");
        }
        writer.write("}\n");
        writer.write("private static String isSpecial(FileObject file) {\n");
        writer.write("    return knownFiles.contains(file.getName());\n");
        writer.write("}\n\n");
    }
    
    private void generateAttributeIndexer(Writer writer) throws IOException {
        
        writer.write("private static String clz(Node root, Node n) {\n");
        writer.write("    AstPath path = new AstPath(root, n);\n");
        writer.write("    String clz = AstUtilities.getFqnName(path);\n");
        writer.write("    return clz;\n");
        writer.write("}\n\n");
        writer.write("private static String sig(MethodDefNode method) {\n");
        writer.write("    return AstUtilities.getDefSignature(method);\n");
        writer.write("}\n\n");
        writer.write("private static String getAttribute(" + 
                (GENERATE_DEBUG_VERSION ? "RubyParseResult result, " : "") +
                "FileObject file, Node root, MethodDefNode method) {\n");

        // Get files
        Set<String> fileSet = new HashSet<String>();
        //Map<Integer,List<MethodDef>> methodMap = new HashMap<Integer, List<Generator.MethodDef>>();
        Map<String,List<MethodDef>> methodMap = new HashMap<String, List<Generator.MethodDef>>();
        for (MethodDef m : methods) {
            final String filename = m.filename;
            assert filename.length() > 2 : filename; // I will be keying by first two chars
            fileSet.add(filename);

            List<MethodDef> l = methodMap.get(filename);
            if (l == null) {
                l = new ArrayList<MethodDef>();
                methodMap.put(filename, l);
            }
            l.add(m);
//
//                // Generate key
//                int firstChar = filename.charAt(0);
//                int secondChar = filename.charAt(1);
        }
        List<String> filenames = new ArrayList<String>(fileSet);
        Collections.sort(filenames);
        // Ensure all filenames have at least two chars

        // Emit switch block
        writer.write("    String n = file.getName();\n    if (n.length() < 2) {\n        return null;\n    }\n");
        writer.write("    char c = n.charAt(0);\n");
        writer.write("    switch (c) {\n");

        char prevChar = 0;
        String prevFileName = "";

        boolean firstCase = true;
        for (String s : filenames) {
            List<MethodDef> l = methodMap.get(s);
            assert l != null;
            char firstChar = s.charAt(0);
            if (firstChar != prevChar) {
                if (!firstCase) {
                    writer.write("        break;\n");
                }
                firstCase = false;
                writer.write("    case '");
                writer.write(firstChar);
                writer.write("':\n");
                prevFileName = "";
            }


            if (!prevFileName.equals(s)) {
                writer.write("        if (\"" + s + "\".equals(n)) {\n");
            }

            String prevClassName = "";
            boolean first = true;
            for (MethodDef m : l) {
                if (!prevClassName.equals(m.classname)) {
                    if (!first) {
                        writer.write("                 return null;\n");
                        writer.write("            }\n");
                    } else {
                        writer.write("            String clz = clz(root,method);\n");
                    }
                    first = false;
                    writer.write("            if (\"" + m.classname + "\".equals(clz)) {\n");
                    
                    if (GENERATE_DEBUG_VERSION) {
                        writer.write("                Map<String,String> classMap = new HashMap<String,String>();\n");
                        // Iterate over the signatures
                        String clz = m.classname;
                        for (MethodDef ml : l) {
                            String name = ml.methodPrefix;
                            int paren = name.indexOf('(');
                            if (paren != -1) {
                                name = name.substring(0, paren);
                            }
                            String fqn = clz + "." + name;
                            String args = ml.arguments.replace("\"", "\\\"");
                            writer.write("                classMap.put(\"" + fqn + "\", \"" + args + "\");\n");
                        }
                        writer.write("                verify(result, classMap);\n\n");
                        
                    }
                    
                    
                    writer.write("                 String sig = sig(method);\n");
                }

                writer.write("                 if (sig.startsWith(\"" + m.methodPrefix + "\")) {\n");
                if (GENERATE_DEBUG_VERSION) {
                    String item = m.classname + "#" + m.methodPrefix;
                    writer.write("                     unused.remove(\"" + item + "\");\n");
                }
                String args = m.arguments;
                
                if (m.previousVersion != null) {
                    // This item is version dependent
                    writer.write("                         String path = file.getPath();\n");
                    writer.write("                         if (path.indexOf(\"-2\") != -1 || path.indexOf(\"-1\") == -1) {\n");
                    if (args.indexOf('"') != -1) {
                        args = args.replace("\"", "\\\"");
                    }
                    writer.write("                             return \"" + args + "\"; // NOI18N\n");
                    writer.write("                         } else {\n");
                    
                    String argsOld = m.previousVersion.arguments;
                    if (argsOld.indexOf('"') != -1) {
                        argsOld = argsOld.replace("\"", "\\\"");
                    }
                    
                    writer.write("                             return \"" + argsOld + "\"; // NOI18N\n");
                    writer.write("                         }\n");
                } else {
                    if (args.indexOf('"') != -1) {
                        args = args.replace("\"", "\\\"");
                    }
                    writer.write("                     return \"" + args + "\";\n");
                }
                writer.write("                 }\n");
                prevClassName = m.classname;
            }
            if (prevClassName.length() > 0) {
                writer.write("                 return null;\n");
                writer.write("            }\n");
            }

            if (!prevFileName.equals(s)) {
                writer.write("            return null;\n");
                writer.write("        }\n");
            }

            prevFileName = s;
            prevChar = firstChar;
        }
        if (!firstCase) {
            writer.write("        break;\n");
        }

        writer.write("    }\n");
        writer.write("    return null;\n");
        writer.write("}\n");
        
        String helperCode = 
"    private static AstElement findElement(List<? extends AstElement> children, String signature) {\n" +
"        for (AstElement child : children) {\n" +
"            if (child.getKind() == ElementKind.METHOD) {\n" +
"                if (signature.endsWith(\".\"+child.getName())) {\n" +
"                    if (signature.endsWith(child.getIn()+\".\"+child.getName())) {\n" +
"                        return child;\n" +
"                    } else {\n" +
"                        System.err.println(\"WARNING - couldn't find element - but it sure looked similar - signature=\" + signature + \" and element=\" + child);\n" +
"                    }\n" +
"                }\n" +
"            }\n" +
"            AstElement result = findElement(child.getChildren(), signature);\n" +
"            if (result != null) {\n" +
"                return result;\n" +
"            }\n" +
"        }\n" +
" \n" +
"        return null;\n" +
"    }\n" +
"    \n" +
"    private static void verify(RubyParseResult result, Map<String,String> signatureToArgumentsMap) {\n" +
"        List<? extends AstElement> elements = result.getStructure().getElements();\n" +
"        for (String signature : signatureToArgumentsMap.keySet()) {\n" +
"            AstElement element = findElement(elements, signature);\n" +
"            if (element == null) {\n" +
"                System.err.println(\"WARNING: No element found for \" + signature);\n" +
"                continue;\n" +
"            }\n" +
"            // Check that the parameters are all there\n" +
"            List<String> parameters = ((AstMethodElement) element).getParameters();\n" +
"            Set<String> parametersSet = new HashSet<String>();\n" +
"            for (String s : parameters) {\n" +
"                if (s.startsWith(\"*\")) {\n" +
"                    s = s.substring(1);\n" +
"                } else if (s.startsWith(\"&\")) {\n" +
"                    s = s.substring(1);\n" +
"                }\n" +
"                parametersSet.add(s);\n" +
"            }\n" +
"            String attributeString = signatureToArgumentsMap.get(signature);\n" +
"            String[] args = attributeString.split(\",\");\n" +
"            for (String s : args) {\n" +
"                int paren = s.indexOf('(');\n" +
"                if (paren != -1) {\n" +
"                    s = s.substring(0, paren);\n" +
"                }\n" +
"                if (!parametersSet.contains(s)) {\n" +
"                    System.err.println(\"WARNING: \" + signature + \" does not contain documented parameter \" + s);\n" +
"                }\n" +
"            }\n" +
"        }\n" +
"    }\n";           
        if (GENERATE_DEBUG_VERSION) {
            writer.write(helperCode);
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
args = new String[] { "/Users/tor/netbeans/hg/main/ruby/src/org/netbeans/modules/ruby/RubyIndexerHelper.java" };
        if (args.length != 1) {
            System.err.println("Usage: " + Generator.class.getName() + " <path to RubyIndexerHelper.java>\n\n" +
                    "The RubyIndexerHelper.java file should be in the ruby/editing module. Running this program\n" +
                    "will overwrite portions of the existing file.\n");
            System.exit(0);
        }
        File f = new File(args[0]);
        if (!f.exists()) {
            System.err.println("File " + f.getPath() + " does not exist.");
            System.exit(0);
        }
        new Generator().run(f);
    }
    
    private void checkData() {
        for (MethodDef m : methods) {
            String hashNames = m.arguments;
            // Parse to make sure we don't have problems later...
            int offset = 0;
            while (true) {
                int paren = hashNames.indexOf('(', offset);
                if (paren != -1) {
                    paren = hashNames.indexOf(')',paren);
                    assert paren != -1;
                    offset = paren;
                } else {
                    break;
                }
            }
        }
        
    }

    private void initializeData() {
        String TABLENAME = "-table";
        String COLUMNNAME = "-column";
        String MODELNAME = "-model";
        String VALIDATIONACTIVE = "validationactive"; // hash type
        String SUBMITMETHOD = "submitmethod"; // hash type

        String TABLE_OPTIONS = "(=>id:bool|primary_key:string|options:hash|temporary:bool|force:bool)";
        String TABLE_COLUMN_OPTIONS = "(=>limit|default:nil|null:bool|precision|scale)";
        String TABLE_COLUMN_TYPE = "(:primary_key|:string|:text|:integer|:float|:decimal|:datetime|:timestamp|:time|:date|:binary|:boolean)";
        String HTML_HASH_OPTIONS="class|id"; // XXX what else?

        methods = new ArrayList<MethodDef>();

        // ActiveRecord
        String clzSchemaStatementsFile = "schema_statements";
        String clzSchemaStatements = "ActiveRecord::ConnectionAdapters::SchemaStatements";
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "create_table(",
                "options"+TABLE_OPTIONS));
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "add_column(",
                "table_name(" + TABLENAME + ")," +
               "column_name(" + COLUMNNAME + ")," +
               "options"+TABLE_COLUMN_OPTIONS + "," +
               "type" + TABLE_COLUMN_TYPE));
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "change_column(",
                "table_name(" + TABLENAME + ")," +
                "column_name(" + COLUMNNAME + ")," +                        
                "options"+TABLE_COLUMN_OPTIONS + "," +
                "type" + TABLE_COLUMN_TYPE));
        
        // Rails 1:
        MethodDef renameTableV1 = new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                        "rename_table(",
                    "name(" + TABLENAME + ")", RailsVersion.V1);
        
        // Rails 2: Renamed parameter name to table_name
        MethodDef renameTable = new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                        "rename_table(",
                    "table_name(" + TABLENAME + ")", RailsVersion.V2);
        renameTable.setPreviousVersion(renameTableV1);
        methods.add(renameTable);
        
        
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "rename_column(",
                    "table_name(" + TABLENAME + ")," +
                           "column_name(" + COLUMNNAME + ")"));
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "change_column_default(",
                    "table_name(" + TABLENAME + ")," +
                           "column_name(" + COLUMNNAME + ")"));

        
        // Rails 1:
        MethodDef dropTableV1 = new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "drop_table(",
                    "name(" + TABLENAME + ")", RailsVersion.V1);
        // Rails 2: Renamed parameter name to table_name
        MethodDef dropTable = new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "drop_table(",
                    "table_name(" + TABLENAME + ")", RailsVersion.V2);
        dropTable.setPreviousVersion(dropTableV1);
        methods.add(dropTable);
        
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "add_index(",
                    "table_name(" + TABLENAME + ")," +
                           "column_name(" + COLUMNNAME + ")"));
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "remove_index(",
                "table_name(" + TABLENAME + ")"));
        methods.add(new MethodDef(clzSchemaStatementsFile, clzSchemaStatements,
                "remove_column(",
                "table_name(" + TABLENAME + ")," +
                           "column_name(" + COLUMNNAME + ")"));
                
        String tableDefClz = "ActiveRecord::ConnectionAdapters::TableDefinition";
        String tableDefClzFile = "schema_definitions";
        methods.add(new MethodDef(tableDefClzFile, tableDefClz,
                "column(",
                    "type"+TABLE_COLUMN_TYPE + "," +
                        "options" + TABLE_COLUMN_OPTIONS));
        
        String activeRecordBaseClz = "ActiveRecord::Base";
        String activeRecordBaseClzFile = "base";
        methods.add(new MethodDef(activeRecordBaseClzFile, activeRecordBaseClz,
                "find(",
                    "args(:first|:all),args(=>conditions|order|group|limit|offset|joins|readonly:bool|include|select|from|readonly:bool|lock:bool)"));

        String associationsClassMethodsFile = "associations";
        String associationsClassMethodsClz = "ActiveRecord::Associations::ClassMethods";
        String hasManyOptions = "(=>class_name|conditions|order|group|foreign_key|dependent|exclusively_dependent|" +
          "finder_sql|counter_sql|extend|include|limit|offset|select|as|through|source|source_type|uniq)";
        methods.add(new MethodDef(associationsClassMethodsFile, associationsClassMethodsClz,
                "has_many(",
                    // NOTE - when you add this to a class, a number of new methods are added to it;
                    // I should teach code completion about that
                    // TODO - I can help with the class_name and foreign_key attributes here!
                    "options" + hasManyOptions + ",association_id(" + TABLENAME + ")"));
        // NOTE - when you add this to a class, a number of new methods are added to it;
        // I should teach code completion about that
        // TODO - I can help with the class_name and foreign_key attributes here!
        String hasOneOptions = "(=>class_name|conditions|order|dependent|foreign_key|include|as)";
        methods.add(new MethodDef(associationsClassMethodsFile, associationsClassMethodsClz,
                "has_one(",
                    "options" + hasOneOptions + "),association_id(" + MODELNAME + ")"));
        // NOTE - when you add this to a class, a number of new methods are added to it;
        // I should teach code completion about that
        // TODO - I can help with the class_name and foreign_key attributes here!
        String belongsToOptions = "(=>class_name|conditions|foreign_key|counter_cache|include|polymorphic)";
        methods.add(new MethodDef(associationsClassMethodsFile, associationsClassMethodsClz,
                "belongs_to(",
            "options" + belongsToOptions + "),association_id(" + MODELNAME + ")"));
        // NOTE - when you add this to a class, a number of new methods are added to it;
        // I should teach code completion about that
        // TODO - I can help with the class_name and foreign_key attributes here!
        String belongsToOptions2 = "(=>class_name|join_table|foreign_key|association_foreign_key|conditions|order|uniq:bool|finder_sql|delete_sql|insert_sql|extend|include|group|limit|offset|select)";
        methods.add(new MethodDef(associationsClassMethodsFile, associationsClassMethodsClz,
                "has_and_belongs_to_many(",
                     "options" + belongsToOptions2 + "),association_id(" + TABLENAME + ")"));

        String aggregationsClz = "ActiveRecord::Aggregations::ClassMethods";
        String aggregationsFile = "aggregations";
        methods.add(new MethodDef(aggregationsFile, aggregationsClz,
                "composed_of(",
                    "options(=>class_name|mapping|allow_nil:bool)"));
        methods.add(new MethodDef("list", "ActiveRecord::Acts::List::ClassMethods",
                "acts_as_list(",
                    "options(=>column|scope)"));
        methods.add(new MethodDef("tree", "ActiveRecord::Acts::Tree::ClassMethods",
                "acts_as_tree(",
                    "options(=>foreign_key|order|counter_cache)"));
        methods.add(new MethodDef("nested_set", "ActiveRecord::Acts::NestedSet::ClassMethods",
                "acts_as_nested_set(",
                    "options(=>parent_column|left_column|right_column|scope)"));
        //methods.add(new MethodDef("transactions", "ActiveRecord::Transactions::ClassMethods",
                // No methods with options or docs here
        String COUNT_OPTIONS = "(=>conditions|joins|include|order|group|select|distinct:bool)";
        String CALCULATE_OPTIONS = "(=>conditions|joins|order|group|select|distinct:bool)";
        String calculationsFile = "calculations";
        String calculationsClz = "ActiveRecord::Calculations::ClassMethods";
        methods.add(new MethodDef(calculationsFile, calculationsClz,
                "calculate(",
                    "options" + CALCULATE_OPTIONS + ",operation(:count|:avg|:min|:max|:sum),column_name(" + COLUMNNAME + ")"));
        methods.add(new MethodDef(calculationsFile, calculationsClz,
                "count(",
                // XXX will the "*" match work?
                "args" + COUNT_OPTIONS));
        methods.add(new MethodDef(calculationsFile, calculationsClz,
                "minimum(",
                "options" + CALCULATE_OPTIONS + ",column_name(" + COLUMNNAME + ")"));
        methods.add(new MethodDef(calculationsFile, calculationsClz,
                        "average(",
                "options" + CALCULATE_OPTIONS + ",column_name(" + COLUMNNAME + ")"));
        methods.add(new MethodDef(calculationsFile, calculationsClz,
                        "sum(",
                "options" + CALCULATE_OPTIONS + ",column_name(" + COLUMNNAME + ")"));
        methods.add(new MethodDef(calculationsFile, calculationsClz,
                        "maximum(",
                    "options" + CALCULATE_OPTIONS + ",column_name(" + COLUMNNAME + ")"));

        String validationsFile = "validations";
        String validationsClz = "ActiveRecord::Validations::ClassMethods";
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_each(",
                    "attrs(=>on:" + VALIDATIONACTIVE + "|allow_nil:bool|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_confirmation_of(",
                    "attr_names(=>on:" + VALIDATIONACTIVE + "|message|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_acceptance_of(",
                    "attr_names(=>on:" + VALIDATIONACTIVE + "|message|if|accept)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_presence_of(",
                    "attr_names(=>on:" + VALIDATIONACTIVE + "|message|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_length_of(",
                    "attrs(=>minimum|maximum|is|within|in|allow_nil:bool|too_long|too_short|wrong_length|on:" + VALIDATIONACTIVE + "|message|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_uniqueness_of(",
                    "attr_names(=>message|scope|case_sensitive:bool|allow_nil:bool|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_format_of(",
                    "attr_names(=>on:" + VALIDATIONACTIVE + "|message|if|with)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_inclusion_of(",
                "attr_names(=>in|message|allow_nil:bool|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_exclusion_of(",
                    "attr_names(=>in|message|allow_nil:bool|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_associated(",
                    "attr_names(=>on:" + VALIDATIONACTIVE + "|if)"));
        methods.add(new MethodDef(validationsFile, validationsClz,
                "validates_numericality_of(",
                    "attr_names(=>on:" + VALIDATIONACTIVE + "|message|if|only_integer:bool|allow_nil:bool)"));
        // TimeStamp, AttributeMethods, XmlSerialization, Locking, Reflection, Observing, Callbacks - nothing
            
            
            // ActionController
        String actionControllerClz = "ActionController::Base";
        String actionControllerFile = "base";
        methods.add(new MethodDef(actionControllerFile, actionControllerClz, 
                "url_for(",
                    "options(=>anchor|only_path:bool|controller:controller|action:action|trailing_slash:bool|host|protocol)"));
        methods.add(new MethodDef(actionControllerFile, actionControllerClz, 
                "redirect_to(",
                    "options(=>anchor|only_path:bool|controller:controller|action:action|trailing_slash:bool|host|protocol),options(:back|\"http://)"));
        methods.add(new MethodDef(actionControllerFile, actionControllerClz, 
                "render(",
                // layout:bool applies if action is used
                // locals: applies if action is used
                // use_full_path: applies if file is used
                // collection:collection: applies if partial is used
                // spacer_template applies if partial is used
                "options(=>action:action|partial:partial|status|template|file:file|text:string|json|inline|nothing)"));
        methods.add(new MethodDef(actionControllerFile, actionControllerClz, 
                "render_to_string(",
                // Same as render(
                "options(=>action:action|partial:partial|status|template|file:file|text:string|json|inline|nothing)"));
        methods.add(new MethodDef("cgi_process", actionControllerClz, 
                "process_cgi(",
                    "session_options(=>database_manager|session_key|session_id|new_session|session_expires|session_domain|session_secure|session_path)"));
        

        //methods.add(new MethodDef("layout", ActionController::Layout::ClassMethods",
                // Nothing with smart args here
        methods.add(new MethodDef("streaming", "ActionController::Streaming", "send_file(",
                    "options(=>filename|type|disposition|stream|buffer_size|status)"));
        methods.add(new MethodDef("streaming", "ActionController::Streaming", "send_data(",
                    "options(=>filename|type|disposition|status)"));

        methods.add(new MethodDef("pagination", "ActionController::Pagination",
                "paginate(",
                    "options(=>singular_name|class_name|per_page|conditions|order|order_by|joins|join|include|selected|count)"));
                // I'm not sure what the options in count_collection_for_pagination are referring to
        methods.add(new MethodDef("verification", "ActionController::Verification::ClassMethods",
                "verify(",
                    "options(=>params|session|flash|method|post:" + SUBMITMETHOD + "|xhr:bool|add_flash:hash|add_headers:hash|redirect_to|render|only:bool|except:bool)"));
        methods.add(new MethodDef("session_management", "ActionController::SessionManagement::ClassMethods",
                "session_store=(",
                    "store(:active_record_store|:drb_store|:mem_cache_store|:memory_store)"));
        methods.add(new MethodDef("session_management", "ActionController::SessionManagement::ClassMethods",
                "session(",
                "args(=>on:bool|off:bool|only|except|database_manager|session_key|session_id|new_session|session_expires|session_domain|session_secure|session_path)"));
                //"ActionController::MimeResponds::InstanceMethods")) {
                // Nothing for me to do here
                //if (signature.startsWith("respond_to(")) {
                //}
        methods.add(new MethodDef("scaffolding","ActionController::Scaffolding::ClassMethods", 
                "scaffold(", 
                "model_id(" + MODELNAME + "),options(=>suffix:bool)"));

        //methods.add(new MethodDef("filters", "ActionController::Filters::ClassMethods",
        // no relevant methods
        //if (signature.startsWith("scaffold(")) {
        //}
        // Nothing for Layout, Dependencies, Benchmarking, Flash, Macros, AutoComplete, Caching, Cookies
            
        // ActionView
        
        // Rails 1
        MethodDef formForV1 = new MethodDef("form_helper", "ActionView::Helpers::FormHelper",
            "form_for(",
                "object_name(" + MODELNAME + "),args(=>url:hash|html:hash|builder)", RailsVersion.V1);
        // Rails 2: Renamed parameter name to record_or_name_or_array
        MethodDef formFor = new MethodDef("form_helper", "ActionView::Helpers::FormHelper",
            "form_for(",
                "record_or_name_or_array(" + MODELNAME + "),(rgs=>url:hash|html:hash|builder)", RailsVersion.V2);
        formFor.setPreviousVersion(formForV1);
        methods.add(formFor);
        
        // Rails 1
        MethodDef fieldsForV1 = new MethodDef("form_helper", "ActionView::Helpers::FormHelper",
            "fields_for(",
                "object_name(" + MODELNAME + "),args(=>url:hash)", RailsVersion.V1);
        // Rails 2
        MethodDef fieldsFor = new MethodDef("form_helper", "ActionView::Helpers::FormHelper",
            "fields_for(",
                "record_or_name_or_array(" + MODELNAME + "),args(=>url:hash)", RailsVersion.V2);
        fieldsFor.setPreviousVersion(fieldsForV1);
        methods.add(fieldsFor);

        String[] mtds = {"text_field(","password_field(","hidden_field(","file_field(","text_area(","check_box(","radio_button(" };
        for (String mtd : mtds) {
            // Rails 1
            MethodDef defV1 = new MethodDef("form_helper", "ActionView::Helpers::FormHelper",
                    mtd, "object_name(" + MODELNAME + ")", RailsVersion.V1);
            // Rails 2
            MethodDef def = new MethodDef("form_helper", "ActionView::Helpers::FormHelper",
                    mtd, "record_or_name_or_array(" + MODELNAME + ")", RailsVersion.V2);
            def.setPreviousVersion(defV1);
            methods.add(def);
        }

        methods.add(new MethodDef("prototype_helper", "ActionView::Helpers::PrototypeHelper",
            "observe_field",
                "options(=>url:hash|function|frequency|update|with|on)"));
        methods.add(new MethodDef("prototype_helper", "ActionView::Helpers::PrototypeHelper",
            "observe_form",
                "options(=>url:hash|function|frequency|update|with|on)"));
        mtds = new String[] { "link_to_remote(", "remote_function(", "submit_to_remote(", "form_remote_tag(" };
        for (String mtd : mtds) {
                methods.add(new MethodDef("prototype_helper", "ActionView::Helpers::PrototypeHelper",
                        mtd, "options(=>url:hash|update)"));
        }
        methods.add(new MethodDef("form_tag_helper", "ActionView::Helpers::FormTagHelper",
            "form_tag(","options(=>anchor|only_path:bool|controller:controller|action:action|trailing_slash:bool|host|protocol)"));
        methods.add(new MethodDef("form_tag_helper", "ActionView::Helpers::FormTagHelper",
            "select_tag(", "options(=>multiple:bool)"));
        methods.add(new MethodDef("form_tag_helper", "ActionView::Helpers::FormTagHelper",
            "text_area_tag(", "options(=>size)"));
        mtds = new String[] { "text_field_tag(", "password_field_tag(", "hidden_field_tag(" };
        for (String mtd : mtds) {
            methods.add(new MethodDef("form_tag_helper", "ActionView::Helpers::FormTagHelper",
                    mtd, "options(=>disabled:bool|size|maxlength)"));
        }
        methods.add(new MethodDef("number_helper", "ActionView::Helpers::NumberHelper",
                "number_to_phone(", "options(=>area_code:bool|delimiter|extension|country_code)"));
        methods.add(new MethodDef("number_helper", "ActionView::Helpers::NumberHelper",
            "number_to_currency(", "options(=>precision|unit|separator|delimiter)"));
        methods.add(new MethodDef("number_helper", "ActionView::Helpers::NumberHelper",
            "number_to_percentage(","options(=>precision|separator)"));
        // This was wrong in Rails 1 as well - these aren't option
        //methods.add(new MethodDef("number_helper", "ActionView::Helpers::NumberHelper",
        //    "number_with_delimiter(",
        //        "options(=>delimiter|separator)"));
        
        methods.add(new MethodDef("date_helper", "ActionView::Helpers::DateHelper",
                "date_select(","options(=>discard_year:bool|discard_month:bool|discard_day:bool|order|disabled:bool)"));
        methods.add(new MethodDef("date_helper", "ActionView::Helpers::DateHelper",
            "time_select(", "options(=>include_seconds:bool)"));
        // XXX Not sure about the rest here
        
        methods.add(new MethodDef("asset_tag_helper", "ActionView::Helpers::AssetTagHelper",
            "auto_discovery_link_tag(", "type(:rss|:atom),tag_options(=>rel|type|title),url_options(=>anchor|only_path:bool|controller:controller|action:action|trailing_slash:bool|host|protocol)"));
        methods.add(new MethodDef("asset_tag_helper", "ActionView::Helpers::AssetTagHelper",
            "image_tag(", "options(=>alt|size)"));

        methods.add(new MethodDef("url_helper", "ActionView::Helpers::UrlHelper",
            "url_for(", "options(=>escape:bool|anchor|only_path:bool|controller:controller|action:action|trailing_slash:bool|host|protocol)"));
        methods.add(new MethodDef("url_helper", "ActionView::Helpers::UrlHelper",
            "link_to(", "options(=>anchor|only_path:bool|controller:controller|action:action|trailing_slash:bool|host|protocol),html_options(=>confirm:string|popup:bool|method" + HTML_HASH_OPTIONS +")"));
        methods.add(new MethodDef("url_helper", "ActionView::Helpers::UrlHelper",
            "button_to(", "options(=>anchor|only_path:bool|controller:controller|action:action|trailing_slash:bool|host|protocol),html_options(=>confirm:string|popup:bool|method|disabled:bool" + HTML_HASH_OPTIONS +")"));
        methods.add(new MethodDef("url_helper", "ActionView::Helpers::UrlHelper",
            "mail_to(", "html_options(=>encode|replace_at|replace_dot|subject|body|cc|bcc" + HTML_HASH_OPTIONS +")"));
        // XXX missing some methods here - link_to_if etc.
        
        methods.add(new MethodDef("benchmark_helper", "ActionView::Helpers::BenchmarkHelper",
            "benchmark(", "level(:debug|:info|:warn|:error)"));

        methods.add(new MethodDef("pagination_helper", "ActionView::Helpers::PaginationHelper",
            "pagination_links(", "options(name|window_size|always_show_anchors:bool|link_to_current_page:bool|params),html_options(=>confirm:string|popup:bool|method" + HTML_HASH_OPTIONS +")"));

        methods.add(new MethodDef("active_record_helper", "ActionView::Helpers::ActiveRecordHelper", 
                "form(", "options(action:action)"));
        methods.add(new MethodDef("active_record_helper", "ActionView::Helpers::ActiveRecordHelper", 
                "error_messages_for(",
                // XXX note sig had "*params" instead of a hash - make
                // sure my name comparison is okay with that
                "params(=>header_tag|id|class|object_name)"));
        // TODO: TagHelper  -- if you generate a tag I can do the
        // attributes conditionally based on the tag you're building

        // TODO FormOptionsHelper -- not sure what to do there


        // RSpec
        methods.add(new MethodDef("kernel", "Kernel",
            "describe(",
                // Behavior types seem to be controller,model,helper,view
                "args(=>behaviour_type|shared:bool)"));
    }
    
    enum RailsVersion { V1, V2 };

    /** Method definition */
    private class MethodDef {
        private String filename;
        private String classname;
        private String methodPrefix;
        private String arguments;
        private RailsVersion version;
        private MethodDef previousVersion;
        
        MethodDef(String filename, String classname, String methodPrefix, String arguments) {
            this.filename = filename;
            this.classname = classname;
            this.methodPrefix = methodPrefix;
            this.arguments = arguments;
        }

        MethodDef(String filename, String classname, String methodPrefix, String arguments, RailsVersion version) {
            this(filename, classname, methodPrefix, arguments);
            this.version = version;
        }
        
        public void setPreviousVersion(MethodDef previousVersion) {
            this.previousVersion = previousVersion;
        }
        
        public String toString() {
            return "MethodDef(" + filename + ","+classname+","+methodPrefix+","+arguments+")";
        }
    }
    
}
