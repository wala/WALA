package com.ibm.wala.cast.js.ssa;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class JavaScriptInstanceOf extends SSAInstruction {
  private final int objVal;
  private final int typeVal;
  private final int result;
  
  public JavaScriptInstanceOf(int result, int objVal, int typeVal) {
    this.objVal = objVal;
    this.typeVal = typeVal;
    this.result = result;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return 
      ((JSInstructionFactory)insts).InstanceOf(
          defs==null? result: defs[0],
          uses==null? objVal: uses[0],
          uses==null? typeVal: uses[1]);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Collections.singleton(JavaScriptTypes.TypeError);
  }

  public boolean isPEI() {
    return true;
  }
  
  @Override
  public int hashCode() {
     return objVal*31771 + typeVal*23 + result;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = " + getValueString(symbolTable, objVal) + " is instance of " + getValueString(symbolTable, typeVal); 
  }

  @Override
  public void visit(IVisitor v) {
     ((JSInstructionVisitor)v).visitJavaScriptInstanceOf(this);
  }
  
  public int getNumberOfDefs() {
    return 1;
  }

  public int getDef(int i) {
    assert i == 0;
    return result;
  }
  
  public int getNumberOfUses() {
    return 2;
  }
  
  public int getUse(int i) {
    switch (i) {
    case 0: return objVal;
    case 1: return typeVal;
    default: Assertions.UNREACHABLE(); return -1;
    }
  }
}
