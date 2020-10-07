/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;

public interface JSInstructionFactory extends AstInstructionFactory {

  JavaScriptCheckReference CheckReference(int iindex, int ref);

  SSAGetInstruction GetInstruction(int iindex, int result, int ref, String field);

  JavaScriptInstanceOf InstanceOf(int iindex, int result, int objVal, int typeVal);

  JavaScriptInvoke Invoke(
      int iindex, int function, int results[], int[] params, int exception, CallSiteReference site);

  JavaScriptInvoke Invoke(
      int iindex, int function, int result, int[] params, int exception, CallSiteReference site);

  JavaScriptInvoke Invoke(
      int iindex, int function, int[] params, int exception, CallSiteReference site);

  SSAPutInstruction PutInstruction(int iindex, int ref, int value, String field);

  JavaScriptTypeOfInstruction TypeOfInstruction(int iindex, int lval, int object);

  JavaScriptWithRegion WithRegion(int iindex, int expr, boolean isEnter);

  PrototypeLookup PrototypeLookup(int iindex, int lval, int object);

  SetPrototype SetPrototype(int iindex, int object, int prototype);
}
