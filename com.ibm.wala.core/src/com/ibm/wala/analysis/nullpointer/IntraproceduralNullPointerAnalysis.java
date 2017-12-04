package com.ibm.wala.analysis.nullpointer;

import com.ibm.wala.cfg.exc.intra.IntraprocNullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.NullPointerFrameWork;
import com.ibm.wala.cfg.exc.intra.NullPointerSolver;
import com.ibm.wala.cfg.exc.intra.NullPointerState;
import com.ibm.wala.cfg.exc.intra.NullPointerState.State;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;

/**
 * Intraprocedural dataflow analysis to detect impossible NullPointerExceptions.
 *
 * This class is basically a copy of {@link IntraprocNullPointerAnalysis}, but
 * does only provide the result of the analysis instead of a pruned CFG.
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class IntraproceduralNullPointerAnalysis {
	static private final IProgressMonitor NO_PROGRESS_MONITOR = null;
	private final NullPointerSolver<ISSABasicBlock> solver;
	private final IR ir;

	public IntraproceduralNullPointerAnalysis(IR ir) {
		if (ir == null || ir.isEmptyIR()) {
			throw new IllegalArgumentException("IR may not be null or empty.");
		}

		this.ir = ir;

		final int maxVarNum = ir.getSymbolTable().getMaxValueNumber();
		SSACFG cfg = ir.getControlFlowGraph();
		final NullPointerFrameWork<ISSABasicBlock> problem = new NullPointerFrameWork<>(
				cfg, ir);
		this.solver = new NullPointerSolver<>(problem, maxVarNum,
				ir, cfg.entry());
		try {
			this.solver.solve(NO_PROGRESS_MONITOR);
		} catch (final CancelException e) {
			// can't happen as we have no monitor
		}
	}

	public State nullPointerExceptionThrowState(SSAInstruction instruction) {
		assert instruction != null;
		if (instruction.isPEI()
				&& instruction.getExceptionTypes().contains(
						TypeReference.JavaLangNullPointerException)) {
			final NullPointerState blockState = this.solver.getIn(this.ir
					.getBasicBlockForInstruction(instruction));
			final RelevantVariableFinder finder = new RelevantVariableFinder(
					instruction);
			assert finder.getVarNum() >= 0;
			return blockState.getState(finder.getVarNum());
		}
		return State.NOT_NULL;
	}
}
