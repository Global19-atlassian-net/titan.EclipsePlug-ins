/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.titan.designer.preferences.PreferenceConstants;
import org.eclipse.titan.designer.productUtilities.ProductConstants;

/**
 * This is a universal identifier class which can handle all
 * reserved keywords. It contains also the name mappings. It is
 * effective because of using reference counter.
 *
 * @author Kristof Szabados
 */
public class Identifier implements ILocateableNode, IVisitableNode {

	public enum Identifier_type {
		/** C++ name.*/		ID_NAME,
		/** ASN.1 name.*/		ID_ASN,
		/** TTCN-3 name.*/	ID_TTCN
	}

	private static final Map<String, Identifier_Internal_Data> ID_MAP_NAME = new ConcurrentHashMap<String, Identifier_Internal_Data>();
	private static final Map<String, Identifier_Internal_Data> ID_MAP_ASN = new ConcurrentHashMap<String, Identifier_Internal_Data>();
	private static final Map<String, Identifier_Internal_Data> ID_MAP_TTCN = new ConcurrentHashMap<String, Identifier_Internal_Data>();

	protected Identifier_Internal_Data idData;
	protected Identifier_type type;

	protected Location location;

	/**
	 * Special names that can not be converted on the normal way.
	 * <p>
	 * Java name, ASN.1 name, TTCN-3 name
	 * */
	private static final String[][] KEYWORDS = {
		/* Java keywords - can never be used */
		{"and", null, null},
		{"and_eq", null, null},
		{"asm", null, null},
		{"assert", null, null},
		{"auto", null, null},
		{"bitand", null, null},
		{"bitor", null, null},
		{"bool", null, null},
		{"boolean",null,null},
		{"break", null, null},
		{"byte",null,null},
		{"case", null, null},
		{"catch", null, null},
		{"char", null, null},
		{"class", null, null},
		{"compl", null, null},
		{"const", null, null},
		{"const_cast", null, null},
		{"continue", null, null},
		{"default", null, null},
		{"delete", null, null},
		{"do", null, null},
		{"double", null, null},
		{"dynamic_cast", null, null},
		{"else", null, null},
		{"enum", null, null},
		{"explicit", null, null},
		{"export", null, null},
		{"extends",null,null},
		{"extern", null, null},
		{"false", null, null},
		{"final", null, null},
		{"finally", null, null},
		{"float", null, null},
		{"for", null, null},
		{"friend", null, null},
		{"goto", null, null},
		{"if", null, null},
		{"implements",null,null},
		{"inline", null, null},
		{"instanceof",null, null},
		{"int", null, null},
		{"interface",null, null},
		{"long", null, null},
		{"mutable", null, null},
		{"namespace", null, null},
		{"native",null,null},
		{"new", null, null},
		{"not", null, null},
		{"not_eq", null, null},
		{"operator", null, null},
		{"or", null, null},
		{"or_eq", null, null},
		{"package",null,null},
		{"private", null, null},
		{"protected", null, null},
		{"public", null, null},
		{"register", null, null},
		{"reinterpret_cast", null, null},
		{"return", null, null},
		{"short", null, null},
		{"signed", null, null},
		{"sizeof", null, null},
		{"static", null, null},
		{"static_cast", null, null},
		{"strictfp",null,null},
		{"struct", null, null},
		{"super",null,null},
		{"switch", null, null},
		{"synchronized",null,null},
		{"template", null, null},
		{"this", null, null},
		{"throw", null, null},
		{"throws", null, null},
		{"transient",null,null},
		{"true", null, null},
		{"try", null, null},
		{"typedef", null, null},
		{"typeid", null, null},
		{"typename", null, null},
		{"union", null, null},
		{"unsigned", null, null},
		{"using", null, null},
		{"virtual", null, null},
		{"void", null, null},
		{"volatile", null, null},
		{"wchar_t", null, null},
		{"while", null, null},
		{"xor", null, null},
		{"xor_eq", null, null},
		/* Java keywords postfixed */
		{"abstract_", "abstract", "abstract"},
		{"asm_", "asm", "asm"},
		{"assert_","assert","assert"},
		{"auto_", "auto", "auto"},
		{"bitand_", "bitand", "bitand"},
		{"bitor_", "bitor", "bitor"},
		{"bool_", "bool", "bool"},
		{"byte_","byte","byte"},
		{"class_", "class", "class"},
		{"compl_", "compl", "compl"},
		{"delete_", "delete", "delete"},
		{"double_", "double", "double"},
		{"enum_", "enum", "enum"},
		{"explicit_", "explicit", "explicit"},
		{"export_", "export", "export"},
		{"extern_", "extern", "extern"},
		{"final_", "final", "final"},
		{"finally_", "finally", "finally"},
		{"friend__", "friend", "friend_"},
		{"implements_", "implements","implements"},
		{"inline_", "inline", "inline"},
		{"instanceof_","instanceof","instanceof"},
		{"int_", "int", "int"},
		{"interface_","interface","interface"},
		{"long_", "long", "long"},
		{"mutable_", "mutable", "mutable"},
		{"namespace_", "namespace", "namespace"},
		{"native_","native","native"},
		{"new_", "new", "new"},
		{"operator_", "operator", "operator"},
		{"package_","package","package"},
		{"private__", "private", "private_"},
		{"protected_", "protected", "protected"},
		{"public__", "public", "public_"},
		{"register_", "register", "register"},
		{"short_", "short", "short"},
		{"signed_", "signed", "signed"},
		{"static_", "static", "static"},
		{"strictfp_","strictfp","strictfp"},
		{"struct_", "struct", "struct"},
		{"super_","super","super"},
		{"switch_", "switch", "switch"},
		{"synchronized_","synchronized","synchronized"},
		{"this_", "this", "this"},
		{"throw_", "throw", "throw"},
		{"throws_", "throws", "throws"},
		{"transient_", "transient","transient"},
		{"try_", "try", "try"},
		{"typedef_", "typedef", "typedef"},
		{"typeid_", "typeid", "typeid"},
		{"typename_", "typename", "typename"},
		{"unsigned_", "unsigned", "unsigned"},
		{"using_", "using", "using"},
		{"virtual_", "virtual", "virtual"},
		{"void_", "void", "void"},
		{"volatile_", "volatile", "volatile"},
		/* Java types used by the generated code postfixed */
		{"String_", "String", "String"},
		{"System_", "System", "System"},
		{"MessageFormat_", "MessageFormat", "MessageFormat"},
		{"ArrayList_", "ArrayList", "ArrayList"},
		{"List_", "List", "List"},
		{"AtomicBoolean_", "AtomicBoolean", "AtomicBoolean"},
		{"AtomicInteger_", "AtomicInteger", "AtomicInteger"},
		{"java_", "java", "java"},
		{"sun_", "sun", "sun"},
		/* Java keywords postfixed - keywords in TTCN */
		{"and__", "and", "and_"},
		{"boolean__","boolean", "boolean_"},
		{"break__", "break", "break_"},
		{"case__", "case", "case_"},
		{"catch__", "catch", "catch_"},
		{"char__", "char", "char_"},
		{"const__", "const", "const_"},
		{"continue__", "continue", "continue_"},
		{"default__", "default", "default_"},
		{"do__", "do", "do_"},
		{"else__", "else", "else_"},
		{"extends__","extends","extends_"},
		{"false__", "false", "false_"},
		{"float__", "float", "float_"},
		{"for__", "for", "for_"},
		{"goto__", "goto", "goto_"},
		{"if__", "if", "if_"},
		{"not__", "not", "not_"},
		{"or__", "or", "or_"},
		{"return__", "return", "return_"},
		{"sizeof__", "sizeof", "sizeof_"},
		{"template__", "template", "template_"},
		{"true__", "true", "true_"},
		{"union__", "union", "union_"},
		{"while__", "while", "while_"},
		{"xor__", "xor", "xor_"},
		/* reserved names of the base library */
		{"CHAR", null, null},
		{"ERROR", null, null},
		{"FAIL", null, null},
		{"INCONC", null, null},
		{"FALSE", null, null},
		{"NONE", null, null},
		{"OPTIONAL", null, null},
		{"PASS", null, null},
		{"PORT", null, null},
		{"TIMER", null, null},
		{"TRUE", null, null},
		{"bit2hex", null, null},
		{"bit2int", null, null},
		{"bit2oct", null, null},
		{"bit2str", null, null},
		{"boolean", null, null},
		{"char2int", null, null},
		{"char2oct", null, null},
		{"component", null, null},
		{"decomp", null, null},
		{"float2int", null, null},
		{"float2str", null, null},
		{"hex2bit", null, null},
		{"hex2int", null, null},
		{"hex2oct", null, null},
		{"hex2str", null, null},
		{"int2bit", null, null},
		{"int2char", null, null},
		{"int2float", null, null},
		{"int2hex", null, null},
		{"int2oct", null, null},
		{"int2str", null, null},
		{"int2unichar", null, null},
		{"isbound", null, null},
		{"ischosen", null, null},
		{"ispresent", null, null},
		{"isvalue", null, null},
		{"lengthof", null, null},
		{"log", null, null},
		{"log2str", null, null},
		{"main", null, null},
		{"match", null, null},
		{"mod", null, null},
		{"oct2bit", null, null},
		{"oct2char", null, null},
		{"oct2hex", null, null},
		{"oct2int", null, null},
		{"oct2str", null, null},
		{"regexp", null, null},
		{"replace", null, null},
		{"rem", null, null},
		{"rnd", null, null},
		{"self", null, null},
		{"stderr", null, null},
		{"stdin", null, null},
		{"stdout", null, null},
		{"str2bit", null, null},
		{"str2float", null, null},
		{"str2hex", null, null},
		{"str2int", null, null},
		{"str2oct", null, null},
		{"substr", null, null},
		{"unichar2int", null, null},
		{"unichar2char", null, null},
		{"valueof", null, null},
		{"verdicttype", null, null},
		{"get_stringencoding", null, null},
		{"oct2unichar", null, null},
		{"remove_bom", null, null},
		{"unichar2oct", null, null},
		{"encode_base64", null, null},
		{"decode_base64", null, null},
		/* reserved names of the base library - keywords in TTCN */
		{"bit2hex__", "bit2hex", "bit2hex_"},
		{"bit2int__", "bit2int", "bit2int_"},
		{"bit2oct__", "bit2oct", "bit2oct_"},
		{"bit2str__", "bit2str", "bit2str_"},
		{"boolean__", "boolean", "boolean_"},
		{"char2int__", "char2int", "char2int_"},
		{"char2oct__", "char2oct", "char2oct_"},
		{"component__", "component", "component_"},
		{"decomp__", "decomp", "decomp_"},
		{"float2int__", "float2int", "float2int_"},
		{"float2str__", "float2str", "float2str_"},
		{"hex2bit__", "hex2bit", "hex2bit_"},
		{"hex2int__", "hex2int", "hex2int_"},
		{"hex2oct__", "hex2oct", "hex2oct_"},
		{"hex2str__", "hex2str", "hex2str_"},
		{"int2bit__", "int2bit", "int2bit_"},
		{"int2char__", "int2char", "int2char_"},
		{"int2float__", "int2float", "int2float_"},
		{"int2hex__", "int2hex", "int2hex_"},
		{"int2oct__", "int2oct", "int2oct_"},
		{"int2str__", "int2str", "int2str_"},
		{"int2unichar__", "int2unichar", "int2unichar_"},
		{"isbound__", "isbound", "isbound_"},
		{"ischosen__", "ischosen", "ischosen_"},
		{"ispresent__", "ispresent", "ispresent_"},
		{"isvalue__", "isvalue", "isvalue_"},
		{"lengthof__", "lengthof", "lengthof_"},
		{"log__", "log", "log_"},
		{"log2str__", "log2str", "log2str_"},
		{"match__", "match", "match_"},
		{"mod__", "mod", "mod_"},
		{"oct2bit__", "oct2bit", "oct2bit_"},
		{"oct2char__", "oct2char", "oct2char_"},
		{"oct2hex__", "oct2hex", "oct2hex_"},
		{"oct2int__", "oct2int", "oct2int_"},
		{"oct2str__", "oct2str", "oct2str_"},
		{"regexp__", "regexp", "regexp_"},
		{"replace__", "replace", "replace_"},
		{"rem__", "rem", "rem_"},
		{"rnd__", "rnd", "rnd_"},
		{"self__", "self", "self_"},
		{"str2bit__", "str2bit", "str2bit_"},
		{"str2float__", "str2float", "str2float_"},
		{"str2hex__", "str2hex", "str2hex_"},
		{"str2int__", "str2int", "str2int_"},
		{"str2oct__", "str2oct", "str2oct_"},
		{"substr__", "substr", "substr_"},
		{"unichar2int__", "unichar2int", "unichar2int_"},
		{"unichar2char__", "unichar2char", "unichar2char_"},
		{"valueof__", "valueof", "valueof_"},
		{"verdicttype__", "verdicttype", "verdicttype_"},
		{"get_stringencoding__", "get_stringencoding", "get_stringencoding_"},
		{"oct2unichar__", "oct2unichar", "oct2unichar_"},
		{"remove_bom__", "remove_bom", "remove_bom_"},
		{"unichar2oct__", "unichar2oct", "unichar2oct_"},
		{"encode_base64__", "encode_base64", "encode_base64_"},
		{"decode_base64__", "decode_base64", "decode_base64_"},
		/* reserved names of the base library - keywords in ASN.1 */
		{"FALSE_", null, "FALSE"},
		{"OPTIONAL_", null, "OPTIONAL"},
		{"TRUE_", null, "TRUE"},
		/* reserved names of the base library - not keywords */
		{"CHAR_", "CHAR", "CHAR"},
		{"ERROR_", "ERROR", "ERROR"},
		{"FAIL_", "FAIL", "FAIL"},
		{"INCONC_", "INCONC", "INCONC"},
		{"NONE_", "NONE", "NONE"},
		{"PASS_", "PASS", "PASS"},
		{"PORT_", "PORT", "PORT"},
		{"TIMER_", "TIMER", "TIMER"},
		{"main_", "main", "main"},
		{"stderr_", "stderr", "stderr"},
		{"stdin_", "stdin", "stdin"},
		{"stdout_", "stdout", "stdout"},
		{"TTCN3_", "TTCN3", "TTCN3"},
		{"AdditionalFunctions_", "AdditionalFunctions", "AdditionalFunctions"},
		{"Optional_", "Optional", "Optional"},
		{"RAW_", "RAW", "RAW"},
		{"TtcnError_", "TtcnError", "TtcnError"},
		{"Severity_", "Severity", "Severity"},
		/* built-in types */
		{"TitanAddress", null, "address"},
		{"ASN_NULL", "NULL", null},
		{"TitanBitString", "BIT STRING", "bitstring"},
		{"TitanBoolean", "BOOLEAN", "boolean"},
		{"BMPString", "BMPString", null},
		{"TitanCharString", null, "charstring"},
		{"TitanUniversalCharString", null, "universal charstring"},
		{"CHARACTER_STRING", "CHARACTER STRING", null},
		{"TitanComponent", null, "component"},
		{"TitanDefault", null, "default"},
		{"EMBEDDED_PDV", "EMBEDDED PDV", null},
		{"EXTERNAL", "EXTERNAL", null},
		{"TitanFloat", "REAL", "float"},
		{"GraphicString", "GraphicString", null},
		{"TitanHexString", null, "hexstring"},
		{"IA5String", "IA5String", null},
		{"TitanInteger", "INTEGER", "integer"},
		{"ISO646String", "ISO646String", null},
		{"NumericString", "NumericString", null},
		{"OBJID", "OBJECT IDENTIFIER", "objid"},
		{"TitanOctetString", "OCTET STRING", "octetstring"},
		{"ObjectDescriptor", "ObjectDescriptor", null},
		{"PrintableString", "PrintableString", null},
		{"T61String", "T61String", null},
		{"TeletexString", "TeletexString", null},
		{"UTF8String", "UTF8String", null},
		{"UniversalString", "UniversalString", null},
		{"TitanVerdictType", null, "verdicttype"},
		{"VideotexString", "VideotexString", null},
		{"VisibleString", "VisibleString", null},
		/* reserved names of built-in types */
		{"TitanAddress_", "TitanAddress", "TitanAddress"},
		{"TitanBitString_", "TitanBitString", "TitanBitString"},
		{"TitanBoolean_", null, "TitanBoolean"},
		{"BMPString_", null, "BMPString"},
		{"TitanCharString_", "TitanCharString", "TitanCharString"},
		{"TitanComponent_", "TitanComponent", "TitanComponent"},
		{"TitanDefault_", null, "TitanDefault"},
		{"EXTERNAL_", null, "EXTERNAL"},
		{"TitanFloat_", "TitanFloat", "TitanFloat"},
		{"Ttcn3Float_", "Ttcn3Float", "Ttcn3Float"},
		{"TitanGeneralString_", "TitanGeneralString", "TitanGeneralString"},
		{"TitanGraphicString_", "TitanGraphicString", "TitanGraphicString"},
		{"TitanHexString_", "TitanHexString", "TitanHexString"},
		{"TitanPort_", "TitanPort", "TitanPort"},
		{"IA5String_", null, "IA5String"},
		{"TitanInteger_", "TitanInteger", "TitanInteger"},
		{"ISO646String_", null, "ISO646String"},
		{"NumericString_", null, "NumericString"},
		{"OBJID_", "OBJID", "OBJID"},
		{"TitanObjectid_", "TitanObjectid", "TitanObjectid"},
		{"TitanOctetString_", "TitanOctetString", "TitanOctetString"},
		{"ObjectDescriptor_", null, "ObjectDescriptor"},
		{"PrintableString_", null, "PrintableString"},
		{"T61String_", null, "T61String"},
		{"TeletexString_", null, "TeletexString"},
		{"UTF8String_", null, "UTF8String"},
		{"UniversalString_", null, "UniversalString"},
		{"TitanVerdictType_", "TitanVerdictType", "TitanVerdictType"},
		{"VideotexString_", null, "VideotexString"},
		{"VisibleString_", null, "VisibleString"},
		/* keywords in TTCN-3, not reserved words in Java, postfixed in TTCN */
		{"action__", "action", "action_"},
		{"activate__", "activate", "activate_"},
		{"address__", "address", "address_"},
		{"alive__", "alive", "alive_"},
		{"all__", "all", "all_"},
		{"alt__", "alt", "alt_"},
		{"altstep__", "altstep", "altstep_"},
		{"and4b__", "and4b", "and4b_"},
		{"any__", "any", "any_"},
		{"anytype__", "anytype", "anytype_"},
		{"apply__", "apply", "apply_"},
		{"bitstring__", "bitstring", "bitstring_"},
		{"call__", "call", "call_"},
		{"charstring__", "charstring", "charstring_"},
		{"check__", "check", "check_"},
		{"clear__", "clear", "clear_"},
		{"complement__", "complement", "complement_"},
		{"connect__", "connect", "connect_"},
		{"control__", "control", "control_"},
		{"create__", "create", "create_"},
		{"deactivate__", "deactivate", "deactivate_"},
		{"derefers__", "derefers", "derefers_"},
		{"disconnect__", "disconnect", "disconnect_"},
		{"display__", "display", "display_"},
		{"done__", "done", "done_"},
		{"encode__", "encode", "encode_"},
		{"enumerated__", "enumerated", "enumerated_"},
		{"error__", "error", "error_"},
		{"except__", "except", "except_"},
		{"exception__", "exception", "exception_"},
		{"execute__", "execute", "execute_"},
		{"extends__", "extends", "extends_"},
		{"extension__", "extension", "extension_"},
		{"external__", "external", "external_"},
		{"fail__", "fail", "fail_"},
		{"from__", "from", "from_"},
		{"function__", "function", "function_"},
		{"getcall__", "getcall", "getcall_"},
		{"getreply__", "getreply", "getreply_"},
		{"getverdict__", "getverdict", "getverdict_"},
		{"group__", "group", "group_"},
		{"halt__", "halt", "halt_"},
		{"hexstring__", "hexstring", "hexstring_"},
		{"ifpresent__", "ifpresent", "ifpresent_"},
		{"import__", "import", "import_"},
		{"in__", "in", "in_"},
		{"inconc__", "inconc", "inconc_"},
		{"infinity__", "infinity", "infinity_"},
		{"inout__", "inout", "inout_"},
		{"integer__", "integer", "integer_"},
		{"interleave__", "interleave", "interleave_"},
		{"kill__", "kill", "kill_"},
		{"killed__", "killed", "killed_"},
		{"label__", "label", "label_"},
		{"language__", "language", "language_"},
		{"length__", "length", "length_"},
		{"map__", "map", "map_"},
		{"message__", "message", "message_"},
		{"mixed__", "mixed", "mixed_"},
		{"modifies__", "modifies", "modifies_"},
		{"module__", "module", "module_"},
		{"modulepar__", "modulepar", "modulepar_"},
		{"mtc__", "mtc", "mtc_"},
		{"noblock__", "noblock", "noblock_"},
		{"none__", "none", "none_"},
		{"not4b__", "not4b", "not4b_"},
		{"not__a__number__","not-a-number","not_a_number_"},
		{"nowait__", "nowait", "nowait_"},
		{"null__", "null", "null_"},
		{"objid__", "objid", "objid_"},
		{"octetstring__", "octetstring", "octetstring_"},
		{"of__", "of", "of_"},
		{"omit__", "omit", "omit_"},
		{"on__", "on", "on_"},
		{"optional__", "optional", "optional_"},
		{"or4b__", "or4b", "or4b_"},
		{"out__", "out", "out_"},
		{"override__", "override", "override_"},
		{"param__", "param", "param_"},
		{"pass__", "pass", "pass_"},
		{"pattern__", "pattern", "pattern_"},
		{"permutation__", "permutation", "permutation_"},
		{"port__", "port", "port_"},
		{"procedure__", "procedure", "procedure_"},
		{"raise__", "raise", "raise_"},
		{"read__", "read", "read_"},
		{"receive__", "receive", "receive_"},
		{"record__", "record", "record_"},
		{"recursive__", "recursive", "recursive_"},
		{"refers__", "refers", "refers_"},
		{"repeat__", "repeat", "repeat_"},
		{"reply__", "reply", "reply_"},
		{"running__", "running", "running_"},
		{"runs__", "runs", "runs_"},
		{"select__", "select", "select_"},
		{"send__", "send", "send_"},
		{"sender__", "sender", "sender_"},
		{"set__", "set", "set_"},
		{"setverdict__", "setverdict", "setverdict_"},
		{"signature__", "signature", "signature_"},
		{"start__", "start", "start_"},
		{"stop__", "stop", "stop_"},
		{"subset__", "subset", "subset_"},
		{"superset__", "superset", "superset_"},
		{"system__", "system", "system_"},
		{"testcase__", "testcase", "testcase_"},
		{"timeout__", "timeout", "timeout_"},
		{"timer__", "timer", "timer_"},
		{"to__", "to", "to_"},
		{"trigger__", "trigger", "trigger_"},
		{"type__", "type", "type_"},
		{"universal__", "universal", "universal_"},
		{"unmap__", "unmap", "unmap_"},
		{"value__", "value", "value_"},
		{"present__", "present", "present_"},
		{"var__", "var", "var_"},
		{"variant__", "variant", "variant_"},
		{"with__", "with", "with_"},
		{"xor4b__", "xor4b", "xor4b_"},
		/* other names that need to be mapped in a non-uniform manner */
		{"major_", "major", "major"},
		{"minor_", "minor", "minor"},
		/* internal / error */
		{"<error>", "<error>", "<error>"},
		{"TTCN_internal_", "<internal>", "<internal>"}
	};

	/**
	 * Special names in the realtime extension that can not be converted on the normal way.
	 * <p>
	 * Java name, ASN.1 name, TTCN-3 name
	 * */
	private static final String[][] REALTIME_KEYWORDS = {
		/* Java keywords - can never be used */
		{"now__", "now", "now_"},
		{"realtime__", "realtime", "realtime_"},
		{"timestamp__", "timestamp", "timestamp_"}
	};

	static {
		//fill the data structures with the keywords, and their associations.
		for (int i = 0; i < KEYWORDS.length; i++) {
			addKeyword(KEYWORDS[i]);
		}

		final IPreferencesService prefs = Platform.getPreferencesService();
		if (prefs.getBoolean(ProductConstants.PRODUCT_ID_DESIGNER, PreferenceConstants.ENABLEREALTIMEEXTENSION, false, null)) {
			for (int i = 0; i < REALTIME_KEYWORDS.length; i++) {
				addKeyword(REALTIME_KEYWORDS[i]);
			}
		}
	}

	private static void addKeyword(final String[] tempKeyword) {
		final String asnName = tempKeyword[1] == null ? Identifier_Internal_Data.INVALID_STRING : tempKeyword[1];
		final String ttcnName = tempKeyword[2] == null ? Identifier_Internal_Data.INVALID_STRING : tempKeyword[2];

		final Identifier_Internal_Data idData = new Identifier_Internal_Data(tempKeyword[0], asnName, ttcnName);
		// add to the map the name
		if (!ID_MAP_NAME.containsKey(idData.getName())) {
			ID_MAP_NAME.put(idData.getName(), idData);
		}
		if (tempKeyword[1] != null && !ID_MAP_ASN.containsKey(asnName)) {
			ID_MAP_ASN.put(asnName, idData);
		}
		if (tempKeyword[2] != null && !ID_MAP_TTCN.containsKey(ttcnName)) {
			ID_MAP_TTCN.put(ttcnName, idData);
		}
	}

	/** not to be called from outside. */
	protected Identifier() {

	}

	public Identifier(final Identifier_type idType, final String name) {
		this(idType, name, NULL_Location.INSTANCE, false);
	}

	public Identifier(final Identifier_type idType, final String name, final Location location) {
		this(idType, name, location, false);
	}

	public Identifier(final Identifier_type idType, final String name, final Location location, final boolean dontregister) {
		this.type = idType;
		this.location = location;
		switch(idType) {
		case ID_ASN:
			if (ID_MAP_ASN.containsKey(name)) {
				idData = ID_MAP_ASN.get(name);
			} else if (name.length() > 0 && name.charAt(0) == '&') {
				final String asnName = name.substring(1);

				String realName;
				if (ID_MAP_ASN.containsKey(asnName)) {
					realName = ID_MAP_ASN.get(asnName).getName();
				} else {
					realName = Identifier_Internal_Data.asnToName(asnName);
				}

				idData = new Identifier_Internal_Data(realName, name, Identifier_Internal_Data.INVALID_STRING);
				if (!dontregister) {
					ID_MAP_ASN.put(name, idData);
				}
			} else {
				final String realName = Identifier_Internal_Data.asnToName(name);

				if (!dontregister && ID_MAP_NAME.containsKey(realName)) {
					idData = ID_MAP_NAME.get(realName);
					if (idData.getAsnName().equals(name)) {
						ID_MAP_ASN.put(name, idData);
					} else if (location != null) {
						location.reportSemanticError("The ASN identifier `" + name + "' clashes with this id: `" + idData.getAsnName() + "'");
					}
				} else {
					idData = new Identifier_Internal_Data(realName, name, null);
					if (!dontregister) {
						ID_MAP_NAME.put(realName, idData);
						ID_MAP_ASN.put(name, idData);
					}
				}
			}
			break;
		case ID_TTCN:
			if (ID_MAP_TTCN.containsKey(name)) {
				idData = ID_MAP_TTCN.get(name);
			} else {
				final String realName = Identifier_Internal_Data.ttcnToName(name);
				if (!dontregister && ID_MAP_NAME.containsKey(realName)) {
					idData = ID_MAP_NAME.get(realName);
					if (idData.getTtcnName().equals(name)) {
						ID_MAP_TTCN.put(name, idData);
					} else if (location != null) {
						location.reportSemanticError("The TTCN identifier `" + name + "' clashes with this id: `" + idData.getTtcnName() + "'");
					}
				} else {
					idData = new Identifier_Internal_Data(realName, null, name);
					if (!dontregister) {
						ID_MAP_NAME.put(realName, idData);
						ID_MAP_TTCN.put(name, idData);
					}
				}
			}
			break;
		case ID_NAME:
		default:
			if (ID_MAP_NAME.containsKey(name)) {
				idData = ID_MAP_NAME.get(name);
			} else {
				idData = new Identifier_Internal_Data(name, null, null);
				if (!dontregister) {
					ID_MAP_NAME.put(name, idData);
				}
			}
			break;
		}
	}

	public final Identifier newInstance() {
		final Identifier temp = new Identifier();

		temp.type = type;
		temp.idData = idData;
		temp.location = location;

		return temp;
	}

	/** @return the type of the identifier. */
	public final Identifier_type getType() {
		return type;
	}

	/** @return the internal (and C++) name. */
	public final String getName() {
		return idData.getName();
	}

	@Override
	/** {@inheritDoc} */
	public final Location getLocation() {
		return location;
	}

	@Override
	/** {@inheritDoc} */
	public final void setLocation(final Location location) {
		this.location = location;
	}

	/** @return the display name according to its origin. */
	public final String getDisplayName() {
		switch(type) {
		case ID_ASN:
			return idData.getAsnName();
		case ID_TTCN:
			return idData.getTtcnName();
		default:
			return idData.getName();
		}
	}

	/** @return the ASN display name. */
	public final String getAsnName() {
		return idData.getAsnName();
	}

	/** @return the TTCN display name. */
	public final String getTtcnName() {
		return idData.getTtcnName();
	}

	/**
	 * @param idType the type of identifier to be checked for.
	 *
	 * @return whether this identifier is valid in a context
	 * */
	public final boolean getHasValid(final Identifier_type idType) {
		switch(idType) {
		case ID_NAME:
			return !Identifier_Internal_Data.INVALID_STRING.equals(getName());
		case ID_TTCN:
			return !Identifier_Internal_Data.INVALID_STRING.equals(getTtcnName());
		case ID_ASN:
			return !Identifier_Internal_Data.INVALID_STRING.equals(getAsnName());
		default:
			// internal problem
			return false;
		}
	}

	public final boolean isvalidAsnTyperef() {
		return idData.isvalidAsnTyperef();
	}

	public final boolean isvalidAsnValueReference() {
		return idData.isvalidAsnValueReference();
	}

	public final boolean isvalidAsnValueSetReference() {
		return isvalidAsnTyperef();
	}

	public final boolean isvalidAsnObjectClassReference() {
		return idData.isvalidAsnObjectClassReference();
	}

	public final boolean isvalidAsnObjectReference() {
		return idData.isvalidAsnObjectReference();
	}

	public final boolean isvalidAsnObjectSetReference() {
		return isvalidAsnTyperef();
	}

	public final boolean isvalidAsnValueFieldReference() {
		return idData.isvalidAsnValueFieldReference();
	}

	public final boolean isvalidAsnObjectFieldReference() {
		return idData.isvalidAsnObjectFieldReference();
	}

	public final boolean isvalidAsnObjectSetFieldReference() {
		return idData.isvalidAsnObjectSetFieldReference();
	}

	public final boolean isvalidAsnWord() {
		return idData.isvalidAsnWord();
	}

	@Override
	/** {@inheritDoc} */
	public final String toString() {
		return getDisplayName();
	}

	@Override
	/** {@inheritDoc} */
	public final boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj instanceof Identifier) {
			final Identifier_Internal_Data otherData = ((Identifier) obj).idData;
			if (idData == otherData) {
				return true;
			}

			return idData.getName().equals(otherData.getName());
		}

		return false;
	}

	@Override
	/** {@inheritDoc} */
	public final int hashCode() {
		return idData.getName().hashCode();
	}

	public static boolean isValidInTtcn(final String id) {
		return Pattern.matches("[A-Za-z][A-Za-z0-9_]*", id);
	}

	// TODO: better check using context data (upper or lower or ...)
	public static boolean isValidInAsn(final String id) {
		return Pattern.matches("&?[A-Za-z]([\\-_]?[A-Za-z0-9]+)*", id);
	}

	public static String getTtcnNameFromAsnName(final String asnName) {
		String ttcnName = asnName;
		if (ID_MAP_ASN.containsKey(asnName)) {
			ttcnName = ID_MAP_ASN.get(asnName).getTtcnName();
		}
		return ttcnName.replace('-', '_');
	}

	/**
	 * Convert asn name to internal (unique) name
	 * */
	public static String getNameFromAsnName(final String asnName) {
		if(ID_MAP_ASN.containsKey(asnName)) {
			final Identifier_Internal_Data data = ID_MAP_ASN.get(asnName);
			return data.getName();
		}

		final String name = Identifier_Internal_Data.asnToName(asnName);
		if(ID_MAP_NAME.containsKey(name)) {
			final Identifier_Internal_Data data = ID_MAP_NAME.get(name);
			ID_MAP_ASN.put(asnName, data);
		}

		return name;
	}

	@Override
	/** {@inheritDoc} */
	public boolean accept(final ASTVisitor v) {
		switch (v.visit(this)) {
		case ASTVisitor.V_ABORT: return false;
		case ASTVisitor.V_SKIP: return true;
		}
		if (v.leave(this)==ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}
}
