package com.ibm.wala.dalvik.analysis.typeInference;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.analysis.typeInference.TypeVariable;
import com.ibm.wala.fixpoint.AbstractOperator;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SymbolTable;

public class DalvikTypeInference extends TypeInference {
	@Override
	protected TypeVariable[] makeStmtRHS(int size) {
		return new DalvikTypeVariable[size];
	}

	private static AbstractOperator<TypeVariable> dalvikPhiOp = new DalvikPhiOperator();

	protected DalvikTypeInference(IR ir, boolean doPrimitives) {
		super(ir, doPrimitives);
	}

	@Override
	protected void initialize() {
		init(ir, this.new DalvikTypeVarFactory(), this.new TypeOperatorFactory());
	}

	public static DalvikTypeInference make(IR ir, boolean doPrimitives) {
		return new DalvikTypeInference(ir, doPrimitives);
	}

	public class DalvikTypeVarFactory extends TypeInference.TypeVarFactory {

		@Override
		public IVariable makeVariable(int valueNumber) {
			SymbolTable st = ir.getSymbolTable();
			if (st.isIntegerConstant(valueNumber) && st.isZero(valueNumber)) {
				return new DalvikTypeVariable(language.getPrimitive(language.getConstantType(Integer.valueOf(0))), true);
			} else {
				if (doPrimitives) {
					if (st.isConstant(valueNumber)) {
						if (st.isBooleanConstant(valueNumber)) {
							return new DalvikTypeVariable(language.getPrimitive(language.getConstantType(Boolean.TRUE)));
						}
					}
				}
				return new DalvikTypeVariable(TypeAbstraction.TOP);
			}
		}
	}

	protected class TypeOperatorFactory extends TypeInference.TypeOperatorFactory {

		@Override
		public void visitPhi(SSAPhiInstruction instruction) {
			assert dalvikPhiOp != null;
			this.result = dalvikPhiOp;
		}

	}

	private static final class DalvikPhiOperator extends AbstractOperator<TypeVariable> {
		private DalvikPhiOperator() {
		}

		@Override
		public byte evaluate(TypeVariable _lhs, TypeVariable[] _rhs) {
			/**
			 * TODO: Find a better solution than downcasting. Downcasting is
			 * really ugly, although I can be sure here that it succeeds because
			 * I control what type the parameters have. There must be a cleaner
			 * solution which does not cause tons of changes in WALA's code, but
			 * I don't see it yet...
			 */
			assert _lhs instanceof DalvikTypeVariable;
			assert _rhs instanceof DalvikTypeVariable[];
			DalvikTypeVariable lhs = (DalvikTypeVariable) _lhs;
			DalvikTypeVariable[] rhs = (DalvikTypeVariable[]) _rhs;
			TypeAbstraction lhsType = lhs.getType();
			TypeAbstraction meet = TypeAbstraction.TOP;
			boolean ignoreZero = containsNonPrimitiveAndZero(rhs);
			for (int i = 0; i < rhs.length; i++) {
				if (rhs[i] != null && rhs[i].getType() != null && !(ignoreZero && rhs[i].isIntZeroConstant())) {
					TypeVariable r = rhs[i];
					meet = meet.meet(r.getType());
				}
			}
			if (lhsType.equals(meet)) {
				return NOT_CHANGED;
			} else {
				lhs.setType(meet);
				return CHANGED;
			}
		}

		private static boolean containsNonPrimitiveAndZero(DalvikTypeVariable[] types) {
			boolean containsNonPrimitive = false;
			boolean containsZero = false;
			for (int i = 0; i < types.length; i++) {
				if (types[i] != null) {
					if (types[i].getType() != null && types[i].getType().getTypeReference() != null
							&& !types[i].getType().getTypeReference().isPrimitiveType()) {
						containsNonPrimitive = true;
					}
					if (types[i].isIntZeroConstant()) {
						containsZero = true;
					}
				}
			}
			return containsNonPrimitive && containsZero;
		}

		@Override
		public String toString() {
			return "phi meet (dalvik)";
		}

		@Override
		public int hashCode() {
			return 2297;
		}

		@Override
		public boolean equals(Object o) {
			return (o instanceof DalvikPhiOperator);
		}
	}
}

