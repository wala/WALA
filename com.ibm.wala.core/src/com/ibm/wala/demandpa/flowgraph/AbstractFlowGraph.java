/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright ©2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient’s reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents’ employees.
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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.graph.labeled.SlowSparseNumberedLabeledGraph;

/**
 * A graph whose edges are labeled with {@link IFlowLabel}s.
 * @author Manu Sridharan
 * 
 */
public abstract class AbstractFlowGraph extends SlowSparseNumberedLabeledGraph<Object, IFlowLabel> implements IFlowGraph {

  private final static IFlowLabel defaultLabel = new IFlowLabel() {
    public IFlowLabel bar() {
      return defaultLabel;
    }

    public boolean isBarred() {
      return false;
    }

    public void visit(IFlowLabelVisitor v, Object dst) {
    }

  };
  /**
   * Map: LocalPointerKey -> SSAInvokeInstruction. If we have (x, foo()), that
   * means that x was def'fed by the return value from the call to foo()
   */
  protected final Map<PointerKey, SSAInvokeInstruction> callDefs = HashMapFactory.make();
  /**
   * Map: {@link LocalPointerKey} -> Set<{@link SSAInvokeInstruction}>. If we
   * have (x, foo()), that means x was passed as a parameter to the call to
   * foo(). The parameter position is not represented and must be recovered.
   */
  protected final Map<PointerKey, Set<SSAInvokeInstruction>> callParams = HashMapFactory.make();
  /**
   * Map: LocalPointerKey -> CGNode. If we have (x, foo), then x is a parameter
   * of method foo. For now, we have to re-discover the parameter position. TODO
   * this should just be a set; we can get the CGNode from the
   * {@link LocalPointerKey}
   */
  protected final Map<PointerKey, CGNode> params = HashMapFactory.make();
  /**
   * Map: {@link LocalPointerKey} -> {@link CGNode}. If we have (x, foo), then
   * x is a return value of method foo. Must re-discover if x is normal or
   * exceptional return value.
   */
  protected final Map<PointerKey, CGNode> returns = HashMapFactory.make();

  public AbstractFlowGraph() {
    super(defaultLabel);
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowLabelGraph#visitSuccs(java.lang.Object,
   *      com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor)
   */
  public void visitSuccs(Object node, IFlowLabelVisitor v) {
    for (Iterator<? extends IFlowLabel> succLabelIter = getSuccLabels(node); succLabelIter.hasNext();) {
      final IFlowLabel label = succLabelIter.next();
      for (Iterator<? extends Object> succNodeIter = getSuccNodes(node, label); succNodeIter.hasNext();) {
        label.visit(v, succNodeIter.next());
      }
    }
  }

  /*
   * @see com.ibm.wala.demandpa.flowgraph.IFlowLabelGraph#visitPreds(java.lang.Object,
   *      com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor)
   */
  public void visitPreds(Object node, IFlowLabelVisitor v) {
    for (Iterator<? extends IFlowLabel> predLabelIter = getPredLabels(node); predLabelIter.hasNext();) {
      final IFlowLabel label = predLabelIter.next();
      for (Iterator<? extends Object> predNodeIter = getPredNodes(node, label); predNodeIter.hasNext();) {
        label.visit(v, predNodeIter.next());
      }
    }
  }

  /**
   * For each invocation in the method, add nodes for actual parameters and return values
   * @param node
   */
  protected void addNodesForInvocations(CGNode node, HeapModel heapModel) {
    final IR ir = node.getIR();
    for (Iterator<CallSiteReference> iter = ir.iterateCallSites(); iter.hasNext(); ) { 
      CallSiteReference site = iter.next();
      SSAAbstractInvokeInstruction[] calls = ir.getCalls(site);
      for (SSAAbstractInvokeInstruction invokeInstr : calls) {
        for (int i = 0; i < invokeInstr.getNumberOfUses(); i++) {
          // just make nodes for parameters; we'll get to them when
          // traversing
          // from the callee
          PointerKey use = heapModel.getPointerKeyForLocal(node, invokeInstr.getUse(i));
          addNode(use);
          Set<SSAInvokeInstruction> s = MapUtil.findOrCreateSet(callParams, use);
          s.add((SSAInvokeInstruction) invokeInstr);
        }

        // for any def'd values, keep track of the fact that they are def'd
        // by a call
        if (invokeInstr.hasDef()) {
          PointerKey def = heapModel.getPointerKeyForLocal(node, invokeInstr.getDef());
          addNode(def);
          callDefs.put(def, (SSAInvokeInstruction) invokeInstr);
        }
        PointerKey exc = heapModel.getPointerKeyForLocal(node, invokeInstr.getException());
        addNode(exc);
        callDefs.put(exc, (SSAInvokeInstruction) invokeInstr);

      }
    }
  }

  public boolean isParam(LocalPointerKey pk) {
    return params.get(pk) != null;
  }

  public Iterator<SSAInvokeInstruction> getInstrsPassingParam(LocalPointerKey pk) {
    Set<SSAInvokeInstruction> instrs = callParams.get(pk);
    if (instrs == null) {
      return EmptyIterator.instance();
    } else {
      return instrs.iterator();
    }
  }

  public SSAInvokeInstruction getInstrReturningTo(LocalPointerKey pk) {
    return callDefs.get(pk);
  }

}
