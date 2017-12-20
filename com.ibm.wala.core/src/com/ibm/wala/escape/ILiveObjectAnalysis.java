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
package com.ibm.wala.escape;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.intset.IntSet;

/**
 * Basic interface for liveness analysis of heap-allocated objects.
 */
public interface ILiveObjectAnalysis {

  /**
   * @param allocMethod a method which holds an allocation site
   * @param allocPC bytecode index of allocation site
   * @param m method in question
   * @param instructionIndex index of an instruction in SSA IR. in m. if -1, it is interpreted as a wildcard meaning "any statement"
   * @throws WalaException
   * @return true if an object allocated at the allocation site &lt;allocMethod,allocPC&gt; may be live immediately after the
   *          statement &lt;m,instructionIndex&gt;
   */
  public boolean mayBeLive(CGNode allocMethod, int allocPC, CGNode m, int instructionIndex) throws WalaException;

  /**
   * @param ik an instance key
   * @param m method in question
   * @param instructionIndex index of an instruction in SSA IR. in m. if -1, it is interpreted as a wildcard meaning "any statement"
   * @throws WalaException
   * @return true if an object allocated at the allocation site &lt;allocMethod,allocPC&gt; may be live immediately after the
   *          statement &lt;m,instructionIndex&gt;
   */
  public boolean mayBeLive(InstanceKey ik, CGNode m, int instructionIndex) throws WalaException;

  /**
   * @param ik an instance key
   * @param m method in question
   * @param instructionIndices indices of instructions in SSA IR.
   * @return true if an object allocated at the allocation site &lt;allocMethod,allocPC&gt; may be live immediately after the
   *          statement &lt;m,instructionIndex&gt; for any instructionIndex in the set
   */
  public boolean mayBeLive(InstanceKey ik, CGNode m, IntSet instructionIndices);

}
