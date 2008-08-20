/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.j2ee.util;

import java.util.HashMap;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * Holds results of type inference for call site receivers.
 * 
 * @author sfink
 */
public class ReceiverTypeInference {

  final private TypeInference ti;

  /**
   * Mapping from call site reference to InvokeInstruction. TODO: this kind of
   * sucks. Redesign?
   */
  private HashMap<CallSiteReference, SSAInvokeInstruction> invokeMap;


  public ReceiverTypeInference(TypeInference ti) {
    this.ti = ti;

    try {
      setupInvokeMap();
    } catch (InvalidClassFileException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  /**
   * Method setupInvokeMap. TODO: redesign stuff so that all this is not
   * necessary. TODO: has that time come .. is this unnecesary yet?
   * 
   * @throws InvalidClassFileException
   */
  private void setupInvokeMap() throws InvalidClassFileException {

    invokeMap = HashMapFactory.make(5);
    IR ir = ti.getIR();
    IMethod method = ir.getMethod();
    // set up mapping from Integer (program counter) -> CallSiteReference
    HashMap<Integer, CallSiteReference> intMap = HashMapFactory.make(5);
    for (CallSiteReference site :  CodeScanner.getCallSites(method)) {
      int pc = site.getProgramCounter();
      intMap.put(Integer.valueOf(pc), site);
    }
    // now set up mapping from CallSiteReference -> InvokeInstruction
    SSAInstruction[] instructions = ir.getInstructions();
    for (int i = 0; i < instructions.length; i++) {
      SSAInstruction s = instructions[i];
      if (s instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction call = (SSAInvokeInstruction) s;
        int pc = call.getProgramCounter();
        CallSiteReference site = intMap.get(new Integer(pc));
        invokeMap.put(site, call);
      }
    }
  }

  public TypeAbstraction getReceiverType(CallSiteReference site) {
    SSAInvokeInstruction instruction = getInvokeInstruction(site);
    if (instruction == null) {
      return null;
    }
    int def = instruction.getReceiver();
    if (def == -1) {
      return null;
    } else {
      return ti.getType(def);
    }
  }

  /**
   * Method getInvokeInstruction.
   * 
   * @param site
   * @return InvokeInstruction
   */
  private SSAInvokeInstruction getInvokeInstruction(CallSiteReference site) {
    return invokeMap.get(site);
  }
}
