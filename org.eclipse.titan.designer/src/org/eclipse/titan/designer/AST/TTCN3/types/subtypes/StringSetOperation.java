/******************************************************************************
 * Copyright (c) 2000-2018 Ericsson Telecom AB
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.eclipse.titan.designer.AST.TTCN3.types.subtypes;

import org.eclipse.titan.designer.AST.TTCN3.types.subtypes.StringSetConstraint.ConstraintType;

/**
 * @author Adam Delic
 * */
public final class StringSetOperation extends StringSubtypeTreeElement {
	public enum OperationType {
		INTERSECTION, INHERIT, UNION, EXCEPT
	}

	private final OperationType operationType;
	private StringSubtypeTreeElement a;
	private StringSubtypeTreeElement b;

	public StringSetOperation(final StringType stringType, final OperationType operationType, final StringSubtypeTreeElement a,
			final StringSubtypeTreeElement b) {
		super(stringType);
		this.operationType = operationType;
		this.a = a;
		this.b = b;
	}

	@Override
	/** {@inheritDoc} */
	public ElementType getElementType() {
		return ElementType.OPERATION;
	}

	@Override
	/** {@inheritDoc} */
	public SubtypeConstraint complement() {
		final StringSetOperation returnValue = new StringSetOperation(stringType, OperationType.EXCEPT, new FullStringSet(stringType), this);
		return returnValue.evaluate();
	}

	@Override
	/** {@inheritDoc} */
	public SubtypeConstraint intersection(final SubtypeConstraint other) {
		final StringSetOperation returnValue = new StringSetOperation(stringType, OperationType.INTERSECTION, this,
				(StringSubtypeTreeElement) other);
		return returnValue.evaluate();
	}

	@Override
	/** {@inheritDoc} */
	public boolean isElement(final Object o) {
		switch (operationType) {
		case INHERIT:
		case INTERSECTION:
			return a.isElement(o) && b.isElement(o);
		case UNION:
			return a.isElement(o) || b.isElement(o);
		case EXCEPT:
			return a.isElement(o) && !b.isElement(o);
		default:
			return false;
		}
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isEmpty() {
		switch (operationType) {
		case INHERIT:
		case INTERSECTION:
			return a.isEmpty().or(b.isEmpty());
		case UNION:
			return a.isEmpty().and(b.isEmpty());
		case EXCEPT: {
			final TernaryBool aEmpty = a.isEmpty();
			return ((aEmpty != TernaryBool.TFALSE) ? aEmpty : ((b.isEmpty() == TernaryBool.TTRUE) ? TernaryBool.TFALSE
					: TernaryBool.TUNKNOWN));
		}
		default:
			return TernaryBool.TUNKNOWN;
		}
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isEqual(final SubtypeConstraint other) {
		return TernaryBool.TUNKNOWN;
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isFull() {
		switch (operationType) {
		case INHERIT:
		case INTERSECTION:
			return a.isFull().and(b.isFull());
		case UNION:
			return a.isFull().or(b.isFull());
		case EXCEPT:
			return a.isFull().and(b.isEmpty());
		default:
			return TernaryBool.TUNKNOWN;
		}
	}

	@Override
	/** {@inheritDoc} */
	public void toString(final StringBuilder sb) {
		sb.append('(');
		a.toString(sb);
		switch (operationType) {
		case INHERIT:
			sb.append(" intersection ");
			break;
		case INTERSECTION:
			sb.append(" intersection ");
			break;
		case UNION:
			sb.append(" union ");
			break;
		case EXCEPT:
			sb.append(" except ");
			break;
		default:
			sb.append(" <unknown operation> ");
		}
		b.toString(sb);
		sb.append(')');
	}

	@Override
	/** {@inheritDoc} */
	public SubtypeConstraint union(final SubtypeConstraint other) {
		final StringSetOperation returnValue = new StringSetOperation(stringType, OperationType.UNION, this, (StringSubtypeTreeElement) other);
		return returnValue.evaluate();
	}

	@Override
	/** {@inheritDoc} */
	public SubtypeConstraint except(final SubtypeConstraint other) {
		final StringSetOperation returnValue = new StringSetOperation(stringType, OperationType.EXCEPT, this, (StringSubtypeTreeElement) other);
		return returnValue.evaluate();
	}

	@Override
	/** {@inheritDoc} */
	public TernaryBool isSubset(final SubtypeConstraint other) {
		return TernaryBool.TUNKNOWN;
	}

	@Override
	/** {@inheritDoc} */
	public StringSubtypeTreeElement evaluate() {
		// recursive evaluation
		a = a.evaluate();
		b = b.evaluate();

		// special simple cases when one side is ET_ALL or ET_NONE but
		// the other can be a tree
		if ((a instanceof EmptyStringSet) || (b instanceof EmptyStringSet)) {
			if ((operationType == OperationType.INHERIT) || (operationType == OperationType.INTERSECTION)) {
				return new EmptyStringSet(stringType);
			}
			if (operationType == OperationType.UNION) {
				return (a instanceof EmptyStringSet) ? a : b;
			}
		}
		if ((b instanceof EmptyStringSet) && (operationType == OperationType.EXCEPT)) {
			return a;
		}
		if ((a instanceof FullStringSet) || (b instanceof FullStringSet)) {
			if ((operationType == OperationType.INHERIT) || (operationType == OperationType.INTERSECTION)) {
				return (a instanceof FullStringSet) ? b : a;
			}
			if (operationType == OperationType.UNION) {
				return (a instanceof FullStringSet) ? a : b;
			}
		}
		if ((b instanceof FullStringSet) && (operationType == OperationType.EXCEPT)) {
			return new EmptyStringSet(stringType);
		}

		// both operands must be single constraints
		// (ALL,NONE,CONSTRAINT),
		// after this point trees will not be further simplified
		if ((a instanceof StringSetOperation) || (b instanceof StringSetOperation)) {
			return this;
		}

		// special case: ALL - some constraint type that can be
		// complemented
		if ((a instanceof FullStringSet) && (operationType == OperationType.EXCEPT) && (b instanceof StringSetConstraint)) {
			switch (((StringSetConstraint) b).getType()) {
			case SIZE_CONSTRAINT:
			case ALPHABET_CONSTRAINT:
				return ((StringSetConstraint) b.complement()).evaluate();
			}
		}

		// special case: when one operand is VALUE_CONSTRAINT then
		// isElement() can be called for the values
		// and drop values or drop the other operand set or both
		// depending on the operation
		switch (operationType) {
		case INHERIT:
		case INTERSECTION:
			if (a instanceof StringSetConstraint) {
				if (((StringSetConstraint) a).getType() == ConstraintType.VALUE_CONSTRAINT) {
					a = ((StringSetConstraint) a).remove(b, false);
					a.evaluate();
					return a;
				}
			}
			if (b instanceof StringSetConstraint) {
				if (((StringSetConstraint) b).getType() == ConstraintType.VALUE_CONSTRAINT) {
					b = ((StringSetConstraint) b).remove(a, false);
					b.evaluate();
					return b;
				}
			}
			break;
		case UNION:
			if (a instanceof StringSetConstraint) {
				if (((StringSetConstraint) a).getType() == ConstraintType.VALUE_CONSTRAINT) {
					a = ((StringSetConstraint) a).remove(b, true);
					a.evaluate();
					break;
				}
			}
			if (b instanceof StringSetConstraint) {
				if (((StringSetConstraint) b).getType() == ConstraintType.VALUE_CONSTRAINT) {
					b = ((StringSetConstraint) b).remove(a, true);
					b.evaluate();
					break;
				}
			}
			break;
		case EXCEPT:
			if (a instanceof StringSetConstraint) {
				if (((StringSetConstraint) a).getType() == ConstraintType.VALUE_CONSTRAINT) {
					a = ((StringSetConstraint) a).remove(b, true);
					a.evaluate();
					return a;
				}
			}
		}

		// operands of same types can be evaluated to one constraint
		// using their
		// set arithmetic member functions
		if (a.getElementType() == b.getElementType()) {
			switch (a.getElementType()) {
			case ALL:
				if (operationType == OperationType.EXCEPT) {
					return new EmptyStringSet(stringType);
				}

				return a;
			case NONE:
				return a;
			case CONSTRAINT:
				if (((StringSetConstraint) a).getType() == ((StringSetConstraint) b).getType()) {
					if (((StringSetConstraint) a).getType() == ConstraintType.PATTERN_CONSTRAINT) {
						break;
					}
					switch (operationType) {
					case INHERIT:
					case INTERSECTION:
						return (StringSubtypeTreeElement) a.intersection(b);
					case UNION:
						return (StringSubtypeTreeElement) a.union(b);
					case EXCEPT:
						return (StringSubtypeTreeElement) a.except(b);
					}
				};
				break;
			case OPERATION:
				//TODO;
				break;
			default:
				break;
			}
		}

		return this;
	}

}
