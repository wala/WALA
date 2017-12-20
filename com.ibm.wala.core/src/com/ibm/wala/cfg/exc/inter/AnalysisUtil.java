/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cfg.exc.inter;

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;

/**
 * Utility class for the exception pruning analysis.
 * 
 * This class has been developed as part of a student project "Studienarbeit" by Markus Herhoffer.
 * It has been adapted and integrated into the WALA project by Juergen Graf.
 *
 * @author Markus Herhoffer &lt;markus.herhoffer@student.kit.edu&gt;
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 */
public final class AnalysisUtil {

  private AnalysisUtil() {
    throw new IllegalStateException("No instances of this class allowed.");
  }
  
  /**
   * Checks if a node is FakeRoot
   * 
   * @param node
   *          the node to check
   * @return true if node is FakeRoot
   */
  public static boolean isFakeRoot(CGNode node) {
    return (node.getMethod().getName().equals(FakeRootMethod.rootMethod.getName()));
  }
  
  /**
   * Returns an array of <code>int</code> with the parameter's var nums of the
   * invoked method in <code>invokeInstruction</code>.
   * 
   * @param invokeInstruction
   *          The instruction that invokes the method.
   * @return an array of <code>int</code> with all parameter's var nums
   *         including the this pointer.
   */
  public static int[] getParameterNumbers(SSAAbstractInvokeInstruction invokeInstruction) {
    final int number = invokeInstruction.getNumberOfParameters();
    final int[] parameterNumbers = new int[number];
    assert (parameterNumbers.length == invokeInstruction.getNumberOfUses());

    for (int i = 0; i < parameterNumbers.length; i++) {
      parameterNumbers[i] = invokeInstruction.getUse(i);
    }

    return parameterNumbers;
  }

  /**
   * Returns a Set of all blocks that invoke another method.
   * 
   * @param cfg
   *          The Control Flow Graph to analyze
   * @return a Set of all blocks that contain an invoke
   */
  public static Set<IExplodedBasicBlock> extractInvokeBlocks(final ControlFlowGraph<SSAInstruction, IExplodedBasicBlock> cfg) {
    final HashSet<IExplodedBasicBlock> invokeBlocks = new HashSet<>();

    for (final IExplodedBasicBlock block : cfg) {
      if (block.getInstruction() instanceof SSAAbstractInvokeInstruction) {
        invokeBlocks.add(block);
      }
    }

    return invokeBlocks;
  }

}
