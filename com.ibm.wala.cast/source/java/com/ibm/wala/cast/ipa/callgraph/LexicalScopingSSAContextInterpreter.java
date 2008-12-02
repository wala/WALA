package com.ibm.wala.cast.ipa.callgraph;

import java.util.Iterator;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingKeysContextSelector.ScopeMappingContext;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.types.FieldReference;

public class LexicalScopingSSAContextInterpreter extends AstContextInsensitiveSSAContextInterpreter implements SSAContextInterpreter {

  public LexicalScopingSSAContextInterpreter(AnalysisOptions options, AnalysisCache cache) {
    super(options, cache);
  }

  public boolean understands(IMethod method, Context context) {
    assert !(context instanceof ScopeMappingContext) || method instanceof AstMethod;
    return method instanceof AstMethod 
        && ( (context instanceof ScopeMappingContext)
                           ||
             !((AstMethod)method).lexicalInfo().getAllExposedUses().isEmpty() );
  }

  public IR getIR(CGNode node) {
    return getAnalysisCache().getSSACache().findOrCreateIR(node.getMethod(), node.getContext(), options.getSSAOptions());
  }
  
  public DefUse getDU(CGNode node) {
    return getAnalysisCache().getSSACache().findOrCreateDU(node.getMethod(), node.getContext(), options.getSSAOptions());
  }
  
}
