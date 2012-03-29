package com.ibm.wala.cfg.exc;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * Interface to retrieve the result of the interprocedural analysis.
 * 
 * @author Juergen Graf <graf@kit.edu>
 */
public interface InterprocAnalysisResult<I, T extends IBasicBlock<I>> {

  /**
   * Returns the result of the interprocedural analysis for the given call graph node.
   */
  ExceptionPruningAnalysis<I, T> getResult(CGNode n);
  
  /**
   * Returns true iff an analysis result exists for the given call graph node.
   */
  boolean containsResult(CGNode n);
  
}
