/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntSet;
import java.util.Iterator;
import java.util.stream.Stream;

/** A wrapper around an SDG to make it look like a supergraph for tabulation. */
class SDGSupergraph implements ISupergraph<Statement, PDG<? extends InstanceKey>> {

  private final ISDG sdg;

  /** Do a backward slice? */
  private final boolean backward;

  public SDGSupergraph(ISDG sdg, boolean backward) {
    this.sdg = sdg;
    this.backward = backward;
  }

  @Override
  public Graph<PDG<? extends InstanceKey>> getProcedureGraph() {
    return Assertions.UNREACHABLE();
  }

  public Object[] getEntry() {
    return Assertions.UNREACHABLE();
  }

  @Override
  public byte classifyEdge(Statement src, Statement dest) {
    return Assertions.UNREACHABLE();
  }

  @Override
  public Iterator<? extends Statement> getCallSites(
      Statement r, PDG<? extends InstanceKey> callee) {
    switch (r.getKind()) {
      case EXC_RET_CALLER -> {
        ExceptionalReturnCaller n = (ExceptionalReturnCaller) r;
        SSAAbstractInvokeInstruction call = n.getInstruction();
        PDG<?> pdg = getProcOf(r);
        return pdg.getCallStatements(call).iterator();
      }
      case NORMAL_RET_CALLER -> {
        NormalReturnCaller n = (NormalReturnCaller) r;
        SSAAbstractInvokeInstruction call = n.getInstruction();
        PDG<?> pdg = getProcOf(r);
        return pdg.getCallStatements(call).iterator();
      }
      case HEAP_RET_CALLER -> {
        HeapStatement.HeapReturnCaller n = (HeapStatement.HeapReturnCaller) r;
        SSAAbstractInvokeInstruction call = n.getCall();
        PDG<?> pdg = getProcOf(r);
        return pdg.getCallStatements(call).iterator();
      }
      default -> {
        return Assertions.UNREACHABLE(r.getKind().toString());
      }
    }
  }

  @Override
  public Iterator<? extends Statement> getCalledNodes(Statement call) {
    return switch (call.getKind()) {
      case NORMAL -> new FilterIterator<>(getSuccNodes(call), this::isEntry);
      case PARAM_CALLER, HEAP_PARAM_CALLER -> getSuccNodes(call);
      default -> {
        Assertions.UNREACHABLE(call.getKind().toString());
        yield null;
      }
    };
  }

  @Override
  public Statement[] getEntriesForProcedure(PDG<? extends InstanceKey> procedure) {
    Statement[] normal = procedure.getParamCalleeStatements();
    Statement[] result = new Statement[normal.length + 1];
    result[0] = new MethodEntryStatement(procedure.getCallGraphNode());
    System.arraycopy(normal, 0, result, 1, normal.length);
    return result;
  }

  @Override
  public Statement[] getExitsForProcedure(PDG<? extends InstanceKey> procedure) {
    Statement[] normal = procedure.getReturnStatements();
    Statement[] result = new Statement[normal.length + 1];
    result[0] = new MethodExitStatement(procedure.getCallGraphNode());
    System.arraycopy(normal, 0, result, 1, normal.length);
    return result;
  }

  @Override
  public Statement getLocalBlock(PDG<? extends InstanceKey> procedure, int i) {
    return procedure.getNode(i);
  }

  @Override
  public int getLocalBlockNumber(Statement n) {
    PDG<?> pdg = getProcOf(n);
    return pdg.getNumber(n);
  }

  @Override
  public Iterator<Statement> getNormalSuccessors(Statement call) {
    if (!backward) {
      return EmptyIterator.instance();
    } else {
      return Assertions.UNREACHABLE();
    }
  }

  @Override
  public int getNumberOfBlocks(PDG<? extends InstanceKey> procedure) {
    return Assertions.UNREACHABLE();
  }

  @Override
  public PDG<? extends InstanceKey> getProcOf(Statement n) {
    CGNode node = n.getNode();
    PDG<? extends InstanceKey> result = sdg.getPDG(node);
    if (result == null) {
      Assertions.UNREACHABLE("panic: " + n + ' ' + node);
    }
    return result;
  }

  @Override
  public Iterator<? extends Statement> getReturnSites(
      Statement call, PDG<? extends InstanceKey> callee) {
    switch (call.getKind()) {
      case PARAM_CALLER -> {
        ParamCaller n = (ParamCaller) call;
        SSAAbstractInvokeInstruction st = n.getInstruction();
        PDG<?> pdg = getProcOf(call);
        return pdg.getCallerReturnStatements(st).iterator();
      }
      case HEAP_PARAM_CALLER -> {
        HeapStatement.HeapParamCaller n = (HeapStatement.HeapParamCaller) call;
        SSAAbstractInvokeInstruction st = n.getCall();
        PDG<?> pdg = getProcOf(call);
        return pdg.getCallerReturnStatements(st).iterator();
      }
      case NORMAL -> {
        NormalStatement n = (NormalStatement) call;
        SSAAbstractInvokeInstruction st = (SSAAbstractInvokeInstruction) n.getInstruction();
        PDG<?> pdg = getProcOf(call);
        return pdg.getCallerReturnStatements(st).iterator();
      }
      default -> {
        return Assertions.UNREACHABLE(call.getKind().toString());
      }
    }
  }

  @Override
  public boolean isCall(Statement n) {
    switch (n.getKind()) {
      case EXC_RET_CALLEE,
          EXC_RET_CALLER,
          HEAP_PARAM_CALLEE,
          NORMAL_RET_CALLEE,
          NORMAL_RET_CALLER,
          PARAM_CALLEE,
          PHI,
          HEAP_RET_CALLEE,
          HEAP_RET_CALLER,
          METHOD_ENTRY,
          METHOD_EXIT,
          CATCH,
          PI -> {
        return false;
      }
      case HEAP_PARAM_CALLER, PARAM_CALLER -> {
        return true;
      }
      case NORMAL -> {
        if (sdg.getCOptions().isIgnoreInterproc()) {
          return false;
        } else {
          NormalStatement s = (NormalStatement) n;
          return s.getInstruction() instanceof SSAAbstractInvokeInstruction;
        }
      }
      default -> {
        return Assertions.UNREACHABLE(n.getKind() + " " + n);
      }
    }
  }

  @Override
  public boolean isEntry(Statement n) {
    return switch (n.getKind()) {
      case PARAM_CALLEE, HEAP_PARAM_CALLEE, METHOD_ENTRY -> true;
      case PHI,
          PI,
          NORMAL_RET_CALLER,
          PARAM_CALLER,
          HEAP_RET_CALLER,
          NORMAL,
          EXC_RET_CALLEE,
          EXC_RET_CALLER,
          HEAP_PARAM_CALLER,
          HEAP_RET_CALLEE,
          NORMAL_RET_CALLEE,
          CATCH ->
          false;
      default -> {
        Assertions.UNREACHABLE(n.toString());
        yield false;
      }
    };
  }

  @Override
  public boolean isExit(Statement n) {
    return switch (n.getKind()) {
      case PARAM_CALLEE,
          HEAP_PARAM_CALLEE,
          HEAP_PARAM_CALLER,
          PHI,
          PI,
          NORMAL_RET_CALLER,
          PARAM_CALLER,
          HEAP_RET_CALLER,
          NORMAL,
          EXC_RET_CALLER,
          METHOD_ENTRY,
          CATCH ->
          false;
      case HEAP_RET_CALLEE, EXC_RET_CALLEE, NORMAL_RET_CALLEE, METHOD_EXIT -> true;
    };
  }

  @Override
  public boolean isReturn(Statement n) {
    return switch (n.getKind()) {
      case EXC_RET_CALLER, NORMAL_RET_CALLER, HEAP_RET_CALLER -> true;
      case EXC_RET_CALLEE,
          HEAP_PARAM_CALLEE,
          HEAP_PARAM_CALLER,
          HEAP_RET_CALLEE,
          NORMAL,
          NORMAL_RET_CALLEE,
          PARAM_CALLEE,
          PARAM_CALLER,
          PHI,
          PI,
          METHOD_ENTRY,
          CATCH ->
          false;
      default -> {
        Assertions.UNREACHABLE(n.getKind().toString());
        yield false;
      }
    };
  }

  @Override
  public void removeNodeAndEdges(Statement N) {
    Assertions.UNREACHABLE();
  }

  @Override
  public void addNode(Statement n) {
    Assertions.UNREACHABLE();
  }

  @Override
  public boolean containsNode(Statement N) {
    return sdg.containsNode(N);
  }

  @Override
  public int getNumberOfNodes() {
    return Assertions.UNREACHABLE();
  }

  @Override
  public Iterator<Statement> iterator() {
    return sdg.iterator();
  }

  @Override
  public Stream<Statement> stream() {
    return sdg.stream();
  }

  @Override
  public void removeNode(Statement n) {
    Assertions.UNREACHABLE();
  }

  @Override
  public void addEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
  }

  @Override
  public int getPredNodeCount(Statement N) {
    return Assertions.UNREACHABLE();
  }

  @Override
  public Iterator<Statement> getPredNodes(Statement N) {
    return sdg.getPredNodes(N);
  }

  @Override
  public int getSuccNodeCount(Statement N) {
    return Assertions.UNREACHABLE();
  }

  @Override
  public Iterator<Statement> getSuccNodes(Statement N) {
    return sdg.getSuccNodes(N);
  }

  @Override
  public boolean hasEdge(Statement src, Statement dst) {
    return sdg.hasEdge(src, dst);
  }

  @Override
  public void removeAllIncidentEdges(Statement node) {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeEdge(Statement src, Statement dst) {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeIncomingEdges(Statement node) {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeOutgoingEdges(Statement node) {
    Assertions.UNREACHABLE();
  }

  @Override
  public int getMaxNumber() {
    return sdg.getMaxNumber();
  }

  @Override
  public Statement getNode(int number) {
    return sdg.getNode(number);
  }

  @Override
  public int getNumber(Statement N) {
    return sdg.getNumber(N);
  }

  @Override
  public Iterator<Statement> iterateNodes(IntSet s) {
    return Assertions.UNREACHABLE();
  }

  @Override
  public IntSet getPredNodeNumbers(Statement node) {
    return sdg.getPredNodeNumbers(node);
  }

  /**
   * @see com.ibm.wala.util.graph.NumberedEdgeManager#getSuccNodeNumbers(java.lang.Object)
   */
  @Override
  public IntSet getSuccNodeNumbers(Statement node) {
    return sdg.getSuccNodeNumbers(node);
  }
}
