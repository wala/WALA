package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;

public abstract class AstPropertyRead extends AbstractReflectiveGet {

  public AstPropertyRead(int iindex, int result, int objectRef, int memberRef) {
    super(iindex, result, objectRef, memberRef);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstInstructionFactory) insts)
        .PropertyRead(
            iIndex(),
            defs == null ? getDef() : defs[0],
            uses == null ? getObjectRef() : uses[0],
            uses == null ? getMemberRef() : uses[1]);
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  /**
   * /* (non-Javadoc)
   *
   * @see com.ibm.wala.ssa.SSAInstruction#visit(com.ibm.wala.ssa.SSAInstruction.IVisitor)
   */
  @Override
  public void visit(IVisitor v) {
    assert v instanceof AstInstructionVisitor;
    ((AstInstructionVisitor) v).visitPropertyRead(this);
  }
}
