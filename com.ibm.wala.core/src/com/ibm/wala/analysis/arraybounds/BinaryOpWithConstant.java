package com.ibm.wala.analysis.arraybounds;

import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.Operator;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSABinaryOpInstruction;

/**
 * Normalizes a binary operation with a constant by providing direct access to
 * assigned = other op constant.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class BinaryOpWithConstant {
	/**
	 *
	 * @param instruction
	 * @param ir
	 * @return normalized BinaryOpWithConstant or null, if normalization was not
	 *         successful.
	 */
	public static BinaryOpWithConstant create(
			SSABinaryOpInstruction instruction, IR ir) {
		BinaryOpWithConstant result = null;

		if (instruction.mayBeIntegerOp()) {
			assert instruction.getNumberOfUses() == 2;
			Integer other = null;
			Integer value = null;

			int constantPos = -1;
			for (int i = 0; i < instruction.getNumberOfUses(); i++) {
				final int constant = instruction.getUse(i);
				if (ir.getSymbolTable().isIntegerConstant(constant)) {
					other = instruction.getUse((i + 1) % 2);
					value = ir.getSymbolTable().getIntValue(constant);
					constantPos = i;
				}
			}

			final IOperator op = instruction.getOperator();

			if (constantPos != -1) {
				if (op == Operator.ADD || op == Operator.SUB
						&& constantPos == 1) {
					result = new BinaryOpWithConstant(op, other, value,
							instruction.getDef());
				}
			}
		}

		return result;
	}

	private final IOperator op;
	private final Integer other;

	private final Integer value;

	private final Integer assigned;

	private BinaryOpWithConstant(IOperator op, Integer other, Integer value,
			Integer assigned) {
		super();
		this.op = op;
		this.other = other;
		this.value = value;
		this.assigned = assigned;
	}

	public Integer getAssignedVar() {
		return this.assigned;
	}

	public Integer getConstantValue() {
		return this.value;
	}

	public IOperator getOp() {
		return this.op;
	}

	public Integer getOtherVar() {
		return this.other;
	}
}
