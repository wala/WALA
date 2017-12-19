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
package com.ibm.wala.cfg.exc.inter;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * A delegating MethodState for the interprocedural analysis.
 * 
 * This class combines two MethodState objects. A MethodState decides if a given method call may throw an exception.
 * If the primary MethodState thinks that the call may throw an exception, the fallback MethodState is asked.
 * 
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 * 
 */
class DelegatingMethodState extends MethodState {

  private final MethodState primary; 
  private final MethodState fallback; 
  
  DelegatingMethodState(final MethodState primary, final MethodState fallback) {
    if (primary == null) {
      throw new IllegalArgumentException("primary method state is null.");
    } else if (fallback == null) {
      throw new IllegalArgumentException("fallback method state is null.");
    }
    
    this.primary = primary;
    this.fallback = fallback;
  }

  @Override
  public boolean throwsException(final SSAAbstractInvokeInstruction node) {
    if (primary.throwsException(node)) {
      return fallback.throwsException(node);
    }

    return false;
  }

}
