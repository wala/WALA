/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.ssa;

import java.util.Collection;

import com.ibm.wala.cast.ir.ssa.AbstractLexicalInvoke;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;

public class JavaScriptInvoke extends AbstractLexicalInvoke {
  /**
   * The value numbers of the arguments passed to the call.
   */
  private final int[] params;

  private int function;

  public JavaScriptInvoke(int function, int results[], int[] params, int exception, CallSiteReference site) {
    super(results, exception, site);
    this.function = function;
    this.params = params;
  }

  public JavaScriptInvoke(int function, int results[], int[] params, int exception, CallSiteReference site, Access[] lexicalReads,
      Access[] lexicalWrites) {
    super(results, exception, site, lexicalReads, lexicalWrites);
    this.function = function;
    this.params = params;
  }

  public JavaScriptInvoke(int function, int result, int[] params, int exception, CallSiteReference site) {
    this(function, new int[] { result }, params, exception, site);
  }

  public JavaScriptInvoke(int function, int[] params, int exception, CallSiteReference site) {
    this(function, null, params, exception, site);
  }

  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    int fn = function;
    int newParams[] = params;
    Access[] reads = lexicalReads;

    if (uses != null) {
      int i = 0;

      fn = uses[i++];

      newParams = new int[params.length];
      for (int j = 0; j < newParams.length; j++)
        newParams[j] = uses[i++];

      if (lexicalReads != null) {
        reads = new Access[lexicalReads.length];
        for (int j = 0; j < reads.length; j++)
          reads[j] = new Access(lexicalReads[j].variableName, lexicalReads[j].variableDefiner, uses[i++]);
      }
    }

    int newLvals[] = new int[results.length];
    System.arraycopy(results, 0, newLvals, 0, results.length);
    int newExp = exception;
    Access[] writes = lexicalWrites;

    if (defs != null) {
      int i = 0;
      if (getNumberOfReturnValues() > 0) {
        newLvals[0] = defs[i++];
      }
      newExp = defs[i++];
      for (int j = 1; j < getNumberOfReturnValues(); j++) {
        newLvals[j] = defs[i++];
      }

      if (lexicalWrites != null) {
        writes = new Access[lexicalWrites.length];
        for (int j = 0; j < writes.length; j++)
          writes[j] = new Access(lexicalWrites[j].variableName, lexicalWrites[j].variableDefiner, defs[i++]);
      }
    }

    return ((JSInstructionFactory)insts).Invoke(fn, newLvals, newParams, newExp, site, reads, writes);
  }

  public String toString(SymbolTable symbolTable) {
    StringBuffer s = new StringBuffer();
    if (getNumberOfReturnValues() > 0) {
      s.append(getValueString(symbolTable, getReturnValue(0)));
      s.append(" = ");
    }
    if (site.getDeclaredTarget().equals(JavaScriptMethods.ctorReference))
      s.append("construct ");
    else if (site.getDeclaredTarget().equals(JavaScriptMethods.dispatchReference))
      s.append("dispatch ");
    else
      s.append("invoke ");
    s.append(getValueString(symbolTable, function));

    if (site != null)
      s.append("@").append(site.getProgramCounter());

    if (params != null) {
      if (params.length > 0) {
        s.append(" ").append(getValueString(symbolTable, params[0]));
      }
      for (int i = 1; i < params.length; i++) {
        s.append(",").append(getValueString(symbolTable, params[i]));
      }
    }

    if (exception == -1) {
      s.append(" exception: NOT MODELED");
    } else {
      s.append(" exception:").append(getValueString(symbolTable, exception));
    }

    if (lexicalReads != null) {
      s.append(" (reads:");
      for (int i = 0; i < lexicalReads.length; i++) {
        s.append(" ").append(lexicalReads[i].variableName).append(":").append(
            getValueString(symbolTable, lexicalReads[i].valueNumber));
      }
      s.append(")");
    }

    if (lexicalWrites != null) {
      s.append(" (writes:");
      for (int i = 0; i < lexicalWrites.length; i++) {
        s.append(" ").append(lexicalWrites[i].variableName).append(":").append(
            getValueString(symbolTable, lexicalWrites[i].valueNumber));
      }
      s.append(")");
    }

    return s.toString();
  }

  /**
   * @see com.ibm.domo.ssa.Instruction#visit(Visitor)
   */
  public void visit(IVisitor v) {
    assert v instanceof JSInstructionVisitor;
    ((JSInstructionVisitor) v).visitJavaScriptInvoke(this);
  }

  public int getNumberOfParameters() {
    if (params == null) {
      return 1;
    } else {
      return params.length + 1;
    }
  }

  /**
   * @see com.ibm.domo.ssa.Instruction#getUse(int)
   */
  public int getUse(int j) {
    if (j == 0)
      return function;
    else if (j <= params.length)
      return params[j - 1];
    else {
      return super.getUse(j);
    }
  }

  public int getFunction() {
    return function;
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  public Collection<TypeReference> getExceptionTypes() {
    return Util.typeErrorExceptions();
  }

  public int hashCode() {
    return site.hashCode() * function * 7529;
  }

  // public boolean equals(Object obj) {
  // if (obj instanceof JavaScriptInvoke) {
  // JavaScriptInvoke other = (JavaScriptInvoke)obj;
  // if (site.equals(other.site)) {
  // if (getNumberOfUses() == other.getNumberOfUses()) {
  // for(int i = 0; i < getNumberOfUses(); i++) {
  // if (getUse(i) != other.getUse(i)) {
  // return false;
  // }
  // }
  //
  // if (getNumberOfDefs() == other.getNumberOfDefs()) {
  // for(int i = 0; i < getNumberOfDefs(); i++) {
  // if (getDef(i) != other.getDef(i)) {
  // return false;
  // }
  // }
  //
  // return true;
  // }
  // }
  // }
  // }
  //
  // return false;
  // }
}
