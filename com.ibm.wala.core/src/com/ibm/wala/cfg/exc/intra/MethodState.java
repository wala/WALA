package com.ibm.wala.cfg.exc.intra;

import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * Provides a way for the nullpointer analysis to decide wether or not a called method
 * may throw an exception.
 * 
 * @author Juergen Graf <graf@kit.edu>
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
