package com.ibm.wala.cfg.exc.inter;

import java.util.Map;

import com.ibm.wala.cfg.exc.intra.MethodState;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;

/**
 * @author Markus Herhoffer <markus.herhoffer@student.kit.edu>
 * @author Juergen Graf <graf@kit.edu>
 * 
 */
public class InterprocMethodState extends MethodState {

  private final Map<CGNode, SingleMethodState> map;
  private final CGNode method;
  private final CallGraph cg;

  public InterprocMethodState(final CGNode method, final CallGraph cg, final Map<CGNode, SingleMethodState> map) {
    this.map = map;
    this.method = method;
    this.cg = cg;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * edu.kit.ipd.wala.intra.MethodState#throwsException(com.ibm.wala.ipa.callgraph
   * .CGNode)
   */
  @Override
  public boolean throwsException(final SSAAbstractInvokeInstruction node) {
    for (final CGNode called : cg.getPossibleTargets(method, node.getCallSite())) {
      final SingleMethodState info = map.get(called);
      
      if (info == null || info.throwsException()) {
        return true;
      }
    }

    return false;
  }

}
