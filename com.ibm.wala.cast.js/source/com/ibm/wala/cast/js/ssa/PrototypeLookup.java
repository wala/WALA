package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.ssa.SSAAbstractUnaryInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;

/**
 * Non-deterministically assigns some object in the prototype chain
 * of val (or val itself) to result.
 */
public class PrototypeLookup extends SSAAbstractUnaryInstruction {

  public PrototypeLookup(int result, int val) {
    super(result, val);
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((JSInstructionFactory)insts).PrototypeLookup((defs != null ? defs[0] : getDef(0)), (uses != null ? uses[0] : getUse(0)));
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, getDef(0)) + " = prototype_values(" + getValueString(symbolTable, getUse(0)) + ")";
  }

  @Override
  public void visit(IVisitor v) {
    ((JSInstructionVisitor)v).visitPrototypeLookup(this);
  }

}
