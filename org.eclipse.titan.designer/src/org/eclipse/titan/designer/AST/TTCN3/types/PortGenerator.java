/******************************************************************************
 * Copyright (c) 2000-2019 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.core.resources.IProject;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.Module;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.compiler.ProjectSourceCompiler;

/**
 * The code generator part for port types.
 * 
 * @author Kristof Szabados
 * */
public final class PortGenerator {

	// The kind of the testport
	public enum TestportType {NORMAL, INTERNAL, ADDRESS};

	// The kind of the port extension
	public enum PortType {REGULAR, PROVIDER, USER};

	/**
	 * Structure to describe in messages.
	 *
	 * originally port_msg_type is something like this
	 * */
	public static class messageTypeInfo {
		/** Java type name of the message */
		private final String mJavaTypeName;

		/** Java template name of the message */
		private final String mJavaTemplateName;

		/** The name to be displayed to the user */
		private final String mDisplayName;

		/**
		 * @param messageType
		 *                the string representing the value type of this
		 *                message in the generated code.
		 * @param messageTemplate
		 *                the string representing the template type of
		 *                this message in the generated code.
		 * @param displayName
		 *                the string representing the name to be
		 *                displayed for the user.
		 * */
		public messageTypeInfo(final String messageType, final String messageTemplate, final String displayName) {
			mJavaTypeName = messageType;
			mJavaTemplateName = messageTemplate;
			mDisplayName = displayName;
		}
	}

	public static enum MessageMappingType_type {
		SIMPLE, DISCARD, FUNCTION, ENCODE, DECODE
	};

	public static enum FunctionPrototype_Type {
		CONVERT, FAST, BACKTRACK, SLIDING
	};

	/**
	 * Structure to describe message mapping targets of an out parameter.
	 *
	 * originally port_msg_type_mapping_target is something like this
	 * */
	public static class MessageTypeMappingTarget {
		private String targetName;
		private String targetDisplayName;
		public int targetIndex;
		private final MessageMappingType_type mappingType;

		//only relevant for function style mapping
		private String functionDisplayName;
		private String functionName;
		private FunctionPrototype_Type functionPrototype;

		//only relevant for encdec style mapping
		private String encdecTypedesriptorName;
		private String encdecEncodingType;
		private String encdecEncodingOptions;
		private String encdecErrorBehaviour;

		/**
		 * The constructor for discard mapping targets
		 * */
		public MessageTypeMappingTarget() {
			this.mappingType = MessageMappingType_type.DISCARD;
		}

		/**
		 * The constructor for simple mapping targets.
		 *
		 * @param targetType
		 *                the string representing the value type of this
		 *                message in the generated code.
		 * @param displayName
		 *                the string representing the name to be
		 *                displayed for the user.
		 * */
		public MessageTypeMappingTarget(final String targetType, final String displayName) {
			this.targetName = targetType;
			this.targetDisplayName = displayName;
			this.mappingType = MessageMappingType_type.SIMPLE;
		}

		/**
		 * The constructor for function mapping targets.
		 *
		 * @param targetType
		 *                the string representing the value type of this
		 *                message in the generated code.
		 * @param displayName
		 *                the string representing the name to be
		 *                displayed for the user.
		 * @param functionName
		 *                the string representing the name of the
		 *                function.
		 * @param functionDisplayName
		 *                the string representing the function in error
		 *                messages.
		 * @param functionPrototype
		 *                the prototype of the function.
		 * */
		public MessageTypeMappingTarget(final String targetType, final String displayName, final String functionName, final String functionDisplayName, final FunctionPrototype_Type functionPrototype) {
			this.targetName = targetType;
			this.targetDisplayName = displayName;
			this.functionName = functionName;
			this.functionDisplayName = functionDisplayName;
			this.functionPrototype = functionPrototype;

			this.mappingType = MessageMappingType_type.FUNCTION;
		}

		/**
		 * The constructor for function mapping targets.
		 *
		 * @param targetType
		 *                the string representing the value type of this
		 *                message in the generated code.
		 * @param displayName
		 *                the string representing the name to be
		 *                displayed for the user.
		 * @param typedescriptorName
		 *                the string representing the typedescriptor.
		 * @param encodingType
		 *                the string representing the encoding type.
		 * @param encodingOptions
		 *                the string representing the options of the
		 *                encoding type.
		 * @param errorbeviour
		 *                the string representing the errorbehaviour
		 *                setting code.
		 * @param mappingType
		 *                encode or decode
		 * */
		public MessageTypeMappingTarget(final String targetType, final String displayName, final String typedescriptorName, final String encodingType, final String encodingOptions, final String errorbeviour, final MessageMappingType_type mappingType) {
			this.targetName = targetType;
			this.targetDisplayName = displayName;
			this.encdecTypedesriptorName = typedescriptorName;
			this.encdecEncodingType = encodingType;
			this.encdecEncodingOptions = encodingOptions;
			this.encdecErrorBehaviour = errorbeviour;
			this.mappingType = mappingType;
		}
	}

	/**
	 * Structure to describe out messages.
	 *
	 * originally port_msg_mapped_type is something like this
	 * */
	public static class MessageMappedTypeInfo {
		/** Java type name of the message */
		private final String mJavaTypeName;

		/** Java template name of the message */
		private final String mJavaTemplateName;

		/** The name to be displayed to the user */
		public final String mDisplayName;

		private final ArrayList<MessageTypeMappingTarget> targets;

		/**
		 * @param messageType
		 *                the string representing the value type of this
		 *                message in the generated code.
		 * @param messageTemplate
		 *                the string representing the template type of
		 *                this message in the generated code.
		 * @param displayName
		 *                the string representing the name to be
		 *                displayed for the user.
		 * */
		public MessageMappedTypeInfo(final String messageType, final String messageTemplate, final String displayName) {
			mJavaTypeName = messageType;
			mJavaTemplateName = messageTemplate;
			mDisplayName = displayName;
			targets = new ArrayList<PortGenerator.MessageTypeMappingTarget>();
		}

		public void addTarget(final MessageTypeMappingTarget newTarget) {
			this.targets.add(newTarget);
		}

		public void addTargets(final ArrayList<MessageTypeMappingTarget> newTargets) {
			this.targets.addAll(newTargets);
		}
	}

	/**
	 * Structure to describe in and out messages.
	 *
	 * originally port_proc_signature is something like this
	 * */
	public static final class procedureSignatureInfo {
		private final String mJavaTypeName;
		private final String mDisplayName;
		private final boolean isNoBlock;
		private final boolean hasExceptions;
		private final boolean hasReturnValue;

		public procedureSignatureInfo(final String procedureType, final String displayName, final boolean isNoBlock, final boolean hasExceptions, final boolean hasReturnValue) {
			this.mJavaTypeName = procedureType;
			this.mDisplayName = displayName;
			this.isNoBlock = isNoBlock;
			this.hasExceptions = hasExceptions;
			this.hasReturnValue = hasReturnValue;
		}
	}

	/**
	 * Structure to describe message providers.
	 *
	 * originally port_msg_provider is something like this
	 * */
	public static class portMessageProvider {
		final private String name;
		final private ArrayList<String> outMessageTypeNames;
		final private boolean realtime;

		public portMessageProvider(final String name, final ArrayList<String> outMessageTypeNames, final boolean realtime) {
			this.name = name;
			this.outMessageTypeNames = outMessageTypeNames;
			this.realtime = realtime;
		}
	}

	/**
	 * Structure describing all data needed to generate the port.
	 *
	 * originally port_def
	 * */
	public static class PortDefinition {
		/** Java type name of the port */
		public String javaName;

		/** The original name in the TTCN-3 code */
		public String displayName;

		/** The name of address in the actual module */
		public String addressName;

		/** The list of incoming messages */
		public ArrayList<messageTypeInfo> inMessages = new ArrayList<PortGenerator.messageTypeInfo>();

		/** The list of outgoing messages */
		public ArrayList<MessageMappedTypeInfo> outMessages = new ArrayList<PortGenerator.MessageMappedTypeInfo>();

		public ArrayList<procedureSignatureInfo> inProcedures = new ArrayList<PortGenerator.procedureSignatureInfo>();

		public ArrayList<procedureSignatureInfo> outProcedures = new ArrayList<PortGenerator.procedureSignatureInfo>();

		/** The type of the testport */
		public TestportType testportType;
		public PortType portType;

		public ArrayList<portMessageProvider> providerMessageOutList;

		public ArrayList<MessageMappedTypeInfo> providerInMessages = new ArrayList<PortGenerator.MessageMappedTypeInfo>();

		public boolean has_sliding;
		public boolean legacy;

		public StringBuilder varDefs;
		public StringBuilder varInit;
		public StringBuilder translationFunctions = new StringBuilder();
		public boolean realtime;

		public PortDefinition(final String genName, final String displayName) {
			javaName = genName;
			this.displayName = displayName;
		}
	}

	private PortGenerator() {
		// private to disable instantiation
	}

	/**
	 * This function can be used to generate the class of port types
	 *
	 * defPortClass in compiler2/port.{h,c}
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param project
	 *                the project where this port is generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	public static void generateClass(final JavaGenData aData, final StringBuilder source, final IProject project, final PortDefinition portDefinition) {
		aData.addImport("java.text.MessageFormat");
		aData.addBuiltinTypeImport( "TitanComponent");
		aData.addBuiltinTypeImport( "TitanFloat");
		aData.addBuiltinTypeImport( "TitanOctetString");
		aData.addBuiltinTypeImport( "Base_Type" );
		aData.addBuiltinTypeImport( "Base_Template.template_sel" );
		aData.addBuiltinTypeImport("TTCN_Logger");
		aData.addBuiltinTypeImport("TitanLoggerApi");
		aData.addCommonLibraryImport("Text_Buf");
		aData.addBuiltinTypeImport("TtcnError");
		aData.addBuiltinTypeImport("TitanCharString");

		if (portDefinition.testportType != TestportType.ADDRESS) {
			aData.addBuiltinTypeImport( "TitanComponent_template");
		}

		boolean hasIncomingReply = false;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (!info.isNoBlock) {
				hasIncomingReply = true;
			}
		}
		boolean hasIncomingException = false;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (info.hasExceptions) {
				hasIncomingException = true;
			}
		}


		generateDeclaration(aData, source, project, portDefinition);

		for (int i = 0 ; i < portDefinition.outMessages.size(); i++) {
			final MessageMappedTypeInfo outType = portDefinition.outMessages.get(i);

			generateSend(aData, source, outType, portDefinition);
		}

		if (!portDefinition.inMessages.isEmpty()) {
			aData.addBuiltinTypeImport( "TitanAlt_Status" );
			aData.addBuiltinTypeImport("TitanCharString");
			aData.addBuiltinTypeImport("TitanComponent_template");
			aData.addBuiltinTypeImport("Index_Redirect");
			aData.addBuiltinTypeImport("Value_Redirect_Interface");

			generateGenericReceive(source, portDefinition, false, false);
			generateGenericReceive(source, portDefinition, true, false);
			generateGenericTrigger(source, portDefinition, false);

			if (portDefinition.testportType == TestportType.ADDRESS) {
				generateGenericReceive(source, portDefinition, false, true);
				generateGenericReceive(source, portDefinition, true, true);
				generateGenericTrigger(source, portDefinition, true);
			}

			generateProcessMessage(source, portDefinition);
		}

		// generic and simplified receive for experimentation
		for (int i = 0 ; i < portDefinition.inMessages.size(); i++) {
			final messageTypeInfo inType = portDefinition.inMessages.get(i);

			generateTypedReceive(source, portDefinition, i, inType, false);
			generateTypedReceive(source, portDefinition, i, inType, true);
			generateTypeTrigger(source, portDefinition, i, inType);
		}

		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			generateCallFunction(source, info, portDefinition);
		}
		for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

			generateReplyFunction(source, info, portDefinition);
		}
		for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

			generateRaiseFunction(source, info, portDefinition);
		}

		if (portDefinition.portType == PortType.USER) {
			aData.addBuiltinTypeImport("TitanPort");

			//TODO would it be possible to specify the exact port type in special cases?
			source.append("public TitanPort get_provider_port() {\n");
			source.append("get_default_destination();\n");
			if (portDefinition.legacy) {
				source.append("return this;\n");
			} else {
				for (int i = 0; i < portDefinition.providerMessageOutList.size(); i++) {
					source.append(MessageFormat.format("for (int i = 0; i < n_{0}; i++) '{'\n", i));
					source.append(MessageFormat.format("if (p_{0}.get(i) != null) '{'\n", i));
					source.append(MessageFormat.format("return p_{0}.get(i);\n", i));
					source.append("}\n");
					source.append("}\n");
				}

				source.append("return null;\n");
			}
			
			source.append("}\n\n");
		}

		if (portDefinition.portType == PortType.USER && !portDefinition.legacy) {
			aData.addBuiltinTypeImport("TitanPort");
			source.append("public void add_port(final TitanPort port) {\n");
			for (int i = 0; i < portDefinition.providerMessageOutList.size(); i++) {
				final String name = portDefinition.providerMessageOutList.get(i).name;

				source.append(MessageFormat.format("if (port instanceof {0}) '{'\n", name));
				source.append(MessageFormat.format("if (p_{0} == null) '{'\n", i));
				source.append(MessageFormat.format("p_{0} = new ArrayList<{1}>();\n", i, name));
				source.append("}\n");
				source.append(MessageFormat.format("n_{0}++;\n", i));
				source.append(MessageFormat.format("p_{0}.add(({1}) port);\n", i, name));
				source.append("return;\n");
				source.append("}\n");
			}

			source.append("throw new TtcnError(\"Internal error: Adding invalid port type.\");\n");
			source.append("}\n\n");

			source.append("public void remove_port(final TitanPort port) {\n");
			for (int i = 0; i < portDefinition.providerMessageOutList.size(); i++) {
				final String name = portDefinition.providerMessageOutList.get(i).name;

				source.append(MessageFormat.format("if (port instanceof {0}) '{'\n", name));
				source.append(MessageFormat.format("if (p_{0} ==  null) '{'\n", i));
				source.append("return;\n");
				source.append("}\n");
				source.append(MessageFormat.format("if (p_{0}.remove(port)) '{'\n", i));
				source.append(MessageFormat.format("n_{0}--;\n", i));
				source.append("}\n");
				source.append(MessageFormat.format("if (n_{0} == 0) '{'\n", i));
				source.append(MessageFormat.format("p_{0} = null;\n", i));
				source.append("}\n");

				source.append("return;\n");
				source.append("}\n");
			}

			source.append("throw new TtcnError(\"Internal error: Removing invalid port type.\");\n");
			source.append("}\n\n");

			source.append("public boolean in_translation_mode() {\n");
			source.append("return ");
			for (int i = 0; i < portDefinition.providerMessageOutList.size(); i++) {
				if (i > 0) {
					source.append(" || ");
				}
				source.append(MessageFormat.format("n_{0} != 0", i));
			}
			source.append(";\n");
			source.append("}\n\n");

			source.append("public void change_port_state(final translation_port_state state) {\n");
			source.append("port_state = state;\n");
			source.append("}\n\n");

			source.append("protected void reset_port_variables() {\n");
			for (int i = 0; i < portDefinition.providerMessageOutList.size(); i++) {
				source.append(MessageFormat.format("for (int i = 0; i < n_{0}; i++) '{'\n", i));
				source.append(MessageFormat.format("p_{0}.get(i).remove_port(this);\n", i));
				source.append("}\n");
				source.append(MessageFormat.format("p_{0} = null;\n", i));
				source.append(MessageFormat.format("n_{0} = 0;\n", i));
			}
			source.append("}\n\n");
		}

		// Port type variables in the provider types.
		if (portDefinition.portType == PortType.PROVIDER) {
			aData.addBuiltinTypeImport("TitanPort");

			source.append("public void add_port(final TitanPort port) {\n");
			source.append("mapped_ports.add(port);\n");
			source.append("}\n\n");

			source.append("public void remove_port(final TitanPort port) {\n");
			source.append("mapped_ports.remove(port);\n");
			source.append("}\n\n");

			source.append("protected void reset_port_variables() {\n");
			source.append("mapped_ports = new ArrayList<TitanPort>();\n");
			source.append("}\n\n");
		}

		if ((portDefinition.testportType != TestportType.INTERNAL || !portDefinition.legacy) &&
				(portDefinition.portType == PortType.REGULAR || (portDefinition.portType == PortType.USER && !portDefinition.legacy))) {
			if (portDefinition.testportType == TestportType.INTERNAL && !portDefinition.legacy) {
				// Implement set_parameter to remove warnings from it in the TitanPort class.
				source.append("public void set_parameter(final String parameter_name, final String parameter_value) {}\n");
			}

			// only print one outgoing_send for each type
			final HashSet<String> used = new HashSet<String>();
			for (int i = 0 ; i < portDefinition.outMessages.size(); i++) {
				final MessageMappedTypeInfo outMessage = portDefinition.outMessages.get(i);
				boolean found = used.contains(outMessage.mJavaTypeName);
				if (!found) {
					// Internal ports with translation capability do not need the implementable outgoing_send functions.
					if (portDefinition.testportType != TestportType.INTERNAL || portDefinition.legacy) {
						final StringBuilder comment = new StringBuilder();
						final StringBuilder body = new StringBuilder();
						comment.append("\t\t/**\n");
						comment.append(MessageFormat.format("\t\t * Sends a(n) {0} message to the system (SUT).\n", outMessage.mDisplayName));
						comment.append("\t\t * <p>\n");
						comment.append("\t\t * Will also be called if the port does not have connections or mappings,\n");
						comment.append("\t\t * but a message is sent on it.\n");
						comment.append("\t\t *\n");
						comment.append("\t\t * @param send_par\n");
						comment.append("\t\t *            the value to be sent.\n");
						body.append(MessageFormat.format("\t\tprotected abstract void outgoing_send(final {0} send_par", outMessage.mJavaTypeName));
						if (portDefinition.testportType == TestportType.ADDRESS) {
							comment.append("\t\t * @param destination_address\n");
							comment.append("\t\t *            the address to send the message to.\n");
							body.append(MessageFormat.format(", final {0} destination_address", portDefinition.addressName));
						}
						if (portDefinition.realtime) {
							comment.append("\t\t * @param timestamp_redirect\n");
							comment.append("\t\t *            the redirected timestamp if any.\n");
							body.append(", final TitanFloat timestamp_redirect");
						}
						comment.append("\t\t * */\n");
						body.append(");\n\n");
						source.append(comment);
						source.append(body);
					}

					// When port translation is enabled
					// we call the outgoing_mapped_send instead of outgoing_send,
					// and this function will call one of the mapped port's outgoing_send
					// functions, or its own outgoing_send function.
					// This is for the types that are present in the out message list of the port
					if (portDefinition.portType == PortType.USER && !portDefinition.legacy) {
						source.append(MessageFormat.format("public void outgoing_mapped_send(final {0} send_par", outMessage.mJavaTypeName));
						if (portDefinition.testportType == TestportType.ADDRESS) {
							source.append(MessageFormat.format(", final {0} destination_address", portDefinition.addressName));
						}
						if (portDefinition.realtime) {
							source.append(", final TitanFloat timestamp_redirect");
						}
						source.append(") {\n");
						for (int j = 0; j < portDefinition.providerMessageOutList.size(); j++) {
							final portMessageProvider tempMessageProvider = portDefinition.providerMessageOutList.get(j);
							found = false;
							for (int k = 0; k < tempMessageProvider.outMessageTypeNames.size(); k++) {
								if (outMessage.mJavaTypeName.equals(tempMessageProvider.outMessageTypeNames.get(k))) {
									found = true;
									break;
								}
							}

							if (found) {
								// Call outgoing_public_send so the outgoing_send can remain
								source.append(MessageFormat.format("for (int i = 0; i < n_{0}; i++) '{'\n", j));
								source.append(MessageFormat.format("if (p_{0}.get(i) != null) '{'\n", j));
								source.append(MessageFormat.format("p_{0}.get(i).outgoing_public_send(send_par", j));
								if (tempMessageProvider.realtime) {
									if (portDefinition.realtime) {
										source.append(", timestamp_redirect");
									} else {
										source.append(", null");
									}
								}
								source.append(");\n");
								source.append("return;\n");
								source.append("}\n");
								source.append("}\n");
							}
						}

						found = false;
						//TODO this might be always true !
						for (int j = 0; j < portDefinition.outMessages.size(); j++) {
							if (portDefinition.outMessages.get(j).mJavaTypeName.equals(outMessage.mJavaTypeName)) {
								found = true;
								break;
							}
						}
						if (found && (portDefinition.testportType != TestportType.INTERNAL || portDefinition.legacy)) {
							source.append("outgoing_send(send_par");
							if (portDefinition.realtime) {
								source.append(", timestamp_redirect");
							}
							source.append(");\n");
						} else if (portDefinition.testportType == TestportType.INTERNAL && !portDefinition.legacy) {
							source.append("throw new TtcnError(\"Cannot send message without successful mapping on a internal port with translation capability.\");\n");
						} else {
							source.append(MessageFormat.format("throw new TtcnError(\"Cannot send message correctly with type {0}.\");\n", outMessage.mJavaTypeName));
						}

						source.append("}\n\n");
					}

					used.add(outMessage.mJavaTypeName);
				}
			}

			if (portDefinition.portType == PortType.USER && !portDefinition.legacy) {
				for (int i = 0 ; i < portDefinition.outMessages.size(); i++) {
					final MessageMappedTypeInfo outMessage = portDefinition.outMessages.get(i);
					for (int j = 0; j < outMessage.targets.size(); j++) {
						final MessageTypeMappingTarget target = outMessage.targets.get(j);
						boolean found = used.contains(target.targetName);
						if (!found) {
							// When standard like port translated port is present,
							// We call the outgoing_mapped_send instead of outgoing_send,
							// and this function will call one of the mapped port's outgoing_send
							// functions, or its own outgoing_send function.
							// This is for the mapping target types.
							source.append(MessageFormat.format("public void outgoing_mapped_send(final {0} send_par", target.targetName));
							if (portDefinition.testportType == TestportType.ADDRESS) {
								source.append(MessageFormat.format(", final {0} destination_address", portDefinition.addressName));
							}
							if (portDefinition.realtime) {
								source.append(", final TitanFloat timestamp_redirect");
							}
							source.append(") {\n");
							for (int k = 0; k < portDefinition.providerMessageOutList.size(); k++) {
								final portMessageProvider tempMessageProvider = portDefinition.providerMessageOutList.get(k);
								found = false;
								for (int l = 0; l < tempMessageProvider.outMessageTypeNames.size(); l++) {
									if (target.targetName.equals(tempMessageProvider.outMessageTypeNames.get(l))) {
										found = true;
										break;
									}
								}

								if (found) {
									// Call outgoing_public_send so the outgoing_send can remain
									source.append(MessageFormat.format("for (int i = 0; i < n_{0}; i++) '{'\n", k));
									source.append(MessageFormat.format("if (p_{0}.get(i) != null) '{'\n", k));
									source.append(MessageFormat.format("p_{0}.get(i).outgoing_public_send(send_par", k));
									if (tempMessageProvider.realtime) {
										if (portDefinition.realtime) {
											source.append(", timestamp_redirect");
										} else {
											source.append(", null");
										}
									}
									source.append(");\n");
									source.append("return;\n");
									source.append("}\n");
									source.append("}\n");
								}
							}

							found = false;
							for (int k = 0; k < portDefinition.outMessages.size(); k++) {
								if (portDefinition.outMessages.get(k).mJavaTypeName.equals(target.targetName)) {
									found = true;
									break;
								}
							}
							if (found) {
								source.append("outgoing_send(send_par");
								if (portDefinition.realtime) {
									source.append(", timestamp_redirect");
								}
								source.append(");\n");
							} else {
								source.append(MessageFormat.format("throw new TtcnError(\"Cannot send message correctly {0}.\");\n", target.targetName));
							}
							source.append("}\n\n");

							used.add(target.targetName);
						}
					}
				}
			}

			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

				final StringBuilder comment = new StringBuilder();
				final StringBuilder body = new StringBuilder();
				comment.append("\t\t/**\n");
				comment.append(MessageFormat.format("\t\t * Calls a(n) {0} signature of the system (SUT).\n", info.mDisplayName));
				comment.append("\t\t *\n");
				comment.append("\t\t * @param call_par\n");
				comment.append("\t\t *            the signature to be called\n");
				body.append(MessageFormat.format("\t\tpublic abstract void outgoing_call(final {0}_call call_par", info.mJavaTypeName));
				if (portDefinition.testportType == TestportType.ADDRESS) {
					comment.append("\t\t * @param destination_address\n");
					comment.append("\t\t *            the address to call the signature on.\n");
					body.append(MessageFormat.format(", final {0} destination_address", portDefinition.addressName));
				}
				if (portDefinition.realtime) {
					comment.append("\t\t * @param timestamp_redirect\n");
					comment.append("\t\t *            the redirected timestamp if any.\n");
					body.append(", final TitanFloat timestamp_redirect");
				}
				comment.append("\t\t * */\n");
				body.append(");\n\n");
				source.append(comment);
				source.append(body);
			}
			for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

				if (!info.isNoBlock) {
					final StringBuilder comment = new StringBuilder();
					final StringBuilder body = new StringBuilder();
					comment.append("\t\t/**\n");
					comment.append(MessageFormat.format("\t\t * Replies to a(n) {0} signature of the system (SUT).\n", info.mDisplayName));
					comment.append("\t\t *\n");
					comment.append("\t\t * @param reply_template\n");
					comment.append("\t\t *            the signature to be replied\n");
					body.append(MessageFormat.format("\t\tpublic abstract void outgoing_reply(final {0}_reply reply_par", info.mJavaTypeName));
					if (portDefinition.testportType == TestportType.ADDRESS) {
						comment.append("\t\t * @param destination_address\n");
						comment.append("\t\t *            the address to reply to.\n");
						body.append(MessageFormat.format(", final {0} destination_address", portDefinition.addressName));
					}
					if (portDefinition.realtime) {
						comment.append("\t\t * @param timestamp_redirect\n");
						comment.append("\t\t *            the redirected timestamp if any.\n");
						body.append(", final TitanFloat timestamp_redirect");
					}
					comment.append("\t\t * */\n");
					body.append(");\n\n");
					source.append(comment);
					source.append(body);
				}
			}
			for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

				if (info.hasExceptions) {
					final StringBuilder comment = new StringBuilder();
					final StringBuilder body = new StringBuilder();
					comment.append("\t\t/**\n");
					comment.append(MessageFormat.format("\t\t * Raise the exception of {0} signature of the system (SUT).\n", info.mDisplayName));
					comment.append("\t\t *\n");
					comment.append("\t\t * @param raise_exception\n");
					comment.append("\t\t *            the exception to be raised\n");
					body.append(MessageFormat.format("\t\tpublic abstract void outgoing_raise(final {0}_exception raise_exception", info.mJavaTypeName));
					if (portDefinition.testportType == TestportType.ADDRESS) {
						comment.append("\t\t * @param destination_address\n");
						comment.append("\t\t *            the address to raise the exception to.\n");
						body.append(MessageFormat.format(", final {0} destination_address", portDefinition.addressName));
					}
					if (portDefinition.realtime) {
						comment.append("\t\t * @param timestamp_redirect\n");
						comment.append("\t\t *            the redirected timestamp if any.\n");
						body.append(", final TitanFloat timestamp_redirect");
					}
					comment.append("\t\t * */\n");
					body.append(");\n\n");
					source.append(comment);
					source.append(body);
				}
			}
		}

		if (portDefinition.portType == PortType.PROVIDER) {
			for (int i = 0; i < portDefinition.outMessages.size(); i++) {
				source.append(MessageFormat.format("\t\tpublic void outgoing_public_send(final {0} send_par", portDefinition.outMessages.get(i).mJavaTypeName));
				if (portDefinition.realtime) {
					source.append(", final TitanFloat timestamp_redirect");
				}
				source.append(") {\n");
				source.append("\t\t\toutgoing_send(send_par");
				if (portDefinition.realtime) {
					source.append(", timestamp_redirect");
				}
				source.append(");\n");
				source.append("\t\t}\n\n");
			}
		}

		if (!portDefinition.inProcedures.isEmpty()) {
			aData.addBuiltinTypeImport("Index_Redirect");
			aData.addBuiltinTypeImport( "TitanAlt_Status" );

			generateGenericGetcall(source, portDefinition, false, false);
			generateGenericGetcall(source, portDefinition, true, false);
			if (portDefinition.testportType == TestportType.ADDRESS) {
				generateGenericGetcall(source, portDefinition, false, true);
				generateGenericGetcall(source, portDefinition, true, true);
			}

			for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

				generateTypedGetcall(source, portDefinition, i, info, false, false);
				generateTypedGetcall(source, portDefinition, i, info, true, false);
				if (portDefinition.testportType == TestportType.ADDRESS) {
					generateTypedGetcall(source, portDefinition, i, info, false, true);
					generateTypedGetcall(source, portDefinition, i, info, true, true);
				}
			}
		}

		if (hasIncomingReply) {
			aData.addBuiltinTypeImport( "TitanAlt_Status" );
			aData.addBuiltinTypeImport("Index_Redirect");

			generateGenericGetreply(source, portDefinition, false, false);
			generateGenericGetreply(source, portDefinition, true, false);
			if (portDefinition.testportType == TestportType.ADDRESS) {
				generateGenericGetreply(source, portDefinition, false, true);
				generateGenericGetreply(source, portDefinition, true, true);
			}

			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

				if (!portDefinition.outProcedures.get(i).isNoBlock) {
					generateTypedGetreply(source, portDefinition, i, info, false, false);
					generateTypedGetreply(source, portDefinition, i, info, true, false);
					if (portDefinition.testportType == TestportType.ADDRESS) {
						generateTypedGetreply(source, portDefinition, i, info, false, true);
						generateTypedGetreply(source, portDefinition, i, info, true, true);
					}
				}
			}
		}

		if (hasIncomingException) {
			aData.addBuiltinTypeImport( "TitanAlt_Status" );

			generateGenericGetexception(source, portDefinition, false, false);
			generateGenericGetexception(source, portDefinition, true, false);
			if (portDefinition.testportType == TestportType.ADDRESS) {
				generateGenericGetexception(source, portDefinition, false, true);
				generateGenericGetexception(source, portDefinition, true, true);
			}

			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

				if (portDefinition.outProcedures.get(i).hasExceptions) {
					generateTypedGetexception(source, portDefinition, i, info, false, false);
					generateTypedGetexception(source, portDefinition, i, info, true, false);
					if (portDefinition.testportType == TestportType.ADDRESS) {
						generateTypedGetexception(source, portDefinition, i, info, false, true);
						generateTypedGetexception(source, portDefinition, i, info, true, true);
					}
				}
			}
		}

		if (portDefinition.portType == PortType.USER) {
			for (int i = 0 ; i < portDefinition.providerInMessages.size(); i++) {
				final MessageMappedTypeInfo inType = portDefinition.providerInMessages.get(i);

				generateTypedIncommingMessageUser(aData, source, i, inType, portDefinition);
			}

			if (!portDefinition.legacy && !portDefinition.providerInMessages.isEmpty()) {
				source.append("@Override\n");
				source.append("public boolean incoming_message_handler(final Base_Type message_ptr, final String message_type, final int sender_component, final TitanFloat timestamp) {\n");
				for (int i = 0; i < portDefinition.providerInMessages.size(); i++) {
					final MessageMappedTypeInfo inType = portDefinition.providerInMessages.get(i);

					if (i > 0) {
						source.append(" else ");
					}

					source.append(MessageFormat.format("if (\"{0}\".equals(message_type)) '{'\n", inType.mDisplayName));
					source.append(MessageFormat.format("if (message_ptr instanceof {0}) '{'\n", inType.mJavaTypeName));
					source.append(MessageFormat.format("incoming_message(({0})message_ptr, sender_component);\n", inType.mJavaTypeName));
					source.append("return true;\n");
					source.append("}\n");
					source.append(MessageFormat.format("throw new TtcnError(\"Internal error: Type of message in incoming message handler function is not the indicated `{0}''.\");\n", inType.mDisplayName));
					source.append('}');
				}
				source.append('\n');
				source.append("return false;\n");
				source.append("}\n\n");
			}
		} else {
			for (int i = 0 ; i < portDefinition.inMessages.size(); i++) {
				final messageTypeInfo inType = portDefinition.inMessages.get(i);


				generateTypedIncommingMessageProvider(aData, source, i, inType, portDefinition);
			}
		}

		for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

			generateTypedIcomingCall(source, i, info, portDefinition);
		}

		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (!info.isNoBlock) {
				generateTypedIcomingReply(source, i, info, portDefinition);
			}
		}

		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (info.hasExceptions) {
				generateTypedIcomingException(source, i, info, portDefinition);
			}
		}

		if (!portDefinition.inProcedures.isEmpty()) {
			generateProcessCall(source, portDefinition);
		}
		if (hasIncomingReply) {
			generateProcessReply(source, portDefinition);
		}
		if (hasIncomingException) {
			generateProcessException(source, portDefinition);
		}

		source.append("\t}\n\n");
	}

	/**
	 * This function generates and returns the name of the class
	 * representing the port type. In some cases this is generated by us, in
	 * other cases the user has to provide it.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param myScope
	 *                the scope of the port.
	 * */
	public static String getClassName(final JavaGenData aData, final StringBuilder source, final PortDefinition portDefinition, final Scope myScope) {
		String className;
		if (portDefinition.testportType == TestportType.INTERNAL) {
			final StringBuilder returnValue = new StringBuilder();
			final Module myModule = myScope.getModuleScopeGen();
			if(!myModule.equals(aData.getModuleScope())) {
				//when the definition is referred from another module
				// the reference shall be qualified with the namespace of my module
				returnValue.append(myModule.getName()).append('.');
			}

			returnValue.append(portDefinition.javaName);

			className = returnValue.toString();
		} else {
			switch (portDefinition.portType) {
			case USER:
				if (portDefinition.legacy) {
					final StringBuilder returnValue = new StringBuilder();
					final Module myModule = myScope.getModuleScopeGen();
					if(!myModule.equals(aData.getModuleScope())) {
						//when the definition is referred from another module
						// the reference shall be qualified with the namespace of my module
						returnValue.append(myModule.getName()).append('.');
					}

					returnValue.append(portDefinition.javaName);

					className = returnValue.toString();

					break;
				}
				// else fall through
			case REGULAR:{
				className = portDefinition.javaName;

				final IProject generatingProject = myScope.getModuleScopeGen().getProject();
				aData.addImport(ProjectSourceCompiler.getPackageUserProvidedRoot(generatingProject) + "." + portDefinition.javaName);
				break;
			}
			case PROVIDER: {
				final StringBuilder returnValue = new StringBuilder();
				final Module myModule = myScope.getModuleScopeGen();
				if(!myModule.equals(aData.getModuleScope())) {
					//when the definition is referred from another module
					// the reference shall be qualified with the namespace of my module
					returnValue.append(myModule.getName()).append('.');
				}

				returnValue.append(portDefinition.javaName);

				className = returnValue.toString();
				break;
			}
			default:
				className = "invalid port type";

				break;
			}
		}

		return className;
	}

	/**
	 * This function generates the declaration of the generated port type
	 * class.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param project
	 *                the project where this port is generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateDeclaration(final JavaGenData aData, final StringBuilder source, final IProject project, final PortDefinition portDefinition) {
		String className;
		String baseClassName;
		String abstractNess = "";
		if (portDefinition.testportType == TestportType.INTERNAL) {
			className = portDefinition.javaName;
			baseClassName = "TitanPort";

			aData.addBuiltinTypeImport( "TitanPort" );
		} else {
			switch (portDefinition.portType) {
			case USER:
				if (portDefinition.legacy) {
					className = portDefinition.javaName;

					baseClassName = portDefinition.providerMessageOutList.get(0).name + "_PROVIDER";

					//aData.addImport(ProjectSourceCompiler.getPackageUserProvidedRoot(project) + "." + baseClassName);
					break;
				}
				// else fall through
			case REGULAR:
				className = portDefinition.javaName + "_BASE";
				baseClassName = "TitanPort";
				abstractNess = " abstract";

				aData.addBuiltinTypeImport( "TitanPort" );
				break;
			case PROVIDER:
				className = portDefinition.javaName;
				baseClassName = portDefinition.javaName + "_PROVIDER";

				aData.addImport(ProjectSourceCompiler.getPackageUserProvidedRoot(project) + "." + baseClassName);
				break;
			default:
				className = "invalid port type";
				baseClassName = "invalid port type";
				break;
			}
		}

		source.append(MessageFormat.format("\tpublic static{0} class {1} extends {2} '{'\n", abstractNess, className, baseClassName));

		if(!portDefinition.inMessages.isEmpty()) {
			source.append("\t\tenum message_selection { ");
			for (int i = 0 ; i < portDefinition.inMessages.size(); i++) {
				if (i > 0) {
					source.append(", ");
				}
				source.append(MessageFormat.format("MESSAGE_{0}", i));
			}
			source.append("};\n");

			source.append("\t\tprivate class Message_queue_item {\n");
			source.append("\t\t\tmessage_selection item_selection;\n");
			source.append("\t\t\t// base type could be: ");
			for (int i = 0 ; i < portDefinition.inMessages.size(); i++) {
				final messageTypeInfo inType = portDefinition.inMessages.get(i);

				if (i > 0) {
					source.append(", ");
				}
				source.append(inType.mJavaTypeName);
			}
			source.append('\n');
			source.append("\t\t\tBase_Type message;\n");
			source.append("\t\t\tint sender_component;\n");
			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append(MessageFormat.format("\t\t\t{0} sender_address;\n", portDefinition.addressName));
			}
			if (portDefinition.realtime) {
				source.append("\t\t\tTitanFloat timestamp;\n");
			}
			source.append("\t\t}\n");

			if (portDefinition.has_sliding) {
				source.append("\t\tTitanOctetString sliding_buffer;\n");
			}

			aData.addImport("java.util.LinkedList");

			source.append("\t\tprivate final LinkedList<Message_queue_item> message_queue = new LinkedList<Message_queue_item>();\n\n");

			source.append("\t\tprivate void remove_msg_queue_head() {\n");
			source.append("\t\t\tmessage_queue.removeFirst();\n");
			source.append("\t\t}\n\n");
		}

		final boolean hasIncomingCall = !portDefinition.inProcedures.isEmpty();
		boolean hasIncomingReply = false;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			if (!portDefinition.outProcedures.get(i).isNoBlock) {
				hasIncomingReply = true;
			}
		}
		boolean hasIncomingException = false;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			if (portDefinition.outProcedures.get(i).hasExceptions) {
				hasIncomingException = true;
			}
		}

		final boolean hasProcedureQueue = hasIncomingCall || hasIncomingReply || hasIncomingException;
		if (hasProcedureQueue) {
			source.append("\t\tenum proc_selection { ");
			boolean isFirst = true;
			for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
				if (!isFirst) {
					source.append(", ");
				}
				isFirst = false;
				source.append(MessageFormat.format("CALL_{0}", i));
			}
			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				if (!portDefinition.outProcedures.get(i).isNoBlock) {
					if (!isFirst) {
						source.append(", ");
					}
					isFirst = false;
					source.append(MessageFormat.format("REPLY_{0}", i));
				}
			}
			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				if (portDefinition.outProcedures.get(i).hasExceptions) {
					if (!isFirst) {
						source.append(", ");
					}
					isFirst = false;
					source.append(MessageFormat.format("EXCEPTION_{0}", i));
				}
			}
			source.append("};\n");

			source.append("\t\tprivate class Procedure_queue_item {\n");
			source.append("\t\t\tproc_selection item_selection;\n");
			source.append("\t\t\t// TODO check if an object would be enough \n");
			for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
				source.append(MessageFormat.format("\t\t\t{0}_call call_{1};\n", portDefinition.inProcedures.get(i).mJavaTypeName, i));
			}
			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.outProcedures.get(i);
				if (!info.isNoBlock) {
					source.append(MessageFormat.format("\t\t\t{0}_reply reply_{1};\n", info.mJavaTypeName, i));
				}
			}
			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.outProcedures.get(i);
				if (info.hasExceptions) {
					source.append(MessageFormat.format("\t\t\t{0}_exception exception_{1};\n", info.mJavaTypeName, i));
				}
			}
			source.append("\t\t\tint sender_component;\n");
			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append(MessageFormat.format("\t\t\t{0} sender_address;\n", portDefinition.addressName));
			}
			if (portDefinition.realtime) {
				source.append("\t\t\tTitanFloat timestamp;\n");
			}
			source.append("\t\t}\n");
			
			if (portDefinition.has_sliding) {
				source.append("\t\tTitanOctetString sliding_buffer;\n");
			}

			aData.addImport("java.util.LinkedList");

			source.append("\t\tprivate final LinkedList<Procedure_queue_item> procedure_queue = new LinkedList<Procedure_queue_item>();\n");
			source.append("\t\tprivate void remove_proc_queue_head() {\n");
			source.append("\t\t\tprocedure_queue.removeFirst();\n");
			source.append("\t\t\tTTCN_Logger.log_port_queue(TitanLoggerApi.Port__Queue_operation.enum_type.extract__op, get_name(), 0 , ++proc_head_count, new TitanCharString(\"\"), new TitanCharString(\"\"));\n");
			source.append("\t\t}\n\n");
		}

		if (!portDefinition.inMessages.isEmpty() || hasProcedureQueue) {
			source.append("\t\tprotected void clear_queue() {\n");
			if(!portDefinition.inMessages.isEmpty()) {
				source.append("\t\t\tmessage_queue.clear();\n");
			}
			if (hasProcedureQueue) {
				source.append("\t\t\tprocedure_queue.clear();\n");
			}
			if (portDefinition.has_sliding) {
				source.append("\t\t\tsliding_buffer = new TitanOctetString(\"\");\n");
			}
			source.append("\t\t}\n\n");
		}

		if (portDefinition.portType == PortType.USER && !portDefinition.legacy) {
			for (int i = 0; i < portDefinition.providerMessageOutList.size(); i++) {
				source.append(MessageFormat.format("\t\tprivate ArrayList<{0}> p_{1};\n", portDefinition.providerMessageOutList.get(i).name, i));
				source.append(MessageFormat.format("\t\tprivate int n_{0};\n", i));
			}
			source.append("\t\tprivate translation_port_state port_state = translation_port_state.UNSET;\n");

			if (portDefinition.varDefs != null) {
				source.append(portDefinition.varDefs);
			}
			if (portDefinition.varInit != null) {
				source.append('\n');
				source.append("\t\t@Override\n");
				source.append("\t\tprotected void init_port_variables() {\n");
				source.append(portDefinition.varInit);
				source.append("\t\t}\n\n");
			}

			source.append("\t\t//translation functions with port clause belonging to this port type\n");
			source.append(portDefinition.translationFunctions);
		}

		if (portDefinition.portType == PortType.PROVIDER) {
			aData.addBuiltinTypeImport("TitanPort");

			source.append("\t\tprivate ArrayList<TitanPort> mapped_ports;\n");
		}

		source.append("\t\t/**\n");
		source.append("\t\t * Constructor of the Test Port.\n");
		source.append("\t\t * <p>\n");
		source.append("\t\t * The name of the port is set to \"<unknown>\". The port is not start or\n");
		source.append("\t\t * active.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param port_name\n");
		source.append("\t\t *                the name of the port to be used, {@code null} can be\n");
		source.append("\t\t *                used to indicate unnamed ports.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tpublic {0}( final String port_name) '{'\n", className));
		source.append("\t\t\tsuper(port_name);\n");
		if (portDefinition.has_sliding) {
			source.append("\t\t\tsliding_buffer = new TitanOctetString(\"\");\n");
		}
		if (portDefinition.portType == PortType.USER && !portDefinition.legacy) {
			for (int i = 0; i < portDefinition.providerMessageOutList.size(); i++) {
				source.append(MessageFormat.format("\t\t\tp_{0} = null;\n", i));
				source.append(MessageFormat.format("\t\t\tn_{0} = 0;\n", i));
			}

			source.append("\t\t\tport_state = translation_port_state.UNSET;\n");
		}

		if (portDefinition.portType == PortType.PROVIDER) {
			source.append("\t\t\tmapped_ports = new ArrayList<TitanPort>();\n");
		}
		source.append("\t\t}\n\n");

		// the default argument is needed if the generated class implements the port type (i.e. it is not a base class)
		source.append(MessageFormat.format("\t\tpublic {0}( ) '{'\n", className));
		source.append(MessageFormat.format("\t\t\tthis((String)null);\n", className));
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the sending functions.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param mappedType
	 *                the information about the outgoing message.
	 * @param hasAddress
	 *                {@code true} if the type has address
	 * */
	private static void generateSendMapping(final JavaGenData aData, final StringBuilder source, final PortDefinition portDefinition, final MessageMappedTypeInfo mappedType, final boolean hasAddress) {
		boolean hasBuffer = false;
		boolean hasDiscard = false;
		boolean reportError = false;
		if (portDefinition.testportType == TestportType.INTERNAL && portDefinition.legacy) {
			source.append("if (destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF)) {\n");
			source.append("throw new TtcnError(MessageFormat.format(\"Message cannot be sent to system on internal port {0}.\", get_name()));\n");
			source.append("}\n");
		}

		for (int i = 0; i < mappedType.targets.size(); i++) {
			final MessageTypeMappingTarget target = mappedType.targets.get(i);
			boolean hasCondition = false;
			if (target.mappingType == MessageMappingType_type.DISCARD) {
				/* "discard" should always be the last mapping */
				hasDiscard = true;
				break;
			} else if(target.mappingType == MessageMappingType_type.DECODE && !hasBuffer) {
				aData.addBuiltinTypeImport("TTCN_Buffer");

				source.append("TTCN_Buffer ttcn_buffer = new TTCN_Buffer(send_par);\n");
				/* has_buffer will be set to TRUE later */
			}
			if (!portDefinition.legacy && portDefinition.portType == PortType.USER) {
				// Mappings should only happen if the port it is mapped to has the same outgoing message type as the mapping target.
				source.append("if (false");
				for (int j = 0; j < portDefinition.providerMessageOutList.size(); j++) {
					final portMessageProvider provider = portDefinition.providerMessageOutList.get(j);
					for (int k = 0; k < provider.outMessageTypeNames.size(); k++) {
						if (target.targetName.equals(provider.outMessageTypeNames.get(k))) {
							source.append(MessageFormat.format(" || n_{0} != 0", j));
						}
					}
				}
				source.append(") {\n");
				// Beginning of the loop of the PARTIALLY_TRANSLATED case to process all messages
				source.append("do {\n");
				source.append("TTCN_Runtime.set_translation_mode(true, this);\n");
				source.append("TTCN_Runtime.set_port_state(-1, \"by test environment\", true);\n");
			}
			if (mappedType.targets.size() > 1) {
				source.append("{\n");
			}
			switch (target.mappingType) {
			case FUNCTION:
				source.append(MessageFormat.format("// out mapping with a prototype({0}) function\n", target.functionPrototype.name()));
				switch (target.functionPrototype) {
				case CONVERT:
					source.append(MessageFormat.format("{0} mapped_par = {1}(send_par);\n", target.targetName, target.functionName));
					break;
				case FAST:
					source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
					source.append(MessageFormat.format("{0}(send_par, mapped_par);\n", target.functionName));
					if (!portDefinition.legacy) {
						hasCondition = true;
					}
					break;
				case SLIDING:
					aData.addBuiltinTypeImport("TitanOctetString");

					source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
					source.append("TitanOctetString send_copy = new TitanOctetString(send_par);\n");
					source.append(MessageFormat.format("if ({0}(send_copy, mapped_par).operator_not_equals(1)) '{'\n", target.functionName));
					hasCondition = true;
					break;
				case BACKTRACK:
					source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
					source.append(MessageFormat.format("if({0}(send_par, mapped_par).operator_equals(0)) '{'\n", target.functionName));
					hasCondition = true;
					break;
				default:
					break;
				}
				break;
			case ENCODE:
				aData.addBuiltinTypeImport("TTCN_Buffer");
				aData.addBuiltinTypeImport("TTCN_EncDec");

				source.append("// out mapping with a built-in encoder\n");
				source.append(target.encdecErrorBehaviour);
				source.append("final TTCN_Buffer ttcn_buffer = new TTCN_Buffer();\n");
				if (target.encdecEncodingOptions == null)  {
					source.append(MessageFormat.format("send_par.encode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, 0);\n", target.encdecTypedesriptorName, target.encdecEncodingType));
				} else {
					source.append(MessageFormat.format("send_par.encode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, {2});\n", target.encdecTypedesriptorName, target.encdecEncodingType, target.encdecEncodingOptions));
				}
				source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
				source.append("ttcn_buffer.get_string(mapped_par);\n");
				break;
			case DECODE:
				aData.addBuiltinTypeImport("TTCN_Buffer");
				aData.addBuiltinTypeImport("TTCN_EncDec");

				source.append("// out mapping with a built-in decoder\n");
				if (hasBuffer) {
					source.append("ttcn_buffer.rewind();\n");
				} else {
					hasBuffer = true;
				}
				source.append(target.encdecErrorBehaviour);
				source.append("TTCN_EncDec.clear_error();\n");
				source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
				if (target.encdecEncodingOptions == null) {
					source.append(MessageFormat.format("mapped_par.decode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, 0);\n", target.encdecTypedesriptorName, target.encdecEncodingType));
				} else {
					source.append(MessageFormat.format("mapped_par.decode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, {2});\n", target.encdecTypedesriptorName, target.encdecEncodingType, target.encdecEncodingOptions));
				}
				source.append("if (TTCN_EncDec.get_last_error_type() == TTCN_EncDec.error_type.ET_NONE) {\n");
				hasCondition = true;
				break;
			default:
				break;
			}

			if (!portDefinition.legacy && portDefinition.portType == PortType.USER) {
				source.append("TTCN_Runtime.set_translation_mode(false, null);\n");
				source.append("if (port_state == translation_port_state.TRANSLATED || port_state == translation_port_state.PARTIALLY_TRANSLATED) {\n");
			}

			source.append("if (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_DUALSEND)) {\n");
			source.append("TTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_DUALSEND);\n");
			source.append("mapped_par.log();\n");
			source.append(MessageFormat.format("TTCN_Logger.log_dualport_map(false, \"{0}\", TTCN_Logger.end_event_log2str(), 0);\n", target.targetDisplayName));
			source.append("}\n");
			if (hasAddress) {
				source.append("outgoing_send(mapped_par, destination_address);\n");
			} else {
				if (portDefinition.testportType != TestportType.INTERNAL || !portDefinition.legacy) {
					source.append("if (destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF)) {\n");
					source.append(MessageFormat.format("outgoing_{0}send(mapped_par", portDefinition.portType == PortType.USER && !portDefinition.legacy ? "mapped_": ""));
					if (portDefinition.testportType == TestportType.ADDRESS) {
						source.append(", null");
					}
					source.append(");\n");
					source.append("} else {\n");
				}

				source.append("final Text_Buf text_buf = new Text_Buf();\n");
				source.append(MessageFormat.format("prepare_message(text_buf, \"{0}\");\n", target.targetDisplayName));
				source.append("send_par.encode_text(text_buf);\n");
				source.append("send_data(text_buf, destination_component);\n");
				if (portDefinition.testportType != TestportType.INTERNAL || !portDefinition.legacy) {
					source.append("}\n");
				}
			}
			if (hasCondition) {
				if (!portDefinition.legacy && portDefinition.portType == PortType.USER) {
					source.append("if (port_state != translation_port_state.PARTIALLY_TRANSLATED) {\n");
					source.append("return;\n");
					source.append("}\n");
				} else {
					source.append("return;\n");
				}
				if (portDefinition.legacy) {
					source.append("}\n");
				}
				reportError = true;
			}

			if (!portDefinition.legacy && portDefinition.portType == PortType.USER) {
				source.append("} else if (port_state == translation_port_state.FRAGMENTED || port_state == translation_port_state.DISCARDED) {\n");
				source.append("return;\n");
				source.append("} else if (port_state == translation_port_state.UNSET) {\n");
				source.append(MessageFormat.format("throw new TtcnError(MessageFormat.format(\"The state of the port '{'0'}' remained unset after the mapping function {0} finished.\", get_name()));\n", target.functionDisplayName));
				source.append("}\n");
			}
			if (mappedType.targets.size() > 1) {
				source.append("}\n");
			}
			if (!portDefinition.legacy && portDefinition.portType == PortType.USER) {
				// End of the do while loop to process all the messages
				source.append("} while (port_state == translation_port_state.PARTIALLY_TRANSLATED);\n");
				// end of the outgoing messages of port with mapping target check
				source.append("}\n");
			}
		}
		if (hasDiscard) {
			if (mappedType.targets.size() > 1) {
				/* there are other mappings, which failed */
				source.append(MessageFormat.format("TTCN_Logger.log_dualport_discard(false, \"{0}\", get_name(), true);\n", mappedType.mDisplayName));
			} else {
				/* this is the only mapping */
				source.append(MessageFormat.format("TTCN_Logger.log_dualport_discard(false, \"{0}\", get_name(), false);\n", mappedType.mDisplayName));
			}
		} else if (reportError) {
			source.append(MessageFormat.format("throw new TtcnError(MessageFormat.format(\"Outgoing message of type {0} could not be handled by the type mapping rules on port '{'0'}'.\", get_name()));\n", mappedType.mDisplayName));
		}
	}

	/**
	 * This function generates the sending functions.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param outType
	 *                the information about the outgoing message.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateSend(final JavaGenData aData, final StringBuilder source, final MessageMappedTypeInfo outType, final PortDefinition portDefinition) {
		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Sends a(n) {0} message to the provided component.\n", outType.mDisplayName));
		source.append("\t\t * <p>\n");
		source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
		source.append("\t\t * available in the timestamp_Redirect parameter.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param send_par\n");
		source.append("\t\t *            the value to be sent.\n");
		source.append("\t\t * @param destination_component\n");
		source.append("\t\t *            the target component to send the message to.\n");
		source.append("\t\t * @param timestamp_redirect\n");
		source.append("\t\t *            the redirected timestamp if any.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tpublic void send(final {0} send_par, final TitanComponent destination_component, final TitanFloat timestamp_redirect) '{'\n", outType.mJavaTypeName));
		source.append("\t\t\tif (!is_started) {\n");
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Sending a message on port {0}, which is not started.\", get_name()));\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (!destination_component.is_bound()) {\n");
		source.append("\t\t\t\tthrow new TtcnError(\"Unbound component reference in the to clause of send operation.\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal TTCN_Logger.Severity log_severity = destination_component.get_component() == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_MMSEND : TTCN_Logger.Severity.PORTEVENT_MCSEND;\n");
		source.append("\t\t\tif (TTCN_Logger.log_this_event(log_severity)) {\n");
		source.append("\t\t\t\tTTCN_Logger.begin_event(log_severity);\n");
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_event_str(\" {0} : \");\n", outType.mDisplayName));
		source.append("\t\t\t\tsend_par.log();\n");
		source.append("\t\t\t\tTTCN_Logger.log_msgport_send(get_name(), destination_component.get_component(), TTCN_Logger.end_event_log2str());\n");
		source.append("\t\t\t}\n");
		if (portDefinition.portType != PortType.USER || (outType.targets.size() == 1 && outType.targets.get(0).mappingType == MessageMappingType_type.SIMPLE)
				|| (portDefinition.portType == PortType.USER && !portDefinition.legacy)) {
			// If not in translation mode then send message as normally would.
			if (portDefinition.portType == PortType.USER && !portDefinition.legacy && (
					outType.targets.size() > 1 || (!outType.targets.isEmpty() && outType.targets.get(0).mappingType != MessageMappingType_type.SIMPLE))) {
				source.append("if (!in_translation_mode()) {\n");
			}
			/* the same message type goes through the external interface */
			source.append("\t\t\tif (destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF)) {\n");
			if (portDefinition.testportType == TestportType.INTERNAL) {
				source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Message cannot be sent to system on internal port {0}.\", get_name()));\n");
			} else {
				source.append("\t\t\t\tget_default_destination();\n");
				source.append(MessageFormat.format("\t\t\t\toutgoing_{0}send(send_par", portDefinition.portType == PortType.USER && !portDefinition.legacy ? "mapped_": ""));
				if (portDefinition.testportType == TestportType.ADDRESS) {
					source.append(", null");
				}
				if (portDefinition.realtime) {
					source.append(", timestamp_redirect");
				}
				source.append(");\n");
			}
			source.append("\t\t\t} else {\n");
			source.append("\t\t\t\tfinal Text_Buf text_buf = new Text_Buf();\n");
			source.append(MessageFormat.format("\t\t\t\tprepare_message(text_buf, \"{0}\");\n",outType.mDisplayName));
			source.append("\t\t\t\tsend_par.encode_text(text_buf);\n");
			source.append("\t\t\t\tsend_data(text_buf, destination_component);\n");
			source.append("\t\t\t}\n");

			if (portDefinition.portType == PortType.USER && !portDefinition.legacy && (
					outType.targets.size() > 1 || (!outType.targets.isEmpty() && outType.targets.get(0).mappingType != MessageMappingType_type.SIMPLE))) {
				source.append("} else {\n");
				generateSendMapping(aData, source, portDefinition, outType, false);
				source.append("}\n");
			}
		} else {
			/* the message type is mapped to another outgoing type of the external interface */
			generateSendMapping(aData, source, portDefinition, outType, false);
		}
		source.append("\t\t}\n\n");

		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append(MessageFormat.format("\t\tpublic void send(final {0} send_par, final {1} destination_address, final TitanFloat timestamp_redirect) '{'\n", outType.mJavaTypeName, portDefinition.addressName));
			source.append("\t\t\tif (!is_started) {\n");
			source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Sending a message on port {0}, which is not started.\", get_name()));\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_DUALSEND)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_DUALSEND);\n");
			source.append("\t\t\t\tsend_par.log();\n");
			source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_dualport_map(false, \"{0}\", TTCN_Logger.end_event_log2str(), 0);\n ",outType.mDisplayName));
			source.append("\t\t\t}\n\n");
			source.append("\t\t\tget_default_destination();\n");
			if (portDefinition.portType != PortType.USER || (outType.targets.size() == 1 && outType.targets.get(0).mappingType == MessageMappingType_type.SIMPLE)) {
				source.append("\t\t\toutgoing_send(send_par, destination_address");
				if (portDefinition.realtime) {
					source.append(", timestamp_redirect");
				}
				source.append(");\n");
			} else {
				generateSendMapping(aData, source, portDefinition, outType, true);
			}
			source.append("\t\t}\n\n");
		}

		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Sends a(n) {0} message to the default component.\n", outType.mDisplayName));
		source.append("\t\t * <p>\n");
		source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
		source.append("\t\t * available in the timestamp_Redirect parameter.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param send_par\n");
		source.append("\t\t *            the value to be sent.\n");
		source.append("\t\t * @param timestamp_redirect\n");
		source.append("\t\t *            the redirected timestamp if any.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tpublic void send(final {0} send_par, final TitanFloat timestamp_redirect) '{'\n", outType.mJavaTypeName));
		source.append("\t\t\tsend(send_par, new TitanComponent(get_default_destination()), timestamp_redirect);\n");
		source.append("\t\t}\n\n");

		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Sends a(n) {0} template message to the provided component.\n", outType.mDisplayName));
		source.append("\t\t * <p>\n");
		source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
		source.append("\t\t * available in the timestamp_Redirect parameter.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param send_par\n");
		source.append("\t\t *            the template to be sent.\n");
		source.append("\t\t * @param destination_component\n");
		source.append("\t\t *            the target component to send the message to.\n");
		source.append("\t\t * @param timestamp_redirect\n");
		source.append("\t\t *            the redirected timestamp if any.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tpublic void send(final {0} send_par, final TitanComponent destination_component, final TitanFloat timestamp_redirect) '{'\n", outType.mJavaTemplateName));
		source.append(MessageFormat.format("\t\t\tfinal {0} send_par_value = send_par.valueof();\n", outType.mJavaTypeName));
		source.append("\t\t\tsend(send_par_value, destination_component, timestamp_redirect);\n");
		source.append("\t\t}\n\n");

		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append(MessageFormat.format("\t\tpublic void send(final {0} send_par, final {1} destination_address, final TitanFloat timestamp_redirect) '{'\n", outType.mJavaTemplateName, portDefinition.addressName));
			source.append(MessageFormat.format("\t\t\tfinal {0} send_par_value = send_par.valueof();\n", outType.mJavaTypeName));
			source.append("\t\t\tsend(send_par_value, destination_address, timestamp_redirect);\n");
			source.append("\t\t}\n\n");
		}

		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Sends a(n) {0} template message to the default component.\n", outType.mDisplayName));
		source.append("\t\t * <p>\n");
		source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
		source.append("\t\t * available in the timestamp_Redirect parameter.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param send_par\n");
		source.append("\t\t *            the template to be sent.\n");
		source.append("\t\t * @param timestamp_redirect\n");
		source.append("\t\t *            the redirected timestamp if any.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tpublic void send(final {0} send_par, final TitanFloat timestamp_redirect) '{'\n", outType.mJavaTemplateName));
		source.append(MessageFormat.format("\t\t\tfinal {0} send_par_value = send_par.valueof();\n", outType.mJavaTypeName));
		source.append("\t\t\tsend(send_par_value, new TitanComponent(get_default_destination()), timestamp_redirect);\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the generic receive or check(receive)
	 * function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateGenericReceive(final StringBuilder source, final PortDefinition portDefinition, final boolean isCheck, final boolean isAddress) {
		final String functionName = isCheck ? "check_receive" : "receive";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";
		final String logger_operation = isCheck ? "check__receive__op" : "receive__op";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template sender_template, final {1} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, senderType));
		source.append("\t\t\tif (message_queue.isEmpty()) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\tfinal Message_queue_item my_head = message_queue.getFirst();\n");
		source.append("\t\t\tif (my_head == null) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");

		if (isAddress) {
			source.append("\t\t\tif (my_head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_MMUNSUCC, MessageFormat.format(\"Matching on port {0} failed: Sender of the first message in the queue is not the system.\" ,get_name()));\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (my_head.sender_address == null) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"{0} operation on port '{'0'}' requires the address of the sender, which was not given by the test port.\", get_name()));\n", functionName));
			source.append("\t\t\t} else if (!sender_template.match(my_head.sender_address, false)) {\n");
			source.append("\t\t\t\tif(TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_MMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_MMUNSUCC);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event(\"Matching on port {0}: Sender address of the first message in the queue does not match the from clause: \", get_name());\n");
			source.append("\t\t\t\t\tsender_template.log_match(my_head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event_log2str();\n");
			source.append("\t\t\t\t\tsender_template.log_match(my_head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.message__, port_name, my_head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.message__does__not__match__template, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		} else {
			source.append("\t\t\tif (!sender_template.match(my_head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMUNSUCC:TTCN_Logger.Severity.MATCHING_MCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event(\"Matching on port {0}: Sender of the first message in the queue does not match the from clause:\", get_name());\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(my_head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		}

		source.append(" else {\n");
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && my_head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(my_head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(my_head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(my_head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		if(isAddress) {
			source.append("\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_MMSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_MMRECV)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event_log2str();\n");
			source.append("\t\t\t\tmy_head.sender_address.log();\n");
			source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_msgport_recv(get_name(), TitanLoggerApi.Msg__port__recv_operation.enum_type.{0} , TitanComponent.SYSTEM_COMPREF, new TitanCharString(\"\") ,", logger_operation));
			source.append("\t\t\t\tTTCN_Logger.end_event_log2str(), msg_head_count+1);\n");
			source.append("\t\t\t}\n");
		} else {
			source.append("\t\t\t\tTTCN_Logger.log(my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMSUCCESS : TTCN_Logger.Severity.MATCHING_MCSUCCESS, ");
			source.append("\t\t\t\tMessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = my_head.sender_component==TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_MMRECV : TTCN_Logger.Severity.PORTEVENT_MCRECV;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tswitch (my_head.item_selection) {\n");
			for (int msg_idx = 0; msg_idx < portDefinition.inMessages.size(); msg_idx++) {
				final messageTypeInfo message_type = portDefinition.inMessages.get(msg_idx);
				source.append(MessageFormat.format("\t\t\t\t\tcase MESSAGE_{0}:\n", msg_idx));
				source.append("\t\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
				source.append(MessageFormat.format("\t\t\t\t\t\tTTCN_Logger.log_event_str(\": {0}: \");\n", message_type.mDisplayName));
				source.append("\t\t\t\t\t\tmy_head.message.log();\n");
				source.append(MessageFormat.format("\t\t\t\t\t\tTTCN_Logger.log_msgport_recv(get_name(), TitanLoggerApi.Msg__port__recv_operation.enum_type.{0}, ", logger_operation));
				source.append("my_head.sender_component, new TitanCharString(\"\"),");
				source.append(MessageFormat.format("TTCN_Logger.end_event_log2str(), msg_head_count+1);\n", msg_idx));
				source.append("\t\t\t\t\tbreak;\n");
			}
			source.append("\t\t\t\t\tdefault:\n");
			source.append("\t\t\t\t\t\tthrow new TtcnError(\"Internal error: unknown message\");\n");
			source.append("\t\t\t\t\t}\n");
			source.append("\t\t\t\t}\n");
		}
		if (!isCheck) {
			source.append("\t\t\t\tremove_msg_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the generic trigger function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateGenericTrigger(final StringBuilder source, final PortDefinition portDefinition, final boolean isAddress) {
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status trigger(final {0}_template sender_template, final {0} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", senderType));
		source.append("\t\t\tif (message_queue.isEmpty()) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\tfinal Message_queue_item my_head = message_queue.getFirst();\n");
		source.append("\t\t\tif (my_head == null) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");

		if (isAddress) {
			source.append("\t\t\tif (my_head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_MMUNSUCC, \"Matching on port {0} will drop a message: Sender of the first message in the queue is not the system.\", get_name());\n ");
			source.append("\t\t\t\tremove_msg_queue_head();\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (my_head.sender_address == null) {\n");
			source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Trigger operation on port {0} requires the address of the sender, which was not given by the test port.\", get_name()));\n");
			source.append("\t\t\t} else if (!sender_template.match(my_head.sender_address, false)) {\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_MMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_MMUNSUCC);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event(\"Matching on port {0}: Sender address of the first message in the queue does not match the from clause: \", port_name);\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(my_head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event_log2str();\n");
			source.append("\t\t\t\t\tsender_template.log_match(my_head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.message__, port_name, my_head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.message__does__not__match__template, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\tremove_msg_queue_head();\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_REPEAT;\n");
			source.append("\t\t\t}");
		} else {
			source.append("\t\t\tif (!sender_template.match(my_head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMUNSUCC:TTCN_Logger.Severity.MATCHING_MCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event(\"Matching on port {0}  will drop a message: Sender of the first message in the queue does not match the from clause: \" , get_name());\n");
			source.append("\t\t\t\t\tsender_template.log_match( new TitanComponent(my_head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\tremove_msg_queue_head();\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_REPEAT;\n");
			source.append("\t\t\t}");
		}

		source.append(" else {\n");
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && my_head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(my_head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(my_head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(my_head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		if(isAddress) {
			source.append("\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_MMSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_MMRECV)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_MMRECV);\n");
			source.append("\t\t\t\t\tmy_head.sender_address.log();\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_msgport_recv(get_name(), TitanLoggerApi.Msg__port__recv_operation.enum_type.trigger__op, TitanComponent.SYSTEM_COMPREF, new TitanCharString(\"\"), TTCN_Logger.end_event_log2str(), msg_head_count+1);\n");
		} else {
			source.append("\t\t\t\tTTCN_Logger.log(my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMSUCCESS : TTCN_Logger.Severity.MATCHING_MCSUCCESS, ");
			source.append(" MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = my_head.sender_component==TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_MMRECV : TTCN_Logger.Severity.PORTEVENT_MCRECV;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tswitch (my_head.item_selection) {\n");
			for (int msg_idx = 0; msg_idx < portDefinition.inMessages.size(); msg_idx++) {
				final messageTypeInfo message_type = portDefinition.inMessages.get(msg_idx);
				source.append(MessageFormat.format("\t\t\t\t\tcase MESSAGE_{0}:\n", msg_idx));
				source.append("\t\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
				source.append(MessageFormat.format("\t\t\t\t\t\tTTCN_Logger.log_event_str(\": {0}: \");\n", message_type.mDisplayName));
				source.append("\t\t\t\t\t\tmy_head.message.log();\n");
				source.append("\t\t\t\t\t\tTTCN_Logger.log_msgport_recv(get_name(), TitanLoggerApi.Msg__port__recv_operation.enum_type.trigger__op, ");
				source.append("my_head.sender_component, new TitanCharString(\"\"),");
				source.append(MessageFormat.format("TTCN_Logger.end_event_log2str(), msg_head_count+1);\n", msg_idx));
				source.append("\t\t\t\t\tbreak;\n");
			}
			source.append("\t\t\t\t\tdefault:\n");
			source.append("\t\t\t\t\t\tthrow new TtcnError(\"Internal error: unknown message\");\n");
			source.append("\t\t\t\t\t}\n");
		}
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tremove_msg_queue_head();\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the receive or check(receive) function for a
	 * type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this message type has in the declaration the
	 *                port type.
	 * @param inType
	 *                the information about the incoming message.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * */
	private static void generateTypedReceive(final StringBuilder source, final PortDefinition portDefinition, final int index, final messageTypeInfo inType, final boolean isCheck) {
		final String typeValueName = inType.mJavaTypeName;
		final String typeTemplateName = inType.mJavaTemplateName;
		final String functionName = isCheck ? "check_receive" : "receive";
		final String printedFunctionName = isCheck ? "Check-receive" : "Receive";
		final String operationName = isCheck ? "check__receive__op" : "receive__op";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1} value_template, final Value_Redirect_Interface value_redirect, final TitanComponent_template sender_template, final TitanComponent sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, typeTemplateName));
		source.append("\t\t\tif (value_template.get_selection() == template_sel.ANY_OR_OMIT) {\n");
		source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"{0} operation using ''*'' as matching template\");\n", printedFunctionName));
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (message_queue.isEmpty()) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t}\n\n");
		source.append("\t\t\tfinal Message_queue_item my_head = message_queue.getFirst();\n");
		source.append("\t\t\tif (my_head == null) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t} else if (!sender_template.match(my_head.sender_component, false)) {\n");
		source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMUNSUCC : TTCN_Logger.Severity.MATCHING_MCUNSUCC;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_event_str(MessageFormat.format(\"Matching on port {0} failed: Sender of the first message in the queue does not match the from clause: \", get_name()));\n");
		source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(my_head.sender_component), false);\n");
		source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\t} else if (my_head.item_selection != message_selection.MESSAGE_{0} || !(my_head.message instanceof {1})) '{'\n", index, typeValueName));
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_str(my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMUNSUCC : TTCN_Logger.Severity.MATCHING_MCUNSUCC, MessageFormat.format(\"Matching on port '{'0'}' failed: Type of the first message in the queue is not {0}.\", get_name()));\n", typeValueName));
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\t'}' else if (!value_template.match(({0}) my_head.message)) '{'\n", typeValueName));
		source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = TTCN_Logger.Severity.MATCHING_MMUNSUCC;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
		source.append("\t\t\t\t\tvalue_template.log_match(my_head.message, false);\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.message__, get_name(), my_head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.message__does__not__match__template, TTCN_Logger.end_event_log2str());\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\tif (value_redirect != null) {\n");
		source.append(MessageFormat.format("\t\t\t\t\tvalue_redirect.set_values(({0}) my_head.message);\n", typeValueName));
		source.append("\t\t\t\t}\n");
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && my_head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(my_head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		source.append("\t\t\t\t\tsender_pointer.operator_assign(my_head.sender_component);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tTTCN_Logger.Severity log_severity = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMSUCCESS : TTCN_Logger.Severity.MATCHING_MCSUCCESS;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_severity)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event_log2str();\n");
		source.append("\t\t\t\t\tvalue_template.log_match(my_head.message, true);\n");
		source.append("\t\t\t\t\tfinal TitanCharString temp = TTCN_Logger.end_event_log2str();\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_matching_success(TitanLoggerApi.PortType.enum_type.message__, port_name, my_head.sender_component, temp);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tlog_severity = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_MMRECV : TTCN_Logger.Severity.PORTEVENT_MCRECV;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_severity)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event_log2str();\n");
		source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_event_str(\": {0} : \");\n", inType.mDisplayName));
		source.append("\t\t\t\t\tmy_head.message.log();\n");
		source.append("\t\t\t\t\tfinal TitanCharString temp = TTCN_Logger.end_event_log2str();\n");
		source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_msgport_recv(port_name, TitanLoggerApi.Msg__port__recv_operation.enum_type.{0}, my_head.sender_component, new TitanCharString(\"\"), temp, msg_head_count + 1);\n", operationName));
		source.append("\t\t\t\t}\n");
		if (!isCheck) {
			source.append("\t\t\t\tremove_msg_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the trigger function for a type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this message type has in the declaration the
	 *                port type.
	 * @param inType
	 *                the information about the incoming message.
	 * */
	private static void generateTypeTrigger(final StringBuilder source, final PortDefinition portDefinition, final int index, final messageTypeInfo inType) {
		final String typeValueName = inType.mJavaTypeName;
		final String typeTemplateName = inType.mJavaTemplateName;

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status trigger(final {0} value_template, final Value_Redirect_Interface value_redirect, final TitanComponent_template sender_template, final TitanComponent sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", typeTemplateName));
		source.append("\t\t\tif (value_template.get_selection() == template_sel.ANY_OR_OMIT) {\n");
		source.append("\t\t\t\tthrow new TtcnError(\"Trigger operation using '*' as matching template\");\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (message_queue.isEmpty()) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_PROBLEM, \"Matching on port {0} will drop a message: Port is not started and the queue is empty.\", get_name());\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t}\n\n");
		source.append("\t\t\tfinal Message_queue_item my_head = message_queue.getFirst();\n");
		source.append("\t\t\tif (my_head == null) {\n");
		source.append("\t\t\t\tif (is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_PROBLEM, \"Matching on port {0} will drop a message: Port is not started and the queue is empty.\", get_name());\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t} else if (!sender_template.match(my_head.sender_component, false)) {\n");
		source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMUNSUCC : TTCN_Logger.Severity.MATCHING_MCUNSUCC;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_event(\"Matching on port {0} will drop a message: Sender of the first message in the queue does not match the from clause: \", get_name());\n");
		source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(my_head.sender_component), false);\n");
		source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tremove_msg_queue_head();\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_REPEAT;\n");
		source.append(MessageFormat.format("\t\t\t} else if (my_head.item_selection != message_selection.MESSAGE_{0} || !(my_head.message instanceof {1})) '{'\n", index, typeValueName));
		source.append("\t\t\t\tTTCN_Logger.log(my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMUNSUCC : TTCN_Logger.Severity.MATCHING_MCUNSUCC, \"Matching on port {0} will drop a message: ");
		source.append(MessageFormat.format("\t\t\t\tType of the first message in the queue is not {0}.\", get_name());\n", typeValueName) );
		source.append("\t\t\t\tremove_msg_queue_head();\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_REPEAT;\n");
		source.append(MessageFormat.format("\t\t\t'}' else if (!value_template.match(({0}) my_head.message)) '{'\n", typeValueName));
		source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMUNSUCC : TTCN_Logger.Severity.MATCHING_MCUNSUCC;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
		source.append(MessageFormat.format("\t\t\t\t\tvalue_template.log_match(my_head.message, false);\n", index));
		source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.message__, get_name(), my_head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.message__does__not__match__template, TTCN_Logger.end_event_log2str());\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tremove_msg_queue_head();\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_REPEAT;\n");
		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\tif (value_redirect != null) {\n");
		source.append(MessageFormat.format("\t\t\t\t\tvalue_redirect.set_values(({0}) my_head.message);\n", typeValueName));
		source.append("\t\t\t\t}\n");
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && my_head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(my_head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		source.append("\t\t\t\t\tsender_pointer.operator_assign(my_head.sender_component);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tTTCN_Logger.Severity log_severity = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_MMSUCCESS : TTCN_Logger.Severity.MATCHING_MCSUCCESS;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_severity)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event_log2str();\n");
		source.append("\t\t\t\t\tvalue_template.log_match(my_head.message, true);\n");
		source.append("\t\t\t\t\tfinal TitanCharString temp = TTCN_Logger.end_event_log2str();\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_matching_success(TitanLoggerApi.PortType.enum_type.message__, port_name, my_head.sender_component, temp);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tlog_severity = my_head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_MMRECV : TTCN_Logger.Severity.PORTEVENT_MCRECV;\n");
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_severity)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event_log2str();\n");
		source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_event_str(\": {0} : \");\n", inType.mDisplayName));
		source.append("\t\t\t\t\tmy_head.message.log();\n");
		source.append("\t\t\t\t\tfinal TitanCharString temp = TTCN_Logger.end_event_log2str();\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_msgport_recv(port_name, TitanLoggerApi.Msg__port__recv_operation.enum_type.trigger__op, my_head.sender_component, new TitanCharString(\"\"), temp, msg_head_count + 1);\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\tremove_msg_queue_head();\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the incoming message mapping part of the
	 * incoming_message function.
	 *
	 * @param aData
	 *                only used to add imports if needed.
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param mappedType
	 *                the information about the outgoing message.
	 * @param hasSimple
	 *                {@code true} if the port definition is simple
	 * */
	private static void generateIncomingMapping(final JavaGenData aData, final StringBuilder source, final PortDefinition portDefinition, final MessageMappedTypeInfo mappedType, final boolean hasSimple) {
		// If has simple is true, then always the first one is the simple mapping,
		// and the first mapping is taken care elsewhere
		int i  = hasSimple ? 1 : 0;
		boolean hasBuffer = false;
		boolean hasDiscard = false;
		boolean reportError = false;
		boolean isSliding = false;
		for ( ; i < mappedType.targets.size(); i++) {
			final MessageTypeMappingTarget target = mappedType.targets.get(i);
			boolean hasCondition = false;
			if (target.mappingType == MessageMappingType_type.DISCARD) {
				/* "discard" should always be the last mapping */
				hasDiscard = true;
				break;
			} else if (target.mappingType == MessageMappingType_type.DECODE && !hasBuffer) {
				aData.addBuiltinTypeImport("TTCN_Buffer");
				if (isSliding) {
					source.append("TTCN_Buffer ttcn_buffer = new TTCN_Buffer(slider);\n");
				} else {
					source.append("TTCN_Buffer ttcn_buffer = new TTCN_Buffer(incoming_par);\n");
				}
			}
			if (!portDefinition.legacy && portDefinition.portType == PortType.USER) {
				aData.addBuiltinTypeImport("TTCN_Runtime");

				source.append("TTCN_Runtime.set_translation_mode(true, this);\n");
				source.append("TTCN_Runtime.set_port_state(-1, \"by test environment.\", true);\n");
			}
			if (mappedType.targets.size() > 1) {
				source.append("{\n");
			}
			switch (target.mappingType) {
			case FUNCTION:
				source.append(MessageFormat.format("// in mapping with a prototype({0}) function\n", target.functionPrototype.name()));
				switch (target.functionPrototype) {
				case CONVERT:
					source.append(MessageFormat.format("{0} mapped_par = new {0}({1}(incoming_par));\n", target.targetName, target.functionName));
					break;
				case FAST:
					source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
					source.append(MessageFormat.format("{0}(incoming_par, mapped_par);\n", target.functionName));
					if (!portDefinition.legacy) {
						hasCondition = true;
					}
					break;
				case SLIDING:
					if (!isSliding) {
						source.append("slider.operator_assign(slider.operator_concatenate(incoming_par));\n");
						isSliding = true;
					}
					source.append("for (;;) {\n");
					source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
					source.append(MessageFormat.format("int decoding_result = {0}(slider, mapped_par).get_int();\n", target.functionName));
					source.append("if (decoding_result == 0) {\n");
					hasCondition = true;
					break;
				case BACKTRACK:
					source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
					source.append(MessageFormat.format("boolean success_flag = {0}(incoming_par, mapped_par).operator_equals(0);\n", target.functionName));
					source.append("if (success_flag) {\n");
					hasCondition = true;
					break;
				default:
					break;
				}
				break;
			case ENCODE:
				aData.addBuiltinTypeImport("TTCN_Buffer");
				aData.addBuiltinTypeImport("TTCN_EncDec");

				source.append("// in mapping with a built-in encoder\n");
				source.append(target.encdecErrorBehaviour);
				source.append("final TTCN_Buffer ttcn_buffer = new TTCN_Buffer();\n");
				if (target.encdecEncodingOptions == null) {
					source.append(MessageFormat.format("send_par.encode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, 0);\n", target.encdecTypedesriptorName, target.encdecEncodingType));
				} else {
					source.append(MessageFormat.format("send_par.encode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, {2});\n", target.encdecTypedesriptorName, target.encdecEncodingType, target.encdecEncodingOptions));
				}
				source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
				source.append("ttcn_buffer.get_string(mapped_par);\n");
				break;
			case DECODE:
				aData.addBuiltinTypeImport("TTCN_Buffer");
				aData.addBuiltinTypeImport("TTCN_EncDec");

				source.append("// in mapping with a built-in decoder\n");
				if (hasBuffer) {
					source.append("ttcn_buffer.rewind();\n");
				} else {
					hasBuffer = true;
				}
				source.append(target.encdecErrorBehaviour);
				source.append("TTCN_EncDec.clear_error();\n");
				source.append(MessageFormat.format("{0} mapped_par = new {0}();\n", target.targetName));
				if (target.encdecEncodingOptions == null) {
					source.append(MessageFormat.format("mapped_par.decode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, 0);\n", target.encdecTypedesriptorName, target.encdecEncodingType));
				} else {
					source.append(MessageFormat.format("mapped_par.decode({0}_descr_, ttcn_buffer, TTCN_EncDec.coding_type.CT_{1}, {2});\n", target.encdecTypedesriptorName, target.encdecEncodingType, target.encdecEncodingOptions));
				}
				source.append("if (TTCN_EncDec.get_last_error_type() == TTCN_EncDec.error_type.ET_NONE) {\n");
				hasCondition = true;
				break;
			default:
				break;
			}

			if (!portDefinition.legacy && portDefinition.portType == PortType.USER) {
				source.append("TTCN_Runtime.set_translation_mode(false, null);\n");
				source.append("if (port_state == translation_port_state.TRANSLATED || port_state == translation_port_state.PARTIALLY_TRANSLATED) {\n");
			}
			source.append("msg_tail_count++;\n");
			source.append("if (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_DUALRECV)) {\n");
			source.append("TTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_DUALRECV);\n");
			source.append("mapped_par.log();\n");
			source.append(MessageFormat.format("TTCN_Logger.log_dualport_map(true, \"{0}\", TTCN_Logger.end_event_log2str(), msg_tail_count);\n", target.targetDisplayName));
			source.append("}\n");
			source.append("final Message_queue_item new_item = new Message_queue_item();\n");
			source.append(MessageFormat.format("new_item.item_selection = message_selection.MESSAGE_{0};\n", target.targetIndex));
			source.append(MessageFormat.format("new_item.message = new {0}(mapped_par);\n", target.targetName));
			source.append("new_item.sender_component = sender_component;\n");
			
			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append("if (sender_address != null) {\n");
				source.append(MessageFormat.format("new_item.sender_address = new {0}(sender_address);\n", portDefinition.addressName));
				source.append("} else {\n");
				source.append("new_item.sender_address = null;\n");
				source.append("}\n");
			}
			source.append("message_queue.addLast(new_item);\n");

			if (hasCondition) {
				if (portDefinition.has_sliding && target.mappingType == MessageMappingType_type.FUNCTION && target.functionPrototype == FunctionPrototype_Type.SLIDING) {
					source.append("continue;\n");
					source.append("} else {\n");
					source.append("mapped_par = null;\n");
					source.append("}\n");
					source.append("if (decoding_result == 2) {\n");
					source.append("return;\n");
					source.append("}\n");
					source.append("}\n");
				} else {
					if (isSliding) {
						source.append("slider.operator_assign(new TitanOctetString(\"\"));\n");
					}
					source.append("return;\n");
					source.append("}\n");
					if (portDefinition.portType == PortType.USER && !portDefinition.legacy) {
						source.append("else if (port_state == translation_port_state.FRAGMENTED || port_state == translation_port_state.DISCARDED) {\n");
						source.append("mapped_par = null;\n");
						source.append("return;\n");
						source.append("} else if (port_state == translation_port_state.UNSET) {\n");
						source.append("mapped_par = null;\n");
						source.append(MessageFormat.format("throw new TtcnError(MessageFormat.format(\"The state of the port '{'0'}' remained unset after the mapping function {0} finished..\", get_name()));\n", target.functionDisplayName));
						source.append("}\n");
					}
					source.append("else {\n");
					source.append("mapped_par = null;\n");
					source.append("}\n");
				}
				reportError = true;
			}
			if (mappedType.targets.size() > 1) {
				source.append("}\n");
			}
		}
		if (hasDiscard) {
			if (mappedType.targets.size() > 1) {
				/* there are other mappings, which failed */
				source.append(MessageFormat.format("TTCN_Logger.log_dualport_discard(true, \"{0}\", get_name(), true);\n", mappedType.mDisplayName));
			} else {
				/* this is the only mapping */
				source.append(MessageFormat.format("TTCN_Logger.log_dualport_discard(true, \"{0}\", get_name(), false);\n", mappedType.mDisplayName));
			}
		} else if (reportError && !hasSimple && !isSliding) {
			source.append(MessageFormat.format("throw new TtcnError(MessageFormat.format(\"Incomming message of type {0} could not be handled by the type mapping rules on port '{'0'}'.\", get_name()));\n", mappedType.mDisplayName));
		}
	}

	/**
	 * This function generates the incoming_message function for a type, for
	 * a user port
	 *
	 * @param aData
	 *                only used to add imports if needed
	 * @param source
	 *                where the source code is to be generated.
	 * @param index
	 *                the index this message type has in the declaration the
	 *                port type.
	 * @param mappedType
	 *                the information about the incoming message.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateTypedIncommingMessageUser(final JavaGenData aData, final StringBuilder source, final int index, final MessageMappedTypeInfo mappedType, final PortDefinition portDefinition) {
		final String typeValueName = mappedType.mJavaTypeName;
		final boolean isSimple = (!portDefinition.legacy || (mappedType.targets.size() == 1)) && mappedType.targets.get(0).mappingType == MessageMappingType_type.SIMPLE;

		final StringBuilder comment = new StringBuilder();
		final StringBuilder header = new StringBuilder();
		comment.append("\t\t/**\n");
		comment.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", mappedType.mDisplayName));
		comment.append("\t\t * Test Port.\n");
		comment.append("\t\t *\n");
		comment.append("\t\t * @param incoming_par\n");
		comment.append("\t\t *            the value to be inserted.\n");
		comment.append("\t\t * @param sender_component\n");
		comment.append("\t\t *            the sender component.\n");
		header.append(MessageFormat.format("\t\tprivate void incoming_message(final {0} incoming_par, final int sender_component", typeValueName));
		if (portDefinition.has_sliding) {
			comment.append("\t\t * @param slider\n");
			comment.append("\t\t *            the sliding buffer.\n");
			header.append(", final TitanOctetString slider");
		}
		if (portDefinition.testportType == TestportType.ADDRESS) {
			comment.append("\t\t * @param sender_address\n");
			comment.append("\t\t *            the address of the sender.\n");
			header.append(MessageFormat.format(", final {0} sender_address", portDefinition.addressName));
		}
		if (portDefinition.realtime) {
			comment.append("\t\t * @param timestamp\n");
			comment.append("\t\t *            the timestamp to return.\n");
			header.append(", final TitanFloat timestamp");
		}
		comment.append("\t\t * */\n");
		source.append(comment);
		source.append(header);

		source.append(") {\n");
		source.append("\t\t\tif (!is_started) {\n");
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Port {0} is not started but a message has arrived on it.\", get_name()));\n");
		source.append("\t\t\t}\n");
		if (isSimple) {
			source.append("\t\t\tmsg_tail_count++;\n");
		}

		source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_MQUEUE)) {\n");
		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_MQUEUE);\n");
			source.append("\t\t\t\tTTCN_Logger.log_char('(');\n");
			source.append("\t\t\t\tsender_address.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_char(')');\n");
			source.append("\t\t\t\tfinal TitanCharString log_sender_address = TTCN_Logger.end_event_log2str();\n");
		} else {
			source.append("\t\t\t\tfinal TitanCharString log_sender_address = new TitanCharString(\"\");\n");
		}
		source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_MQUEUE);\n");
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_event_str(\" {0} : \");\n", mappedType.mDisplayName));
		source.append("\t\t\t\tincoming_par.log();\n");
		source.append("\t\t\t\tfinal TitanCharString log_parameter = TTCN_Logger.end_event_log2str();\n");
		if (isSimple) {
			source.append("\t\t\t\tTTCN_Logger.log_port_queue(TitanLoggerApi.Port__Queue_operation.enum_type.enqueue__msg, port_name, sender_component, msg_tail_count, log_sender_address, log_parameter);\n");
		} else {
			source.append("\t\t\t\tTTCN_Logger.log_port_queue(TitanLoggerApi.Port__Queue_operation.enum_type.enqueue__msg, port_name, sender_component, msg_tail_count + 1, log_sender_address, log_parameter);\n");
		}
		source.append("\t\t\t}\n");

		if (!portDefinition.legacy) {
			if ((!isSimple && !mappedType.targets.isEmpty()) || (isSimple && mappedType.targets.size() > 1)) {
				source.append("\t\t\tif (in_translation_mode()) {\n");
				generateIncomingMapping(aData, source, portDefinition, mappedType, isSimple);
				source.append("\t\t\t}\n");
			} else {
				generateIncomingMapping(aData, source, portDefinition, mappedType, isSimple);
			}
		} else if (!isSimple) {
			generateIncomingMapping(aData, source, portDefinition, mappedType, isSimple);
		}

		if (isSimple) {
			source.append("\t\t\tfinal Message_queue_item new_item = new Message_queue_item();\n");
			source.append(MessageFormat.format("\t\t\tnew_item.item_selection = message_selection.MESSAGE_{0};\n", index));
			source.append(MessageFormat.format("\t\t\tnew_item.message = new {0}(incoming_par);\n", typeValueName));
			source.append("\t\t\tnew_item.sender_component = sender_component;\n");
			if (portDefinition.realtime) {
				source.append("\t\t\tif(timestamp.is_bound()) {\n");
				source.append("\t\t\t\tnew_item.timestamp.operator_assign(timestamp);\n");
				source.append("\t\t\t}\n");
			}
			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append("\t\t\tif (sender_address != null) {\n");
				source.append(MessageFormat.format("\t\t\t\tnew_item.sender_address = new {0}(sender_address);\n", portDefinition.addressName));
				source.append("\t\t\t} else {\n");
				source.append("\t\t\t\tnew_item.sender_address = null;\n");
				source.append("\t\t\t}\n");
			}
			source.append("\t\t\tmessage_queue.addLast(new_item);\n");
		}
		source.append("\t\t}\n\n");

		if (portDefinition.testportType != TestportType.INTERNAL) {
			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append("\t\t/**\n");
				source.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", mappedType.mDisplayName));
				source.append("\t\t * Test Port.\n");
				source.append("\t\t *\n");
				source.append("\t\t * @param incoming_par\n");
				source.append("\t\t *            the value to be inserted.\n");
				source.append("\t\t * @param sender_address\n");
				source.append("\t\t *            the address of the sender.\n");
				source.append("\t\t * */\n");
				source.append(MessageFormat.format("\t\tprotected void incoming_message(final {0} incoming_par, final {1} sender_address) '{'\n", typeValueName, portDefinition.addressName));
				source.append(MessageFormat.format("\t\t\tincoming_message(incoming_par, TitanComponent.SYSTEM_COMPREF{0}, sender_address", portDefinition.realtime ? ", new TitanFloat()":""));
				if (portDefinition.has_sliding) {
					source.append(", sliding_buffer");
				}
				source.append(");\n");
				source.append("\t\t}\n");

				source.append("\t\t/**\n");
				source.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", mappedType.mDisplayName));
				source.append("\t\t * Test Port.\n");
				source.append("\t\t *\n");
				source.append("\t\t * @param incoming_par\n");
				source.append("\t\t *            the value to be inserted.\n");
				source.append("\t\t * */\n");
				source.append(MessageFormat.format("\t\tprotected void incoming_message(final {0} incoming_par) '{'\n", typeValueName));
				source.append(MessageFormat.format("\t\t\tincoming_message(incoming_par, TitanComponent.SYSTEM_COMPREF{0}, null", portDefinition.realtime ? ", new TitanFloat()":""));
			} else {
				source.append("\t\t/**\n");
				source.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", mappedType.mDisplayName));
				source.append("\t\t * Test Port.\n");
				source.append("\t\t *\n");
				source.append("\t\t * @param incoming_par\n");
				source.append("\t\t *            the value to be inserted.\n");
				source.append("\t\t * */\n");
				source.append(MessageFormat.format("\t\tprotected void incoming_message(final {0} incoming_par) '{'\n", typeValueName));
				source.append(MessageFormat.format("\t\t\tincoming_message(incoming_par, TitanComponent.SYSTEM_COMPREF{0}", portDefinition.realtime ? ", new TitanFloat()":""));
			}
			if (portDefinition.has_sliding) {
				source.append(", sliding_buffer");
			}
			source.append(");\n");
			source.append("\t\t}\n");
		}
	}

	/**
	 * This function generates the incoming_message function for a type, for
	 * a provider or regular port
	 *
	 * @param aData
	 *                only used to add imports if needed
	 * @param source
	 *                where the source code is to be generated.
	 * @param index
	 *                the index this message type has in the declaration the
	 *                port type.
	 * @param inType
	 *                the information about the incoming message.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateTypedIncommingMessageProvider(final JavaGenData aData, final StringBuilder source, final int index, final messageTypeInfo inType, final PortDefinition portDefinition) {
		final String typeValueName = inType.mJavaTypeName;

		final StringBuilder comment = new StringBuilder();
		final StringBuilder header = new StringBuilder();
		comment.append("\t\t/**\n");
		comment.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", inType.mDisplayName));
		comment.append("\t\t * Test Port.\n");
		comment.append("\t\t *\n");
		comment.append("\t\t * @param incoming_par\n");
		comment.append("\t\t *            the value to be inserted.\n");
		comment.append("\t\t * @param sender_component\n");
		comment.append("\t\t *            the sender component.\n");
		header.append(MessageFormat.format("\t\tprivate void incoming_message(final {0} incoming_par, final int sender_component", typeValueName));
		if (portDefinition.testportType == TestportType.ADDRESS) {
			comment.append("\t\t * @param sender_address\n");
			comment.append("\t\t *            the address of the sender.\n");
			header.append(MessageFormat.format(", final {0} sender_address", portDefinition.addressName));
		}
		if (portDefinition.realtime) {
			comment.append("\t\t * @param timestamp\n");
			comment.append("\t\t *            the timestamp to return.\n");
			header.append(", final TitanFloat timestamp");
		}
		comment.append("\t\t * */\n");
		source.append(comment);
		source.append(header);

		source.append(") {\n");
		if (portDefinition.portType == PortType.PROVIDER) {
			// We forward the incoming_message to the mapped port
			source.append("\t\t\tfor (int i = 0; i < mapped_ports.size(); i++) {\n");
			source.append(MessageFormat.format("\t\t\t\tif (mapped_ports.get(i) != null && mapped_ports.get(i).incoming_message_handler(incoming_par, \"{0}\", sender_component, {1})) '{'\n", inType.mDisplayName, portDefinition.realtime ? "timestamp": "new TitanFloat()"));
			source.append("\t\t\t\t\treturn;\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t}\n");
		}
		source.append("\t\t\tif (!is_started) {\n");
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Port {0} is not started but a message has arrived on it.\", get_name()));\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tmsg_tail_count++;\n");
		source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_MQUEUE)) {\n");
		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_MQUEUE);\n");
			source.append("\t\t\t\tTTCN_Logger.log_char('(');\n");
			source.append("\t\t\t\tsender_address.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_char(')');\n");
			source.append("\t\t\t\tfinal TitanCharString log_sender_address = TTCN_Logger.end_event_log2str();\n");
		} else {
			source.append("\t\t\t\tfinal TitanCharString log_sender_address = new TitanCharString(\"\");\n");
		}
		source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_MQUEUE);\n");
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_event_str(\" {0} : \");\n", inType.mDisplayName));
		source.append("\t\t\t\tincoming_par.log();\n");
		source.append("\t\t\t\tfinal TitanCharString log_parameter = TTCN_Logger.end_event_log2str();\n");
		source.append("\t\t\t\tTTCN_Logger.log_port_queue(TitanLoggerApi.Port__Queue_operation.enum_type.enqueue__msg, port_name, sender_component, msg_tail_count, log_sender_address, log_parameter);\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Message_queue_item new_item = new Message_queue_item();\n");
		source.append(MessageFormat.format("\t\t\tnew_item.item_selection = message_selection.MESSAGE_{0};\n", index));
		source.append(MessageFormat.format("\t\t\tnew_item.message = new {0}(incoming_par);\n", typeValueName));
		source.append("\t\t\tnew_item.sender_component = sender_component;\n");
		if (portDefinition.realtime) {
			source.append("\t\t\tif(timestamp.is_bound()) {\n");
			source.append("\t\t\t\tnew_item.timestamp = new TitanFloat(timestamp);\n");
			source.append("\t\t\t}\n");
		}
		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\tif (sender_address != null) {\n");
			source.append(MessageFormat.format("\t\t\t\tnew_item.sender_address = new {0}(sender_address);\n", portDefinition.addressName));
			source.append("\t\t\t} else {\n");
			source.append("\t\t\t\tnew_item.sender_address = null;\n");
			source.append("\t\t\t}\n");
		}
		source.append("\t\t\tmessage_queue.addLast(new_item);\n");
		source.append("\t\t}\n\n");

		if (portDefinition.testportType != TestportType.INTERNAL) {
			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append("\t\t/**\n");
				source.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", inType.mDisplayName));
				source.append("\t\t * Test Port.\n");
				source.append("\t\t *\n");
				source.append("\t\t * @param incoming_par\n");
				source.append("\t\t *            the value to be inserted.\n");
				source.append("\t\t * @param sender_address\n");
				source.append("\t\t *            the address of the sender.\n");
				source.append("\t\t * */\n");
				source.append(MessageFormat.format("\t\tprotected void incoming_message(final {0} incoming_par, final {1} sender_address) '{'\n", typeValueName, portDefinition.addressName));
				source.append(MessageFormat.format("\t\t\tincoming_message(incoming_par, TitanComponent.SYSTEM_COMPREF{0}, sender_address", portDefinition.realtime ? ", new TitanFloat()":""));
				if (portDefinition.has_sliding) {
					source.append(", sliding_buffer");
				}
				source.append(");\n");
				source.append("\t\t}\n");

				source.append("\t\t/**\n");
				source.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", inType.mDisplayName));
				source.append("\t\t * Test Port.\n");
				source.append("\t\t *\n");
				source.append("\t\t * @param incoming_par\n");
				source.append("\t\t *            the value to be inserted.\n");
				source.append("\t\t * */\n");
				source.append(MessageFormat.format("\t\tprotected void incoming_message(final {0} incoming_par) '{'\n", typeValueName));
				source.append(MessageFormat.format("\t\t\tincoming_message(incoming_par, TitanComponent.SYSTEM_COMPREF{0}, null", portDefinition.realtime ? ", new TitanFloat()":""));
			} else {
				source.append("\t\t/**\n");
				source.append(MessageFormat.format("\t\t * Inserts a message of {0} type into the incoming message queue of this\n", inType.mDisplayName));
				source.append("\t\t * Test Port.\n");
				source.append("\t\t *\n");
				source.append("\t\t * @param incoming_par\n");
				source.append("\t\t *            the value to be inserted.\n");
				source.append("\t\t * */\n");
				source.append(MessageFormat.format("\t\tprotected void incoming_message(final {0} incoming_par) '{'\n", typeValueName));
				source.append(MessageFormat.format("\t\t\tincoming_message(incoming_par, TitanComponent.SYSTEM_COMPREF{0}", portDefinition.realtime ? ", new TitanFloat()":""));
			}
			if (portDefinition.has_sliding) {
				source.append(", sliding_buffer");
			}
			source.append(");\n");
			source.append("\t\t}\n");
		}
	}

	/**
	 * This function generates the process_message function for a type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateProcessMessage(final StringBuilder source, final PortDefinition portDefinition) {
		source.append("\t\t@Override\n");
		source.append("\t\tprotected boolean process_message(final String message_type, final Text_Buf incoming_buf, final int sender_component, final TitanOctetString slider) {\n");
		//indent first if
		source.append("\t\t\t");

		if (portDefinition.portType == PortType.USER) {
			for (int i = 0 ; i < portDefinition.providerInMessages.size(); i++) {
				final MessageMappedTypeInfo inType = portDefinition.providerInMessages.get(i);

				source.append(MessageFormat.format("if (\"{0}\".equals(message_type)) '{'\n", inType.mDisplayName));
				source.append(MessageFormat.format("\t\t\t\tfinal {0} incoming_par = new {0}();\n", inType.mJavaTypeName));
				source.append("\t\t\t\tincoming_par.decode_text(incoming_buf);\n");
				source.append(MessageFormat.format("\t\t\t\tincoming_message(incoming_par, sender_component{0}", portDefinition.realtime ? ", new TitanFloat()":""));
				if (portDefinition.has_sliding) {
					source.append(", slider");
				}
				if (portDefinition.testportType == TestportType.ADDRESS) {
					source.append(", null");
				}
				source.append(");\n");
				source.append("\t\t\t\treturn true;\n");
				source.append("\t\t\t} else ");
			}
		} else {
			for (int i = 0 ; i < portDefinition.inMessages.size(); i++) {
				final messageTypeInfo inType = portDefinition.inMessages.get(i);
	
				source.append(MessageFormat.format("if (\"{0}\".equals(message_type)) '{'\n", inType.mDisplayName));
				source.append(MessageFormat.format("\t\t\t\tfinal {0} incoming_par = new {0}();\n", inType.mJavaTypeName));
				source.append("\t\t\t\tincoming_par.decode_text(incoming_buf);\n");
				source.append(MessageFormat.format("\t\t\t\tincoming_message(incoming_par, sender_component{0}", portDefinition.realtime ? ", new TitanFloat()":""));
				if (portDefinition.has_sliding) {
					source.append(", slider");
				}
				if (portDefinition.testportType == TestportType.ADDRESS) {
					source.append(", null");
				}
				source.append(");\n");
				source.append("\t\t\t\treturn true;\n");
				source.append("\t\t\t} else ");
			}
		}

		source.append("{\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the call function for a signature
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param info
	 *                information about the signature type.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateCallFunction(final StringBuilder source, final procedureSignatureInfo info, final PortDefinition portDefinition) {
		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Calls a(n) {0} signature on the provided component.\n", info.mDisplayName));
		source.append("\t\t * <p>\n");
		source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
		source.append("\t\t * available in the timestamp_Redirect parameter.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param call_template\n");
		source.append("\t\t *            the signature to be called\n");
		source.append("\t\t * @param destination_component\n");
		source.append("\t\t *            the target component to send the message to.\n");
		source.append("\t\t * @param timestamp_redirect\n");
		source.append("\t\t *            the redirected timestamp if any.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tpublic void call(final {0}_template call_template, final TitanComponent destination_component, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName));
		source.append("\t\t\tif (!is_started) {\n");
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Calling a signature on port {0}, which is not started.\", get_name()));\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (!destination_component.is_bound()) {\n");
		source.append("\t\t\t\tthrow new TtcnError(\"Unbound component reference in the to clause of call operation.\");\n");
		source.append("\t\t\t}\n\n");

		source.append(MessageFormat.format("\t\t\tfinal {0}_call call_temp = call_template.create_call();\n", info.mJavaTypeName));
		source.append("\t\t\tfinal TTCN_Logger.Severity log_sev = destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF) ? TTCN_Logger.Severity.PORTEVENT_PMOUT : TTCN_Logger.Severity.PORTEVENT_PCOUT;\n");
		source.append("\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
		source.append("\t\t\t\tcall_temp.log();\n");
		source.append("\t\t\t\tTTCN_Logger.log_procport_send(get_name(), TitanLoggerApi.Port__oper.enum_type.call__op, destination_component.get_component(), new TitanCharString(\"\"), TTCN_Logger.end_event_log2str());\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF)) {\n");
		if (portDefinition.testportType == TestportType.INTERNAL) {
			source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal port {0} cannot send call to system.\", get_name()));\n");
		} else {
			source.append("\t\t\t\toutgoing_call(call_temp");
			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append(", null");
			}
			if (portDefinition.realtime) {
				source.append(", timestamp_redirect");
			}
			source.append(");\n");
		}

		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\tfinal Text_Buf text_buf = new Text_Buf();\n");
		source.append(MessageFormat.format("\t\t\t\tprepare_call(text_buf, \"{0}\");\n", info.mDisplayName));
		source.append("\t\t\t\tcall_temp.encode_text(text_buf);\n");
		source.append("\t\t\t\tsend_data(text_buf, destination_component);\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");

		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append(MessageFormat.format("\t\tpublic void call(final {0}_template call_template, final {1} destination_address, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName, portDefinition.addressName));
			source.append("\t\t\tif (!is_started) {\n");
			source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Calling a signature on port {0}, which is not started.\", get_name()));\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PMOUT)) {");
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
			source.append("\t\t\t\tdestination_address.log();\n");
			source.append("\t\t\t\tfinal TitanCharString log_temp = TTCN_Logger.end_event_log2str();\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
			source.append("\t\t\t\tcall_template.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_procport_send(get_name(), TitanLoggerApi.Port__oper.enum_type.call__op, TitanComponent.SYSTEM_COMPREF, log_temp, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t}\n");
			source.append(MessageFormat.format("\t\t\tfinal {0}_call call_temp = call_template.create_call();\n", info.mJavaTypeName));
			source.append("\t\t\toutgoing_call(call_temp, destination_address");
			if (portDefinition.realtime) {
				source.append(", timestamp_redirect");
			}
			source.append(");\n");
			source.append("\t\t}\n\n");
		}

		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Calls a(n) {0} signature on the default component.\n", info.mDisplayName));
		source.append("\t\t * <p>\n");
		source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
		source.append("\t\t * available in the timestamp_Redirect parameter.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param call_template\n");
		source.append("\t\t *            the signature to be called\n");
		source.append("\t\t * @param timestamp_redirect\n");
		source.append("\t\t *            the redirected timestamp if any.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tpublic void call(final {0}_template call_template, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName));
		source.append("\t\t\tcall(call_template, new TitanComponent(get_default_destination()), timestamp_redirect);\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the reply function for a signature
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param info
	 *                information about the signature type.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateReplyFunction(final StringBuilder source, final procedureSignatureInfo info, final PortDefinition portDefinition) {
		if (!info.isNoBlock) {
			source.append("\t\t/**\n");
			source.append(MessageFormat.format("\t\t * Replies to a(n) {0} signature on the provided component.\n", info.mDisplayName));
			source.append("\t\t * <p>\n");
			source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
			source.append("\t\t * available in the timestamp_Redirect parameter.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param reply_template\n");
			source.append("\t\t *            the signature to be replied\n");
			source.append("\t\t * @param destination_component\n");
			source.append("\t\t *            the target component to send the message to.\n");
			source.append("\t\t * @param timestamp_redirect\n");
			source.append("\t\t *            the redirected timestamp if any.\n");
			source.append("\t\t * */\n");
			source.append(MessageFormat.format("\t\tpublic void reply(final {0}_template reply_template, final TitanComponent destination_component, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName));
			source.append("\t\t\tif (!is_started) {\n");
			source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Replying to a signature on port {0}, which is not started.\", get_name()));\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (!destination_component.is_bound()) {\n");
			source.append("\t\t\t\tthrow new TtcnError(\"Unbound component reference in the to clause of reply operation.\");\n");
			source.append("\t\t\t}\n\n");

			source.append(MessageFormat.format("\t\t\tfinal {0}_reply reply_temp = reply_template.create_reply();\n", info.mJavaTypeName));
			source.append("\t\t\tfinal TTCN_Logger.Severity log_sev = destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF) ? TTCN_Logger.Severity.PORTEVENT_PMOUT : TTCN_Logger.Severity.PORTEVENT_PCOUT;\n");
			source.append("\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
			source.append("\t\t\t\treply_temp.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_procport_send(get_name(), TitanLoggerApi.Port__oper.enum_type.reply__op, destination_component.get_component(), new TitanCharString(\"\"), TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF)) {\n");
			if (portDefinition.testportType == TestportType.INTERNAL) {
				source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal port {0} cannot reply to system.\", get_name()));\n");
			} else {
				source.append("\t\t\t\toutgoing_reply(reply_temp");
				if (portDefinition.testportType == TestportType.ADDRESS) {
					source.append(", null");
				}
				if (portDefinition.realtime) {
					source.append(", timestamp_redirect");
				}
				source.append(");\n");
			}
			source.append("\t\t\t} else {\n");
			source.append("\t\t\t\tfinal Text_Buf text_buf = new Text_Buf();\n");
			source.append(MessageFormat.format("\t\t\t\tprepare_reply(text_buf, \"{0}\");\n", info.mDisplayName));
			source.append("\t\t\t\treply_temp.encode_text(text_buf);\n");
			source.append("\t\t\t\tsend_data(text_buf, destination_component);\n");
			source.append("\t\t\t}\n");
			source.append("\t\t}\n\n");

			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append(MessageFormat.format("\t\tpublic void reply(final {0}_template reply_template, final {1} destination_address, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName, portDefinition.addressName));
				source.append("\t\t\tif (!is_started) {\n");
				source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Replying to a signature on port {0}, which is not started.\", get_name()));\n");
				source.append("\t\t\t}\n");
				source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PMOUT)) {");
				source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
				source.append("\t\t\t\tdestination_address.log();\n");
				source.append("\t\t\t\tfinal TitanCharString log_temp = TTCN_Logger.end_event_log2str();\n");
				source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
				source.append("\t\t\t\treply_template.log();\n");
				source.append("\t\t\t\tTTCN_Logger.log_procport_send(get_name(), TitanLoggerApi.Port__oper.enum_type.reply__op, TitanComponent.SYSTEM_COMPREF, log_temp, TTCN_Logger.end_event_log2str());\n");
				source.append("\t\t\t}\n");
				source.append(MessageFormat.format("\t\t\tfinal {0}_reply reply_temp = reply_template.create_reply();\n", info.mJavaTypeName));
				source.append("\t\t\toutgoing_reply(reply_temp, destination_address");
				if (portDefinition.realtime) {
					source.append(", timestamp_redirect");
				}
				source.append(");\n");
				source.append("\t\t}\n\n");
			}

			source.append("\t\t/**\n");
			source.append(MessageFormat.format("\t\t * Replies to a(n) {0} signature on the default component.\n", info.mDisplayName));
			source.append("\t\t * <p>\n");
			source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
			source.append("\t\t * available in the timestamp_Redirect parameter.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param reply_template\n");
			source.append("\t\t *            the signature to be replied\n");
			source.append("\t\t * @param timestamp_redirect\n");
			source.append("\t\t *            the redirected timestamp if any.\n");
			source.append("\t\t * */\n");
			source.append(MessageFormat.format("\t\tpublic void reply(final {0}_template reply_template, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName));
			source.append("\t\t\treply(reply_template, new TitanComponent(get_default_destination()), timestamp_redirect);\n");
			source.append("\t\t}\n\n");
		}
	}

	/**
	 * This function generates the raise function for a signature
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param info
	 *                information about the signature type.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateRaiseFunction(final StringBuilder source, final procedureSignatureInfo info, final PortDefinition portDefinition) {
		if (info.hasExceptions) {
			source.append("\t\t/**\n");
			source.append(MessageFormat.format("\t\t * Raise the exception of {0} signature on the provided component.\n", info.mDisplayName));
			source.append("\t\t * <p>\n");
			source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
			source.append("\t\t * available in the timestamp_Redirect parameter.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param raise_exception\n");
			source.append("\t\t *            the exception to be raised\n");
			source.append("\t\t * @param destination_component\n");
			source.append("\t\t *            the target component to send the message to.\n");
			source.append("\t\t * @param timestamp_redirect\n");
			source.append("\t\t *            the redirected timestamp if any.\n");
			source.append("\t\t * */\n");
			source.append(MessageFormat.format("\t\tpublic void raise(final {0}_exception raise_exception, final TitanComponent destination_component, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName));
			source.append("\t\t\tif (!is_started) {\n");
			source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Raising an exception on port {0}, which is not started.\", get_name()));\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (!destination_component.is_bound()) {\n");
			source.append("\t\t\t\tthrow new TtcnError(\"Unbound component reference in the to clause of raise operation.\");\n");
			source.append("\t\t\t}\n\n");

			source.append("\t\t\tfinal TTCN_Logger.Severity log_sev = destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF) ? TTCN_Logger.Severity.PORTEVENT_PMOUT : TTCN_Logger.Severity.PORTEVENT_PCOUT;\n");
			source.append("\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
			source.append("\t\t\t\traise_exception.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_procport_send(get_name(), TitanLoggerApi.Port__oper.enum_type.exception__op, destination_component.get_component(), new TitanCharString(\"\"), TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (destination_component.operator_equals(TitanComponent.SYSTEM_COMPREF)) {\n");
			if (portDefinition.testportType == TestportType.INTERNAL) {
				source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Internal port {0} cannot raise an exception to system.\", get_name()));\n");
			} else {
				source.append("\t\t\t\toutgoing_raise(raise_exception");
				if (portDefinition.testportType == TestportType.ADDRESS) {
					source.append(", null");
				}
				if (portDefinition.realtime) {
					source.append(", timestamp_redirect");
				}
				source.append(");\n");
			}
			source.append("\t\t\t} else {\n");
			source.append("\t\t\t\tfinal Text_Buf text_buf = new Text_Buf();\n");
			source.append(MessageFormat.format("\t\t\t\tprepare_exception(text_buf, \"{0}\");\n", info.mDisplayName));
			source.append("\t\t\t\traise_exception.encode_text(text_buf);\n");
			source.append("\t\t\t\tsend_data(text_buf, destination_component);\n");
			source.append("\t\t\t}\n");
			source.append("\t\t}\n\n");

			if (portDefinition.testportType == TestportType.ADDRESS) {
				source.append(MessageFormat.format("\t\tpublic void raise(final {0}_exception raise_exception, final {1} destination_address, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName, portDefinition.addressName));
				source.append("\t\t\tif (!is_started) {\n");
				source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Raising an exception on port {0}, which is not started.\", get_name()));\n");
				source.append("\t\t\t}\n");
				source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PMOUT)) {");
				source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
				source.append("\t\t\t\tdestination_address.log();\n");
				source.append("\t\t\t\tfinal TitanCharString log_temp = TTCN_Logger.end_event_log2str();\n");
				source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMOUT);\n");
				source.append("\t\t\t\traise_exception.log();\n");
				source.append("\t\t\t\tTTCN_Logger.log_procport_send(get_name(), TitanLoggerApi.Port__oper.enum_type.exception__op, TitanComponent.SYSTEM_COMPREF, log_temp, TTCN_Logger.end_event_log2str());\n");
				source.append("\t\t\t}\n");
				source.append("\t\t\toutgoing_raise(raise_exception, destination_address");
				if (portDefinition.realtime) {
					source.append(", timestamp_redirect");
				}
				source.append(");\n");
				source.append("\t\t}\n\n");
			}

			source.append("\t\t/**\n");
			source.append(MessageFormat.format("\t\t * Raise the exception of {0} signature on the default component.\n", info.mDisplayName));
			source.append("\t\t * <p>\n");
			source.append("\t\t * When timestamp redirection is support the timestamp will be made\n");
			source.append("\t\t * available in the timestamp_Redirect parameter.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param raise_exception\n");
			source.append("\t\t *            the exception to be raised\n");
			source.append("\t\t * @param timestamp_redirect\n");
			source.append("\t\t *            the redirected timestamp if any.\n");
			source.append("\t\t * */\n");
			source.append(MessageFormat.format("\t\tpublic void raise(final {0}_exception raise_exception, final TitanFloat timestamp_redirect) '{'\n", info.mJavaTypeName));
			source.append("\t\t\traise(raise_exception, new TitanComponent(get_default_destination()), timestamp_redirect);\n");
			source.append("\t\t}\n\n");
		}
	}

	/**
	 * This function generates the generic getcall or check(getcall)
	 * function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateGenericGetcall(final StringBuilder source, final PortDefinition portDefinition, final boolean isCheck, final boolean isAddress) {
		final String functionName = isCheck ? "check_getcall" : "getcall";
		final String printedFunctionName = isCheck ? "Check-getcall" : "Getcall";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template sender_template, final {1} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, senderType));
		source.append("\t\t\tif (procedure_queue.size() == 0) {\n");
		source.append("\t\t\t\tif(is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item head = procedure_queue.getFirst();\n");
		if (isAddress) {
			source.append("\t\t\tif (head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PMUNSUCC, MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue is not the system.\", get_name()));\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (head.sender_address == null) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"{0} operation on port '{'0'}' requires the address of the sender, which was not given by the test port.\", get_name()));\n", printedFunctionName));
			source.append("\t\t\t} else if (!sender_template.match(head.sender_address, false)) {\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC);\n");
			source.append("\t\t\t\t\tsender_template.log_match(head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), TitanComponent.SYSTEM_COMPREF, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}\n");
		} else {
			source.append("\t\t\tif (!sender_template.match(head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event_str(MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue does not match the from clause: \", get_name()));\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}\n");
		}
		source.append("\t\t\tswitch (head.item_selection) {\n");
		for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
			source.append(MessageFormat.format("\t\t\tcase CALL_{0}:\n", i));
		}

		source.append("\t\t\t{\n");
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		if(isAddress) {
			source.append("\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_PMSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PMIN)) {\n");
			source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.call__op, head.sender_component, {0}, new TitanCharString(\"\"), msg_head_count+1);\n", isCheck ? "true":"false"));
			source.append("\t\t\t\t}\n");
		} else {
			source.append("\t\t\t\tTTCN_Logger.log(head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMSUCCESS : TTCN_Logger.Severity.MATCHING_PCSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component ==  TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_PMIN : TTCN_Logger.Severity.PORTEVENT_PCIN;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.call__op, head.sender_component, {0} ,new TitanCharString(\"\"), msg_head_count+1);\n", isCheck ? "true" : "false"));
			source.append("\t\t\t\t}\n");
		}
		if (!isCheck) {
			source.append("\t\t\t\tremove_proc_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log({0}, MessageFormat.format(\"Matching on port '{'0'}' failed: First entity in the queue is not a call.\", get_name()));\n", isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC"));
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the getcall or check(getcall) function for a
	 * signature type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateTypedGetcall(final StringBuilder source, final PortDefinition portDefinition, final int index, final procedureSignatureInfo info, final boolean isCheck, final boolean isAddress) {
		final String functionName = isCheck ? "check_getcall" : "getcall";
		final String printedFunctionName = isCheck ? "Check-getcall" : "Getcall";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template getcall_template, final {2}_template sender_template, final {1}_call_redirect param_ref, final {2} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, info.mJavaTypeName, senderType));
		source.append("\t\t\tif (procedure_queue.size() == 0) {\n");
		source.append("\t\t\t\tif(is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item head = procedure_queue.getFirst();\n");
		if (isAddress) {
			source.append("\t\t\tif (head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__is__not__system, new TitanCharString(\"\"));\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (head.sender_address == null) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"{0} operation on port '{'0'}' requires the address of the sender, which was not given by the test port.\", get_name()));\n", printedFunctionName));
			source.append("\t\t\t} else if (!sender_template.match(head.sender_address, false)) {\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC);\n");
			source.append("\t\t\t\t\tsender_template.log_match(head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), TitanComponent.SYSTEM_COMPREF, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		} else {
			source.append("\t\t\tif (!sender_template.match(head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		}
		source.append(MessageFormat.format(" else if (head.item_selection != proc_selection.CALL_{0}) '{'\n", index));
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log({0}, MessageFormat.format(\"Matching on port '{'0'}' failed: The first entity in the queue is not a call for signature {1}.\", get_name()));\n", isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC", portDefinition.displayName));
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\t'}' else if (!getcall_template.match_call(head.call_{0}, true)) '{'\n", index));
		source.append(MessageFormat.format("\t\t\t\tfinal TTCN_Logger.Severity log_sev = {0};\n", isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC"));
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
		source.append(MessageFormat.format("\t\t\t\t\tgetcall_template.log_match_call(head.call_{0}, false);\n", index));
		source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.parameters__of__call__do__not__match__template, TTCN_Logger.end_event_log2str());\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t} else {\n");
		source.append(MessageFormat.format("\t\t\t\tparam_ref.set_parameters(head.call_{0});\n", index));
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		generate_proc_incoming_data_logging(source, "call", "getcall_template.log_match_call", isAddress, isCheck, index);
		if (!isCheck) {
			source.append("\t\t\t\tremove_proc_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the generic getreply or check(getreply)
	 * function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateGenericGetreply(final StringBuilder source, final PortDefinition portDefinition, final boolean isCheck, final boolean isAddress) {
		final String functionName = isCheck ? "check_getreply" : "getreply";
		final String printedFunctionName = isCheck ? "Check-getreply" : "Getreply";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template sender_template, final {1} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, senderType));
		source.append("\t\t\tif (procedure_queue.size() == 0) {\n");
		source.append("\t\t\t\tif(is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item head = procedure_queue.getFirst();\n");
		if (isAddress) {
			source.append("\t\t\tif (head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PMUNSUCC, MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue is not the system.\", get_name()));\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (head.sender_address == null) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"{0} operation on port '{'0'}' requires the address of the sender, which was not given by the test port.\", get_name()));\n", printedFunctionName));
			source.append("\t\t\t} else if (!sender_template.match(head.sender_address, false)) {\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC);\n");
			source.append("\t\t\t\t\tsender_template.log_match(head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), TitanComponent.SYSTEM_COMPREF, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}\n");
		} else {
			source.append("\t\t\tif (!sender_template.match(head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event_str(MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue does not match the from clause: \", get_name()));\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}\n");
		}
		source.append("\t\t\tswitch (head.item_selection) {\n");
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			if (!portDefinition.outProcedures.get(i).isNoBlock) {
				source.append(MessageFormat.format("\t\t\tcase REPLY_{0}:\n", i));
			}
		}

		source.append("\t\t\t{\n");
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		if(isAddress) {
			source.append("\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_PMSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PMIN)) {\n");
			source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.reply__op, head.sender_component, {0}, new TitanCharString(\"\"), msg_head_count+1);\n", isCheck ? "true" : "false"));
			source.append("\t\t\t\t}\n");
		} else {
			source.append("\t\t\t\tTTCN_Logger.log(head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMSUCCESS : TTCN_Logger.Severity.MATCHING_PCSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component ==  TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_PMIN : TTCN_Logger.Severity.PORTEVENT_PCIN;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.reply__op, head.sender_component, {0} ,new TitanCharString(\"\"), msg_head_count+1);\n", isCheck ? "true" : "false"));
			source.append("\t\t\t\t}\n");
		}
		if (!isCheck) {
			source.append("\t\t\t\tremove_proc_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log({0}, MessageFormat.format(\"Matching on port '{'0'}' failed: First entity in the queue is not a reply.\", get_name()));\n", isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC"));
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the getreply or check(getreply) function for
	 * a signature type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateTypedGetreply(final StringBuilder source, final PortDefinition portDefinition, final int index, final procedureSignatureInfo info, final boolean isCheck, final boolean isAddress) {
		final String functionName = isCheck ? "check_getreply" : "getreply";
		final String printedFunctionName = isCheck ? "Check-getreply" : "Getreply";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template getreply_template, final {2}_template sender_template, final {1}_reply_redirect param_ref, final {2} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, info.mJavaTypeName, senderType));
		if (info.hasReturnValue) {
			source.append("\t\t\tif (getreply_template.return_value().get_selection() == template_sel.ANY_OR_OMIT) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"{0} operation using '''*''' as return value matching template\");\n", printedFunctionName));
			source.append("\t\t\t}\n");
		}
		source.append("\t\t\tif (procedure_queue.size() == 0) {\n");
		source.append("\t\t\t\tif(is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item head = procedure_queue.getFirst();\n");
		if (isAddress) {
			source.append("\t\t\tif (head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PMUNSUCC, MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue is not the system.\", get_name()));\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (head.sender_address == null) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"{0} operation on port '{'0'}' requires the address of the sender, which was not given by the test port.\", get_name()));\n", printedFunctionName));
			source.append("\t\t\t} else if (!sender_template.match(head.sender_address, false)) {\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC);\n");
			source.append("\t\t\t\t\tsender_template.log_match(head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), TitanComponent.SYSTEM_COMPREF, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		} else {
			source.append("\t\t\tif (!sender_template.match(head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event_str(MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue does not match the from clause: \", get_name()));\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		}
		source.append(MessageFormat.format(" else if (head.item_selection != proc_selection.REPLY_{0}) '{'\n", index));
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log({0}, MessageFormat.format(\"Matching on port '{'0'}' failed: The first entity in the queue is not a reply for signature {1}.\", get_name()));\n ",  isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC",portDefinition.displayName));
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\t'}' else if (!getreply_template.match_reply(head.reply_{0}, true)) '{'\n", index));
		source.append(MessageFormat.format("\t\t\t\tfinal TTCN_Logger.Severity log_sev = {0};\n", isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC"));
		source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
		source.append(MessageFormat.format("\t\t\t\t\tgetreply_template.log_match_reply(head.reply_{0}, false);\n", index));
		source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.parameters__of__reply__do__not__match__template, TTCN_Logger.end_event_log2str());\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t} else {\n");
		source.append(MessageFormat.format("\t\t\t\tparam_ref.set_parameters(head.reply_{0});\n", index));
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		generate_proc_incoming_data_logging(source, "reply", "getreply_template.log_match_reply", isAddress, isCheck, index);
		if (!isCheck) {
			source.append("\t\t\t\tremove_proc_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the generic get_exception or check(catch)
	 * function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateGenericGetexception(final StringBuilder source, final PortDefinition portDefinition, final boolean isCheck, final boolean isAddress) {
		final String functionName = isCheck ? "check_catch" : "get_exception";
		final String printedFunctionName = isCheck ? "Check-catch" : "Catch";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template sender_template, final {1} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, senderType));
		source.append("\t\t\tif (procedure_queue.size() == 0) {\n");
		source.append("\t\t\t\tif(is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item head = procedure_queue.getFirst();\n");
		if (isAddress) {
			source.append("\t\t\tif (head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PMUNSUCC, MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue is not the system.\", get_name()));\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (head.sender_address == null) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"{0} operation on port '{'0'}' requires the address of the sender, which was not given by the test port.\", get_name()));\n", printedFunctionName));
			source.append("\t\t\t} else if (!sender_template.match(head.sender_address, false)) {\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC);\n");
			source.append("\t\t\t\t\tsender_template.log_match(head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), TitanComponent.SYSTEM_COMPREF, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}\n");
		} else {
			source.append("\t\t\tif (!sender_template.match(head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_event_str(MessageFormat.format(\"Matching on port {0} failed: Sender of the first entity in the queue does not match the from clause: \", get_name()));\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}\n");
		}
		source.append("\t\t\tswitch (head.item_selection) {\n");
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			if (portDefinition.outProcedures.get(i).hasExceptions) {
				source.append(MessageFormat.format("\t\t\tcase EXCEPTION_{0}:\n", i));
			}
		}

		source.append("\t\t\t{\n");
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		if(isAddress) {
			source.append("\t\t\t\tTTCN_Logger.log(TTCN_Logger.Severity.MATCHING_PMSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PMIN)) {\n");
			source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.exception__op, head.sender_component, {0}, new TitanCharString(\"\"), msg_head_count+1);\n", isCheck ? "true" : "false"));
			source.append("\t\t\t\t}\n");
		} else {
			source.append("\t\t\t\tTTCN_Logger.log(head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMSUCCESS : TTCN_Logger.Severity.MATCHING_PCSUCCESS,  MessageFormat.format(\"Matching on port {0} succeeded.\", get_name()));\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component ==  TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_PMIN : TTCN_Logger.Severity.PORTEVENT_PCIN;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.exception__op, head.sender_component, {0} ,new TitanCharString(\"\"), msg_head_count+1);\n", isCheck ? "true" : "false"));
			source.append("\t\t\t\t}\n");
		}
		if (!isCheck) {
			source.append("\t\t\t\tremove_proc_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tdefault:\n");
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log({0}, MessageFormat.format(\"Matching on port '{'0'}' failed: First entity in the queue is not an exception.\", get_name()));\n", isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC"));
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the get_exception or check(catch) function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * */
	private static void generateTypedGetexception(final StringBuilder source, final PortDefinition portDefinition, final int index, final procedureSignatureInfo info, final boolean isCheck, final boolean isAddress) {
		final String functionName = isCheck ? "check_catch" : "get_exception";
		final String printedFunctionName = isCheck ? "Check-catch" : "Catch";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_exception_template catch_template, final {2}_template sender_template, final {2} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, info.mJavaTypeName, senderType));
		if (info.hasReturnValue) {
			source.append("\t\t\tif (catch_template.is_any_or_omit()) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(\"{0} operation using '''*''' as matching template\");\n", printedFunctionName));
			source.append("\t\t\t}\n");
		}
		source.append("\t\t\tif (procedure_queue.size() == 0) {\n");
		source.append("\t\t\t\tif(is_started) {\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_MAYBE;\n");
		source.append("\t\t\t\t} else {\n");
		source.append("\t\t\t\t\tTTCN_Logger.log_str(TTCN_Logger.Severity.MATCHING_PROBLEM, MessageFormat.format(\"Matching on port {0} failed: Port is not started and the queue is empty.\", get_name()));\n");
		source.append("\t\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item head = procedure_queue.getFirst();\n");
		if (isAddress) {
			source.append("\t\t\tif (head.sender_component != TitanComponent.SYSTEM_COMPREF) {\n");
			source.append("\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__is__not__system, new TitanCharString(\"\"));\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t} else if (head.sender_address == null) {\n");
			source.append(MessageFormat.format("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"{0} operation on port '{'0'}' requires the address of the sender, which was not given by the test port.\", get_name()));\n", printedFunctionName));
			source.append("\t\t\t} else if (!sender_template.match(head.sender_address, false)) {\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC);\n");
			source.append("\t\t\t\t\tsender_template.log_match(head.sender_address, false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		} else {
			source.append("\t\t\tif (!sender_template.match(head.sender_component, false)) {\n");
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\t\tsender_template.log_match(new TitanComponent(head.sender_component), false);\n");
			source.append("\t\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), head.sender_component, TitanLoggerApi.MatchingFailureType_reason.enum_type.sender__does__not__match__from__clause, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t\t}\n");
			source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
			source.append("\t\t\t}");
		}
		source.append(MessageFormat.format(" else if (head.item_selection != proc_selection.EXCEPTION_{0}) '{'\n", index));
		source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_matching_failure(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), {0}, TitanLoggerApi.MatchingFailureType_reason.enum_type.not__an__exception__for__signature, new TitanCharString(\"{1} \"));\n", isAddress ? "TitanComponent.SYSTEM_COMPREF" : "head.sender_component", info.mDisplayName));
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\t'}' else if (!catch_template.match(head.exception_{0}, true)) '{'\n", index));
		if(isAddress) {
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMUNSUCC)) {\n");
		} else {
			source.append("\t\t\t\tfinal TTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMUNSUCC : TTCN_Logger.Severity.MATCHING_PCUNSUCC;\n");
			source.append("\t\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
		}
		source.append(MessageFormat.format("\t\t\t\t\tTTCN_Logger.begin_event({0});\n", isAddress ? "TTCN_Logger.Severity.MATCHING_PMUNSUCC" : "log_sev"));
		source.append(MessageFormat.format("\t\t\t\t\tcatch_template.log_match(head.exception_{0}, false);\n", index));
		source.append("\t\t\t\t\tTTCN_Logger.end_event_log2str();\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_NO;\n");
		source.append("\t\t\t} else {\n");
		source.append(MessageFormat.format("\t\t\t\tcatch_template.set_value(head.exception_{0});\n", index));
		if (portDefinition.realtime) {
			source.append("\t\t\t\tif (timestamp_redirect != null && head.timestamp.is_bound()) {\n");
			source.append("\t\t\t\t\ttimestamp_redirect.operator_assign(head.timestamp);\n");
			source.append("\t\t\t\t}\n");
		}
		source.append("\t\t\t\tif (sender_pointer != null) {\n");
		if (isAddress) {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_address);\n");
		} else {
			source.append("\t\t\t\t\tsender_pointer.operator_assign(head.sender_component);\n");
		}
		source.append("\t\t\t\t}\n");
		generate_proc_incoming_data_logging(source, "exception", "catch_template.log_match", isAddress, isCheck, index);
		if (!isCheck) {
			source.append("\t\t\t\tremove_proc_queue_head();\n");
		}
		source.append("\t\t\t\treturn TitanAlt_Status.ALT_YES;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the type incoming call function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateTypedIcomingCall(final StringBuilder source, final int index, final procedureSignatureInfo info, final PortDefinition portDefinition) {
		final StringBuilder comment = new StringBuilder();
		final StringBuilder header = new StringBuilder();
		comment.append("\t\t/**\n");
		comment.append(MessageFormat.format("\t\t * Inserts a procedure call of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
		comment.append("\t\t * Test Port.\n");
		comment.append("\t\t *\n");
		comment.append("\t\t * @param incoming_par\n");
		comment.append("\t\t *            the value to be inserted.\n");
		comment.append("\t\t * @param sender_component\n");
		comment.append("\t\t *            the sender component.\n");
		header.append(MessageFormat.format("\t\tprotected void incoming_call(final {0}_call incoming_par, final int sender_component", info.mJavaTypeName));
		if (portDefinition.testportType == TestportType.ADDRESS) {
			comment.append("\t\t * @param sender_address\n");
			comment.append("\t\t *            the address of the sender.\n");
			header.append(MessageFormat.format(", final {0} sender_address", portDefinition.addressName));
		}
		if (portDefinition.realtime) {
			comment.append("\t\t * @param timestamp\n");
			comment.append("\t\t *            the timestamp to return.\n");
			header.append(", final TitanFloat timestamp");
		}
		comment.append("\t\t * */\n");
		source.append(comment);
		source.append(header);

		source.append(") {\n" );
		source.append("\t\t\tif (!is_started) {\n" );
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Port {0} is not started but a call has arrived on it.\", get_name()));\n");
		source.append("\t\t\t}\n" );
		source.append("\t\t\tif(TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE)) {\n");
		if(portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE);\n");
			source.append("\t\t\t\tTTCN_Logger.log_char('(');\n");
			source.append("\t\t\t\tsender_address.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_char(')');\n");
			source.append("\t\t\t\tTitanCharString tempLog = TTCN_Logger.end_event_log2str();\n");
		}
		source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE);\n");
		source.append("\t\t\t\tTTCN_Logger.log_char(' ');\n");
		source.append("\t\t\t\tincoming_par.log();\n");
		source.append("\t\t\t\tTTCN_Logger.log_port_queue(TitanLoggerApi.Port__Queue_operation.enum_type.enqueue__call, get_name(), sender_component, proc_tail_count, ");
		if(portDefinition.testportType == TestportType.ADDRESS) {
			source.append("tempLog, TTCN_Logger.end_event_log2str());\n");
		} else {
			source.append("new TitanCharString(\"\"), TTCN_Logger.end_event_log2str());\n");
		}
		source.append("}\n");
		source.append("\t\t\tfinal Procedure_queue_item newItem = new Procedure_queue_item();\n" );
		source.append(MessageFormat.format("\t\t\tnewItem.item_selection = proc_selection.CALL_{0};\n", index));
		source.append(MessageFormat.format("\t\t\tnewItem.call_{0} = new {1}_call(incoming_par);\n", index, info.mJavaTypeName));
		source.append("\t\t\tnewItem.sender_component = sender_component;\n" );
		if (portDefinition.realtime) {
			source.append("\t\t\tif(timestamp.is_bound()) {\n");
			source.append("\t\t\t\tnewItem.timestamp = new TitanFloat(timestamp);\n");
			source.append("\t\t\t}\n");
		}
		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\tif (sender_address != null) {\n" );
			source.append(MessageFormat.format("\t\t\t\tnewItem.sender_address = new {0}(sender_address);\n", portDefinition.addressName));
			source.append("\t\t\t} else {\n" );
			source.append("\t\t\t\tnewItem.sender_address = null;\n" );
			source.append("\t\t\t}\n" );
		}
		source.append("\t\t\tprocedure_queue.add(newItem);\n" );
		source.append("\t\t}\n\n");

		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t/**\n");
			source.append(MessageFormat.format("\t\t * Inserts a procedure call of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
			source.append("\t\t * Test Port.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param incoming_par\n");
			source.append("\t\t *            the value to be inserted.\n");
			source.append("\t\t * @param sender_component\n");
			source.append("\t\t *            the sender component.\n");
			source.append("\t\t * */\n");
			source.append(MessageFormat.format("\t\tprotected void incoming_call(final {0}_call incoming_par, final int sender_component) '{'\n", info.mJavaTypeName));
			source.append(MessageFormat.format("\t\t\tincoming_call(incoming_par, sender_component{0}, null);\n", portDefinition.realtime ? ", new TitanFloat()":""));
			source.append("\t\t}\n\n");
		}

		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Inserts a procedure call of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
		source.append("\t\t * Test Port, coming from the system component.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param incoming_par\n");
		source.append("\t\t *            the value to be inserted.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tprotected void incoming_call(final {0}_call incoming_par) '{'\n", info.mJavaTypeName));
		source.append(MessageFormat.format("\t\t\tincoming_call(incoming_par, TitanComponent.SYSTEM_COMPREF{0});\n", portDefinition.realtime ? ", new TitanFloat()":""));
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the type incoming reply function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateTypedIcomingReply(final StringBuilder source, final int index, final procedureSignatureInfo info, final PortDefinition portDefinition) {
		final StringBuilder comment = new StringBuilder();
		final StringBuilder header = new StringBuilder();
		comment.append("\t\t/**\n");
		comment.append(MessageFormat.format("\t\t * Inserts a procedure reply of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
		comment.append("\t\t * Test Port.\n");
		comment.append("\t\t *\n");
		comment.append("\t\t * @param incoming_par\n");
		comment.append("\t\t *            the value to be inserted.\n");
		comment.append("\t\t * @param sender_component\n");
		comment.append("\t\t *            the sender component.\n");
		header.append(MessageFormat.format("\t\tprotected void incoming_reply(final {0}_reply incoming_par, final int sender_component", info.mJavaTypeName));
		if (portDefinition.testportType == TestportType.ADDRESS) {
			comment.append("\t\t * @param sender_address\n");
			comment.append("\t\t *            the address of the sender.\n");
			header.append(MessageFormat.format(", final {0} sender_address", portDefinition.addressName));
		}
		if (portDefinition.realtime) {
			comment.append("\t\t * @param timestamp\n");
			comment.append("\t\t *            the timestamp to return.\n");
			header.append(", final TitanFloat timestamp");
		}
		comment.append("\t\t * */\n");
		source.append(comment);
		source.append(header);

		source.append(") {\n" );
		source.append("\t\t\tif (!is_started) {\n" );
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Port {0} is not started but a reply has arrived on it.\", get_name()));\n");
		source.append("\t\t\t}\n" );
		source.append("\t\t\tif(TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE)) {\n");
		if(portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE);\n");
			source.append("\t\t\t\tTTCN_Logger.log_char('(');\n");
			source.append("\t\t\t\tsender_address.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_char(')');\n");
			source.append("\t\t\t\tTitanCharString tempLog = TTCN_Logger.end_event_log2str();\n");
		}
		source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE);\n");
		source.append("\t\t\t\tTTCN_Logger.log_char(' ');\n");
		source.append("\t\t\t\tincoming_par.log();\n");
		source.append("\t\t\t\tTTCN_Logger.log_port_queue(TitanLoggerApi.Port__Queue_operation.enum_type.enqueue__reply, get_name(), sender_component, proc_tail_count, ");
		if(portDefinition.testportType == TestportType.ADDRESS) {
			source.append("tempLog, TTCN_Logger.end_event_log2str());\n");
		} else {
			source.append("new TitanCharString(\"\"), TTCN_Logger.end_event_log2str());\n");
		}
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item newItem = new Procedure_queue_item();\n" );
		source.append(MessageFormat.format("\t\t\tnewItem.item_selection = proc_selection.REPLY_{0};\n", index));
		source.append(MessageFormat.format("\t\t\tnewItem.reply_{0} = new {1}_reply(incoming_par);\n", index, info.mJavaTypeName));
		source.append("\t\t\tnewItem.sender_component = sender_component;\n" );
		if (portDefinition.realtime) {
			source.append("\t\t\tif(timestamp.is_bound()) {\n");
			source.append("\t\t\t\tnewItem.timestamp = new TitanFloat(timestamp);\n");
			source.append("\t\t\t}\n");
		}
		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\tif (sender_address != null) {\n" );
			source.append(MessageFormat.format("\t\t\t\tnewItem.sender_address = new {0}(sender_address);\n", portDefinition.addressName));
			source.append("\t\t\t} else {\n" );
			source.append("\t\t\t\tnewItem.sender_address = null;\n" );
			source.append("\t\t\t}\n" );
		}
		source.append("\t\t\tprocedure_queue.add(newItem);\n" );
		source.append("\t\t}\n\n");

		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t/**\n");
			source.append(MessageFormat.format("\t\t * Inserts a procedure reply of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
			source.append("\t\t * Test Port.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param incoming_par\n");
			source.append("\t\t *            the value to be inserted.\n");
			source.append("\t\t * @param sender_component\n");
			source.append("\t\t *            the sender component.\n");
			source.append("\t\t * */\n");
			source.append(MessageFormat.format("\t\tprotected void incoming_reply(final {0}_reply incoming_par, final int sender_component) '{'\n", info.mJavaTypeName));
			source.append(MessageFormat.format("\t\t\tincoming_reply(incoming_par, sender_component{0}, null);\n", portDefinition.realtime ? ", new TitanFloat()":""));
			source.append("\t\t}\n\n");
		}

		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Inserts a procedure reply of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
		source.append("\t\t * Test Port, coming from the system component.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param incoming_par\n");
		source.append("\t\t *            the value to be inserted.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tprotected void incoming_reply(final {0}_reply incoming_par) '{'\n", info.mJavaTypeName));
		source.append(MessageFormat.format("\t\t\tincoming_reply(incoming_par, TitanComponent.SYSTEM_COMPREF{0});\n", portDefinition.realtime ? ", new TitanFloat()":""));
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the type incoming exception function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateTypedIcomingException(final StringBuilder source, final int index, final procedureSignatureInfo info, final PortDefinition portDefinition) {
		final StringBuilder comment = new StringBuilder();
		final StringBuilder header = new StringBuilder();
		comment.append("\t\t/**\n");
		comment.append(MessageFormat.format("\t\t * Inserts a procedure exception of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
		comment.append("\t\t * Test Port.\n");
		comment.append("\t\t *\n");
		comment.append("\t\t * @param incoming_par\n");
		comment.append("\t\t *            the value to be inserted.\n");
		comment.append("\t\t * @param sender_component\n");
		comment.append("\t\t *            the sender component.\n");
		header.append(MessageFormat.format("\t\tprotected void incoming_exception(final {0}_exception incoming_par, final int sender_component", info.mJavaTypeName));
		if (portDefinition.testportType == TestportType.ADDRESS) {
			comment.append("\t\t * @param sender_address\n");
			comment.append("\t\t *            the address of the sender.\n");
			header.append(MessageFormat.format(", final {0} sender_address", portDefinition.addressName));
		}
		if (portDefinition.realtime) {
			comment.append("\t\t * @param timestamp\n");
			comment.append("\t\t *            the timestamp to return.\n");
			header.append(", final TitanFloat timestamp");
		}
		comment.append("\t\t * */\n");
		source.append(comment);
		source.append(header);

		source.append(") {\n" );
		source.append("\t\t\tif (!is_started) {\n" );
		source.append("\t\t\t\tthrow new TtcnError(MessageFormat.format(\"Port {0} is not started but an exception has arrived on it.\", get_name()));\n");
		source.append("\t\t\t}\n" );
		source.append("\t\t\tif(TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE)) {\n");
		if(portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE);\n");
			source.append("\t\t\t\tTTCN_Logger.log_char('(');\n");
			source.append("\t\t\t\tsender_address.log();\n");
			source.append("\t\t\t\tTTCN_Logger.log_char(')');\n");
			source.append("\t\t\t\tTitanCharString tempLog = TTCN_Logger.end_event_log2str();\n");
		}
		source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PQUEUE);\n");
		source.append("\t\t\t\tTTCN_Logger.log_char(' ');\n");
		source.append("\t\t\t\tincoming_par.log();\n");
		source.append("\t\t\t\tTTCN_Logger.log_port_queue(TitanLoggerApi.Port__Queue_operation.enum_type.enqueue__exception, get_name(), sender_component, proc_tail_count, ");
		if(portDefinition.testportType == TestportType.ADDRESS) {
			source.append("tempLog, TTCN_Logger.end_event_log2str());\n");
		} else {
			source.append("new TitanCharString(\"\"), TTCN_Logger.end_event_log2str());\n");
		}
		source.append("\t\t\t}\n");
		source.append("\t\t\tfinal Procedure_queue_item newItem = new Procedure_queue_item();\n" );
		source.append(MessageFormat.format("\t\t\tnewItem.item_selection = proc_selection.EXCEPTION_{0};\n", index));
		source.append(MessageFormat.format("\t\t\tnewItem.exception_{0} = new {1}_exception(incoming_par);\n", index, info.mJavaTypeName));
		source.append("\t\t\tnewItem.sender_component = sender_component;\n" );
		if (portDefinition.realtime) {
			source.append("\t\t\tif(timestamp.is_bound()) {\n");
			source.append("\t\t\t\tnewItem.timestamp = new TitanFloat(timestamp);\n");
			source.append("\t\t\t}\n");
		}
		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t\tif (sender_address != null) {\n" );
			source.append(MessageFormat.format("\t\t\t\tnewItem.sender_address = new {0}(sender_address);\n", portDefinition.addressName));
			source.append("\t\t\t} else {\n" );
			source.append("\t\t\t\tnewItem.sender_address = null;\n" );
			source.append("\t\t\t}\n" );
		}
		source.append("\t\t\tprocedure_queue.add(newItem);\n" );
		source.append("\t\t}\n\n");

		if (portDefinition.testportType == TestportType.ADDRESS) {
			source.append("\t\t/**\n");
			source.append(MessageFormat.format("\t\t * Inserts a procedure exception of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
			source.append("\t\t * Test Port.\n");
			source.append("\t\t *\n");
			source.append("\t\t * @param incoming_par\n");
			source.append("\t\t *            the value to be inserted.\n");
			source.append("\t\t * @param sender_component\n");
			source.append("\t\t *            the sender component.\n");
			source.append("\t\t * */\n");
			source.append(MessageFormat.format("\t\tprotected void incoming_exception(final {0}_exception incoming_par, final int sender_component) '{'\n", info.mJavaTypeName));
			source.append(MessageFormat.format("\t\t\tincoming_exception(incoming_par, sender_component{0}, null);\n", portDefinition.realtime ? ", new TitanFloat()":""));
			source.append("\t\t}\n\n");
		}

		source.append("\t\t/**\n");
		source.append(MessageFormat.format("\t\t * Inserts a procedure exception of {0} signature into the incoming procedure queue of this\n", info.mDisplayName));
		source.append("\t\t * Test Port, coming from the system component.\n");
		source.append("\t\t *\n");
		source.append("\t\t * @param incoming_par\n");
		source.append("\t\t *            the value to be inserted.\n");
		source.append("\t\t * */\n");
		source.append(MessageFormat.format("\t\tprotected void incoming_exception(final {0}_exception incoming_par) '{'\n", info.mJavaTypeName));
		source.append(MessageFormat.format("\t\t\tincoming_exception(incoming_par, TitanComponent.SYSTEM_COMPREF{0});\n", portDefinition.realtime ? ", new TitanFloat()":""));
		source.append("\t\t}\n\n");
	}

	/**
	 * This function generates the process_call function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateProcessCall(final StringBuilder source, final PortDefinition portDefinition) {
		source.append("\t\t@Override\n");
		source.append("\t\tprotected boolean process_call(final String signature_name, final Text_Buf incoming_buf, final int sender_component) {\n");
		//indent first if
		source.append("\t\t\t");
		for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

			if (i != 0) {
				source.append("\t\t\t} else ");
			}
			source.append(MessageFormat.format("if (\"{0}\".equals(signature_name)) '{'\n", info.mDisplayName));
			source.append(MessageFormat.format("\t\t\t\tfinal {0}_call incoming_par = new {0}_call();\n", info.mJavaTypeName));
			source.append("\t\t\t\tincoming_par.decode_text(incoming_buf);\n");
			source.append(MessageFormat.format("\t\t\t\tincoming_call(incoming_par, sender_component{0});\n", portDefinition.realtime ? ", new TitanFloat()":""));
			source.append("\t\t\t\treturn true;\n");
		}


		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n");
	}

	/**
	 * This function generates the process_reply function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateProcessReply(final StringBuilder source, final PortDefinition portDefinition) {
		source.append("\t\t@Override\n");
		source.append("\t\tprotected boolean process_reply(final String signature_name, final Text_Buf incoming_buf, final int sender_component) {\n");
		//indent first if
		source.append("\t\t\t");
		boolean isFirst = true;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (!info.isNoBlock) {
				if (!isFirst) {
					source.append("\t\t\t} else ");
				}
				isFirst = false;
				source.append(MessageFormat.format("if (\"{0}\".equals(signature_name)) '{'\n", info.mDisplayName));
				source.append(MessageFormat.format("\t\t\t\tfinal {0}_reply incoming_par = new {0}_reply();\n", info.mJavaTypeName));
				source.append("\t\t\t\tincoming_par.decode_text(incoming_buf);\n");
				source.append(MessageFormat.format("\t\t\t\tincoming_reply(incoming_par, sender_component{0});\n", portDefinition.realtime ? ", new TitanFloat()":""));
				source.append("\t\t\t\treturn true;\n");
			}
		}

		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n");
	}

	/**
	 * This function generates the process_exception function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	private static void generateProcessException(final StringBuilder source, final PortDefinition portDefinition) {
		source.append("\t\t@Override\n");
		source.append("\t\tprotected boolean process_exception(final String signature_name, final Text_Buf incoming_buf, final int sender_component) {\n");
		//indent first if
		source.append("\t\t\t");
		boolean isFirst = true;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (info.hasExceptions) {
				if (!isFirst) {
					source.append("\t\t\t} else ");
				}
				isFirst = false;
				source.append(MessageFormat.format("if (\"{0}\".equals(signature_name)) '{'\n", info.mDisplayName));
				source.append(MessageFormat.format("\t\t\t\tfinal {0}_exception incoming_par = new {0}_exception();\n", info.mJavaTypeName));
				source.append("\t\t\t\tincoming_par.decode_text(incoming_buf);\n");
				source.append(MessageFormat.format("\t\t\t\tincoming_exception(incoming_par, sender_component{0});\n", portDefinition.realtime ? ", new TitanFloat()":""));
				source.append("\t\t\t\treturn true;\n");
			}
		}


		source.append("\t\t\t} else {\n");
		source.append("\t\t\t\treturn false;\n");
		source.append("\t\t\t}\n");
		source.append("\t\t}\n");
	}

	/**
	 * A utility function for generating code for the standalone version of
	 * receive
	 * /trigger/getcall/getreply/catch/check/check-receive/check-getcall
	 * /check-getreply/check-catch/timeout/done/killed statements.
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param statement
	 *                the code generated for the statement as an expression.
	 * @param statementName
	 *                the name of the statement for display in error message
	 * @param canRepeat
	 *                {@code true} if the statement can repeat.
	 * @param location
	 *                the location of the statement to report errors to.
	 * */
	public static void generateCodeStandalone(final JavaGenData aData, final StringBuilder source, final String statement, final String statementName, final boolean canRepeat, final Location location) {
		aData.addBuiltinTypeImport("TitanAlt_Status");
		aData.addBuiltinTypeImport("TTCN_Default");
		aData.addBuiltinTypeImport("TtcnError");
		aData.addCommonLibraryImport("TTCN_Snapshot");

		final String tempLabel = aData.getTemporaryVariableName();

		source.append(MessageFormat.format("{0}: for( ; ; ) '{'\n", tempLabel));
		source.append("TitanAlt_Status alt_flag = TitanAlt_Status.ALT_UNCHECKED;\n");
		source.append("TitanAlt_Status default_flag = TitanAlt_Status.ALT_UNCHECKED;\n");
		source.append("TTCN_Snapshot.take_new(false);\n");
		source.append("for( ; ; ) {\n");
		source.append("if (alt_flag != TitanAlt_Status.ALT_NO) {\n");

		source.append(MessageFormat.format("alt_flag = {0};\n", statement));

		source.append("if (alt_flag == TitanAlt_Status.ALT_YES) {\n");
		source.append("break;\n");
		if (canRepeat) {
			source.append("} else if (alt_flag == TitanAlt_Status.ALT_REPEAT) {\n");
			source.append(MessageFormat.format("continue {0};\n", tempLabel));
			source.append("}\n");
		} else {
			source.append("}\n");
		}
		source.append("}\n");
		source.append("if (default_flag != TitanAlt_Status.ALT_NO) {\n");
		source.append("default_flag = TTCN_Default.try_altsteps();\n");
		source.append("if (default_flag == TitanAlt_Status.ALT_YES || default_flag == TitanAlt_Status.ALT_BREAK) {\n");
		source.append("break;\n");
		source.append("} else if (default_flag == TitanAlt_Status.ALT_REPEAT) {\n");
		source.append(MessageFormat.format("continue {0};\n", tempLabel));
		source.append("}\n");
		source.append("}\n");
		source.append("if (alt_flag == TitanAlt_Status.ALT_NO && default_flag == TitanAlt_Status.ALT_NO) {\n");
		source.append(MessageFormat.format("throw new TtcnError(\"Stand-alone {0} statement failed in file {1}, line {2}.\");\n", statementName, location.getFile().getProjectRelativePath(), location.getLine()));
		source.append("}\n");
		source.append("TTCN_Snapshot.take_new(true);\n");
		source.append("}\n");
		source.append("break;\n");
		source.append("}\n");
	}

	/**
	 * Generate code for logging
	 *
	 * Called from generateTypedGetcall, generateTypedgetreply,
	 * generateTypedexception
	 *
	 * @param source
	 *                where the source code is to be generated
	 * @param opStr
	 *                "call", "reply" or "exception"
	 * @param matchStr
	 *                "catch_template.log_match",
	 *                "getcall_template.log_match_call" or
	 *                "getreply_template.log_match_reply"
	 * @param isAddress
	 *                generate for address or not?
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param index
	 *                the index this signature type has in the selector.
	 */
	private static void generate_proc_incoming_data_logging(final StringBuilder source, final String opStr, final String matchStr, final boolean isAddress, final boolean isCheck, final int index) {
		String procOp = "";
		if ("call".equals(opStr)) {
			procOp = "call";
		} else if("reply".equals(opStr)) {
			procOp = "reply";
		} else if("exception".equals(opStr)) {
			procOp = "exception";
		}
		if(isAddress) {
			source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.MATCHING_PMSUCCESS)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.MATCHING_PMSUCCESS);\n");
			source.append(MessageFormat.format("\t\t\t\t{0}(head.{1}_{2},false);\n", matchStr, opStr, index));
			source.append("\t\t\t\tTTCN_Logger.log_matching_success(TitanLoggerApi.PortType.enum_type.procedure__, get_name(), TitanComponent.SYSTEM_COMPREF, TTCN_Logger.end_event_log2str());\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tif (TTCN_Logger.log_this_event(TTCN_Logger.Severity.PORTEVENT_PMIN)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(TTCN_Logger.Severity.PORTEVENT_PMIN);\n");
			source.append(MessageFormat.format("\t\t\t\thead.{0}_{1}.log();\n", opStr, index));
			source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.{0}__op, head.sender_component, {1}, TTCN_Logger.end_event_log2str(), msg_head_count+1);\n", procOp, isCheck ? "true" : "false"));
			source.append("\t\t\t}\n");
		} else {
			source.append("\t\t\tTTCN_Logger.Severity log_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.MATCHING_PMSUCCESS : TTCN_Logger.Severity.MATCHING_PCSUCCESS;\n");
			source.append("\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append("\t\t\t\tTTCN_Logger.log_event_str(MessageFormat.format(\"Matching on port {0} succeeded: \", get_name()));\n");
			source.append(MessageFormat.format("\t\t\t\t{0}(head.{1}_{2}, false);\n", matchStr, opStr, index));
			source.append("\t\t\t\tTTCN_Logger.end_event();\n");
			source.append("\t\t\t}\n");
			source.append("\t\t\tlog_sev = head.sender_component == TitanComponent.SYSTEM_COMPREF ? TTCN_Logger.Severity.PORTEVENT_PMIN : TTCN_Logger.Severity.PORTEVENT_PCIN;\n");
			source.append("\t\t\tif (TTCN_Logger.log_this_event(log_sev)) {\n");
			source.append("\t\t\t\tTTCN_Logger.begin_event(log_sev);\n");
			source.append(MessageFormat.format("\t\t\t\thead.{0}_{1}.log();\n", opStr, index));
			source.append(MessageFormat.format("\t\t\t\tTTCN_Logger.log_procport_recv(get_name(), TitanLoggerApi.Port__oper.enum_type.{0}__op, head.sender_component, {1} ,TTCN_Logger.end_event_log2str(), msg_head_count+1);\n", procOp, isCheck ? "true" : "false"));
			source.append("\t\t\t}\n");
		}
	}

	/**
	 * This function can be used to generate the necessary member functions
	 * of port array types
	 *
	 *
	 * @param aData
	 *                used to access build settings.
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * */
	public static void generatePortArrayBodyMembers(final JavaGenData aData, final StringBuilder source, final PortDefinition portDefinition, final long arraySize, final long indexOffset) {
		aData.addBuiltinTypeImport("Index_Redirect");

		for (int i = 0 ; i < portDefinition.inMessages.size(); i++) {
			final messageTypeInfo inType = portDefinition.inMessages.get(i);

			generateArrayBodyTypedReceive(source, i, inType, false, arraySize, indexOffset);
			generateArrayBodyTypedReceive(source, i, inType, true, arraySize, indexOffset);
			generateArrayBodyTypeTrigger(source, i, inType, arraySize, indexOffset);
		}

		for (int i = 0 ; i < portDefinition.inProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.inProcedures.get(i);

			generateArrayBodyTypedGetcall(source, portDefinition, i, info, false, false, arraySize, indexOffset);
			generateArrayBodyTypedGetcall(source, portDefinition, i, info, true, false, arraySize, indexOffset);
			if (portDefinition.testportType == TestportType.ADDRESS) {
				generateArrayBodyTypedGetcall(source, portDefinition, i, info, false, true, arraySize, indexOffset);
				generateArrayBodyTypedGetcall(source, portDefinition, i, info, true, true, arraySize, indexOffset);
			}
		}

		boolean hasIncomingReply = false;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (!info.isNoBlock) {
				hasIncomingReply = true;
			}
		}
		boolean hasIncomingException = false;
		for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
			final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

			if (info.hasExceptions) {
				hasIncomingException = true;
			}
		}

		if (hasIncomingReply) {
			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

				if (!portDefinition.outProcedures.get(i).isNoBlock) {
					generateArrayBodyTypedGetreply(source, portDefinition, i, info, false, false, arraySize, indexOffset);
					generateArrayBodyTypedGetreply(source, portDefinition, i, info, true, false, arraySize, indexOffset);
					if (portDefinition.testportType == TestportType.ADDRESS) {
						generateArrayBodyTypedGetreply(source, portDefinition, i, info, false, true, arraySize, indexOffset);
						generateArrayBodyTypedGetreply(source, portDefinition, i, info, true, true, arraySize, indexOffset);
					}
				}
			}
		}

		if (hasIncomingException) {
			for (int i = 0 ; i < portDefinition.outProcedures.size(); i++) {
				final procedureSignatureInfo info = portDefinition.outProcedures.get(i);

				if (portDefinition.outProcedures.get(i).hasExceptions) {
					generateArrayBodyTypedGetexception(source, portDefinition, i, info, false, false, arraySize, indexOffset);
					generateArrayBodyTypedGetexception(source, portDefinition, i, info, true, false, arraySize, indexOffset);
					if (portDefinition.testportType == TestportType.ADDRESS) {
						generateArrayBodyTypedGetexception(source, portDefinition, i, info, false, true, arraySize, indexOffset);
						generateArrayBodyTypedGetexception(source, portDefinition, i, info, true, true, arraySize, indexOffset);
					}
				}
			}
		}
	}

	/**
	 * This function generates the receive or check(receive) function for a
	 * array of port type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param index
	 *                the index this message type has in the declaration the
	 *                port type.
	 * @param inType
	 *                the information about the incoming message.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param arraySize
	 *                the size of the array.
	 * @param indexOffset
	 *                the index offset of this array.
	 * */
	private static void generateArrayBodyTypedReceive(final StringBuilder source, final int index, final messageTypeInfo inType, final boolean isCheck, final long arraySize, final long indexOffset) {
		final String typeValueName = inType.mJavaTypeName;
		final String typeTemplateName = inType.mJavaTemplateName;
		final String functionName = isCheck ? "check_receive" : "receive";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1} value_template, final Value_Redirect_Interface value_redirect, final TitanComponent_template sender_template, final TitanComponent sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, typeTemplateName));
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.incr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\tTitanAlt_Status result = TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\tfor (int i = 0; i < {0}; i++) '{'\n", arraySize));
		source.append(MessageFormat.format("\t\t\t\tfinal TitanAlt_Status ret_val = get_at(i).{0}(value_template, value_redirect, sender_template, sender_pointer, timestamp_redirect, index_redirect);\n", functionName));
		source.append("\t\t\t\tif (ret_val == TitanAlt_Status.ALT_YES) {\n");
		source.append("\t\t\t\t\tif (index_redirect != null) {\n");
		source.append(MessageFormat.format("\t\t\t\t\t\tindex_redirect.add_index(i + {0});\n", indexOffset));
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t} else if (ret_val == TitanAlt_Status.ALT_REPEAT || (ret_val == TitanAlt_Status.ALT_MAYBE && result == TitanAlt_Status.ALT_NO)) {\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.decr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\treturn result;\n");
		source.append("\t\t}\n");
	}

	/**
	 * This function generates the trigger function for an array of port
	 * type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param index
	 *                the index this message type has in the declaration the
	 *                port type.
	 * @param inType
	 *                the information about the incoming message.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param arraySize
	 *                the size of the array.
	 * @param indexOffset
	 *                the index offset of this array.
	 * */
	private static void generateArrayBodyTypeTrigger(final StringBuilder source, final int index, final messageTypeInfo inType, final long arraySize, final long indexOffset) {
		final String typeValueName = inType.mJavaTypeName;
		final String typeTemplateName = inType.mJavaTemplateName;

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status trigger(final {0} value_template, final Value_Redirect_Interface value_redirect, final TitanComponent_template sender_template, final TitanComponent sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", typeTemplateName));
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.incr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\tTitanAlt_Status result = TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\tfor (int i = 0; i < {0}; i++) '{'\n", arraySize));
		source.append("\t\t\t\tfinal TitanAlt_Status ret_val = get_at(i).trigger(value_template, value_redirect, sender_template, sender_pointer, timestamp_redirect, index_redirect);\n");
		source.append("\t\t\t\tif (ret_val == TitanAlt_Status.ALT_YES) {\n");
		source.append("\t\t\t\t\tif (index_redirect != null) {\n");
		source.append(MessageFormat.format("\t\t\t\t\t\tindex_redirect.add_index(i + {0});\n", indexOffset));
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t} else if (ret_val == TitanAlt_Status.ALT_REPEAT || (ret_val == TitanAlt_Status.ALT_MAYBE && result == TitanAlt_Status.ALT_NO)) {\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.decr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\treturn result;\n");
		source.append("\t\t}\n");
	}

	/**
	 * This function generates the getcall or check(getcall) function for an
	 * array of port type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * @param arraySize
	 *                the size of the array.
	 * @param indexOffset
	 *                the index offset of this array.
	 * */
	private static void generateArrayBodyTypedGetcall(final StringBuilder source, final PortDefinition portDefinition, final int index, final procedureSignatureInfo info, final boolean isCheck, final boolean isAddress, final long arraySize, final long indexOffset) {
		final String functionName = isCheck ? "check_getcall" : "getcall";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template getcall_template, final {2}_template sender_template, final {1}_call_redirect param_ref, final {2} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, info.mJavaTypeName, senderType));
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.incr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\tTitanAlt_Status result = TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\tfor (int i = 0; i < {0}; i++) '{'\n", arraySize));
		source.append(MessageFormat.format("\t\t\t\tfinal TitanAlt_Status ret_val = get_at(i).{0}(getcall_template, sender_template, param_ref, sender_pointer, timestamp_redirect, index_redirect);\n", functionName));
		source.append("\t\t\t\tif (ret_val == TitanAlt_Status.ALT_YES) {\n");
		source.append("\t\t\t\t\tif (index_redirect != null) {\n");
		source.append(MessageFormat.format("\t\t\t\t\t\tindex_redirect.add_index(i + {0});\n", indexOffset));
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t} else if (ret_val == TitanAlt_Status.ALT_REPEAT || (ret_val == TitanAlt_Status.ALT_MAYBE && result == TitanAlt_Status.ALT_NO)) {\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.decr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\treturn result;\n");
		source.append("\t\t}\n");
	}

	/**
	 * This function generates the getreply or check(getreply) function for
	 * an array of port type
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * @param arraySize
	 *                the size of the array.
	 * @param indexOffset
	 *                the index offset of this array.
	 * */
	private static void generateArrayBodyTypedGetreply(final StringBuilder source, final PortDefinition portDefinition, final int index, final procedureSignatureInfo info, final boolean isCheck, final boolean isAddress, final long arraySize, final long indexOffset) {
		final String functionName = isCheck ? "check_getreply" : "getreply";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_template getreply_template, final {2}_template sender_template, final {1}_reply_redirect param_ref, final {2} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, info.mJavaTypeName, senderType));
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.incr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\tTitanAlt_Status result = TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\tfor (int i = 0; i < {0}; i++) '{'\n", arraySize));
		source.append(MessageFormat.format("\t\t\t\tfinal TitanAlt_Status ret_val = get_at(i).{0}(getreply_template, sender_template, param_ref, sender_pointer, timestamp_redirect, index_redirect);\n", functionName));
		source.append("\t\t\t\tif (ret_val == TitanAlt_Status.ALT_YES) {\n");
		source.append("\t\t\t\t\tif (index_redirect != null) {\n");
		source.append(MessageFormat.format("\t\t\t\t\t\tindex_redirect.add_index(i + {0});\n", indexOffset));
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t} else if (ret_val == TitanAlt_Status.ALT_REPEAT || (ret_val == TitanAlt_Status.ALT_MAYBE && result == TitanAlt_Status.ALT_NO)) {\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.decr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\treturn result;\n");
		source.append("\t\t}\n");
	}

	/**
	 * This function generates the get_exception or check(catch) function.
	 *
	 * @param source
	 *                where the source code is to be generated.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param portDefinition
	 *                the definition of the port.
	 * @param index
	 *                the index this signature type has in the selector.
	 * @param info
	 *                the information about the signature.
	 * @param isCheck
	 *                generate the check or the non-checking version.
	 * @param isAddress
	 *                generate for address or not?
	 * @param arraySize
	 *                the size of the array.
	 * @param indexOffset
	 *                the index offset of this array.
	 * */
	private static void generateArrayBodyTypedGetexception(final StringBuilder source, final PortDefinition portDefinition, final int index, final procedureSignatureInfo info, final boolean isCheck, final boolean isAddress, final long arraySize, final long indexOffset) {
		final String functionName = isCheck ? "check_catch" : "get_exception";
		final String senderType = isAddress ? portDefinition.addressName : "TitanComponent";

		source.append(MessageFormat.format("\t\tpublic TitanAlt_Status {0}(final {1}_exception_template catch_template, final {2}_template sender_template, final {2} sender_pointer, final TitanFloat timestamp_redirect, final Index_Redirect index_redirect) '{'\n", functionName, info.mJavaTypeName, senderType));
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.incr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\tTitanAlt_Status result = TitanAlt_Status.ALT_NO;\n");
		source.append(MessageFormat.format("\t\t\tfor (int i = 0; i < {0}; i++) '{'\n", arraySize));
		source.append(MessageFormat.format("\t\t\t\tfinal TitanAlt_Status ret_val = get_at(i).{0}(catch_template, sender_template, sender_pointer, timestamp_redirect, index_redirect);\n", functionName));
		source.append("\t\t\t\tif (ret_val == TitanAlt_Status.ALT_YES) {\n");
		source.append("\t\t\t\t\tif (index_redirect != null) {\n");
		source.append(MessageFormat.format("\t\t\t\t\t\tindex_redirect.add_index(i + {0});\n", indexOffset));
		source.append("\t\t\t\t\t}\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t\tbreak;\n");
		source.append("\t\t\t\t} else if (ret_val == TitanAlt_Status.ALT_REPEAT || (ret_val == TitanAlt_Status.ALT_MAYBE && result == TitanAlt_Status.ALT_NO)) {\n");
		source.append("\t\t\t\t\tresult = ret_val;\n");
		source.append("\t\t\t\t}\n");
		source.append("\t\t\t}\n");
		source.append("\t\t\tif (index_redirect != null) {\n");
		source.append("\t\t\t\tindex_redirect.decr_pos();\n");
		source.append("\t\t\t}\n");

		source.append("\t\t\treturn result;\n");
		source.append("\t\t}\n");
	}
}
