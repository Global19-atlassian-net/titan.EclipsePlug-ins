package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.ArrayList;

import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.compiler.JavaGenData;

/**
 * Utility class for generating the value and template classes for signature
 * types.
 *
 *
 * @author Kristof Szabados
 * */
public final class SignatureGenerator {

	enum signatureParamaterDirection {PAR_IN, PAR_OUT, PAR_INOUT};

	public static class SignatureParameter {
		private final signatureParamaterDirection direction;

		/** Java type name of the parameter */
		private final String mJavaTypeName;

		/** Java template name of the parameter */
		private final String mJavaTemplateName;

		/** Parameter name */
		private final String mJavaName;

		public SignatureParameter(final signatureParamaterDirection direction, final String paramType, final String paramTemplate, final String paramName) {
			this.direction = direction;
			mJavaTypeName = paramType;
			mJavaTemplateName = paramTemplate;
			mJavaName = FieldSubReference.getJavaGetterName(paramName);
		}
	}

	public static class SignatureReturnType {
		/** Java type name of the return type */
		private final String mJavaTypeName;

		/** Java template name of the return type */
		private final String mJavaTemplateName;

		public SignatureReturnType(final String paramType, final String paramTemplate) {
			mJavaTypeName = paramType;
			mJavaTemplateName = paramTemplate;
		}
	}

	public static class SignatureException {
		/** Java type name of the exception */
		private final String mJavaTypeName;

		/** Java template name of the exception */
		private final String mJavaTemplateName;

		/** The name to be displayed for the user */
		private final String mDisplayName;

		public SignatureException(final String paramType, final String paramTemplate, final String displayName) {
			mJavaTypeName = paramType;
			mJavaTemplateName = paramTemplate;
			mDisplayName = displayName;
		}
	}

	public static class SignatureDefinition {
		private final String genName;
		private final String displayName;
		private final ArrayList<SignatureParameter> formalParameters;
		private final SignatureReturnType returnType;
		private final boolean isNoBlock;
		private final ArrayList<SignatureException> signatureExceptions;

		public SignatureDefinition(final String genName, final String displayName, final ArrayList<SignatureParameter> formalParameters, final SignatureReturnType returnType, final boolean isNoBlock, final ArrayList<SignatureException> signatureExceptions) {
			this.genName = genName;
			this.displayName = displayName;
			this.formalParameters = formalParameters;
			this.returnType = returnType;
			this.isNoBlock = isNoBlock;
			this.signatureExceptions = signatureExceptions;
		}
	}

	private SignatureGenerator() {
		// private to disable instantiation
	}

	/**
	 * This function can be used to generate the class of signature types
	 *
	 * defSignatureClasses in compiler2/ttcn3/signature.{h,c}
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param def
	 *                the signature definition to generate code for.
	 * */
	public static void generateClasses(final JavaGenData aData, final StringBuilder source, final SignatureDefinition def) {
		aData.addBuiltinTypeImport("TitanBoolean");
		if (!def.signatureExceptions.isEmpty() || !def.formalParameters.isEmpty()) {
			aData.addBuiltinTypeImport("Base_Template.template_sel");
		}
		if (def.formalParameters.isEmpty()) {
			aData.addBuiltinTypeImport("TitanNull_Type");
		}

		generateCallClass(aData, source, def);
		generateRedirectClass(aData, source, def);
		generateReplyClass(aData, source, def);
		generateReplyRedirectClass(aData, source, def);
		generateExceptionClass(aData, source, def);
		generateTemplateClass(aData, source, def);
	}

	/**
	 * This function can be used to generate for signature types that class
	 * that handles calls.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param def
	 *                the signature definition to generate code for.
	 * */
	private static void generateCallClass(final JavaGenData aData, final StringBuilder source, final SignatureDefinition def) {
		source.append(MessageFormat.format("public static class {0}_call '{'\n", def.genName));
		source.append("// in and inout parameters\n");
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("private {0} param_{1};\n", formalPar.mJavaTypeName, formalPar.mJavaName));
			}
		}

		source.append(MessageFormat.format("public {0}_call() '{'\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("param_{0} = new {1}();\n", formalPar.mJavaName, formalPar.mJavaTypeName));
			}
		}
		source.append("}\n");

		source.append(MessageFormat.format("public {0}_call(final {0}_call otherValue) '{'\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("param_{0} = new {1}(otherValue.get{2}());\n", formalPar.mJavaName, formalPar.mJavaTypeName, formalPar.mJavaName));
			}
		}
		source.append("}\n");

		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("public {0} get{1}() '{'\n", formalPar.mJavaTypeName, formalPar.mJavaName));
				source.append(MessageFormat.format("return param_{0};\n", formalPar.mJavaName));
				source.append("}\n");

				source.append(MessageFormat.format("public {0} constGet{1}() '{'\n", formalPar.mJavaTypeName, formalPar.mJavaName));
				source.append(MessageFormat.format("return param_{0};\n", formalPar.mJavaName));
				source.append("}\n");
			}
		}

		source.append("public void encode_text(final Text_Buf text_buf) {");
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("param_{0}.encode_text(text_buf);\n", formalPar.mJavaName));
			}
		}
		source.append("}\n");

		source.append("public void decode_text(final Text_Buf text_buf) {");
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("param_{0}.decode_text(text_buf);\n", formalPar.mJavaName));
			}
		}
		source.append("}\n\n");

		source.append("/**\n");
		source.append(" * Logs this value.\n");
		source.append(" */\n");
		source.append("public void log() {\n");
		source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0} : '{' \");\n", def.displayName));
		boolean isFirst = true;
		for (int i = 0; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);
			if (formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				if (isFirst) {
					source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0} := \");\n", formalPar.mJavaName));
					isFirst = false;
				} else {
					source.append(MessageFormat.format("TTCN_Logger.log_event_str(\", {0} := \");\n", formalPar.mJavaName));
				}
				source.append(MessageFormat.format("param_{0}.log();\n", formalPar.mJavaName));
			}
		}
		source.append("TTCN_Logger.log_event_str(\" }\");\n");
		source.append("}\n");

		source.append("}\n");
	}

	/**
	 * This function can be used to generate for signature types that class
	 * that handles redirections.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param def
	 *                the signature definition to generate code for.
	 * */
	private static void generateRedirectClass(final JavaGenData aData, final StringBuilder source, final SignatureDefinition def) {
		source.append(MessageFormat.format("public static class {0}_call_redirect '{'\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("private {0} ptr_{1};\n", formalPar.mJavaTypeName, formalPar.mJavaName));
			}
		}

		source.append(MessageFormat.format("public {0}_call_redirect( ) '{'", def.genName));
		source.append("}\n");

		boolean longConstructorNeeded = false;
		for (int i = 0 ; i < def.formalParameters.size() && !longConstructorNeeded; i++) {
			if (def.formalParameters.get(i).direction != signatureParamaterDirection.PAR_OUT) {
				longConstructorNeeded = true;
			}
		}

		if (longConstructorNeeded) {
			source.append(MessageFormat.format("public {0}_call_redirect( ", def.genName));
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);
	
				if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
					if (i != 0) {
						source.append(", ");
					}
	
					source.append(MessageFormat.format("final {0} par_{1}", formalPar.mJavaTypeName, formalPar.mJavaName));
				}
			}
			source.append(" ) {\n");
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);
	
				if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
					source.append(MessageFormat.format("ptr_{0} = par_{0};\n", formalPar.mJavaName));
				}
			}
			source.append("}\n");
		}

		source.append(MessageFormat.format("public void set_parameters( final {0}_call call_par) '{'\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("if (ptr_{0} != null) '{'\n", formalPar.mJavaName));
				source.append(MessageFormat.format("ptr_{0}.assign(call_par.constGet{0}());\n", formalPar.mJavaName));
				source.append("}\n");
			}
		}
		source.append("}\n");
		source.append("}\n");
	}

	/**
	 * This function can be used to generate for signature types that class
	 * that handles replies.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param def
	 *                the signature definition to generate code for.
	 * */
	private static void generateReplyClass(final JavaGenData aData, final StringBuilder source, final SignatureDefinition def) {
		if(!def.isNoBlock) {
			source.append(MessageFormat.format("public static class {0}_reply '{'\n", def.genName));
			source.append("// out parameters\n");
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("private {0} param_{1};\n", formalPar.mJavaTypeName, formalPar.mJavaName));
				}
			}
			if (def.returnType != null) {
				source.append("// the reply value of the signature\n");
				source.append(MessageFormat.format("private {0} reply_value;\n", def.returnType.mJavaTypeName));
			}

			source.append(MessageFormat.format("public {0}_reply() '{'\n", def.genName));
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("param_{0} = new {1}();\n", formalPar.mJavaName, formalPar.mJavaTypeName));
				}
			}
			if (def.returnType != null) {
				source.append(MessageFormat.format("reply_value = new {0}();\n", def.returnType.mJavaTypeName));
			}
			source.append("}\n");

			source.append(MessageFormat.format("public {0}_reply(final {0}_reply other_value) '{'\n", def.genName));
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("param_{0} = new {1}(other_value.get{2}());\n", formalPar.mJavaName, formalPar.mJavaTypeName, formalPar.mJavaName));
				}
			}
			if (def.returnType != null) {
				source.append(MessageFormat.format("reply_value = new {0}(other_value.getreturn_value());\n", def.returnType.mJavaTypeName));
			}
			source.append("}\n");

			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("public {0} get{1}() '{'\n", formalPar.mJavaTypeName, formalPar.mJavaName));
					source.append(MessageFormat.format("return param_{0};\n", formalPar.mJavaName));
					source.append("}\n");

					source.append(MessageFormat.format("public {0} constGet{1}() '{'\n", formalPar.mJavaTypeName, formalPar.mJavaName));
					source.append(MessageFormat.format("return param_{0};\n", formalPar.mJavaName));
					source.append("}\n");
				}
			}

			if (def.returnType != null) {
				source.append(MessageFormat.format("public {0} getreturn_value() '{'\n", def.returnType.mJavaTypeName));
				source.append("return reply_value;\n");
				source.append("}\n");

				source.append(MessageFormat.format("public {0} constGetreturn_value() '{'\n", def.returnType.mJavaTypeName));
				source.append("return reply_value;\n");
				source.append("}\n");
			}

			source.append("public void encode_text(final Text_Buf text_buf) {");
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("param_{0}.encode_text(text_buf);\n", formalPar.mJavaName));
				}
			}
			if (def.returnType != null) {
				source.append("reply_value.encode_text(text_buf);\n");
			}
			source.append("}\n");

			source.append("public void decode_text(final Text_Buf text_buf) {");
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("param_{0}.decode_text(text_buf);\n", formalPar.mJavaName));
				}
			}
			if (def.returnType != null) {
				source.append("reply_value.decode_text(text_buf);\n");
			}
			source.append("}\n\n");

			source.append("/**\n");
			source.append(" * Logs this value.\n");
			source.append(" */\n");
			source.append("public void log() {\n");
			source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0} : '{' \");\n", def.displayName));
			boolean isFirst = true;
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					if (isFirst) {
						source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0} := \");\n", formalPar.mJavaName));
						isFirst = false;
					} else {
						source.append(MessageFormat.format("TTCN_Logger.log_event_str(\", {0} := \");\n", formalPar.mJavaName));
					}
					source.append(MessageFormat.format("param_{0}.log();\n", formalPar.mJavaName));
				}
			}
			if (def.returnType != null) {
				source.append("TTCN_Logger.log_event_str(\" } value \");\n");
				source.append("reply_value.log();\n");
			} else {
				source.append("TTCN_Logger.log_event_str(\" }\");\n");
			}
			source.append("}\n");

			source.append("}\n");
		}
	}

	/**
	 * This function can be used to generate for signature types that class
	 * that handles reply redirections.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param def
	 *                the signature definition to generate code for.
	 * */
	private static void generateReplyRedirectClass(final JavaGenData aData, final StringBuilder source, final SignatureDefinition def) {
		if(!def.isNoBlock) {
			source.append(MessageFormat.format("public static class {0}_reply_redirect '{'\n", def.genName));
			if (def.returnType != null) {
				source.append("// the reply value of the signature\n");
				source.append(MessageFormat.format("private {0} ret_val_redir;\n", def.returnType.mJavaTypeName));
			}
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("private {0} ptr_{1};\n", formalPar.mJavaTypeName, formalPar.mJavaName));
				}
			}

			source.append(MessageFormat.format("public {0}_reply_redirect( ", def.genName));
			if (def.returnType != null) {
				source.append(MessageFormat.format("final {0} return_redir", def.returnType.mJavaTypeName));
			}
			source.append(" ) {\n");
			if (def.returnType != null) {
				source.append(MessageFormat.format("ret_val_redir = return_redir;\n", def.returnType.mJavaTypeName));
			}
			source.append("}\n");

			boolean longConstructorNeeded = false;
			for (int i = 0 ; i < def.formalParameters.size() && !longConstructorNeeded; i++) {
				if (def.formalParameters.get(i).direction != signatureParamaterDirection.PAR_IN) {
					longConstructorNeeded = true;
				}
			}

			if (longConstructorNeeded) {
				source.append(MessageFormat.format("public {0}_reply_redirect( ", def.genName));
				boolean first = true;
				if (def.returnType != null) {
					source.append(MessageFormat.format("final {0} return_redir", def.returnType.mJavaTypeName));
					first = false;
				}
				for (int i = 0 ; i < def.formalParameters.size(); i++) {
					final SignatureParameter formalPar = def.formalParameters.get(i);

					if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
						if (!first) {
							source.append(", ");
						}
						source.append(MessageFormat.format("final {0} par_{1}", formalPar.mJavaTypeName, formalPar.mJavaName));
						first = false;
					}
				}
				source.append(" ) {\n");
				if (def.returnType != null) {
					source.append(MessageFormat.format("ret_val_redir = return_redir;\n", def.returnType.mJavaTypeName));
				}
				for (int i = 0 ; i < def.formalParameters.size(); i++) {
					final SignatureParameter formalPar = def.formalParameters.get(i);

					if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
						source.append(MessageFormat.format("ptr_{0} = par_{0};\n", formalPar.mJavaName));
					}
				}
				source.append("}\n");
			}

			source.append(MessageFormat.format("public void set_parameters( final {0}_reply reply_par) '{'\n", def.genName));
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("if (ptr_{0} != null) '{'\n", formalPar.mJavaName));
					source.append(MessageFormat.format("ptr_{0}.assign(reply_par.constGet{0}());\n", formalPar.mJavaName));
					source.append("}\n");
				}
				
			}
			if (def.returnType != null) {
				source.append("if (ret_val_redir != null) {\n");
				source.append(MessageFormat.format("ret_val_redir.assign(reply_par.constGetreturn_value());\n", def.returnType.mJavaTypeName));
				source.append("}\n");
			}
			source.append("}\n");
			source.append("}\n");
		}
	}

	/**
	 * This function can be used to generate for signature types that class
	 * that handles exceptions.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param def
	 *                the signature definition to generate code for.
	 * */
	private static void generateExceptionClass(final JavaGenData aData, final StringBuilder source, final SignatureDefinition def) {
		if (!def.signatureExceptions.isEmpty()) {
			source.append(MessageFormat.format("public static class {0}_exception '{'\n", def.genName));
			source.append("public enum exception_selection_type {");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);
				source.append(MessageFormat.format(" ALT_{0},", exception.mJavaTypeName));
			}
			source.append(" UNBOUND_VALUE };\n");

			source.append("private exception_selection_type exception_selection;\n");
			source.append("//originally a union which can not be mapped to Java\n");
			source.append("private Base_Type field;\n");

			source.append("/**\n");
			source.append(" * Deletes the value, setting it to unbound.\n");
			source.append(" *\n");
			source.append(" * clean_up() in the core\n");
			source.append(" * */\n");
			source.append("public void cleanUp() {\n");
			source.append("field = null;\n");
			source.append("exception_selection = exception_selection_type.UNBOUND_VALUE;\n");
			source.append("}\n");

			source.append(MessageFormat.format("private void copy_value(final {0}_exception otherValue) '{'\n", def.genName));
			source.append("switch (otherValue.exception_selection){\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);
				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("field = new {0}(({0})otherValue.field);\n", exception.mJavaTypeName));
				source.append("break;\n");
			}
				source.append("default:\n");
				source.append(MessageFormat.format("throw new TtcnError(\"Copying an uninitialized exception of signature {0}.\");\n", def.displayName));
				source.append("}\n");
				source.append("exception_selection = otherValue.exception_selection;\n");
			source.append("}\n");

			source.append(MessageFormat.format("public {0}_exception() '{'\n", def.genName));
			source.append("exception_selection = exception_selection_type.UNBOUND_VALUE;\n");
			source.append("}\n");

			source.append(MessageFormat.format("public {0}_exception(final {0}_exception otherValue)  '{'\n", def.genName));
			source.append("copy_value(otherValue);\n");
			source.append("}\n");

			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);
				source.append(MessageFormat.format("public {0}_exception( final {1} otherValue) '{'\n", def.genName, exception.mJavaTypeName));
				source.append(MessageFormat.format("field = new {0}(otherValue);\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("exception_selection = exception_selection_type.ALT_{0};\n", exception.mJavaTypeName));
				source.append("}\n");

				source.append(MessageFormat.format("public {0}_exception( final {1}_template otherValue) '{'\n", def.genName, exception.mJavaTypeName));
				source.append(MessageFormat.format("field = new {0}(otherValue.valueOf());\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("exception_selection = exception_selection_type.ALT_{0};\n", exception.mJavaTypeName));
				source.append("}\n");
			}

			if ( aData.isDebug() ) {
				source.append("/**\n");
				source.append(" * Assigns the other value to this value.\n");
				source.append(" * Overwriting the current content in the process.\n");
				source.append(" *<p>\n");
				source.append(" * operator= in the core.\n");
				source.append(" *\n");
				source.append(" * @param otherValue\n");
				source.append(" *                the other value to assign.\n");
				source.append(" * @return the new value object.\n");
				source.append(" */\n");
			}
			source.append(MessageFormat.format("public {0}_exception assign( final {0}_exception otherValue ) '{'\n", def.genName));
			source.append("if(this != otherValue) {\n");
			source.append("cleanUp();\n");
			source.append("copy_value(otherValue);\n");
			source.append("}\n");
			source.append("return this;\n");
			source.append("}\n");

			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("//originally {0}_field\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("public {0} get{0}() '{'\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("if (exception_selection != exception_selection_type.ALT_{0}) '{'\n", exception.mJavaTypeName));
				source.append("cleanUp();\n");
				source.append(MessageFormat.format("field = new {0}();\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("exception_selection = exception_selection_type.ALT_{0};\n", exception.mJavaTypeName));
				source.append("}\n");
				source.append(MessageFormat.format("return ({0})field;\n", exception.mJavaTypeName));
				source.append("}\n");

				source.append(MessageFormat.format("//originally const {0}_field\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("public {0} constGet{0}() '{'\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("if (exception_selection != exception_selection_type.ALT_{0}) '{'\n", exception.mJavaTypeName));

				source.append(MessageFormat.format("throw new TtcnError(\"Referencing to non-selected type integer in an exception of signature {0}.\");\n", def.displayName));
				source.append("}\n");
				source.append(MessageFormat.format("return ({0})field;\n", exception.mJavaTypeName));
				source.append("}\n");
			}

			source.append("public exception_selection_type get_selection() {\n");
			source.append("return exception_selection;\n");
			source.append("}\n");

			source.append("public void encode_text(final Text_Buf text_buf) {\n");
			source.append("switch (exception_selection) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("text_buf.push_int({0});\n", i));
				source.append("field.encode_text(text_buf);\n");
				source.append("break;\n");
			}
			source.append("default:\n");
			source.append(MessageFormat.format("throw new TtcnError(\"Text encoder: Encoding an uninitialized exception of signature {0}.\");\n", def.displayName));
			source.append("}\n");
			source.append("}\n");

			source.append("public void decode_text(final Text_Buf text_buf) {\n");
			source.append("final TitanInteger temp = text_buf.pull_int();\n");
			source.append("switch (temp.getInt()) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case {0}:\n", i));
				source.append(MessageFormat.format("get{0}().decode_text(text_buf);\n", exception.mJavaTypeName));
				source.append("break;\n");
			}
			source.append("default:\n");
			source.append(MessageFormat.format("throw new TtcnError(\"Text decoder: Unrecognized selector was received for an exception of signature {0}.\");\n", def.displayName));
			source.append("}\n");
			source.append("}\n\n");

			source.append("/**\n");
			source.append(" * Logs this value.\n");
			source.append(" */\n");
			source.append("public void log() {\n");
			source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0}, \");\n", def.displayName));
			source.append("switch (exception_selection) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0} : \");\n", exception.mDisplayName));
				source.append("field.log();\n");
				source.append("break;\n");
			}
			source.append("default:\n");
			source.append("TTCN_Logger.log_event_str(\"<uninitialized exception>\");\n");
			source.append("}\n");
			source.append("}\n");

			source.append("}\n");

			source.append(MessageFormat.format("public static class {0}_exception_template '{'\n", def.genName));
			source.append(MessageFormat.format("private {0}_exception.exception_selection_type exception_selection;\n", def.genName));
			source.append("//originally a union which can not be mapped to Java\n");
			source.append("private Base_Template field;\n");
			source.append("private Base_Type redirection_field;\n");

			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("public {0}_exception_template(final {1} init_template) '{'\n", def.genName, exception.mJavaTemplateName));
				source.append(MessageFormat.format("exception_selection = {0}_exception.exception_selection_type.ALT_{1};\n", def.genName, exception.mJavaTypeName));
				source.append(MessageFormat.format("field = new {0}(init_template);\n", exception.mJavaTemplateName));
				source.append("}\n");

				source.append(MessageFormat.format("public {0}_exception_template(final {1} init_template, final {2} value_redirect) '{'\n", def.genName, exception.mJavaTemplateName, exception.mJavaTypeName));
				source.append(MessageFormat.format("exception_selection = {0}_exception.exception_selection_type.ALT_{1};\n", def.genName, exception.mJavaTypeName));
				source.append(MessageFormat.format("field = new {0}(init_template);\n", exception.mJavaTemplateName));
				source.append("redirection_field = value_redirect;\n");
				source.append("}\n\n");
			}

			if (aData.isDebug()) {
				source.append("/**\n");
				source.append(" * Matches the provided ecpetion value against this template. In legacy mode\n");
				source.append(" * omitted value fields are not matched against the template field.\n");
				source.append(" *\n");
				source.append(" * @param other_value\n");
				source.append(" *                the value to be matched.\n");
				source.append(" * @param legacy\n");
				source.append(" *                use legacy mode.\n");
				source.append(" * */\n");
			}
			source.append(MessageFormat.format("public boolean match(final {0}_exception other_value, final boolean legacy) '{'\n", def.genName));
			source.append("if (exception_selection != other_value.get_selection()) {\n");
			source.append("return false;\n");
			source.append("}\n");

			source.append("switch (exception_selection) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("return (({0}) field).match(other_value.get{1}(), legacy);\n", exception.mJavaTemplateName, exception.mJavaTypeName));
			}
			source.append("default:\n");
			source.append(MessageFormat.format("throw new TtcnError(\"Internal error: Invalid selector when matching an exception of signature {0}.\");\n", def.displayName));
			source.append("}\n");
			source.append("}\n\n");

			source.append(MessageFormat.format("public void log_match(final {0}_exception other_value, final boolean legacy) '{'\n", def.genName));
			source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0}, \");\n", def.displayName));
			source.append("if (exception_selection == other_value.get_selection()) {\n");
			source.append("switch (exception_selection) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0} : \");\n", exception.mDisplayName));
				source.append(MessageFormat.format("field.log_match(other_value.constGet{0}(), legacy);\n", exception.mJavaTypeName));
				source.append("break;");
			}
			source.append("default:\n");
			source.append("TTCN_Logger.log_event_str(\"<invalid selector>\");");
			source.append("}\n");
			source.append("} else {\n");
			source.append("other_value.log();\n");
			source.append("TTCN_Logger.log_event_str(\" with \");\n");
			source.append("switch (exception_selection) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"{0} : \");\n", exception.mDisplayName));
				source.append("field.log();\n");
				source.append("break;");
			}
			source.append("default:\n");
			source.append("TTCN_Logger.log_event_str(\"<invalid selector>\");");
			source.append("}\n");
			source.append("if (match(other_value, legacy)) TTCN_Logger.log_event_str(\" matched\");\n");
			source.append("else TTCN_Logger.log_event_str(\" unmatched\");\n");
			source.append("}\n");
			source.append("}\n");

			source.append(MessageFormat.format("public void set_value(final {0}_exception source_value) '{'\n", def.genName));
			source.append("if (exception_selection == source_value.get_selection()) {\n");
			source.append("switch (exception_selection) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append("if (redirection_field != null) {\n");
				source.append(MessageFormat.format("redirection_field.assign(source_value.constGet{0}());\n", exception.mJavaTypeName));
				source.append("}\n");
				source.append("return;\n");
			}
			source.append("default:\n");
			source.append("break;\n");
			source.append("}\n");
			source.append("}\n");
			source.append("}\n\n");

			source.append("public boolean is_any_or_omit() {\n");
			source.append("switch (exception_selection) {\n");
			for ( int i = 0; i < def.signatureExceptions.size(); i++) {
				final SignatureException exception = def.signatureExceptions.get(i);

				source.append(MessageFormat.format("case ALT_{0}:\n", exception.mJavaTypeName));
				source.append(MessageFormat.format("return (({0}) field).get_selection() == template_sel.ANY_OR_OMIT;\n", exception.mJavaTemplateName));
			}
			source.append("default:\n");
			source.append("break;\n");
			source.append("}\n");

			source.append(MessageFormat.format("throw new TtcnError(\"Internal error: Invalid selector when checking for '*' in an exception template of signature {0}.\");\n", def.displayName));
			source.append("}\n");
			source.append("}\n");
		}
	}

	/**
	 * This function can be used to generate for signature types the
	 * template class.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param def
	 *                the signature definition to generate code for.
	 * */
	private static void generateTemplateClass(final JavaGenData aData, final StringBuilder source, final SignatureDefinition def) {
		source.append(MessageFormat.format("public static class {0}_template '{'\n", def.genName));
		source.append("// all the parameters\n");
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			source.append(MessageFormat.format("private {0} param_{1};\n", formalPar.mJavaTemplateName, formalPar.mJavaName));
		}
		if (def.returnType != null) {
			source.append(MessageFormat.format("private {0} reply_value;\n", def.returnType.mJavaTemplateName));
		}

		if (aData.isDebug()) {
			source.append("/**\n");
			source.append(" * Initializes to unbound/uninitialized template.\n");
			source.append(" * */\n");
		}
		source.append(MessageFormat.format("public {0}_template() '{'\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			source.append(MessageFormat.format("param_{0} = new {1}(template_sel.ANY_VALUE);\n", formalPar.mJavaName, formalPar.mJavaTemplateName));
		}
		source.append("}\n\n");

		if (def.formalParameters.isEmpty()) {
			if (aData.isDebug()) {
				source.append("/**\n");
				source.append(" * Initializes to an empty specific value template.\n");
				source.append(" *\n");
				source.append(" * @param otherValue\n");
				source.append(" *                the null value.\n");
				source.append(" * */\n");
			}
			source.append(MessageFormat.format("public {0}_template(final TitanNull_Type otherValue) '{'\n", def.genName));
			if (def.returnType != null) {
				source.append(MessageFormat.format("reply_value = new {0}(template_sel.ANY_VALUE);\n", def.returnType.mJavaTemplateName));
			}
			source.append("}\n\n");
		}

		if (aData.isDebug()) {
			source.append("/**\n");
			source.append(" * Initializes to a given template.\n");
			source.append(" * The elements of the provided template are copied.\n");
			source.append(" *\n");
			source.append(" * @param otherValue\n");
			source.append(" *                the value to initialize to.\n");
			source.append(" * */\n");
		}
		source.append(MessageFormat.format("public {0}_template(final {0}_template otherValue) '{'\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			source.append(MessageFormat.format("param_{0} = new {1}(otherValue.get{0}());\n", formalPar.mJavaName, formalPar.mJavaTemplateName));
		}
		source.append("}\n\n");

		if (def.formalParameters.isEmpty()) {
			if ( aData.isDebug() ) {
				source.append("/**\n");
				source.append(" * Sets the current value to unbound.\n");
				source.append(" * Overwriting the current content in the process.\n");
				source.append(" *<p>\n");
				source.append(" * operator= in the core.\n");
				source.append(" *\n");
				source.append(" * @param otherValue\n");
				source.append(" *                the other value to assign.\n");
				source.append(" * @return the new value object.\n");
				source.append(" */\n");
			}
			source.append(MessageFormat.format("public {0}_template assign(final TitanNull_Type otherValue) '{'\n", def.genName));
			source.append("return this;\n");
			source.append("}\n\n");
		}

		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			source.append(MessageFormat.format("public {0} get{1}() '{'\n", formalPar.mJavaTemplateName, formalPar.mJavaName ));
			source.append(MessageFormat.format("return param_{0};\n", formalPar.mJavaName ));
			source.append("}\n");

			source.append(MessageFormat.format("public {0} constGet{1}() '{'\n", formalPar.mJavaTemplateName, formalPar.mJavaName ));
			source.append(MessageFormat.format("return param_{0};\n", formalPar.mJavaName ));
			source.append("}\n");
		}

		if (def.returnType != null) {
			source.append(MessageFormat.format("public {0} return_value() '{'\n", def.returnType.mJavaTemplateName));
			source.append("return reply_value;\n");
			source.append("}\n");
		}

		source.append(MessageFormat.format("public {0}_call create_call() '{'\n", def.genName));
		source.append(MessageFormat.format("{0}_call return_value = new {0}_call();\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("return_value.get{0}().assign(param_{0}.valueOf());\n", formalPar.mJavaName ));
			}
		}
		source.append("return return_value;\n");
		source.append("}\n");

		if(!def.isNoBlock) {
			source.append(MessageFormat.format("public {0}_reply create_reply() '{'\n", def.genName));
			source.append(MessageFormat.format("{0}_reply return_value = new {0}_reply();\n", def.genName));
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("return_value.get{0}().assign(param_{0}.valueOf());\n", formalPar.mJavaName ));
				}
			}

			if (def.returnType != null) {
				source.append("return_value.getreturn_value().assign(reply_value.valueOf());\n");
			}
			source.append("return return_value;\n");
			source.append("}\n");
		}

		source.append(MessageFormat.format("public boolean match_call(final {0}_call match_value) '{'\n", def.genName));
		source.append("return match_call(match_value, false);\n");
		source.append("}\n");

		source.append(MessageFormat.format("public boolean match_call(final {0}_call match_value, final boolean legacy) '{'\n", def.genName));
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("if (!param_{0}.match(match_value.get{0}(), legacy)) '{'return false;'}'\n", formalPar.mJavaName ));
			}
		}
		source.append("return true;\n");
		source.append("}\n");

		if(!def.isNoBlock) {
			source.append(MessageFormat.format("public boolean match_reply(final {0}_reply match_value) '{'\n", def.genName));
			source.append("return match_reply(match_value, false);\n");
			source.append("}\n");

			source.append(MessageFormat.format("public boolean match_reply(final {0}_reply match_value, final boolean legacy) '{'\n", def.genName));
			for (int i = 0 ; i < def.formalParameters.size(); i++) {
				final SignatureParameter formalPar = def.formalParameters.get(i);

				if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
					source.append(MessageFormat.format("if (!param_{0}.match(match_value.get{0}(), legacy)) '{'return false;'}'\n", formalPar.mJavaName ));
				}
			}
			if (def.returnType != null) {
				source.append("if (!reply_value.match(match_value.getreturn_value(), legacy)) {return false;}\n");
			}
			source.append("return true;\n");
			source.append("}\n");
		}

		if (def.returnType != null) {
			source.append(MessageFormat.format("public {0}_template set_value_template(final {1} new_template) '{'\n", def.genName, def.returnType.mJavaTypeName));
			source.append(MessageFormat.format("return set_value_template(new {0}(new_template));\n", def.returnType.mJavaTemplateName));
			source.append("}\n");
			source.append(MessageFormat.format("public {0}_template set_value_template(final {1} new_template) '{'\n", def.genName, def.returnType.mJavaTemplateName));
			source.append(MessageFormat.format("final {0}_template temp = new {0}_template(this);\n", def.genName));
			source.append("temp.reply_value = new_template;\n");
			source.append("return temp;\n");
			source.append("}\n");
		}

		source.append("public void encode_text(final Text_Buf text_buf) {\n");
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("param_{0}.encode_text(text_buf);\n", formalPar.mJavaName ));
			}
		}
		source.append("}\n");

		source.append("public void decode_text(final Text_Buf text_buf) {\n");
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				source.append(MessageFormat.format("param_{0}.decode_text(text_buf);\n", formalPar.mJavaName ));
			}
		}
		source.append("}\n\n");

		source.append("@Override\n");
		source.append("public void log() {\n");
		boolean isFirst = true;
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if (isFirst) {
				source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"'{' {0} := \");\n", formalPar.mJavaName));
				isFirst = false;
			} else {
				source.append(MessageFormat.format("TTCN_Logger.log_event_str(\", {0} := \");\n", formalPar.mJavaName));
			}
			source.append(MessageFormat.format("param_{0}.log();\n", formalPar.mJavaName ));
		}
		source.append("TTCN_Logger.log_event_str(\" }\");\n");
		source.append("}\n");

		source.append(MessageFormat.format("public void log_match_call(final {0}_call match_value, final boolean legacy) '{'\n", def.genName));
		isFirst = true;
		for (int i = 0 ; i < def.formalParameters.size(); i++) {
			final SignatureParameter formalPar = def.formalParameters.get(i);

			if(formalPar.direction != signatureParamaterDirection.PAR_OUT) {
				if (isFirst) {
					source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"'{' {0} := \");\n", formalPar.mJavaName ));
					isFirst = false;
				} else {
					source.append(MessageFormat.format("TTCN_Logger.log_event_str(\", {0} := \");\n", formalPar.mJavaName ));
				}
				source.append(MessageFormat.format("param_{0}.log_match(match_value.get{0}(), legacy);\n", formalPar.mJavaName ));
			}
		}
		source.append("TTCN_Logger.log_event_str(\" }\");\n");
		source.append("}\n");

		if(!def.isNoBlock) {
			source.append(MessageFormat.format("public void log_match_reply(final {0}_reply match_value, final boolean legacy) '{'\n", def.genName));
			if (def.formalParameters.size() > 0) {
				isFirst = true;
				for (int i = 0 ; i < def.formalParameters.size(); i++) {
					final SignatureParameter formalPar = def.formalParameters.get(i);

					if(formalPar.direction != signatureParamaterDirection.PAR_IN) {
						if (isFirst) {
							source.append(MessageFormat.format("TTCN_Logger.log_event_str(\"'{' {0} := \");\n", formalPar.mJavaName ));
							isFirst = false;
						} else {
							source.append(MessageFormat.format("TTCN_Logger.log_event_str(\", {0} := \");\n", formalPar.mJavaName ));
						}
						source.append(MessageFormat.format("param_{0}.log_match(match_value.get{0}(), legacy);\n", formalPar.mJavaName ));
					}
				}
				if (def.returnType != null) {
					source.append("TTCN_Logger.log_event_str(\" } value \");\n");
					source.append("reply_value.log_match(match_value.getreturn_value(), legacy);\n");
				}
			} else {
				if (def.returnType == null) {
					source.append("TTCN_Logger.log_event_str(\"{ } with {} matched\");\n");
				} else {
					source.append("TTCN_Logger.log_event_str(\"{ } with {} matched value \");\n");
					source.append("reply_value.log_match(match_value.getreturn_value(), legacy);\n");
				}
			}
			source.append("}\n");
		}

		source.append("}\n");
	}
}
