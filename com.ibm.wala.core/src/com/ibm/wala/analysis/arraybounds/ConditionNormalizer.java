package com.ibm.wala.analysis.arraybounds;

import com.ibm.wala.shrikeBT.IConditionalBranchInstruction.Operator;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;

/**
 * ConditionNormalizer normalizes a branch condition. See Constructor for more
 * information.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class ConditionNormalizer {
	private final int lhs;
	private int rhs;
	private Operator op;

	/**
	 * Creates a normalization of cnd such that lhs op rhs is true.
	 *
	 * Normalization means, that the given variable lhs, will be on the left
	 * hand side of the comparison, also if the branch is not taken, the
	 * operation needs to be negated.
	 *
	 * p.a. the condition is !(rhs &gt;= lhs), it will be normalized to lhs &gt; rhs
	 *
	 * @param cnd
	 *            condition to normalize
	 * @param lhs
	 *            variable, that should be on the left hand side
	 * @param branchIsTaken
	 *            if the condition is for the branching case or not
	 */
	public ConditionNormalizer(SSAConditionalBranchInstruction cnd, int lhs,
			boolean branchIsTaken) {
		this.lhs = lhs;

		if (cnd.getNumberOfUses() != 2) {
			throw new IllegalArgumentException(
					"Condition uses not exactly two variables.");
		}

		this.op = (Operator) cnd.getOperator();
		for (int i = 0; i < cnd.getNumberOfUses(); i++) {
			final int var = cnd.getUse(i);
			if ((var != lhs)) {
				if (cnd.getUse((i + 1) % 2) != lhs) {
					throw new IllegalArgumentException(
							"Lhs not contained in condition.");
				}

				if (i == 0) {
					// make sure the other is lhs
					this.op = swapOperator(this.op);
				}
				if (!branchIsTaken) {
					this.op = negateOperator(this.op);
				}
				this.rhs = var;
			}
		}
	}

	public int getLhs() {
		return this.lhs;
	}

	public Operator getOp() {
		return this.op;
	}

	public int getRhs() {
		return this.rhs;
	}

	private static Operator negateOperator(Operator op) {
		switch (op) {
		case EQ:
			return Operator.NE;
		case NE:
			return Operator.EQ;
		case LT:
			return Operator.GE;
		case GE:
			return Operator.LT;
		case GT:
			return Operator.LE;
		case LE:
			return Operator.GT;
		default:
			throw new RuntimeException(
					"Programming Error: Got unknown operator.");
		}
	}

	private static Operator swapOperator(Operator op) {
		switch (op) {
		case EQ:
			return op;
		case NE:
			return op;
		case LT:
			return Operator.GT;
		case GE:
			return Operator.LE;
		case GT:
			return Operator.LT;
		case LE:
			return Operator.GE;
		default:
			throw new RuntimeException(
					"Programming Error: Got unknown operator.");
		}
	}
}
