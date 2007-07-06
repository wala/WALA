package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.ipa.callgraph.CallGraph;

public class PointerFlowGraphFactory {

  public PointerFlowGraph make(PointerAnalysis pa, CallGraph cg) {
    return new PointerFlowGraph(pa, cg);
  }

}
