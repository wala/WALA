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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.cfg.CFGProvider;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.warnings.WarningSet;

/**
 *
 * An object that provides an interface to local method information needed
 * for CFA.
 * 
 * @author sfink
 */
public interface SSAContextInterpreter extends RTAContextInterpreter, CFGProvider {

  /**
   * @return the IR that models the method context, or null if it's an unmodelled native method
   */
  public IR getIR(CGNode node, WarningSet warnings);

  public DefUse getDU(CGNode node, WarningSet warnings);
  
  /**
   * @return the number of the statements in the IR, or -1 if it's an unmodelled native method.
   */
  public int getNumberOfStatements(CGNode node, WarningSet warnings);

}
