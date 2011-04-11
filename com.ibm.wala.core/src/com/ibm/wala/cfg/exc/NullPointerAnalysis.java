package com.ibm.wala.cfg.exc;

import com.ibm.wala.cfg.exc.intra.ExplodedCFGNullPointerAnalysis;
import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.cfg.exc.intra.ParameterState;
import com.ibm.wala.cfg.exc.intra.SSACFGNullPointerAnalysis;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.types.TypeReference;

/**
 * Tries to detect impossible (or always appearing) NullPointerExceptions and removes impossible
 * control flow from the CFG.
 * 
 * @author Juergen Graf <graf@kit.edu>
 *
 */
public final class NullPointerAnalysis { 

  public static final TypeReference[] DEFAULT_IGNORE_EXCEPTIONS = {
    TypeReference.JavaLangOutOfMemoryError, 
    TypeReference.JavaLangExceptionInInitializerError, 
    TypeReference.JavaLangNegativeArraySizeException
  };

  private NullPointerAnalysis() {
    throw new IllegalStateException("No instances of this class allowed.");
  }
  
	public static ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> 
	createIntraproceduralExplodedCFGAnalysis(IR ir) {
		return createIntraproceduralExplodedCFGAnalysis(DEFAULT_IGNORE_EXCEPTIONS, ir, null, null);
	}

	public static ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> 
	createIntraproceduralExplodedCFGAnalysis(TypeReference[] ignoredExceptions, IR ir) {
	  return createIntraproceduralExplodedCFGAnalysis(ignoredExceptions, ir, null, null);
	}
	 
	public static ExceptionPruningAnalysis<SSAInstruction, IExplodedBasicBlock> 
	createIntraproceduralExplodedCFGAnalysis(TypeReference[] ignoredExceptions, IR ir, ParameterState paramState, MethodState mState) {
		return new ExplodedCFGNullPointerAnalysis(ignoredExceptions, ir, paramState, mState);
	}

	public static ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> 
	createIntraproceduralSSACFGAnalyis(IR ir) {
		return createIntraproceduralSSACFGAnalyis(DEFAULT_IGNORE_EXCEPTIONS, ir, null, null);
	}

	public static ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> 
	createIntraproceduralSSACFGAnalyis(TypeReference[] ignoredExceptions, IR ir) {
		return createIntraproceduralSSACFGAnalyis(ignoredExceptions, ir, null, null);
	}
	
  public static ExceptionPruningAnalysis<SSAInstruction, ISSABasicBlock> 
  createIntraproceduralSSACFGAnalyis(TypeReference[] ignoredExceptions, IR ir, ParameterState paramState, MethodState mState) {
    return new SSACFGNullPointerAnalysis(ignoredExceptions, ir, paramState, mState);
  }
  
}
