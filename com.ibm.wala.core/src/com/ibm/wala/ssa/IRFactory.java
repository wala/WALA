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
package com.ibm.wala.ssa;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * This is intended as an internal interface; clients probably shouldn't be using
 * this directly.
 * 
 * If you have a call graph in hand, to get the IR for a CGNode, use
 * callGraph.getInterpreter().getIR();
 * 
 * Otherwise, look at SSACache.
 * 
 * @author Julian Dolby
 *
 */
public interface IRFactory {

  IR makeIR(IMethod method, Context C, ClassHierarchy cha, SSAOptions options, WarningSet warnings);

  ControlFlowGraph makeCFG(IMethod method, Context C, ClassHierarchy cha, WarningSet warnings);

}
