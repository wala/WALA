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

package com.ibm.wala.cfg.exc.intra;

import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * Provides a way for the nullpointer analysis to decide whether or not a called method
 * may throw an exception.
 * 
 * @author Juergen Graf &lt;graf@kit.edu&gt;
 *
 */
public abstract class MethodState {

  public abstract boolean throwsException(SSAAbstractInvokeInstruction node);
  
  public static final MethodState DEFAULT = new MethodState() {
    
    @Override
    public boolean throwsException(SSAAbstractInvokeInstruction node) {
      // per default assume that every call may throw an exception
      return true;
    }
  };
}
