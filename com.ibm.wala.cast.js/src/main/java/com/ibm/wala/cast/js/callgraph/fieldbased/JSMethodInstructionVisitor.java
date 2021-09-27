/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.callgraph.fieldbased;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.js.ssa.JSAbstractInstructionVisitor;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;

/**
 * A {@link JSAbstractInstructionVisitor} that is used to only visit instructions of a single
 * method.
 *
 * @author mschaefer
 */
public class JSMethodInstructionVisitor extends JSAbstractInstructionVisitor {

  protected final IMethod method;
  protected final SymbolTable symtab;
  protected final DefUse du;

  public JSMethodInstructionVisitor(IMethod method, SymbolTable symtab, DefUse du) {
    this.method = method;
    this.symtab = symtab;
    this.du = du;
  }

  /**
   * Determine whether {@code invk} corresponds to a function declaration or function expression.
   *
   * <p>TODO: A bit hackish. Is there a more principled way to do this?
   */
  protected boolean isFunctionConstructorInvoke(JavaScriptInvoke invk) {
    /*
     * Function objects are allocated by explicit constructor invocations like this:
     *
     *   v8 = global:global Function
     *   v4 = construct v8@2 v6:#L<fullFunctionName> exception:<nd>
     */
    if (invk.getDeclaredTarget().equals(JavaScriptMethods.ctorReference)) {
      int fn = invk.getFunction();
      SSAInstruction fndef = du.getDef(fn);
      if (fndef instanceof AstGlobalRead) {
        AstGlobalRead agr = (AstGlobalRead) fndef;
        if (agr.getGlobalName().equals("global Function")) {
          if (invk.getNumberOfPositionalParameters() != 2) {
            return false;
          }
          // this may be a genuine use of "new Function()", not a declaration/expression
          if (!symtab.isStringConstant(invk.getUse(1))
              || symtab.getStringValue(invk.getUse(1)).isEmpty()) {
            return false;
          }
          return true;
        }
      }
    }
    return false;
  }
}
