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
package com.ibm.wala.demandpa.alg;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.demandpa.flowgraph.AbstractFlowGraph;
import com.ibm.wala.demandpa.flowgraph.AbstractFlowLabelVisitor;
import com.ibm.wala.demandpa.flowgraph.AssignGlobalLabel;
import com.ibm.wala.demandpa.flowgraph.AssignLabel;
import com.ibm.wala.demandpa.flowgraph.DemandPointerFlowGraph;
import com.ibm.wala.demandpa.flowgraph.GetFieldLabel;
import com.ibm.wala.demandpa.flowgraph.NewLabel;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel.IFlowLabelVisitor;
import com.ibm.wala.demandpa.util.MemoryAccessMap;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * Simple field-based points-to analysis using {@link DemandPointerFlowGraph}.
 * Primarily for testing purposes.
 * 
 * @author Manu Sridharan
 * 
 */
public class TestNewGraphPointsTo extends AbstractDemandPointsTo {

  public TestNewGraphPointsTo(CallGraph cg, HeapModel model, MemoryAccessMap fam, ClassHierarchy cha, AnalysisOptions options) {
    super(cg, model, fam, cha, options);
  }

  public Collection<InstanceKey> getPointsTo(PointerKey pk) throws UnimplementedError {

    Assertions._assert(pk instanceof LocalPointerKey, "we only handle locals");
    LocalPointerKey queriedPk = (LocalPointerKey) pk;
    // Create an (initially empty) dependence graph
    final AbstractFlowGraph g = new DemandPointerFlowGraph(cg, heapModel, fam, cha);

    // initialize the graph with the subgraph of x's method
    g.addSubgraphForNode(queriedPk.getNode());

    // do a DFS traversal of the assign and match edges
    // in the graph, adding instance keys to the points-to set
    final HashSet<InstanceKey> p2set = new HashSet<InstanceKey>();
    final HashSet<PointerKey> marked = new HashSet<PointerKey>();
    final Stack<PointerKey> worklist = new Stack<PointerKey>();
    class Helper {
      void prop(PointerKey thePk) {
        if (!marked.contains(thePk)) {
          marked.add(thePk);
          worklist.push(thePk);
        }
      }

      void propAll(Iterator<? extends Object> keys) {
        while (keys.hasNext()) {
          prop((PointerKey) keys.next());
        }
      }
    }
    final Helper h = new Helper();
    // final Mapper pkExtractor = new Mapper() {
    //
    // public Object map(Object obj_) {
    // return ((PointerKeyAndCallSite) obj_).getKey();
    // }
    //
    // };
    h.prop(queriedPk);
    final IFlowLabelVisitor v = new AbstractFlowLabelVisitor() {

      @Override
      public void visitNew(NewLabel label, Object dst) {
        p2set.add((InstanceKey) dst);
      }

      @Override
      public void visitGetField(GetFieldLabel label, Object dst) {
        h.propAll(g.getWritesToInstanceField(null, label.getField()));
      }

      @Override
      public void visitAssignGlobal(AssignGlobalLabel label, Object dst) {
        h.propAll(g.getWritesToStaticField((StaticFieldKey) dst));
      }

      @Override
      public void visitAssign(AssignLabel label, Object dst) {
        h.prop((PointerKey) dst);
      }

    };
    while (!worklist.isEmpty()) {
      PointerKey curPk = worklist.pop();
      g.visitSuccs(curPk, v);
      // interprocedural edges
      if (curPk instanceof LocalPointerKey) {
        LocalPointerKey localPk = (LocalPointerKey) curPk;
        if (g.isParam(localPk)) {
          CGNode cgNode = localPk.getNode();
          int paramPos = localPk.getValueNumber() - 1;
          for (Iterator<? extends CGNode> iter = cg.getPredNodes(cgNode); iter.hasNext();) {
            CGNode caller = iter.next();
            IR ir = caller.getIR();
            for (Iterator<CallSiteReference> iterator = ir.iterateCallSites(); iterator.hasNext();) {
              CallSiteReference call = iterator.next();
              // TODO on-the-fly call graph
              if (cg.getPossibleTargets(caller, call).contains(cgNode)) {
                g.addSubgraphForNode(caller);
                SSAAbstractInvokeInstruction[] callInstrs = ir.getCalls(call);
                for (int i = 0; i < callInstrs.length; i++) {
                  SSAAbstractInvokeInstruction callInstr = callInstrs[i];
                  PointerKey actualPk = heapModel.getPointerKeyForLocal(caller, callInstr.getUse(paramPos));
                  if (Assertions.verifyAssertions) {
                    Assertions._assert(g.containsNode(actualPk));
                    Assertions._assert(g.containsNode(localPk));
                  }
                  h.prop(actualPk);
                }
              }
            }
          }
        }
        SSAInvokeInstruction callInstr = g.getInstrReturningTo(localPk);
        if (callInstr != null) {
          boolean isExceptional = localPk.getValueNumber() == callInstr.getException();

          CallSiteReference callSiteRef = callInstr.getCallSite();
          // get call targets
          Set<CGNode> possibleCallees = cg.getPossibleTargets(localPk.getNode(), callSiteRef);
          // construct graph for each target
          for (CGNode callee : possibleCallees) {
            // TODO on-the-fly call graph stuff
            g.addSubgraphForNode(callee);
            PointerKey retVal = isExceptional ? heapModel.getPointerKeyForExceptionalReturnValue(callee) : heapModel
                .getPointerKeyForReturnValue(callee);
            if (Assertions.verifyAssertions) {
              Assertions._assert(g.containsNode(retVal));
            }
            h.prop(retVal);
          }
        }
        // Assertions._assert(sameContents(orig, newRets), "orig " + orig +
        // "\nnew " + newRets);
        // h.propAll(new IteratorMapper(pkExtractor,
        // g.getReturnSuccs((LocalPointerKey) curPk)));
        // h.propAll(new IteratorMapper(pkExtractor,
        // g.getParamSuccs((LocalPointerKey) curPk)));
      }
    }

    return p2set;
  }

}
