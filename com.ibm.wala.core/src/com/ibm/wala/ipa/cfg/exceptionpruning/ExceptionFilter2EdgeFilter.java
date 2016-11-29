package com.ibm.wala.ipa.cfg.exceptionpruning;

import java.util.Collection;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.cfg.EdgeFilter;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;

/**
 * This class converts an exception filter to an edge filter.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 * @param <Block>
 */
public class ExceptionFilter2EdgeFilter<Block extends ISSABasicBlock>
implements EdgeFilter<Block> {

	private final ExceptionFilter<SSAInstruction> filter;
	private final ClassHierarchy cha;
	private final ControlFlowGraph<SSAInstruction, Block> cfg;

	public ExceptionFilter2EdgeFilter(ExceptionFilter<SSAInstruction> filter,
			ClassHierarchy cha, ControlFlowGraph<SSAInstruction, Block> cfg) {
		this.cfg = cfg;
		this.filter = filter;
		this.cha = cha;
	}

	@Override
	public boolean hasExceptionalEdge(Block src, Block dst) {
		boolean hasExceptionalEdge = this.cfg.getExceptionalSuccessors(src)
				.contains(dst);
		final SSAInstruction relevantInstruction = src.getLastInstruction();
		if (hasExceptionalEdge && relevantInstruction != null) {
			if (this.weKnowAllExceptions(relevantInstruction)) {

				final Collection<TypeReference> thrownExceptions = relevantInstruction
						.getExceptionTypes();
				final Collection<FilteredException> filteredExceptions = this.filter
						.filteredExceptions(relevantInstruction);

				final boolean isFiltered = ExceptionMatcher.isFiltered(
						thrownExceptions, filteredExceptions, this.cha);
				hasExceptionalEdge = !isFiltered;
			}
		}

		return hasExceptionalEdge;
	}

	@Override
	public boolean hasNormalEdge(Block src, Block dst) {
		boolean result = true;

		if (src.getLastInstructionIndex() >= 0) {
  		final SSAInstruction relevantInstruction = src.getLastInstruction();
  		if (relevantInstruction != null
  				&& this.filter.alwaysThrowsException(relevantInstruction)) {
  			result = false;
  		}
		} 

		return result && this.cfg.getNormalSuccessors(src).contains(dst);
	}

	/**
	 * SSAInstruction::getExceptionTypes() does not return exceptions thrown by
	 * throw or invoke instructions, so we may not remove edges from those
	 * instructions, even if all exceptions returned by
	 * instruction.getExceptionTypes() are to be filtered.
	 *
	 * @param instruction
	 * @return if we know all exceptions, that can occur at this address from
	 *         getExceptionTypes()
	 */
	private boolean weKnowAllExceptions(SSAInstruction instruction) {
		return !((instruction instanceof SSAAbstractInvokeInstruction) || (instruction instanceof SSAAbstractThrowInstruction));
	}
}
