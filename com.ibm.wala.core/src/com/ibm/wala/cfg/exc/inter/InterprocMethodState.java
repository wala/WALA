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

import java.util.Map;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * A MethodState for the interprocedural analysis.
 * 
 * This class has been developed as part of a student project "Studienarbeit" by Markus Herhoffer.
 * It has been adapted and integrated into the WALA project by Juergen Graf.
 * 
 * @author Markus Herhoffer &lt;markus.herhoffer@student.kit.edu&gt;
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 * 
 */
class InterprocMethodState extends MethodState {

  private final Map<CGNode, IntraprocAnalysisState> map;
  private final CGNode method;
  private final CallGraph cg;

  InterprocMethodState(final CGNode method, final CallGraph cg, final Map<CGNode, IntraprocAnalysisState> map) {
    this.map = map;
    this.method = method;
    this.cg = cg;
  }

  /*
   * (non-Javadoc)
   * 
   * @see edu.kit.ipd.wala.intra.MethodState#throwsException(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public boolean throwsException(final SSAAbstractInvokeInstruction node) {
    for (final CGNode called : cg.getPossibleTargets(method, node.getCallSite())) {
      final IntraprocAnalysisState info = map.get(called);
      
      if (info == null || info.hasExceptions()) {
        return true;
      }
    }

    return false;
  }

}
