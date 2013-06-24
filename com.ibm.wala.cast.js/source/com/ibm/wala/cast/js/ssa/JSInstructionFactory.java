/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.ir.ssa.AstInstructionFactory;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;

public interface JSInstructionFactory extends AstInstructionFactory {

  JavaScriptCheckReference CheckReference(int ref);
  
  SSAGetInstruction GetInstruction(int result, int ref, String field);
  
  JavaScriptInstanceOf InstanceOf(int result, int objVal, int typeVal);
  
  JavaScriptInvoke Invoke(int function, int results[], int[] params, int exception, CallSiteReference site);
  
  JavaScriptInvoke Invoke(int function, int result, int[] params, int exception, CallSiteReference site);
  
  JavaScriptInvoke Invoke(int function, int[] params, int exception, CallSiteReference site);
 
  JavaScriptPropertyRead PropertyRead(int result, int objectRef, int memberRef);
  
  JavaScriptPropertyWrite PropertyWrite(int objectRef, int memberRef, int value);

  SSAPutInstruction PutInstruction(int ref, int value, String field);
  
  JavaScriptTypeOfInstruction TypeOfInstruction(int lval, int object);
  
  JavaScriptWithRegion WithRegion(int expr, boolean isEnter);
  
  PrototypeLookup PrototypeLookup(int lval, int object);

  SetPrototype SetPrototype(int object, int prototype);

} 
