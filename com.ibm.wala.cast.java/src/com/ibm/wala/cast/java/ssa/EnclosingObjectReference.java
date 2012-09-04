package com.ibm.wala.cast.java.ssa;

import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

/**
 * The CAst source language front end for Java has explicit support for lexically-enclosing objects, rather than compiling them
 * away into extra fields and access-control thwarting accessor methods as is done in bytecode. This instruction represents a read
 * of the object of the given type that lexically encloses its use value.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class EnclosingObjectReference extends SSAInstruction {
  private final TypeReference type;

  private final int lval;

  public EnclosingObjectReference(int lval, TypeReference type) {
    this.lval = lval;
    this.type = type;
  }

  public boolean hasDef() {
    return true;
  }

  public int getDef() {
    return lval;
  }

  public int getDef(int i) {
    assert i == 0;

    return lval;
  }

  public int getNumberOfDefs() {
    return 1;
  }

  public TypeReference getEnclosingType() {
    return type;
  }

  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstJavaInstructionFactory) insts).EnclosingObjectReference(defs == null ? lval : defs[0], type);
  }

  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, lval) + " = enclosing " + type.getName();
  }

  public void visit(IVisitor v) {
    ((AstJavaInstructionVisitor) v).visitEnclosingObjectReference(this);
  }

  public int hashCode() {
    return lval * type.hashCode();
  }

  public Collection<TypeReference> getExceptionTypes() {
    return Collections.emptySet();
  }

  public boolean isFallThrough() {
    return true;
  }

}
