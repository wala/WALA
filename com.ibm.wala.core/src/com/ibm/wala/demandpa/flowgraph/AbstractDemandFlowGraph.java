/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.demandpa.flowgraph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallerSiteContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.ref.ReferenceCleanser;

/**
 * A graph representing program flow, constructed method-by-method on demand
 */
public abstract class AbstractDemandFlowGraph extends AbstractFlowGraph {
  private final static boolean DEBUG = false;

  /**
   * Counter for wiping soft caches
   */
  private static int wipeCount = 0;

  /**
   * node numbers of CGNodes we have already visited
   */
  final BitVectorIntSet cgNodesVisited = new BitVectorIntSet();

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowGraph#addSubgraphForNode(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public void addSubgraphForNode(CGNode node) throws IllegalArgumentException {
    if (node == null) {
      throw new IllegalArgumentException("node == null");
    }
    IR ir = node.getIR();
    if (ir == null) {
      throw new IllegalArgumentException("no ir for node " + node);
    }
    int n = cg.getNumber(node);
    if (!cgNodesVisited.contains(n)) {
      cgNodesVisited.add(n);
      unconditionallyAddConstraintsFromNode(node, ir);
      addNodesForInvocations(node, ir);
      addNodesForParameters(node, ir);
    }
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowGraph#hasSubgraphForNode(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public boolean hasSubgraphForNode(CGNode node) {
    return cgNodesVisited.contains(cg.getNumber(node));
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowGraph#getParamSuccs(com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)
   */
  public Iterator<PointerKeyAndCallSite> getParamSuccs(LocalPointerKey pk) {
    // TODO cache this result
    // TODO take some cgnode as parameter if we have calling context?
    CGNode cgNode = params.get(pk);
    if (cgNode == null) {
      return EmptyIterator.instance();
    }
    int paramPos = pk.getValueNumber() - 1;
    ArrayList<PointerKeyAndCallSite> paramSuccs = new ArrayList<>();
    // iterate over callers
    for (CGNode caller : cg) {
      // TODO optimization: we don't need to add the graph if null is passed
      // as the argument
      addSubgraphForNode(caller);
      IR ir = caller.getIR();
      for (CallSiteReference call : Iterator2Iterable.make(ir.iterateCallSites())) {
        if (cg.getPossibleTargets(caller, call).contains(cgNode)) {
          SSAAbstractInvokeInstruction[] callInstrs = ir.getCalls(call);
          for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
            PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
            assert containsNode(actualPk);
            assert containsNode(pk);
            paramSuccs.add(new PointerKeyAndCallSite(actualPk, call));
          }
        }
      }
    }
    return paramSuccs.iterator();
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowGraph#getParamPreds(com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)
   */
  public Iterator<PointerKeyAndCallSite> getParamPreds(LocalPointerKey pk) {
    // TODO
    Set<SSAAbstractInvokeInstruction> instrs = callParams.get(pk);
    if (instrs == null) {
      return EmptyIterator.instance();
    }
    ArrayList<PointerKeyAndCallSite> paramPreds = new ArrayList<>();
    for (SSAAbstractInvokeInstruction callInstr : instrs) {
      for (int i = 0; i < callInstr.getNumberOfUses(); i++) {
        if (pk.getValueNumber() != callInstr.getUse(i))
          continue;
        CallSiteReference callSiteRef = callInstr.getCallSite();
        // get call targets
        Collection<CGNode> possibleCallees = cg.getPossibleTargets(pk.getNode(), callSiteRef);
        // construct graph for each target
        for (CGNode callee : possibleCallees) {
          addSubgraphForNode(callee);
          // TODO test this!!!
          // TODO test passing null as an argument
          PointerKey paramVal = heapModel.getPointerKeyForLocal(callee, i + 1);
          assert containsNode(paramVal);
          paramPreds.add(new PointerKeyAndCallSite(paramVal, callSiteRef));
        }
      }
    }
    return paramPreds.iterator();

  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowGraph#getReturnSuccs(com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)
   */
  public Iterator<PointerKeyAndCallSite> getReturnSuccs(LocalPointerKey pk) {
    SSAAbstractInvokeInstruction callInstr = callDefs.get(pk);
    if (callInstr == null)
      return EmptyIterator.instance();
    ArrayList<PointerKeyAndCallSite> returnSuccs = new ArrayList<>();
    boolean isExceptional = pk.getValueNumber() == callInstr.getException();

    CallSiteReference callSiteRef = callInstr.getCallSite();
    // get call targets
    Collection<CGNode> possibleCallees = cg.getPossibleTargets(pk.getNode(), callSiteRef);
    // construct graph for each target
    for (CGNode callee : possibleCallees) {
      addSubgraphForNode(callee);
      PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
          .getPointerKeyForReturnValue(callee);
      assert containsNode(retVal);
      returnSuccs.add(new PointerKeyAndCallSite(retVal, callSiteRef));
    }

    return returnSuccs.iterator();
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowGraph#getReturnPreds(com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey)
   */
  public Iterator<PointerKeyAndCallSite> getReturnPreds(LocalPointerKey pk) {
    CGNode cgNode = returns.get(pk);
    if (cgNode == null) {
      return EmptyIterator.instance();
    }
    boolean isExceptional = pk == heapModel.getPointerKeyForExceptionalReturnValue(cgNode);
    ArrayList<PointerKeyAndCallSite> returnPreds = new ArrayList<>();
    // iterate over callers
    for (CGNode caller : cg) {
      // TODO we don't need to add the graph if null is passed
      // as the argument
      addSubgraphForNode(caller);
      IR ir = caller.getIR();
      for (CallSiteReference call : Iterator2Iterable.make(ir.iterateCallSites())) {
        if (cg.getPossibleTargets(caller, call).contains(cgNode)) {
          SSAAbstractInvokeInstruction[] callInstrs = ir.getCalls(call);
          for (SSAAbstractInvokeInstruction callInstr : callInstrs) {
            PointerKey returnPk = heapModel.getPointerKeyForLocal(caller, isExceptional ? callInstr.getException() : callInstr
                .getDef());
            assert containsNode(returnPk);
            assert containsNode(pk);
            returnPreds.add(new PointerKeyAndCallSite(returnPk, call));
          }
        }
      }
    }
    return returnPreds.iterator();
  }

  protected abstract void addNodesForParameters(CGNode node, IR ir);

  protected void unconditionallyAddConstraintsFromNode(CGNode node, IR ir) {

    if (DEBUG) {
      System.err.println(("Adding constraints for CGNode " + node));
    }

    if (SSAPropagationCallGraphBuilder.PERIODIC_WIPE_SOFT_CACHES) {
      wipeCount++;
      if (wipeCount >= SSAPropagationCallGraphBuilder.WIPE_SOFT_CACHE_INTERVAL) {
        wipeCount = 0;
        ReferenceCleanser.clearSoftCaches();
      }
    }

    debugPrintIR(ir);

    if (ir == null) {
      return;
    }

    addNodeInstructionConstraints(node, ir);
    addNodePassthruExceptionConstraints(node, ir);
    addNodeConstantConstraints(node, ir);
  }

  /**
   * Add pointer flow constraints based on instructions in a given node
   */
  protected void addNodeInstructionConstraints(CGNode node, IR ir) {
    FlowStatementVisitor v = makeVisitor(node);
    ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg = ir.getControlFlowGraph();
    for (ISSABasicBlock b : cfg) {
      addBlockInstructionConstraints(node, cfg, b, v);
    }
  }

  /**
   * Add constraints for a particular basic block.
   */
  protected void addBlockInstructionConstraints(CGNode node, ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg,
      ISSABasicBlock b, FlowStatementVisitor v) {
    v.setBasicBlock(b);

    // visit each instruction in the basic block.
    for (SSAInstruction s : b) {
      if (s != null) {
        s.visit(v);
      }
    }

    addPhiConstraints(node, cfg, b);
  }

  private void addPhiConstraints(CGNode node, ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg, ISSABasicBlock b) {

    // visit each phi instruction in each successor block
    for (IBasicBlock ibb : Iterator2Iterable.make(cfg.getSuccNodes(b))) {
      ISSABasicBlock sb = (ISSABasicBlock) ibb;
      if (sb.isExitBlock()) {
        // an optimization based on invariant that exit blocks should
        // have no
        // phis.
        continue;
      }
      int n = 0;
      // set n to be whichPred(this, sb);
      for (IBasicBlock back : Iterator2Iterable.make(cfg.getPredNodes(sb))) {
        if (back == b) {
          break;
        }
        ++n;
      }
      assert n < cfg.getPredNodeCount(sb);
      for (SSAPhiInstruction phi : Iterator2Iterable.make(sb.iteratePhis())) {
        // Assertions.UNREACHABLE();
        if (phi == null) {
          continue;
        }
        PointerKey def = heapModel.getPointerKeyForLocal(node, phi.getDef());
        if (phi.getUse(n) > 0) {
          PointerKey use = heapModel.getPointerKeyForLocal(node, phi.getUse(n));
          addNode(def);
          addNode(use);
          addEdge(def, use, AssignLabel.noFilter());
        }
        // }
        // }
      }
    }
  }

  protected abstract FlowStatementVisitor makeVisitor(CGNode node);

  private static void debugPrintIR(IR ir) {
    if (DEBUG) {
      if (ir == null) {
        System.err.println("\n   No statements\n");
      } else {
        try {
          System.err.println(ir.toString());
        } catch (Error e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }
  }

  final Map<CGNode, Set<CallerSiteContext>> callerCache = HashMapFactory.make();

  @Override
  public Set<CallerSiteContext> getPotentialCallers(PointerKey formalPk) {
    CGNode callee = null;
    if (formalPk instanceof LocalPointerKey) {
      callee = ((LocalPointerKey) formalPk).getNode();
    } else if (formalPk instanceof ReturnValueKey) {
      callee = ((ReturnValueKey) formalPk).getNode();
    } else {
      throw new IllegalArgumentException("formalPk must represent a local");
    }
    Set<CallerSiteContext> ret = callerCache.get(callee);
    if (ret == null) {
      ret = HashSetFactory.make();
      for (CGNode caller : Iterator2Iterable.make(cg.getPredNodes(callee))) {
        for (CallSiteReference call : Iterator2Iterable.make(cg.getPossibleSites(caller, callee))) {
          ret.add(new CallerSiteContext(caller, call));
        }
      }
      callerCache.put(callee, ret);
    }
    return ret;
  }

  @Override
  public Set<CGNode> getPossibleTargets(CGNode node, CallSiteReference site, LocalPointerKey actualPk) {
    return cg.getPossibleTargets(node, site);
  }

  protected interface FlowStatementVisitor extends SSAInstruction.IVisitor {
    void setBasicBlock(ISSABasicBlock b);
  }

  public AbstractDemandFlowGraph(final CallGraph cg, final HeapModel heapModel, final MemoryAccessMap mam, final IClassHierarchy cha) {
    super(mam, heapModel, cha, cg);
  }

}
