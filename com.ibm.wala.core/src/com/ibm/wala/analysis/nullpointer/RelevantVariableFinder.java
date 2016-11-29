package com.ibm.wala.analysis.nullpointer;

import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstruction.IVisitor;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;

/**
 * Helper class to find the variable that may be null.
 * 
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 *
 */
public class RelevantVariableFinder implements IVisitor {
	private int varNumNew;
	private final int varNum;

	public RelevantVariableFinder(SSAInstruction instrcution) {
		this.varNumNew = -1;
		instrcution.visit(this);
		this.varNum = this.varNumNew;
	}

	public int getVarNum() {
		return this.varNum;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitArrayLength(com.ibm.wala
	 * .ssa.SSAArrayLengthInstruction)
	 */
	@Override
	public void visitArrayLength(SSAArrayLengthInstruction instruction) {
		this.varNumNew = instruction.getArrayRef();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitArrayLoad(com.ibm.wala.
	 * ssa.SSAArrayLoadInstruction)
	 */
	@Override
	public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
		this.varNumNew = instruction.getArrayRef();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitArrayStore(com.ibm.wala
	 * .ssa.SSAArrayStoreInstruction)
	 */
	@Override
	public void visitArrayStore(SSAArrayStoreInstruction instruction) {
		this.varNumNew = instruction.getArrayRef();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitBinaryOp(com.ibm.wala.ssa
	 * .SSABinaryOpInstruction)
	 */
	@Override
	public void visitBinaryOp(SSABinaryOpInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitCheckCast(com.ibm.wala.
	 * ssa.SSACheckCastInstruction)
	 */
	@Override
	public void visitCheckCast(SSACheckCastInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitComparison(com.ibm.wala
	 * .ssa.SSAComparisonInstruction)
	 */
	@Override
	public void visitComparison(SSAComparisonInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitConditionalBranch(com.ibm
	 * .wala.ssa.SSAConditionalBranchInstruction)
	 */
	@Override
	public void visitConditionalBranch(
			SSAConditionalBranchInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitConversion(com.ibm.wala
	 * .ssa.SSAConversionInstruction)
	 */
	@Override
	public void visitConversion(SSAConversionInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitGet(com.ibm.wala.ssa.
	 * SSAGetInstruction)
	 */
	@Override
	public void visitGet(SSAGetInstruction instruction) {
		if (!instruction.isStatic()) {
			this.varNumNew = instruction.getRef();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitGetCaughtException(com.
	 * ibm.wala.ssa.SSAGetCaughtExceptionInstruction)
	 */
	@Override
	public void visitGetCaughtException(
			SSAGetCaughtExceptionInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitGoto(com.ibm.wala.ssa.
	 * SSAGotoInstruction)
	 */
	@Override
	public void visitGoto(SSAGotoInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitInstanceof(com.ibm.wala
	 * .ssa.SSAInstanceofInstruction)
	 */
	@Override
	public void visitInstanceof(SSAInstanceofInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitInvoke(com.ibm.wala.ssa
	 * .SSAInvokeInstruction)
	 */
	@Override
	public void visitInvoke(SSAInvokeInstruction instruction) {
		if (!instruction.isStatic()) {
			this.varNumNew = instruction.getReceiver();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitLoadMetadata(com.ibm.wala
	 * .ssa.SSALoadMetadataInstruction)
	 */
	@Override
	public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitMonitor(com.ibm.wala.ssa
	 * .SSAMonitorInstruction)
	 */
	@Override
	public void visitMonitor(SSAMonitorInstruction instruction) {
		this.varNumNew = instruction.getRef();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitNew(com.ibm.wala.ssa.
	 * SSANewInstruction)
	 */
	@Override
	public void visitNew(SSANewInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitPhi(com.ibm.wala.ssa.
	 * SSAPhiInstruction)
	 */
	@Override
	public void visitPhi(SSAPhiInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitPi(com.ibm.wala.ssa.
	 * SSAPiInstruction)
	 */
	@Override
	public void visitPi(SSAPiInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.ibm.wala.ssa.SSAInstruction.IVisitor#visitPut(com.ibm.wala.ssa.
	 * SSAPutInstruction)
	 */
	@Override
	public void visitPut(SSAPutInstruction instruction) {
		if (!instruction.isStatic()) {
			this.varNumNew = instruction.getRef();
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitReturn(com.ibm.wala.ssa
	 * .SSAReturnInstruction)
	 */
	@Override
	public void visitReturn(SSAReturnInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitSwitch(com.ibm.wala.ssa
	 * .SSASwitchInstruction)
	 */
	@Override
	public void visitSwitch(SSASwitchInstruction instruction) {
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitThrow(com.ibm.wala.ssa.
	 * SSAThrowInstruction)
	 */
	@Override
	public void visitThrow(SSAThrowInstruction instruction) {
		this.varNumNew = instruction.getException();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * com.ibm.wala.ssa.SSAInstruction.IVisitor#visitUnaryOp(com.ibm.wala.ssa
	 * .SSAUnaryOpInstruction)
	 */
	@Override
	public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
	}

}
