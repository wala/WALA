package com.ibm.wala.cast.js.ssa;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

public class JavaScriptCheckReference extends SSAInstruction {
  private final int ref;
  
  public JavaScriptCheckReference(int ref) {
    this.ref = ref;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
     return ((JSInstructionFactory)insts).CheckReference(uses==null? ref: uses[0]);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.singleton(JavaScriptTypes.ReferenceError);
  }

  @Override
  public int hashCode() {
    return 87621 * ref;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return "check " + getValueString(symbolTable, ref);
  }

  @Override
  public void visit(IVisitor v) {
    ((JSInstructionVisitor)v).visitCheckRef(this);
  }

  public boolean isPEI() {
    return true;
  }
  
  public int getNumberOfUses() {
    return 1;
  }
  
  public int getUse(int i) {
    assert i == 0;
    return ref;
  }
  
}
