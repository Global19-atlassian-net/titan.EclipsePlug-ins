/******************************************************************************
 * Copyright (c) 2000-2021 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-2.0/EPL-2.0.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.definitions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.titan.designer.AST.ASTVisitor;
import org.eclipse.titan.designer.AST.Assignment;
import org.eclipse.titan.designer.AST.Assignment.Assignment_type;
import org.eclipse.titan.designer.AST.FieldSubReference;
import org.eclipse.titan.designer.AST.ILocateableNode;
import org.eclipse.titan.designer.AST.INamedNode;
import org.eclipse.titan.designer.AST.IReferenceChain;
import org.eclipse.titan.designer.AST.ISubReference;
import org.eclipse.titan.designer.AST.IType;
import org.eclipse.titan.designer.AST.IValue;
import org.eclipse.titan.designer.AST.Identifier;
import org.eclipse.titan.designer.AST.Location;
import org.eclipse.titan.designer.AST.NULL_Location;
import org.eclipse.titan.designer.AST.Reference;
import org.eclipse.titan.designer.AST.ReferenceChain;
import org.eclipse.titan.designer.AST.ReferenceFinder;
import org.eclipse.titan.designer.AST.ReferenceFinder.Hit;
import org.eclipse.titan.designer.AST.Scope;
import org.eclipse.titan.designer.AST.Type;
import org.eclipse.titan.designer.AST.TTCN3.Expected_Value_type;
import org.eclipse.titan.designer.AST.TTCN3.IIncrementallyUpdateable;
import org.eclipse.titan.designer.AST.TTCN3.TTCN3Scope;
import org.eclipse.titan.designer.AST.TTCN3.definitions.FormalParameter.parameterEvaluationType;
import org.eclipse.titan.designer.AST.TTCN3.templates.ITTCN3Template.Template_type;
import org.eclipse.titan.designer.AST.TTCN3.templates.NamedParameter;
import org.eclipse.titan.designer.AST.TTCN3.templates.NamedParameters;
import org.eclipse.titan.designer.AST.TTCN3.templates.NotUsed_Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.ParsedActualParameters;
import org.eclipse.titan.designer.AST.TTCN3.templates.TTCN3Template;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstance;
import org.eclipse.titan.designer.AST.TTCN3.templates.TemplateInstances;
import org.eclipse.titan.designer.compiler.JavaGenData;
import org.eclipse.titan.designer.editors.ProposalCollector;
import org.eclipse.titan.designer.editors.SkeletonTemplateProposal;
import org.eclipse.titan.designer.editors.actions.DeclarationCollector;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3CodeSkeletons;
import org.eclipse.titan.designer.editors.ttcn3editor.TTCN3Keywords;
import org.eclipse.titan.designer.parsers.CompilationTimeStamp;
import org.eclipse.titan.designer.parsers.ttcn3parser.ReParseException;
import org.eclipse.titan.designer.parsers.ttcn3parser.TTCN3ReparseUpdater;

/**
 * The FormalParameterList class represents the formal parameter lists found in
 * TTCN3.
 *
 * @author Kristof Szabados
 * */
public class FormalParameterList extends TTCN3Scope implements ILocateableNode, IIncrementallyUpdateable {
	private static final String FULLNAMEPART = ".<unknown_parameter>";
	public static final String DUPLICATEPARAMETERFIRST = "Duplicate parameter with name `{0}'' was first declared here";
	public static final String DUPLICATEPARAMETERREPEATED = "Duplicate parameter with name `{0}'' was declared here again";
	public static final String HIDINGSCOPEELEMENT = "Parameter with identifier `{0}'' is not unique in the scope hierarchy";
	public static final String HIDDENSCOPEELEMENT = "Previous definition with identifier `{0}'' in higher scope unit is here";
	public static final String HIDINGMODULEIDENTIFIER = "Parameter with name `{0}'' hides a module identifier";
	public static final String MISSINGPARAMETER = "There is no value specified for formal parameter `{0}''";
	private static final String ILLEGALACTIVATEPARAMETER = "Parameter {0} of {1} refers to {2},"
			+ " which is a local definition within a statement block and may have shorter lifespan than the activated default."
			+ " Only references to variables and timers defined in the component type can be passed to activated defaults";
	private static final String CANNOTBESTARTED = "a parameter or embedded in a parameter of a function used in a start operation."
			+ " {0} `{1}'' cannot be start on a parallel test component";
	private static final String WILLREMAINUNCHANGED =
		"{0} `{1}'' started on parallel test components. Its `out'' and `inout'' parameters will remain unchanged at the end of the operation.";

	private final List<FormalParameter> parameters = new ArrayList<FormalParameter>();
	private Location location = NULL_Location.INSTANCE;

	/** the definition containing this formal parameter list. */
	private Definition myDefinition;

	/** the time when this formal parameter list was check the last time. */
	private CompilationTimeStamp lastTimeChecked;

	/**
	 * the time when this formal parameter list was checked for unigueness
	 * the last time.
	 */
	private CompilationTimeStamp lastTimeUniquenessChecked;

	/** A map of the parameters. */
	private Map<String, FormalParameter> parameterMap;

	/**
	 * The minimum number of actual parameters that might be considered a
	 * valid list for this list of formal parameters.
	 */
	private int minimumNofParameters = 0;

	// stores whether functions with this formal parameter list can be
	// started or not
	private boolean isStartable;

	public FormalParameterList(final List<FormalParameter> parameters) {
		for (final FormalParameter parameter : parameters) {
			if (parameter != null && parameter.getIdentifier() != null) {
				this.parameters.add(parameter);
				parameter.setFullNameParent(this);
				parameter.setFormalParamaterList(this);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public final StringBuilder getFullName(final INamedNode child) {
		final StringBuilder builder = super.getFullName(child);

		for (final FormalParameter parameter : parameters) {
			if (parameter == child) {
				final Identifier identifier = parameter.getIdentifier();
				return builder.append(INamedNode.DOT).append((identifier != null) ? identifier.getDisplayName() : FULLNAMEPART);
			}
		}

		return builder;
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

	public final Definition getMyDefinition() {
		return myDefinition;
	}

	public final void setMyDefinition(final Definition definition) {
		myDefinition = definition;
	}

	public final boolean getStartability() {
		return isStartable;
	}

	public final int getNofParameters() {
		return parameters.size();
	}

	public final FormalParameter getParameterByIndex(final int index) {
		return parameters.get(index);
	}

	public final FormalParameter getParameterById(final Identifier id) {
		if (parameterMap == null || id == null) {
			return null;
		}

		return parameterMap.get(id.getName());
	}

	/**
	 * Checks if a "-" was specified in the formal parameter list.
	 *
	 * @return true if one of the parameters has notused as default value.
	 * */
	public final boolean hasNotusedDefaultValue() {
		for (int i = 0; i < parameters.size(); i++) {
			if (parameters.get(i).hasNotusedDefaultValue()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Checks whether the formal parameter list has only formal parameters
	 * that have default values. In case of template, they can be called
	 * without parenthesis.
	 *
	 * @return true if each formal parameter has a default value, false
	 *         otherwise.
	 * */
	public final boolean hasOnlyDefaultValues() {
		for (int i = 0; i < parameters.size(); i++) {
			if (null == parameters.get(i).getDefaultParameter()) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Sets the scope of the formal parameter list.
	 *
	 * @param scope
	 *                the scope to be set
	 * */
	public final void setMyScope(final Scope scope) {
		setParentScope(scope);
		for (final FormalParameter parameter : parameters) {
			parameter.setMyScope(this);
		}
	}

	/** reset the properties tracking the use of the formal parameters */
	public void reset () {
		for (final FormalParameter parameter : parameters) {
			parameter.reset();
		}
	}

	/**
	 * Checks the uniqueness of the parameters, and also builds a hashmap of
	 * them to speed up further searches.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * */
	private void checkUniqueness(final CompilationTimeStamp timestamp) {
		if (lastTimeUniquenessChecked != null && !lastTimeUniquenessChecked.isLess(timestamp)) {
			return;
		}

		if (parameterMap != null) {
			parameterMap.clear();
		}

		for (final FormalParameter parameter: parameters) {
			if (parameter != null) {
				final String parameterName = parameter.getIdentifier().getName();

				if (parameterMap == null) {
					parameterMap = new HashMap<String, FormalParameter>(parameters.size());
				}

				if (parameterMap.containsKey(parameterName)) {
					final Location otherLocation = parameterMap.get(parameterName).getIdentifier().getLocation();
					otherLocation.reportSingularSemanticError(MessageFormat.format(DUPLICATEPARAMETERFIRST, parameter
							.getIdentifier().getDisplayName()));
					final Location paramLocation = parameter.getIdentifier().getLocation();
					paramLocation.reportSemanticError(MessageFormat.format(DUPLICATEPARAMETERREPEATED, parameter.getIdentifier()
							.getDisplayName()));
				} else {
					parameterMap.put(parameterName, parameter);
				}
			}
		}

		lastTimeUniquenessChecked = timestamp;
	}

	public final void check(final CompilationTimeStamp timestamp, final Assignment_type definitionType) {
		if (lastTimeChecked != null && !lastTimeChecked.isLess(timestamp)) {
			return;
		}

		minimumNofParameters = 0;
		isStartable = true;

		for (int i = 0, size = parameters.size(); i < size; i++) {
			FormalParameter parameter = parameters.get(i);
			final Identifier identifier = parameter.getIdentifier();
			if (parentScope != null) {
				if (parentScope.hasAssignmentWithId(timestamp, identifier)) {
					parameter.getLocation().reportSemanticError(
							MessageFormat.format(HIDINGSCOPEELEMENT, identifier.getDisplayName()));
					parentScope.hasAssignmentWithId(timestamp, identifier);
					final List<ISubReference> subReferences = new ArrayList<ISubReference>();
					subReferences.add(new FieldSubReference(identifier));
					final Reference reference = new Reference(null, subReferences);
					final Assignment assignment = parentScope.getAssBySRef(timestamp, reference);
					if (assignment != null && assignment.getLocation() != null) {
						assignment.getLocation().reportSingularSemanticWarning(
								MessageFormat.format(HIDDENSCOPEELEMENT, identifier.getDisplayName()));
					}
				} else if (parentScope.isValidModuleId(identifier)) {
					parameter.getLocation().reportSemanticWarning(
							MessageFormat.format(HIDINGMODULEIDENTIFIER, identifier.getDisplayName()));
				}
			}

			parameter.check(timestamp);
			if (parameter.getAssignmentType() != parameter.getRealAssignmentType()) {
				parameter = parameter.setParameterType(parameter.getRealAssignmentType());
				parameters.set(i, parameter);
			}

			if (Assignment_type.A_TEMPLATE.semanticallyEquals(definitionType)) {
				if (!Assignment_type.A_PAR_VAL_IN.semanticallyEquals(parameter.getAssignmentType())
						&& !Assignment_type.A_PAR_TEMP_IN.semanticallyEquals(parameter.getAssignmentType())) {
					parameter.getLocation().reportSemanticError("A template cannot have " + parameter.getAssignmentName());
				}
			} else if (Assignment_type.A_TESTCASE.semanticallyEquals(definitionType)) {
				if (Assignment_type.A_PAR_TIMER.semanticallyEquals(parameter.getAssignmentType())
						&& Assignment_type.A_PAR_PORT.semanticallyEquals(parameter.getAssignmentType())) {
					parameter.getLocation().reportSemanticError("A testcase cannot have " + parameter.getAssignmentName());
				}
			} else if (Assignment_type.A_PORT.semanticallyEquals(definitionType)) {
				switch (parameter.getAssignmentType()) {
				case A_PAR_VAL:
				case A_PAR_VAL_IN:
				case A_PAR_VAL_OUT:
				case A_PAR_VAL_INOUT: {
					final IReferenceChain refChain = ReferenceChain.getInstance(IReferenceChain.CIRCULARREFERENCE, true);
					parameter.getType(CompilationTimeStamp.getBaseTimestamp()).checkMapParameter(timestamp, refChain, parameter.getLocation());
					refChain.release();
					break;
				}
				default:
					parameter.getLocation().reportSemanticError(MessageFormat.format("The `map'/`unmap' parameters of a port type cannot have {0}", parameter.getAssignmentName()));
					break;
				}
			} else {
				// everything is allowed for functions and altsteps
			}

			// startability check
			switch (parameter.getAssignmentType()) {
			case A_PAR_VAL:
			case A_PAR_VAL_IN:
			case A_PAR_VAL_OUT:
			case A_PAR_VAL_INOUT:
			case A_PAR_TEMP_IN:
			case A_PAR_TEMP_OUT:
			case A_PAR_TEMP_INOUT: {
				final IType tempType = parameter.getType(timestamp);
				if (isStartable && tempType != null && tempType.isComponentInternal(timestamp)) {
					isStartable = false;
				}
				break;
			}
			default:
				isStartable = false;
				break;
			}

			if (!parameter.hasDefaultValue()) {
				minimumNofParameters = i + 1;
			}
		}

		checkUniqueness(timestamp);

		lastTimeChecked = timestamp;
	}

	/**
	 * Checks the properties of the parameter list, that can only be checked
	 * after the semantic check was completely run.
	 */
	public final void postCheck() {
		for (final FormalParameter parameter: parameters) {
			parameter.postCheck();
		}
	}

	/**
	 * check that @lazy paramterization not used in cases currently
	 * unsupported
	 */
	public final void checkNoLazyParams() {
		for (final FormalParameter formalParameter: parameters) {
			if (formalParameter.getEvaluationType() != parameterEvaluationType.NORMAL_EVAL) {
				final Location tempLocation = formalParameter.getLocation();
				tempLocation.reportSemanticError(MessageFormat.format(
						"Formal parameter `{0}'' cannot be @{1}, not supported in this case.", formalParameter.getAssignmentName(), formalParameter.getEvaluationType() == parameterEvaluationType.LAZY_EVAL? "lazy" : "fuzzy"));
			}
		}
	}

	/**
	 * Read the parsed actual parameters, and collate the lazy and non-lazy actual parameters
	 * according to their associated formal parameters.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param parsedParameters
	 *                the parsed actual parameters (may contain named, and unnamed parts too).
	 * @param actualLazyParameters
	 *                the list of actual lazy parameters returned for later usage.
	 * @param actualNonLazyParameters
	 *                the list of actual non lazy parameters returned for later usage.
	 */
	public final void collateLazyAndNonLazyActualParameters(final CompilationTimeStamp timestamp, final ParsedActualParameters parsedParameters,
			final ActualParameterList actualLazyParameters,final ActualParameterList actualNonLazyParameters) {

		final TemplateInstances unnamed = parsedParameters.getInstances();
		final NamedParameters named = parsedParameters.getNamedParameters();
		int nofLocated = unnamed.getNofTis();

		final Map<FormalParameter, Integer> formalParameterMap = new HashMap<FormalParameter, Integer>();
		for (int i = 0, size = parameters.size(); i < size; i++) {
			formalParameterMap.put(parameters.get(i), Integer.valueOf(i));
		}

		final TemplateInstances finalUnnamed = new TemplateInstances(unnamed);

		for (int i = 0, size = named.getNofParams(); i < size; i++) {
			final NamedParameter namedParameter = named.getParamByIndex(i);
			final FormalParameter formalParameter = parameterMap.get(namedParameter.getName().getName());
			final int isAt = formalParameterMap.get(formalParameter);
			for (; nofLocated < isAt; nofLocated++) {
				final NotUsed_Template temp = new NotUsed_Template();
				if (!parameters.get(nofLocated).hasDefaultValue()) {
					temp.setIsErroneous(true);
				}

				final TemplateInstance instance = new TemplateInstance(null, null, temp);
				instance.setLocation(parsedParameters.getLocation());
				finalUnnamed.addTemplateInstance(instance);
			}
			finalUnnamed.addTemplateInstance(namedParameter.getInstance());
			nofLocated++;
		}

		finalUnnamed.setLocation(parsedParameters.getLocation());

		final int upperLimit = (finalUnnamed.getNofTis() < parameters.size()) ? finalUnnamed.getNofTis() : parameters.size();

		for (int i = 0; i < upperLimit; i++) {
			final TemplateInstance instance = finalUnnamed.getInstanceByIndex(i);
			final FormalParameter formalParameter = parameters.get(i);
			if (instance.getType() == null && instance.getDerivedReference() == null
					&& Template_type.TEMPLATE_NOTUSED.equals(instance.getTemplateBody().getTemplatetype())) {

				final ActualParameter defaultValue = formalParameter.getDefaultValue();
				final Default_ActualParameter temp = new Default_ActualParameter(defaultValue);
				if (defaultValue != null && !defaultValue.getIsErroneous()) {
					temp.setLocation(defaultValue.getLocation());
				}

				if(formalParameter.getEvaluationType() == parameterEvaluationType.LAZY_EVAL) {
					actualLazyParameters.addParameter(temp);
				} else if(formalParameter.getEvaluationType() == parameterEvaluationType.NORMAL_EVAL) {
					actualNonLazyParameters.addParameter(temp);
				}
			} else {
				final ActualParameter actualParameter = formalParameter.checkActualParameter(timestamp, instance, Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
				actualParameter.setLocation(instance.getLocation());
				if(formalParameter.getEvaluationType() == parameterEvaluationType.LAZY_EVAL) {
					actualLazyParameters.addParameter(actualParameter);
				} else if(formalParameter.getEvaluationType() == parameterEvaluationType.NORMAL_EVAL){
					actualNonLazyParameters.addParameter(actualParameter);
				}
			}
		}

		for (int i = upperLimit; i < parameters.size(); i++) {
			final FormalParameter formalParameter = parameters.get(i);
			final ActualParameter defaultValue = formalParameter.getDefaultValue();
			final Default_ActualParameter temp = new Default_ActualParameter(defaultValue);
			if (defaultValue != null && !defaultValue.getIsErroneous()) {
				temp.setLocation(defaultValue.getLocation());
			}

			if(formalParameter.getEvaluationType() == parameterEvaluationType.LAZY_EVAL) {
				actualLazyParameters.addParameter(temp);
			} else if(formalParameter.getEvaluationType() == parameterEvaluationType.NORMAL_EVAL) {
				actualNonLazyParameters.addParameter(temp);
			}
		}
	}

	/**
	 * Check if a list of parsed actual parameters is semantically correct
	 * according to a list of formal parameters (the called entity).
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param parsedParameters
	 *                the parsed actual parameters (may contain named, and
	 *                unnamed parts too).
	 * @param actualParameters
	 *                the list of actual parameters returned for later
	 *                usage.
	 *
	 * @return true if a semantic error was found, false otherwise
	 * */
	public final boolean checkActualParameterList(final CompilationTimeStamp timestamp, final ParsedActualParameters parsedParameters,
			final ActualParameterList actualParameters) {
		parsedParameters.setFormalParList(this);

		checkUniqueness(timestamp);

		boolean isErroneous = false;

		final TemplateInstances unnamed = parsedParameters.getInstances();
		final NamedParameters named = parsedParameters.getNamedParameters();
		int nofLocated = unnamed.getNofTis();

		final Map<FormalParameter, Integer> formalParameterMap = new HashMap<FormalParameter, Integer>();
		for (int i = 0, size = parameters.size(); i < size; i++) {
			formalParameterMap.put(parameters.get(i), Integer.valueOf(i));
		}

		final TemplateInstances finalUnnamed = new TemplateInstances(unnamed);

		for (int i = 0, size = named.getNofParams(); i < size; i++) {
			final NamedParameter namedParameter = named.getParamByIndex(i);

			if (parameterMap != null && parameterMap.containsKey(namedParameter.getName().getName())) {
				final FormalParameter formalParameter = parameterMap.get(namedParameter.getName().getName());
				final int isAt = formalParameterMap.get(formalParameter);

				if (isAt >= nofLocated) {
					for (; nofLocated < isAt; nofLocated++) {
						final NotUsed_Template temp = new NotUsed_Template();
						if (!parameters.get(nofLocated).hasDefaultValue()) {
							temp.setIsErroneous(true);
						}
						final TemplateInstance instance = new TemplateInstance(null, null, temp);
						instance.setLocation(parsedParameters.getLocation());
						finalUnnamed.addTemplateInstance(instance);
					}
					finalUnnamed.addTemplateInstance(namedParameter.getInstance());
					nofLocated++;
				} else {
					isErroneous = true;

					if (isAt >= unnamed.getNofTis()) {
						namedParameter.getLocation().reportSemanticError(
								MessageFormat.format("Named parameter `{0}'' is out of order", namedParameter
										.getName().getDisplayName()));
					} else {
						namedParameter.getLocation().reportSemanticError(
								MessageFormat.format("Named parameter `{0}'' is assigned more than once",
										namedParameter.getName().getDisplayName()));
					}
				}
			} else {
				String name;
				switch (myDefinition.getAssignmentType()) {
				case A_TYPE:
					switch (((Def_Type) myDefinition).getType(timestamp).getTypetype()) {
					case TYPE_FUNCTION:
						name = "Function reference";
						break;
					case TYPE_ALTSTEP:
						name = "Altstep reference";
						break;
					case TYPE_TESTCASE:
						name = "Testcase reference";
						break;
					default:
						name = "";
						break;
					}
					break;
				default:
					name = myDefinition.getAssignmentName();
					break;
				}

				isErroneous = true;
				namedParameter.getLocation().reportSemanticError(
						MessageFormat.format("{0} `{1}'' has no formal parameter with name `{2}''", name,
								myDefinition.getFullName(), namedParameter.getName().getDisplayName()));
			}
		}

		if (isErroneous) {
			return false;
		}

		finalUnnamed.setLocation(parsedParameters.getLocation());

		return checkActualParameterList(timestamp, finalUnnamed, actualParameters);
	}

	/**
	 * Check if a list of parsed actual parameters is semantically correct
	 * according to a list of formal parameters (the called entity).
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param instances
	 *                the list of actual parameters containing both the
	 *                named and the unnamed part converted into an unnamed
	 *                list, that is full and in correct order.
	 * @param actualParameters
	 *                the list of actual parameters returned for later
	 *                usage.
	 *
	 * @return true if a semantic error was found, false otherwise
	 * */
	private boolean checkActualParameterList(final CompilationTimeStamp timestamp, final TemplateInstances instances,
			final ActualParameterList actualParameters) {
		boolean isErroneous = false;

		if (minimumNofParameters == parameters.size()) {
			if (instances.getNofTis() != parameters.size()) {
				instances.getLocation().reportSemanticError(
						MessageFormat.format("Too {0} parameters: {1} was expected instead of {2}",
								(instances.getNofTis() < parameters.size()) ? "few" : "many", parameters.size(),
										instances.getNofTis()));
				isErroneous = true;
			}
		} else {
			if (instances.getNofTis() < minimumNofParameters) {
				instances.getLocation().reportSemanticError(
						MessageFormat.format("Too few parameters: at least {0} was expected instaed of {1}",
								minimumNofParameters, instances.getNofTis()));
				isErroneous = true;
			} else if (instances.getNofTis() > parameters.size()) {
				instances.getLocation().reportSemanticError(
						MessageFormat.format("Too many parameters: at most {0} was expected instead of {1}",
								parameters.size(), instances.getNofTis()));
				isErroneous = true;
			}
		}

		final int upperLimit = (instances.getNofTis() < parameters.size()) ? instances.getNofTis() : parameters.size();
		for (int i = 0; i < upperLimit; i++) {
			final TemplateInstance instance = instances.getInstanceByIndex(i);
			final FormalParameter formalParameter = parameters.get(i);

			if (instance.getType() == null && instance.getDerivedReference() == null
					&& Template_type.TEMPLATE_NOTUSED.equals(instance.getTemplateBody().getTemplatetype())) {
				if (formalParameter.hasDefaultValue()) {
					final ActualParameter defaultValue = formalParameter.getDefaultValue();
					final Default_ActualParameter temp = new Default_ActualParameter(defaultValue);
					actualParameters.addParameter(temp);
					if (defaultValue == null || defaultValue.getIsErroneous()) {
						isErroneous = true;
					} else {
						temp.setLocation(defaultValue.getLocation());
					}
				} else if (instance.getTemplateBody().getIsErroneous(timestamp)) {
					instances.getLocation().reportSemanticError(
							MessageFormat.format(MISSINGPARAMETER, formalParameter.getIdentifier().getDisplayName()));
					isErroneous = true;
				} else {
					instance.getLocation().reportSemanticError(
							"Not used symbol (`-'') cannot be used for parameter that does not have a default value");
					final ActualParameter temp = new Value_ActualParameter(null);
					temp.setLocation(instances.getLocation());
					temp.setIsErroneous();
					actualParameters.addParameter(temp);
					isErroneous = true;
				}
			} else {
				final ActualParameter actualParameter = formalParameter.checkActualParameter(timestamp, instance,
						Expected_Value_type.EXPECTED_DYNAMIC_VALUE);
				actualParameter.setLocation(instance.getLocation());
				actualParameters.addParameter(actualParameter);
				if (actualParameter.getIsErroneous()) {
					isErroneous = true;
				}
			}
		}

		for (int i = upperLimit; i < parameters.size(); i++) {
			final FormalParameter formalParameter = parameters.get(i);
			if (formalParameter.hasDefaultValue()) {
				final ActualParameter defaultValue = formalParameter.getDefaultValue();
				final Default_ActualParameter temp = new Default_ActualParameter(defaultValue);
				actualParameters.addParameter(temp);
				if (defaultValue == null || defaultValue.getIsErroneous()) {
					isErroneous = true;
				} else {
					temp.setLocation(defaultValue.getLocation());
				}
			} else {
				final ActualParameter temp = new Value_ActualParameter(null);
				temp.setLocation(instances.getLocation());
				temp.setIsErroneous();
				actualParameters.addParameter(temp);
				isErroneous = true;
			}
		}

		return isErroneous;
	}

	/**
	 * Check if a list of parsed actual parameters is semantically correct
	 * according to a list of formal parameters in an activate
	 * statement/operation.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param actualParameters
	 *                the list of actual parameters containing both the
	 *                named and the unnamed part converted into an unnamed
	 *                list, that is full and in correct order.
	 * @param description
	 *                the description of the assignment to be used for
	 *                reporting errors
	 *
	 * @return true if a semantic error was not found, false otherwise
	 * */
	public final boolean checkActivateArgument(final CompilationTimeStamp timestamp, final ActualParameterList actualParameters,
			final String description) {
		if (actualParameters == null) {
			return false;
		}

		boolean returnValue = true;
		for (int i = 0; i < actualParameters.getNofParameters(); i++) {
			final ActualParameter actualParameter = actualParameters.getParameter(i);

			if (!(actualParameter instanceof Referenced_ActualParameter)) {
				continue;
			}

			final FormalParameter formalParameter = parameters.get(i);

			switch (formalParameter.getAssignmentType()) {
			case A_PAR_VAL_OUT:
			case A_PAR_VAL_INOUT:
			case A_PAR_TEMP_OUT:
			case A_PAR_TEMP_INOUT:
			case A_PAR_TIMER:
				// the checking shall be performed for these
				// parameter types
				break;
			case A_PAR_PORT:
				// port parameters are always correct because
				// ports can be defined only in component types
				continue;
			default:
				return false;
			}

			final Reference reference = ((Referenced_ActualParameter) actualParameter).getReference();
			final Assignment assignment = reference.getRefdAssignment(timestamp, true);
			if (assignment == null) {
				return false;
			}

			switch (assignment.getAssignmentType()) {
			case A_VAR:
			case A_VAR_TEMPLATE:
			case A_TIMER:
				// it is not allowed to pass references of local
				// variables or timers
				if (assignment.isLocal()) {
					reference.getLocation().reportSemanticError(
							MessageFormat.format(ILLEGALACTIVATEPARAMETER, i + 1, description,
									assignment.getDescription()));
					returnValue = false;
				}
				break;
			case A_PAR_VAL:
			case A_PAR_VAL_IN:
			case A_PAR_VAL_OUT:
			case A_PAR_VAL_INOUT:
			case A_PAR_TEMP_IN:
			case A_PAR_TEMP_OUT:
			case A_PAR_TEMP_INOUT:
			case A_PAR_TIMER: {
				// it is not allowed to pass references pointing
				// to formal parameters
				// except for activate() statements within
				// testcases
				final FormalParameter referencedFormalParameter = (FormalParameter) assignment;
				final FormalParameterList formalParameterList = referencedFormalParameter.getMyParameterList();
				if (formalParameterList != null) {
					final Definition definition = formalParameterList.getMyDefinition();
					if (definition != null && !Assignment_type.A_TESTCASE.semanticallyEquals(definition.getAssignmentType())) {
						reference.getLocation().reportSemanticError(
								MessageFormat.format(ILLEGALACTIVATEPARAMETER, i + 1, description,
										assignment.getDescription()));
						returnValue = false;
					}
				}
				break;
			}
			default:
				break;
			}
		}

		return returnValue;
	}

	/**
	 * Checks the parameter list for startability: reports error if the
	 * owner function cannot be started on a PTC. Used by functions and
	 * function types.
	 *
	 * @param timestamp
	 *                the timestamp of the actual semantic check cycle.
	 * @param what
	 *                shall contain a short description of the caller.
	 * @param namedNode
	 *                shall contain the namedNode of the function of
	 *                function type.
	 * @param errorLocation
	 *                the location to report the error to, if needed.
	 * */
	public void checkStartability(final CompilationTimeStamp timestamp, final String what, final INamedNode namedNode,
			final Location errorLocation) {

		for (int i = 0; i < parameters.size(); i++) {
			final FormalParameter parameter = parameters.get(i);
			switch (parameter.getAssignmentType()) {
			case A_PAR_VAL_OUT:
			case A_PAR_TEMP_OUT:
			case A_PAR_VAL_INOUT:
			case A_PAR_TEMP_INOUT:
				errorLocation.reportSemanticWarning(MessageFormat.format(WILLREMAINUNCHANGED,what, namedNode.getFullName()));
				//intentionally not broken
			case A_PAR_VAL:
			case A_PAR_VAL_IN:
			case A_PAR_TEMP_IN:
			 {
				final IType tempType = parameter.getType(timestamp);
				if (tempType != null && tempType.isComponentInternal(timestamp)) {
					final Set<IType> typeSet = new HashSet<IType>();
					final String errorString = MessageFormat.format(CANNOTBESTARTED, what, namedNode.getFullName(),
							parameter.getDescription());
					tempType.checkComponentInternal(timestamp, typeSet, errorString);
				}
				break;
			}
			default:
				errorLocation.reportSemanticError(MessageFormat.format(
						"{0} `{1}'' cannot be started on a parallel test component because it has {2}", what,
						namedNode.getFullName(), parameter.getDescription()));
				break;
			}
		}
	}

	/**
	 * Checks the compatibility of two formal parameter lists.
	 * They are compatible if every parameter is compatible,
	 *   has the same attribute, type, restriction and name.
	 *
	 * Please note that all errors will be reported to the location provided as the last parameter.
	 * In themselves both formal parameter lists might be OK,
	 *   so the error needs to be reported to the location where they are compared.
	 *
	 * @param timestamp the compilation timestamp
	 * @param fpList the formal parameter list to be compared to the actual one.
	 * @param callSite the location where errors should be reported to.
	 * */
	public void checkCompatibility(final CompilationTimeStamp timestamp, final FormalParameterList fpList, final Location callSite) {
		if (parameters.size() != fpList.parameters.size()) {
			callSite.reportSemanticError(MessageFormat.format("{0} formal parameters was expected instead of {1}", parameters.size(), fpList.parameters.size()));
		}

		final int upperLimit = Math.min(parameters.size(), fpList.parameters.size());
		for(int i = 0; i < upperLimit; i++) {
			final FormalParameter typeParameter = parameters.get(i);
			final FormalParameter functionParameter = fpList.getParameterByIndex(i);

			if (typeParameter.getIsErroneous() || functionParameter.getIsErroneous()) {
				continue;
			}

			if(!typeParameter.getAssignmentType().semanticallyEquals(functionParameter.getAssignmentType())) {
				callSite.reportSemanticError(MessageFormat.format("The kind of the {0}th parameter is not the same: {1} was expected instead of {2}", i, typeParameter.getAssignmentName(), functionParameter.getAssignmentName()));
			}

			if(typeParameter.getAssignmentType() != Assignment_type.A_TIMER &&
					functionParameter.getAssignmentType() != Assignment_type.A_TIMER) {
				final Type typeParameterType = typeParameter.getType(timestamp);
				final Type functionParameterType = functionParameter.getType(timestamp);
				if(!typeParameterType.isIdentical(timestamp, functionParameterType)) {
					callSite.reportSemanticError(MessageFormat.format("The type of the {0}th parameter is not the same: `{1}'' was expected instead of `{2}''", i, typeParameterType.getTypename(), functionParameterType.getTypename()));
				}
			}

			if(typeParameter.getTemplateRestriction() != functionParameter.getTemplateRestriction()) {
				callSite.reportSemanticError(MessageFormat.format("The template restriction of the {0}th parameter is not the same: `{1}'' was expected instead of `{2}''", i, typeParameter.getTemplateRestriction().getDisplayName(), functionParameter.getTemplateRestriction().getDisplayName()));
			}

			if (typeParameter.getEvaluationType() != functionParameter.getEvaluationType()) {
				callSite.reportSemanticError(MessageFormat.format("{0}th parameter evaluation type (normal, @lazy or @fuzzy) mismatch", i));
			}

			if(!typeParameter.getIdentifier().equals(functionParameter.getIdentifier())) {
				callSite.reportSemanticWarning(MessageFormat.format("The name of the {0}th parameter is not the same: `{1}'' was expected instead of `{2}''", i, typeParameter.getIdentifier().getDisplayName(), functionParameter.getIdentifier().getDisplayName()));
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public final boolean hasAssignmentWithId(final CompilationTimeStamp timestamp, final Identifier identifier) {
		if (parameterMap != null && parameterMap.containsKey(identifier.getName())) {
			return true;
		}
		if (parentScope != null) {
			return parentScope.hasAssignmentWithId(timestamp, identifier);
		}
		return false;
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getAssBySRef(final CompilationTimeStamp timestamp, final Reference reference) {
		return getAssBySRef(timestamp, reference, null);
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getAssBySRef(final CompilationTimeStamp timestamp, final Reference reference, final IReferenceChain refChain) {
		if (reference.getModuleIdentifier() != null || parameterMap == null) {
			return getParentScope().getAssBySRef(timestamp, reference);
		}

		final Identifier identifier = reference.getId();
		if (identifier == null) {
			return getParentScope().getAssBySRef(timestamp, reference);
		}

		final Assignment assignment = parameterMap.get(identifier.getName());
		if (assignment != null) {
			return assignment;
		}

		return getParentScope().getAssBySRef(timestamp, reference);

	}

	/**
	 * Creates a representation of this formal parameter list for use as the
	 * part of the description of a proposal.
	 *
	 * @param builder
	 *                the StringBuilder to append the representation to.
	 * @return the StringBuilder after appending the representation.
	 * */
	public final StringBuilder getAsProposalDesriptionPart(final StringBuilder builder) {
		for (int i = 0, size = parameters.size(); i < size; i++) {
			if (i != 0) {
				builder.append(", ");
			}
			parameters.get(i).getAsProposalDesriptionPart(builder);
		}
		return builder;
	}

	/**
	 * Creates a representation of this formal parameter list for use as a
	 * proposal part.
	 *
	 * @param builder
	 *                the StringBuilder to append the representation to.
	 * @return the StringBuilder after appending the representation.
	 * */
	public final StringBuilder getAsProposalPart(final StringBuilder builder) {
		for (int i = 0, size = parameters.size(); i < size; i++) {
			if (i != 0) {
				builder.append(", ");
			}
			parameters.get(i).getAsProposalPart(builder);
		}

		return builder;
	}

	@Override
	/** {@inheritDoc} */
	public final void addProposal(final ProposalCollector propCollector) {
		if (propCollector.getReference().getModuleIdentifier() == null) {
			for (final FormalParameter parameter : parameters) {
				parameter.addProposal(propCollector, 0);
			}
		}
		super.addProposal(propCollector);
	}

	@Override
	/** {@inheritDoc} */
	public void addSkeletonProposal(final ProposalCollector propCollector) {
		for (final SkeletonTemplateProposal templateProposal : TTCN3CodeSkeletons.FORMAL_VALUE_PARAMETER_PROPOSALS) {
			propCollector.addTemplateProposal(templateProposal.getPrefix(), templateProposal.getProposal(),
					TTCN3CodeSkeletons.SKELETON_IMAGE);
		}
		for (final SkeletonTemplateProposal templateProposal : TTCN3CodeSkeletons.FORMAL_TEMPLATE_PARAMETER_PROPOSALS) {
			propCollector.addTemplateProposal(templateProposal.getPrefix(), templateProposal.getProposal(),
					TTCN3CodeSkeletons.SKELETON_IMAGE);
		}
		for (final SkeletonTemplateProposal templateProposal : TTCN3CodeSkeletons.FORMAL_TIMER_PARAMETER_PROPOSALS) {
			propCollector.addTemplateProposal(templateProposal.getPrefix(), templateProposal.getProposal(),
					TTCN3CodeSkeletons.SKELETON_IMAGE);
		}
	}

	@Override
	/** {@inheritDoc} */
	public final void addKeywordProposal(final ProposalCollector propCollector) {
		propCollector.addProposal(TTCN3Keywords.FORMAL_PARAMETER_SCOPE, null, TTCN3Keywords.KEYWORD);
		super.addKeywordProposal(propCollector);
	}

	@Override
	/** {@inheritDoc} */
	public final void addDeclaration(final DeclarationCollector declarationCollector) {
		if (declarationCollector.getReference().getModuleIdentifier() == null) {
			final Identifier identifier = declarationCollector.getReference().getId();
			if (parameterMap != null && parameterMap.containsKey(identifier.getName())) {
				parameterMap.get(identifier.getName()).addDeclaration(declarationCollector, 0);
			}
		}
		super.addDeclaration(declarationCollector);
	}

	/**
	 * Handles the incremental parsing of this list of formal parameters.
	 *
	 * @param reparser
	 *                the parser doing the incremental parsing.
	 * @param isDamaged
	 *                true if the location contains the damaged area, false
	 *                if only its' location needs to be updated.
	 * */
	@Override
	public final void updateSyntax(final TTCN3ReparseUpdater reparser, final boolean isDamaged) throws ReParseException {
		if (isDamaged) {
			throw new ReParseException();
		}

		for (final FormalParameter parameter : parameters) {
			parameter.updateSyntax(reparser, isDamaged);
			reparser.updateLocation(parameter.getLocation());
		}
	}

	@Override
	/** {@inheritDoc} */
	public Assignment getEnclosingAssignment(final int offset) {
		if (parameters == null) {
			return null;
		}
		for (final FormalParameter parameter : parameters) {
			if (parameter.getLocation().containsOffset(offset)) {
				return parameter;
			}
		}
		return null;
	}

	@Override
	/** {@inheritDoc} */
	public void findReferences(final ReferenceFinder referenceFinder, final List<Hit> foundIdentifiers) {
		if (parameters != null) {
			for (final FormalParameter formalPar : parameters) {
				formalPar.findReferences(referenceFinder, foundIdentifiers);
			}
		}
	}

	@Override
	/** {@inheritDoc} */
	public boolean accept(final ASTVisitor v) {
		switch (v.visit(this)) {
		case ASTVisitor.V_ABORT:
			return false;
		case ASTVisitor.V_SKIP:
			return true;
		}
		if (parameters != null) {
			for (final FormalParameter formalPar : parameters) {
				if (!formalPar.accept(v)) {
					return false;
				}
			}
		}
		if (v.leave(this) == ASTVisitor.V_ABORT) {
			return false;
		}
		return true;
	}

	public void setGenName(final String prefix) {
		for (final FormalParameter parameter : parameters) {
			final String parameterName = parameter.getIdentifier().getName();
			if (!Assignment_type.A_TIMER.equals(parameter.getAssignmentType())) {
				final Type parameterType = parameter.getType(CompilationTimeStamp.getBaseTimestamp());
				if (parameterType != null) {
					parameterType.setGenName(prefix, parameterName);
				}
			}

			if (parameter.hasDefaultValue()) {
				final StringBuilder embeddedName = new StringBuilder(prefix);
				embeddedName.append('_');
				embeddedName.append(parameterName);
				embeddedName.append("_defval");
				final ActualParameter defaultValue = parameter.getDefaultValue();
				if (defaultValue instanceof Value_ActualParameter) {
					final IValue value = ((Value_ActualParameter) defaultValue).getValue();
					//value.setGenNamePrefix("const_");//currently does not need the prefix
					value.setGenNameRecursive(embeddedName.toString());
				} else if (defaultValue instanceof Template_ActualParameter) {
					final TemplateInstance instance = ((Template_ActualParameter) defaultValue).getTemplateInstance();
					final TTCN3Template template = instance.getTemplateBody();
					//template.setGenNamePrefix("template_");//currently does not need the prefix
					template.setGenNameRecursive(embeddedName.toString());
				}
			}
		}
	}

	/**
	 * Add generated java code on this level.
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 */
	public void generateCode( final JavaGenData aData, final StringBuilder source ) {
		final int size = parameters.size();
		if ( size == 0 ) {
			return;
		}
		source.append( ' ' );
		for ( int i = 0; i < size; i++ ) {
			final FormalParameter parameter = parameters.get( i );
			if ( i > 0 ) {
				source.append( ", " );
			}
			parameter.generateCodeString( aData, source );
		}
		source.append( ' ' );
	}

	/** Generates the Java equivalent of the formal parameter list.
	 *  The names of unused parameters are not displayed (unless forced).
	 *
	 *  @param aData the structure to put imports into and get temporal variable names from.
	 *
	 *  originally generate_code(char *str, size_t display_unused)
	 */
	public StringBuilder generateCode(final JavaGenData aData) {
		final StringBuilder result = new StringBuilder();

		for ( int i = 0 ; i < parameters.size(); i++) {
			if (i > 0) {
				result.append(", ");
			}
			result.append(parameters.get(i).generateCodeFpar(aData));
		}

		return result;
	}

	/**
	 * Generates the value assignments of the default value of parameters.
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 * */
	public void generateCodeDefaultValues(final JavaGenData aData, final StringBuilder source) {
		for ( int i = 0 ; i < parameters.size(); i++) {
			parameters.get(i).generateCodeDefaultValue(aData, source);
		}
	}

	/**
	 * Generate code for actual parameter list with the provided prefix.
	 *
	 * @param prefix the prefix to use
	 *
	 * originally: generate_code_actual_parlist
	 * */
	public StringBuilder generateCodeActualParlist(final String prefix) {
		final StringBuilder result = new StringBuilder();

		for ( int i = 0 ; i < parameters.size(); i++) {
			if (i > 0) {
				result.append(", ");
			}
			result.append(prefix);
			result.append(parameters.get(i).getIdentifier().getName());
		}

		return result;
	}

	/**
	 * Generate Java code for the shadows of formal parameters if needed.
	 * These variables are used to let the user change in parameters value inside th function, with no effect outside of it.
	 *
	 * generate_shadow_objects in the compiler
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 */
	public void generateCodeShadowObjects(final JavaGenData aData, final StringBuilder source) {
		for ( int i = 0 ; i < parameters.size(); i++) {
			parameters.get(i).generateCodeShadowObject(aData, source);
		}
	}

	/**
	 * Generates the code that cleans up the out parameters upon entering a function.
	 *
	 * @param aData the structure to put imports into and get temporal variable names from.
	 * @param source the source code generated
	 * */
	public void generateCodeSetUnbound(final JavaGenData aData, final StringBuilder source) {
		for ( int i = 0 ; i < parameters.size(); i++) {
			parameters.get(i).generateCodeSetUnbound(aData, source);
		}
	}
}
