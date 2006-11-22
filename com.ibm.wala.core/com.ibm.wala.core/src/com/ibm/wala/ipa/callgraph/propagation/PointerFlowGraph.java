/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.ReferenceCleanser;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * A graphical view that represents the flow of pointers between abstract heap
 * locations
 * 
 * Each node in this graph is a PointerKey, representing an abstraction of a
 * pointer. There is an edge p -> q iff pointer values flow from p to q via an
 * assignment of some kind in the program.
 * 
 * This uses some lazy logic to avoid walking IRs until necessary.
 * 
 * @author Julian Dolby
 * @author sfink
 */
public class PointerFlowGraph extends AbstractGraph<PointerKey> {

  private final PointerAnalysis pa;

  private final CallGraph cg;

  private SlowSparseNumberedGraph<PointerKey> delegate = new SlowSparseNumberedGraph<PointerKey>();

  /**
   * nodes for which we have processed the statements
   */
  private final Collection<CGNode> processedNodes = HashSetFactory.make();

  private final WarningSet warnings = new WarningSet();

  private final EdgeManager<PointerKey> edgeManager = new LazyEdgeManager();

  private static int wipeCount = 0;

  private final static int WIPE_THRESHOLD = 1000;

  protected PointerFlowGraph(PointerAnalysis pa, CallGraph cg) {
    this.pa = pa;
    this.cg = cg;
  }

  private void processAllNodes() {
    for (Iterator it = cg.iterateNodes(); it.hasNext();) {
      CGNode node = (CGNode) it.next();
      if (!processedNodes.contains(node)) {
        processNode(node);
      }
    }
  }

  private void processCallers(CGNode node) {
    for (Iterator it = cg.getPredNodes(node); it.hasNext();) {
      CGNode p = (CGNode) it.next();
      if (!processedNodes.contains(p)) {
        processNode(p);
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   */
  public NodeManager<PointerKey> getNodeManager() {
    return delegate.getNodeManager();
  }

  /*
   * (non-Javadoc)
   * 
   */
  protected EdgeManager<PointerKey> getEdgeManager() {
    return edgeManager;
  }

  private class LazyEdgeManager implements EdgeManager<PointerKey> {

    private void lazySucc(Object N) {
      if (N instanceof AbstractLocalPointerKey) {
        AbstractLocalPointerKey lpk = (AbstractLocalPointerKey) N;
        CGNode node = lpk.getNode();
        if (!processedNodes.contains(node)) {
          processNode(node);
        }
        if (lpk instanceof ReturnValueKey) {
          processCallers(node);
        }
      } else {
        // flow to the heap. give up.
        // todo: use some smarter logic
        processAllNodes();
      }
    }

    private void lazyPred(Object N) {
      if (N instanceof AbstractLocalPointerKey) {
        AbstractLocalPointerKey lpk = (AbstractLocalPointerKey) N;
        CGNode node = lpk.getNode();
        if (!processedNodes.contains(node)) {
          processNode(node);
        }
        if (lpk instanceof LocalPointerKey) {
          LocalPointerKey p = (LocalPointerKey) lpk;
          if (p.isParameter()) {
            processCallers(node);
          }
        }
      } else {
        // flow to the heap. give up.
        // todo: use some smarter logic
        processAllNodes();
      }
    }

    public Iterator<? extends PointerKey> getPredNodes(PointerKey N) {
      lazyPred(N);
      return delegate.getPredNodes(N);
    }

    public int getPredNodeCount(PointerKey N) {
      lazyPred(N);
      return delegate.getPredNodeCount(N);
    }

    public Iterator<? extends PointerKey> getSuccNodes(PointerKey N) {
      lazySucc(N);
      return delegate.getSuccNodes(N);
    }

    public int getSuccNodeCount(PointerKey N) {
      lazySucc(N);
      return delegate.getSuccNodeCount(N);
    }

    public void addEdge(PointerKey src, PointerKey dst) {
      delegate.addEdge(src, dst);
    }

    public void removeEdge(PointerKey src, PointerKey dst) {
      delegate.removeEdge(src, dst);
    }

    public void removeAllIncidentEdges(PointerKey node) {
      Assertions.UNREACHABLE();
    }

    public void removeIncomingEdges(PointerKey node) {
      Assertions.UNREACHABLE();
    }

    public void removeOutgoingEdges(PointerKey node) {
      Assertions.UNREACHABLE();
    }

    public boolean hasEdge(PointerKey src, PointerKey dst) {
      lazySucc(src);
      lazyPred(dst);
      return delegate.hasEdge(src, dst);
    }
  };

  /**
   * Walk the statements in a node and add edges to the graph. Side effect: add
   * node to the set of processed nodes.
   */
  private void processNode(CGNode node) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(!processedNodes.contains(node));
    }
    processedNodes.add(node);
    IR ir = getIR(node);
    if (ir != null) {
      visit(node, ir);
    } else {
      Trace.println("PointerFlowGraph.build got null ir for " + node);
    }
  }

  private IR getIR(CGNode node) {
    wipeCount++;
    if (wipeCount > WIPE_THRESHOLD) {
      wipeCount = 0;
      ReferenceCleanser.clearSoftCaches();
    }
    SSAContextInterpreter interp = cg.getInterpreter(node);
    if (Assertions.verifyAssertions) {
      if (interp == null) {
        cg.getInterpreter(node);
        Assertions._assert(interp != null, "null interp for " + node);
      }
    }
    return interp.getIR(node, warnings);
  }

  private void visit(CGNode node, IR ir) {
    // add edges induced by individual instructions
    for (Iterator it = ir.getControlFlowGraph().iterateNodes(); it.hasNext();) {
      SSACFG.BasicBlock bb = (SSACFG.BasicBlock) it.next();
      InstructionVisitor v = makeInstructionVisitor(node, ir, bb);
      for (Iterator it2 = bb.iterateAllInstructions(); it2.hasNext();) {
        SSAInstruction i = (SSAInstruction) it2.next();
        if (i != null) {
          i.visit(v);
        }
      }
    }
    // add edges relating to thrown exceptions that reach the exit block.
    List<ProgramCounter> peis = SSAPropagationCallGraphBuilder.getIncomingPEIs(ir, ir.getExitBlock());
    PointerKey exception = pa.getHeapModel().getPointerKeyForExceptionalReturnValue(node);
    addExceptionEdges(node, pa, ir, peis, exception);
  }

  protected InstructionVisitor makeInstructionVisitor(CGNode node, IR ir, BasicBlock bb) {
    return new InstructionVisitor(node,ir, bb);
  }

  protected class InstructionVisitor extends SSAInstruction.Visitor {

    private final CGNode node;

    private final IR ir;

    private final IBasicBlock bb;

    public InstructionVisitor(CGNode node, IR ir, BasicBlock bb) {
      this.node = node;
      this.ir = ir;
      this.bb = bb;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.SSAInstruction.Visitor#visitArrayLoad(com.ibm.wala.ssa.SSAArrayLoadInstruction)
     */
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      // skip arrays of primitive type
      if (instruction.typeIsPrimitive()) {
        return;
      }
      PointerKey result = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getDef());
      PointerKey arrayRef = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getArrayRef());
      if (result == null) {
        // not sure why this happens. give up.
        return;
      }
      delegate.addNode(result);
      for (Iterator it = pa.getPointsToSet(arrayRef).iterator(); it.hasNext();) {
        InstanceKey ik = (InstanceKey) it.next();
        TypeReference C = ik.getConcreteType().getReference().getArrayElementType();
        if (C.isPrimitiveType()) {
          return;
        }
        PointerKey p = pa.getHeapModel().getPointerKeyForArrayContents(ik);
        if (p == null) {
          return;
        }
        delegate.addNode(p);
        delegate.addEdge(p, result);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.SSAInstruction.Visitor#visitArrayStore(com.ibm.wala.ssa.SSAArrayStoreInstruction)
     */
    public void visitArrayStore(SSAArrayStoreInstruction instruction) {
      // skip arrays of primitive type
      if (instruction.typeIsPrimitive()) {
        return;
      }
      PointerKey value = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getValue());
      PointerKey arrayRef = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getArrayRef());
      if (value == null || arrayRef == null) {
        // skip operations on null constants
        return;
      }
      delegate.addNode(value);
      for (Iterator it = pa.getPointsToSet(arrayRef).iterator(); it.hasNext();) {
        InstanceKey ik = (InstanceKey) it.next();
        TypeReference C = ik.getConcreteType().getReference().getArrayElementType();
        if (C.isPrimitiveType()) {
          return;
        }
        PointerKey p = pa.getHeapModel().getPointerKeyForArrayContents(ik);
        if (p == null) {
          return;
        }
        delegate.addNode(p);
        delegate.addEdge(value, p);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.SSAInstruction.Visitor#visitCheckCast(com.ibm.wala.ssa.SSACheckCastInstruction)
     */
    public void visitCheckCast(SSACheckCastInstruction instruction) {
      PointerKey result = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getResult());
      PointerKey value = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getVal());
      if (value != null) {
        delegate.addNode(value);
        delegate.addNode(result);
        delegate.addEdge(value, result);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.SSAInstruction.Visitor#visitCheckCast(com.ibm.wala.ssa.SSACheckCastInstruction)
     */
    public void visitPi(SSAPiInstruction instruction) {
      PointerKey result = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getDef());
      PointerKey value = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getVal());
      delegate.addNode(value);
      delegate.addNode(result);
      delegate.addEdge(value, result);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.SSAInstruction.Visitor#visitReturn(com.ibm.wala.ssa.SSAReturnInstruction)
     */
    public void visitReturn(SSAReturnInstruction instruction) {
      // skip returns of primitive type
      if (instruction.returnsPrimitiveType() || instruction.returnsVoid()) {
        return;
      }
      PointerKey returnValue = pa.getHeapModel().getPointerKeyForReturnValue(node);

      PointerKey result = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getResult());
      if (result == null) {
        // this can happen for return of null constant
        return;
      }

      delegate.addNode(returnValue);
      delegate.addNode(result);
      delegate.addEdge(result, returnValue);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.SSAInstruction.Visitor#visitGet(com.ibm.wala.ssa.SSAGetInstruction)
     */
    public void visitGet(SSAGetInstruction instruction) {
      FieldReference field = instruction.getDeclaredField();

      // skip getfields of primitive type (optimisation)
      if (field.getFieldType().isPrimitiveType()) {
        return;
      }
      PointerKey def = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getDef());
      delegate.addNode(def);

      IField f = cg.getClassHierarchy().resolveField(field);
      if (f == null) {
        return;
      }
      if (instruction.isStatic()) {
        PointerKey fKey = pa.getHeapModel().getPointerKeyForStaticField(f);
        delegate.addNode(fKey);
        delegate.addEdge(fKey, def);

      } else {
        PointerKey ref = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getRef());
        for (Iterator it = pa.getPointsToSet(ref).iterator(); it.hasNext();) {
          InstanceKey ik = (InstanceKey) it.next();
          PointerKey p = pa.getHeapModel().getPointerKeyForInstanceField(ik, f);
          delegate.addNode(p);
          delegate.addEdge(p, def);
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.Instruction.Visitor#visitPut(com.ibm.wala.ssa.PutInstruction)
     */
    public void visitPut(SSAPutInstruction instruction) {

      FieldReference field = instruction.getDeclaredField();
      // skip putfields of primitive type
      if (field.getFieldType().isPrimitiveType()) {
        return;
      }
      IField f = cg.getClassHierarchy().resolveField(field);
      if (f == null) {
        return;
      }
      PointerKey val = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getVal());
      if (val == null) {
        // this can happen if val is a null constant
        return;
      }
      if (instruction.isStatic()) {
        PointerKey fKey = pa.getHeapModel().getPointerKeyForStaticField(f);
        delegate.addNode(fKey);
        delegate.addNode(val);
        delegate.addEdge(val, fKey);
      } else {
        PointerKey ref = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getRef());
        for (Iterator it = pa.getPointsToSet(ref).iterator(); it.hasNext();) {
          InstanceKey ik = (InstanceKey) it.next();
          PointerKey p = pa.getHeapModel().getPointerKeyForInstanceField(ik, f);
          if (Assertions.verifyAssertions && p == null) {
            Assertions.UNREACHABLE();
          }
          delegate.addNode(val);
          delegate.addNode(p);
          delegate.addEdge(val, p);
        }
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.Instruction.Visitor#visitInvoke(com.ibm.wala.ssa.InvokeInstruction)
     */
    public void visitInvoke(SSAInvokeInstruction instruction) {

      for (Iterator it = node.getPossibleTargets(instruction.getCallSite()).iterator(); it.hasNext();) {
        CGNode target = (CGNode) it.next();

        // some methods, like unmodelled natives, do not have IR.
        if (getIR(target) == null)
          continue;

        // handle parameter passing
        for (int i = 0; i < instruction.getNumberOfUses(); i++) {
          // we rely on the invariant that the value number for the ith
          // parameter
          // is i+1
          final int vn = i + 1;
          if (target.getMethod().getParameterType(i).isReferenceType()) {
            PointerKey actual = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getUse(i));
            PointerKey formal = pa.getHeapModel().getPointerKeyForLocal(target, vn);
            if (actual != null) {
              delegate.addNode(actual);
              if (formal != null) {
                delegate.addNode(formal);
                delegate.addEdge(actual, formal);
              }
            }
          }
        }

        // handle return value.
        if (instruction.hasDef() && instruction.getDeclaredResultType().isReferenceType()) {
          PointerKey result = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getDef());
          PointerKey ret = pa.getHeapModel().getPointerKeyForReturnValue(target);
          delegate.addNode(result);
          delegate.addNode(ret);
          delegate.addEdge(ret, result);
        }
        // generate contraints from exception return value.
        PointerKey e = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getException());
        PointerKey er = pa.getHeapModel().getPointerKeyForExceptionalReturnValue(target);
        delegate.addNode(e);
        delegate.addNode(er);
        delegate.addEdge(er, e);
      }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.Instruction.Visitor#visitThrow(com.ibm.wala.ssa.ThrowInstruction)
     */
    public void visitThrow(SSAThrowInstruction instruction) {
      // don't do anything: we handle exceptional edges
      // in a separate pass
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.ssa.Instruction.Visitor#visitGetCaughtException(com.ibm.wala.ssa.GetCaughtExceptionInstruction)
     */
    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
      List<ProgramCounter> peis = SSAPropagationCallGraphBuilder.getIncomingPEIs(ir, bb);
      PointerKey def = pa.getHeapModel().getPointerKeyForLocal(node, instruction.getDef());
      addExceptionEdges(node, pa, ir, peis, def);
    }
  }

  /**
   * Generate constraints which assign exception values into an exception
   * pointer
   * 
   * @param node
   *          governing node
   * @param peis
   *          list of PEI instructions
   * @param exceptionVar
   *          PointerKey representing a pointer to an exception value
   */
  private void addExceptionEdges(CGNode node, PointerAnalysis pa, IR ir, List<ProgramCounter> peis, PointerKey exceptionVar) {
    delegate.addNode(exceptionVar);
    for (Iterator<ProgramCounter> it = peis.iterator(); it.hasNext();) {
      ProgramCounter peiLoc = it.next();
      SSAInstruction pei = ir.getPEI(peiLoc);

      if (pei instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction s = (SSAAbstractInvokeInstruction) pei;
        PointerKey e = pa.getHeapModel().getPointerKeyForLocal(node, s.getException());
        delegate.addNode(e);
        delegate.addEdge(e, exceptionVar);
      } else if (pei instanceof SSAThrowInstruction) {
        SSAThrowInstruction s = (SSAThrowInstruction) pei;
        PointerKey e = pa.getHeapModel().getPointerKeyForLocal(node, s.getException());
        delegate.addNode(e);
        delegate.addEdge(e, exceptionVar);
      }
    }
  }

  public PointerAnalysis getPointerAnalysis() {
    return pa;
  }

  public Iterator<? extends PointerKey> iterateNodes() {
    // give up on laziness
    processAllNodes();
    return super.iterateNodes();
  }

}
