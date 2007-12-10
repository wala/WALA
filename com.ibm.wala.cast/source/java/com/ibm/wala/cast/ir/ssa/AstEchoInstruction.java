package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.ssa.*;

import java.util.*;

public class AstEchoInstruction extends SSAInstruction {
  private final int[] rvals;
  
  public AstEchoInstruction(int[] rvals) {
    this.rvals = rvals;
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return new AstEchoInstruction(uses==null? rvals: uses);
  }

  public int getNumberOfDefs() {
    return 0;
  }

  public int getDef(int i) {
    Assertions.UNREACHABLE();
    return -1;
  }

  public int getNumberOfUses() {
    return rvals.length;
  }

  public int getUse(int i) {
    return rvals[i];
  }

  public int hashCode() {
    int v = 1;
    for(int i = 0;i < rvals.length; i++) {
      v *= rvals[i];
    }

    return v;
  }

  public String toString(SymbolTable symbolTable, ValueDecorator d) {
    StringBuffer result = new StringBuffer("echo/print ");
    for(int i = 0; i < rvals.length; i++) {
      result.append(getValueString(symbolTable, d, rvals[i])).append(" ");
    }

    return result.toString();
  }

  public void visit(IVisitor v) {
    ((AstInstructionVisitor)v).visitEcho(this);
  }

  public boolean isFallThrough() {
    return true;
  }

  public Collection getExceptionTypes() {
    return Collections.EMPTY_SET;
  }

}
