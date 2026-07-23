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

  @Override
  public void visitArrayLength(SSAArrayLengthInstruction instruction) {
    this.varNumNew = instruction.getArrayRef();
  }

  @Override
  public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
    this.varNumNew = instruction.getArrayRef();
  }

  @Override
  public void visitArrayStore(SSAArrayStoreInstruction instruction) {
    this.varNumNew = instruction.getArrayRef();
  }

  @Override
  public void visitBinaryOp(SSABinaryOpInstruction instruction) {}

  @Override
  public void visitCheckCast(SSACheckCastInstruction instruction) {}

  @Override
  public void visitComparison(SSAComparisonInstruction instruction) {}

  @Override
  public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {}

  @Override
  public void visitConversion(SSAConversionInstruction instruction) {}

  @Override
  public void visitGet(SSAGetInstruction instruction) {
    if (!instruction.isStatic()) {
      this.varNumNew = instruction.getRef();
    }
  }

  @Override
  public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {}

  @Override
  public void visitGoto(SSAGotoInstruction instruction) {}

  @Override
  public void visitInstanceof(SSAInstanceofInstruction instruction) {}

  @Override
  public void visitInvoke(SSAInvokeInstruction instruction) {
    if (!instruction.isStatic()) {
      this.varNumNew = instruction.getReceiver();
    }
  }

  @Override
  public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {}

  @Override
  public void visitMonitor(SSAMonitorInstruction instruction) {
    this.varNumNew = instruction.getRef();
  }

  @Override
  public void visitNew(SSANewInstruction instruction) {}

  @Override
  public void visitPhi(SSAPhiInstruction instruction) {}

  @Override
  public void visitPi(SSAPiInstruction instruction) {}

  @Override
  public void visitPut(SSAPutInstruction instruction) {
    if (!instruction.isStatic()) {
      this.varNumNew = instruction.getRef();
    }
  }

  @Override
  public void visitReturn(SSAReturnInstruction instruction) {}

  @Override
  public void visitSwitch(SSASwitchInstruction instruction) {}

  @Override
  public void visitThrow(SSAThrowInstruction instruction) {
    this.varNumNew = instruction.getException();
  }

  @Override
  public void visitUnaryOp(SSAUnaryOpInstruction instruction) {}
}
