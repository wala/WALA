/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;

public interface IRView {

  SymbolTable getSymbolTable();

  ControlFlowGraph<SSAInstruction, ISSABasicBlock> getControlFlowGraph();

  ISSABasicBlock[] getBasicBlocksForCall(CallSiteReference callSite);

  SSAInstruction[] getInstructions();

  SSAInstruction getPEI(ProgramCounter peiLoc);

  IMethod getMethod();

  ISSABasicBlock getExitBlock();

  Iterator<NewSiteReference> iterateNewSites();

  Iterator<CallSiteReference> iterateCallSites();

  Iterator<ISSABasicBlock> getBlocks();

  String[] getLocalNames(int i, int v);
  
}
