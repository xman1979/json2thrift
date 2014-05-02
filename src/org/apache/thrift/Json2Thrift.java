/*
 * Input JSON file, and output thrift IDL
 */ 
/*
The MIT License (MIT)

Copyright (c) <2014> <Xiaodong Ma adam.maxiaodong@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package org.apache.thrift;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Json2Thrift {
    private static boolean m_bFieldRequired = false;
    private static String DEFAULT_STRUCT_NAME = "MyStruct";

    private static void printHelp() {
        System.out.println("List of options:");
        System.out.println("    -f or --json-file, the input JsonFile ");
        System.out.println("    -s or --struct-name, the major struct name");
        System.out.println("    -r or --field-required, use required not optional (default)");
    }

    private static String readFile( String file ) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader( new FileReader (file));
            String         line = null;
            StringBuilder  stringBuilder = new StringBuilder();
            String         ls = System.getProperty("line.separator");

            while( ( line = reader.readLine() ) != null ) {
                stringBuilder.append( line );
                stringBuilder.append( ls );
            }
            reader.close();
            return stringBuilder.toString();
        } catch (IOException e) {
            return null;
        }
    }

    private static String keyMapping(String key) {
    	if (m_keywordMap.containsKey(key)) {
    		return m_keywordMap.get(key);
    	}
        String mappedKey = key.replace("-", "_").
        		replace("$", "_").
        		replace("*", "_").
        		replace(":", "_");
        return mappedKey;
    }

    private static void obj2Thrift(JSONObject obj, String structName, List<String> structs) {
        String struct = "struct " + structName + " {\n";
        Integer fieldId = 1;
        Iterator<String> keys = obj.keys();
        while(keys.hasNext()) {
            String key = keys.next();
            String mappedKey = keyMapping(key);
            struct += "    " + fieldId.toString() + ": ";
            struct += m_bFieldRequired ? "required ": "optional ";
            Object o;
            try {
                o = obj.get(key);
                if (o instanceof JSONObject) {
                    String subName = mappedKey + "_obj";
                    obj2Thrift((JSONObject) o, subName, structs);
                    struct += subName + " " + mappedKey + ",\n";
                    fieldId++;
                }
                else if (o instanceof JSONArray) {
                    JSONArray arrObj = (JSONArray)o;
                    if (arrObj.length() > 0) {
                        Object so = arrObj.get(0);
                        if (so instanceof JSONObject) {
                            String subName = mappedKey + "_elm";
                            obj2Thrift((JSONObject)so, subName, structs);
                            struct += "list<" + subName + "> " + mappedKey + ",\n";
                            fieldId++;
                        }
                        else if (so instanceof Double) {
                            struct += "list<double> " + mappedKey + ",\n";
                            fieldId++;
                        }
                        else if (so instanceof Integer) {
                            struct += "list<i32> " + mappedKey + ",\n";
                            fieldId++;
                        }
                        else if (so instanceof String) {
                            struct += "list<string> " + mappedKey + ",\n";
                            fieldId++;
                        }
                        else if (so instanceof Boolean) {
                            struct += "list<bool> " + mappedKey + ",\n";
                            fieldId++;
                        }
                        else if (so instanceof Long) {
                            struct += "list<i64> " + mappedKey + ",\n";
                            fieldId++;
                        }
                        else {
                            struct += "list<string> " + mappedKey + ",\n";
                            fieldId++;
                        }
                    }
                    else {
                        struct += "list<string> " + mappedKey + ",\n";
                        fieldId++;
                    }
                }
                else if (o instanceof Double) {
                    struct += "double " + mappedKey + ",\n";
                    fieldId++;
                }
                else if (o instanceof Integer) {
                    struct += "i32 " + mappedKey + ",\n";
                    fieldId++;
                }
                else if (o instanceof String) {
                    struct += "string " + mappedKey + ",\n";
                    fieldId++;
                }
                else if (o instanceof Boolean) {
                    struct += "bool " + mappedKey + ",\n";
                    fieldId++;
                }
                else if (o instanceof Long) {
                    struct += "i64 " + mappedKey + ",\n";
                    fieldId++;
                }
            } catch (JSONException e) {
                System.err.println(e.toString());
            }
        }
        struct += "}\n";
        structs.add(struct);
    }

    public static void main(String[] args) {
        String jsonFile = null;
        String myStruct = DEFAULT_STRUCT_NAME;

        List<String> structs = new ArrayList<String>();

        try {
            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("-f") || args[i].equals("--json-file")) {
                    if ( (++i) == args.length) {
                        throw new IllegalArgumentException("Missing input json file path");
                    }
                    jsonFile = args[i];
                }
                else if (args[i].equals("-s") || args[i].equals("--struct-name")) {
                    if ( (++i) == args.length) {
                        throw new IllegalArgumentException("Missing struct name");
                    }
                    myStruct= args[i];
                }
                else if (args[i].equals("-r") || args[i].equals("--field-required")) {
                    m_bFieldRequired = true;
                }
            }
        } catch (Exception e1) {
            System.err.println("Failed to parse command line:" + e1.getMessage());
            printHelp();
            return;
        }
        if (jsonFile == null) {
            printHelp();
            return;
        }

        String inputString = readFile(jsonFile);
        if (inputString == null) {
            System.err.println("Failed to open file:" + jsonFile);
            return;
        }

        JSONObject obj;
        try {
            obj = new JSONObject(inputString);
        } catch (JSONException e) {
            System.err.println("Bad json input from file:" + jsonFile);
            return;
        }
        obj2Thrift(obj, myStruct, structs);
        System.out.println("/* Automatically generated by json2Thrift tool. */\n");

        for (String s : structs) {
            System.out.println(s);
        }
    }
    private static final Map<String, String> m_keywordMap = new HashMap<String, String>();
    static {
        m_keywordMap.put("false"                ,"_false_");
        m_keywordMap.put("true"                 ,"_true_");
        m_keywordMap.put("namespace"            ,"_namespace_");
        m_keywordMap.put("cpp_namespace"        ,"_cpp_namespace_");
        m_keywordMap.put("cpp_include"          ,"_cpp_include_");
        m_keywordMap.put("cpp_type"             ,"_cpp_type_");
        m_keywordMap.put("java_package"         ,"_java_package_");
        m_keywordMap.put("cocoa_prefix"         ,"_cocoa_prefix_");
        m_keywordMap.put("csharp_namespace"     ,"_csharp_namespace_");
        m_keywordMap.put("delphi_namespace"     ,"_delphi_namespace_");
        m_keywordMap.put("php_namespace"        ,"_php_namespace_");
        m_keywordMap.put("py_module"            ,"_py_module_");
        m_keywordMap.put("perl_package"         ,"_perl_package_");
        m_keywordMap.put("ruby_namespace"       ,"_ruby_namespace_");
        m_keywordMap.put("smalltalk_category"   ,"_smalltalk_category_");
        m_keywordMap.put("smalltalk_prefix"     ,"_smalltalk_prefix_");
        m_keywordMap.put("xsd_all"              ,"_xsd_all_");
        m_keywordMap.put("xsd_optional"         ,"_xsd_optional_");
        m_keywordMap.put("xsd_nillable"         ,"_xsd_nillable_");
        m_keywordMap.put("xsd_namespace"        ,"_xsd_namespace_");
        m_keywordMap.put("xsd_attrs"            ,"_xsd_attrs_");
        m_keywordMap.put("include"              ,"_include_");
        m_keywordMap.put("void"                 ,"_void_");
        m_keywordMap.put("bool"                 ,"_bool_");
        m_keywordMap.put("byte"                 ,"_byte_");
        m_keywordMap.put("i16"                  ,"_i16_");
        m_keywordMap.put("i32"                  ,"_i32_");
        m_keywordMap.put("i64"                  ,"_i64_");
        m_keywordMap.put("double"               ,"_double_");
        m_keywordMap.put("string"               ,"_string_");
        m_keywordMap.put("binary"               ,"_binary_");
        m_keywordMap.put("slist"                ,"_slist_");
        m_keywordMap.put("senum"                ,"_senum_");
        m_keywordMap.put("map"                  ,"_map_");
        m_keywordMap.put("list"                 ,"_list_");
        m_keywordMap.put("set"                  ,"_set_");
        m_keywordMap.put("oneway"               ,"_oneway_");
        m_keywordMap.put("typedef"              ,"_typedef_");
        m_keywordMap.put("struct"               ,"_struct_");
        m_keywordMap.put("union"                ,"_union_");
        m_keywordMap.put("exception"            ,"_exception_");
        m_keywordMap.put("extends"              ,"_extends_");
        m_keywordMap.put("throws"               ,"_throws_");
        m_keywordMap.put("service"              ,"_service_");
        m_keywordMap.put("enum"                 ,"_enum_");
        m_keywordMap.put("const"                ,"_const_");
        m_keywordMap.put("required"             ,"_required_");
        m_keywordMap.put("optional"             ,"_optional_");
        m_keywordMap.put("async"                ,"_async_");
        m_keywordMap.put("BEGIN"                ,"_BEGIN_");
        m_keywordMap.put("END"                  ,"_END_");
        m_keywordMap.put("__CLASS__"            ,"___CLASS___");
        m_keywordMap.put("__DIR__"              ,"___DIR___");
        m_keywordMap.put("__FILE__"             ,"___FILE___");
        m_keywordMap.put("__FUNCTION__"         ,"___FUNCTION___");
        m_keywordMap.put("__LINE__"             ,"___LINE___");
        m_keywordMap.put("__METHOD__"           ,"___METHOD___");
        m_keywordMap.put("__NAMESPACE__"        ,"___NAMESPACE___");
        m_keywordMap.put("abstract"             ,"_abstract_");
        m_keywordMap.put("alias"                ,"_alias_");
        m_keywordMap.put("and"                  ,"_and_");
        m_keywordMap.put("args"                 ,"_args_");
        m_keywordMap.put("as"                   ,"_as_");
        m_keywordMap.put("assert"               ,"_assert_");
        m_keywordMap.put("begin"                ,"_begin_");
        m_keywordMap.put("break"                ,"_break_");
        m_keywordMap.put("case"                 ,"_case_");
        m_keywordMap.put("catch"                ,"_catch_");
        m_keywordMap.put("class"                ,"_class_");
        m_keywordMap.put("clone"                ,"_clone_");
        m_keywordMap.put("continue"             ,"_continue_");
        m_keywordMap.put("declare"              ,"_declare_");
        m_keywordMap.put("def"                  ,"_def_");
        m_keywordMap.put("default"              ,"_default_");
        m_keywordMap.put("del"                  ,"_del_");
        m_keywordMap.put("delete"               ,"_delete_");
        m_keywordMap.put("do"                   ,"_do_");
        m_keywordMap.put("dynamic"              ,"_dynamic_");
        m_keywordMap.put("elif"                 ,"_elif_");
        m_keywordMap.put("else"                 ,"_else_");
        m_keywordMap.put("elseif"               ,"_elseif_");
        m_keywordMap.put("elsif"                ,"_elsif_");
        m_keywordMap.put("end"                  ,"_end_");
        m_keywordMap.put("enddeclare"           ,"_enddeclare_");
        m_keywordMap.put("endfor"               ,"_endfor_");
        m_keywordMap.put("endforeach"           ,"_endforeach_");
        m_keywordMap.put("endif"                ,"_endif_");
        m_keywordMap.put("endswitch"            ,"_endswitch_");
        m_keywordMap.put("endwhile"             ,"_endwhile_");
        m_keywordMap.put("ensure"               ,"_ensure_");
        m_keywordMap.put("except"               ,"_except_");
        m_keywordMap.put("exec"                 ,"_exec_");
        m_keywordMap.put("finally"              ,"_finally_");
        m_keywordMap.put("float"                ,"_float_");
        m_keywordMap.put("for"                  ,"_for_");
        m_keywordMap.put("foreach"              ,"_foreach_");
        m_keywordMap.put("function"             ,"_function_");
        m_keywordMap.put("global"               ,"_global_");
        m_keywordMap.put("goto"                 ,"_goto_");
        m_keywordMap.put("if"                   ,"_if_");
        m_keywordMap.put("implements"           ,"_implements_");
        m_keywordMap.put("import"               ,"_import_");
        m_keywordMap.put("in"                   ,"_in_");
        m_keywordMap.put("inline"               ,"_inline_");
        m_keywordMap.put("instanceof"           ,"_instanceof_");
        m_keywordMap.put("interface"            ,"_interface_");
        m_keywordMap.put("is"                   ,"_is_");
        m_keywordMap.put("lambda"               ,"_lambda_");
        m_keywordMap.put("module"               ,"_module_");
        m_keywordMap.put("native"               ,"_native_");
        m_keywordMap.put("new"                  ,"_new_");
        m_keywordMap.put("next"                 ,"_next_");
        m_keywordMap.put("nil"                  ,"_nil_");
        m_keywordMap.put("not"                  ,"_not_");
        m_keywordMap.put("or"                   ,"_or_");
        m_keywordMap.put("pass"                 ,"_pass_");
        m_keywordMap.put("public"               ,"_public_");
        m_keywordMap.put("print"                ,"_print_");
        m_keywordMap.put("private"              ,"_private_");
        m_keywordMap.put("protected"            ,"_protected_");
        m_keywordMap.put("public"               ,"_public_");
        m_keywordMap.put("raise"                ,"_raise_");
        m_keywordMap.put("redo"                 ,"_redo_");
        m_keywordMap.put("rescue"               ,"_rescue_");
        m_keywordMap.put("retry"                ,"_retry_");
        m_keywordMap.put("register"             ,"_register_");
        m_keywordMap.put("return"               ,"_return_");
        m_keywordMap.put("self"                 ,"_self_");
        m_keywordMap.put("sizeof"               ,"_sizeof_");
        m_keywordMap.put("static"               ,"_static_");
        m_keywordMap.put("super"                ,"_super_");
        m_keywordMap.put("switch"               ,"_switch_");
        m_keywordMap.put("synchronized"         ,"_synchronized_");
        m_keywordMap.put("then"                 ,"_then_");
        m_keywordMap.put("this"                 ,"_this_");
        m_keywordMap.put("throw"                ,"_throw_");
        m_keywordMap.put("transient"            ,"_transient_");
        m_keywordMap.put("try"                  ,"_try_");
        m_keywordMap.put("undef"                ,"_undef_");
        m_keywordMap.put("union"                ,"_union_");
        m_keywordMap.put("unless"               ,"_unless_");
        m_keywordMap.put("unsigned"             ,"_unsigned_");
        m_keywordMap.put("until"                ,"_until_");
        m_keywordMap.put("use"                  ,"_use_");
        m_keywordMap.put("var"                  ,"_var_");
        m_keywordMap.put("virtual"              ,"_virtual_");
        m_keywordMap.put("volatile"             ,"_volatile_");
        m_keywordMap.put("when"                 ,"_when_");
        m_keywordMap.put("while"                ,"_while_");
        m_keywordMap.put("with"                 ,"_with_");
        m_keywordMap.put("xor"                  ,"_xor_");
        m_keywordMap.put("yield"                ,"_yield_");
    }
}

