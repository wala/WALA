package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;

public abstract class AstPropertyWrite extends AbstractReflectivePut {

  public AstPropertyWrite(int iindex, int objectRef, int memberRef, int value) {
    super(iindex, objectRef, memberRef, value);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts)
        .PropertyWrite(
            iIndex(),
            uses == null ? getObjectRef() : uses[0],
            uses == null ? getMemberRef() : uses[1],
            uses == null ? getValue() : uses[2]);
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return super.toString(symbolTable) + " = " + getValueString(symbolTable, getValue());
  }

  /** @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor) */
  @Override
  public void visit(IVisitor v) {
    assert v instanceof AstInstructionVisitor;
    ((AstInstructionVisitor) v).visitPropertyWrite(this);
  }

  @Override
  public boolean isPEI() {
    return true;
  }
}
