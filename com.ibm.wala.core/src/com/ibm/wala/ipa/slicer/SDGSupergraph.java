/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import java.util.Iterator;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.slicer.ParamStatement.ExceptionalReturnCaller;
import com.ibm.wala.ipa.slicer.ParamStatement.NormalReturnCaller;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * A wrapper around an SDG to make it look like a supergraph for tabulation.
 * 
 * @author sjfink
 * 
 */
class SDGSupergraph implements ISupergraph<Statement, PDG> {

  private final ISDG sdg;

  /**
   * We are interested in flow to or from the following statement.
   */
  private final Statement src;

  /**
   * Do a backward slice?
   */
  private final boolean backward;

  public SDGSupergraph(ISDG sdg, Statement src, boolean backward) {
    this.sdg = sdg;
    this.src = src;
    this.backward = backward;
  }

  public Object[] getEntry(Statement n) {
    Assertions.UNREACHABLE();
    return null;
  }

  public byte classifyEdge(Statement src, Statement dest) {
    Assertions.UNREACHABLE();
    return 0;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCallSites(java.lang.Object)
   */
  public Iterator<? extends Statement> getCallSites(Statement r) {
    switch (r.getKind()) {
    case EXC_RET_CALLER: {
      ParamStatement.ExceptionalReturnCaller n = (ExceptionalReturnCaller) r;
      SSAAbstractInvokeInstruction call = n.getCall();
      PDG pdg = getProcOf(r);
      return pdg.getCallerParamStatements(call).iterator();
    }
    case NORMAL_RET_CALLER: {
      ParamStatement.NormalReturnCaller n = (NormalReturnCaller) r;
      SSAAbstractInvokeInstruction call = n.getCall();
      PDG pdg = getProcOf(r);
      return pdg.getCallerParamStatements(call).iterator();
    }
    case HEAP_RET_CALLER: {
      HeapStatement.ReturnCaller n = (HeapStatement.ReturnCaller) r;
      SSAAbstractInvokeInstruction call = n.getCall();
      PDG pdg = getProcOf(r);
      return pdg.getCallerParamStatements(call).iterator();
    }
    default:
      Assertions.UNREACHABLE(r.getKind().toString());
      return null;
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getCalledNodes(java.lang.Object)
   */
  public Iterator<? extends Statement> getCalledNodes(Statement call) {
    switch (call.getKind()) {
    case NORMAL:
      Filter f = new Filter() {
        public boolean accepts(Object o) {
          Statement s = (Statement) o;
          return isEntry(s);
        }
      };
      return new FilterIterator<Statement>(getSuccNodes(call), f);
    case PARAM_CALLER:
      return getSuccNodes(call);
    default:
      Assertions.UNREACHABLE(call.getKind().toString());
      return null;
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getEntriesForProcedure(java.lang.Object)
   */
  public Statement[] getEntriesForProcedure(PDG procedure) {
    if (procedure.equals(getMain()) && !backward) {
      Statement[] normal = procedure.getParamCalleeStatements();
      Statement[] result = new Statement[normal.length + 1];
      result[0] = getMainEntry();
      System.arraycopy(normal, 0, result, 1, normal.length);
      return result;
    } else {
      return procedure.getParamCalleeStatements();
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getExitsForProcedure(java.lang.Object)
   */
  public Statement[] getExitsForProcedure(PDG procedure) {
    if (procedure.equals(getMain()) && backward) {
      Statement[] normal = procedure.getReturnStatements();
      Statement[] result = new Statement[normal.length + 1];
      result[0] = getMainExit();
      System.arraycopy(normal, 0, result, 1, normal.length);
      return result;
    } else {
      return procedure.getReturnStatements();
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlock(java.lang.Object,
   *      int)
   */
  public Statement getLocalBlock(PDG procedure, int i) {
    return procedure.getNode(i);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getLocalBlockNumber(java.lang.Object)
   */
  public int getLocalBlockNumber(Statement n) {
    PDG pdg = getProcOf(n);
    return pdg.getNumber(n);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMain()
   */
  public PDG getMain() {
    return getProcOf(src);
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMainEntry()
   */
  public Statement getMainEntry() {
    Assertions.productionAssertion(!backward, "todo: support backward");
    return src;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getMainExit()
   */
  public Statement getMainExit() {
    // We pretend that sink is the "main exit" .. we don't care about
    // flow past the sink.
    Assertions.productionAssertion(backward, "todo: support forward");
    return src;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getNormalSuccessors(java.lang.Object)
   */
  public Iterator<Statement> getNormalSuccessors(Statement call) {
    if (!backward) {
      return EmptyIterator.instance();
    } else {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getNumberOfBlocks(java.lang.Object)
   */
  public int getNumberOfBlocks(PDG procedure) {
    Assertions.UNREACHABLE();
    return 0;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getProcOf(java.lang.Object)
   */
  public PDG getProcOf(Statement n) {
    CGNode node = n.getNode();
    PDG result = sdg.getPDG(node);
    if (result == null) {
      Assertions.UNREACHABLE("panic: " + n + " " + node);
    }
    return result;
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#getReturnSites(java.lang.Object)
   */
  public Iterator<? extends Statement> getReturnSites(Statement call) {
    switch (call.getKind()) {
    case PARAM_CALLER: {
      ParamStatement.ParamCaller n = (ParamStatement.ParamCaller) call;
      SSAAbstractInvokeInstruction st = n.getCall();
      PDG pdg = getProcOf(call);
      return pdg.getCallerReturnStatements(st).iterator();
    }
    case HEAP_PARAM_CALLER: {
      HeapStatement.ParamCaller n = (HeapStatement.ParamCaller) call;
      SSAAbstractInvokeInstruction st = n.getCall();
      PDG pdg = getProcOf(call);
      return pdg.getCallerReturnStatements(st).iterator();
    }
    case NORMAL: {
      NormalStatement n = (NormalStatement) call;
      SSAAbstractInvokeInstruction st = (SSAAbstractInvokeInstruction) n.getInstruction();
      PDG pdg = getProcOf(call);
      return pdg.getCallerReturnStatements(st).iterator();
    }
    default:
      Assertions.UNREACHABLE(call.getKind().toString());
      return null;
    }
  }

  /*
   * @see com.ibm.wala.dataflow.IFDS.ISupergraph#isCall(java.lang.Object)
   */
  public boolean isCall(Statement n) {
    switch (n.getKind()) {
    case EXC_RET_CALLEE:
    case EXC_RET_CALLER:
    case HEAP_PARAM_CALLEE:
    case NORMAL_RET_CALLEE:
    case NORMAL_RET_CALLER:
    case PARAM_CALLEE:
    case PHI:
    case HEAP_RET_CALLEE:
    case HEAP_RET_CALLER:
    case METHOD_ENTRY:
      return false;
    case HEAP_PARAM_CALLER:
    case PARAM_CALLER:
      return true;
    case NORMAL:
      if (sdg.getCOptions().equals(ControlDependenceOptions.NONE)) {
        return false;
      } else {
        NormalStatement s = (NormalStatement) n;
        return s.getInstruction() instanceof SSAAbstractInvokeInstruction;
      }
    default:
      Assertions.UNREACHABLE(n.toString());
      return false;
    }
  }

  public boolean isEntry(Statement n) {
    switch (n.getKind()) {
    case PARAM_CALLEE:
    case HEAP_PARAM_CALLEE:
    case METHOD_ENTRY:
      return true;
    case PHI:
    case PI:
    case NORMAL_RET_CALLER:
    case PARAM_CALLER:
    case HEAP_RET_CALLER:
    case NORMAL:
    case EXC_RET_CALLEE:
    case EXC_RET_CALLER:
    case HEAP_PARAM_CALLER:
    case HEAP_RET_CALLEE:
    case NORMAL_RET_CALLEE:
    case CATCH:
      return false;
    default:
      Assertions.UNREACHABLE(n.toString());
      return false;
    }
  }

  public boolean isExit(Statement n) {
    switch (n.getKind()) {
    case PARAM_CALLEE:
    case HEAP_PARAM_CALLEE:
    case PHI:
    case NORMAL_RET_CALLER:
    case PARAM_CALLER:
    case HEAP_RET_CALLER:
    case NORMAL:
    case EXC_RET_CALLER:
    case METHOD_ENTRY:
      return false;
    case HEAP_RET_CALLEE:
    case EXC_RET_CALLEE:
    case NORMAL_RET_CALLEE:
      return true;
    default:
      Assertions.UNREACHABLE(n.toString());
      return false;
    }
  }

  public boolean isReturn(Statement n) {
    switch (n.getKind()) {
    case EXC_RET_CALLER:
    case NORMAL_RET_CALLER:
    case HEAP_RET_CALLER:
      return true;
    case EXC_RET_CALLEE:
    case HEAP_PARAM_CALLEE:
    case HEAP_PARAM_CALLER:
    case HEAP_RET_CALLEE:
    case NORMAL:
    case NORMAL_RET_CALLEE:
    case PARAM_CALLEE:
    case PARAM_CALLER:
    case PHI:
    case PI:
    case METHOD_ENTRY:
    case CATCH:
      return false;
    default:
      Assertions.UNREACHABLE(n.getKind().toString());
      return false;
    }
  }

  public void removeNodeAndEdges(Statement N) {
    Assertions.UNREACHABLE();

  }

  public void addNode(Statement n) {
    Assertions.UNREACHABLE();

  }

  public boolean containsNode(Statement N) {
    return sdg.containsNode(N);
  }

  public int getNumberOfNodes() {
    Assertions.UNREACHABLE();
    return 0;
  }

  public Iterator<Statement> iterator() {
    return sdg.iterator();
  }

  public void removeNode(Statement n) {
    Assertions.UNREACHABLE();

  }

  public void addEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();

  }

  public int getPredNodeCount(Statement N) {
    Assertions.UNREACHABLE();
    return 0;
  }

  public Iterator<? extends Statement> getPredNodes(Statement N) {
    return sdg.getPredNodes(N);
  }

  public int getSuccNodeCount(Statement N) {
    Assertions.UNREACHABLE();
    return 0;
  }

  public Iterator<? extends Statement> getSuccNodes(Statement N) {
    if (backward) {
      if (N.equals(src)) {
        return EmptyIterator.instance();
      } else {
        return sdg.getSuccNodes(N);
      }
    } else {
      return sdg.getSuccNodes(N);
    }
  }

  public boolean hasEdge(Statement src, Statement dst) {
    if (backward) {
      if (src.equals(this.src)) {
        return IteratorUtil.contains(getSuccNodes(src), dst);
      } else {
        return sdg.hasEdge(src, dst);
      }
    } else {
      if (dst.equals(this.src)) {
        return IteratorUtil.contains(getPredNodes(dst), src);
      } else {
        return sdg.hasEdge(src, dst);
      }
    }
  }

  public void removeAllIncidentEdges(Statement node) {
    Assertions.UNREACHABLE();

  }

  public void removeEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();

  }

  public void removeIncomingEdges(Statement node) {
    Assertions.UNREACHABLE();

  }

  public void removeOutgoingEdges(Statement node) {
    Assertions.UNREACHABLE();

  }

  public int getMaxNumber() {
    return sdg.getMaxNumber();
  }

  public Statement getNode(int number) {
    return sdg.getNode(number);
  }

  public int getNumber(Statement N) {
    return sdg.getNumber(N);
  }

  public Iterator<Statement> iterateNodes(IntSet s) {
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getPredNodeNumbers(Statement node) {
    return sdg.getPredNodeNumbers(node);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedEdgeManager#getSuccNodeNumbers(java.lang.Object)
   */
  public IntSet getSuccNodeNumbers(Statement node) {
    if (backward) {
      if (node.equals(src)) {
        return new SparseIntSet();
      } else {
        return sdg.getSuccNodeNumbers(node);
      }
    } else {
      return sdg.getSuccNodeNumbers(node);
    }
  }

}
