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
package com.ibm.wala.dataflow.graph;

import java.util.Map;

import com.ibm.wala.fixedpoint.impl.DefaultFixedPointSolver;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntegerUnionFind;

/**
 * Iterative solver for a Killdall dataflow framework
 */
public abstract class DataflowSolver<T, V extends IVariable<V>> extends DefaultFixedPointSolver<V> {

  /**
   * the dataflow problem to solve
   */
  private final IKilldallFramework<T, V> problem;

  /**
   * The "IN" variable for each node.
   */
  private final Map<Object, V> node2In = HashMapFactory.make();

  /**
   * The "OUT" variable for each node, when node transfer requested.
   */
  private final Map<Object, V> node2Out = HashMapFactory.make();

  /**
   * The variable for each edge, when edge transfers requested (indexed by Pair(src, dst))
   */
  private final Map<Object, V> edge2Var = HashMapFactory.make();

  /**
   */
  public DataflowSolver(IKilldallFramework<T, V> problem) {
    // tune the implementation for common case of 2 uses for each
    // dataflow def
    super(2);
    this.problem = problem;
  }

  /**
   * @param n a node
   * @return a fresh variable to represent the lattice value at the IN or OUT of n
   */
  protected abstract V makeNodeVariable(T n, boolean IN);

  protected abstract V makeEdgeVariable(T src, T dst);

  @Override
  protected void initializeVariables() {
    Graph<T> G = problem.getFlowGraph();
    ITransferFunctionProvider<T,V> functions = problem.getTransferFunctionProvider();
    // create a variable for each node.
    for (T N : G) {
      assert N != null;
      V v = makeNodeVariable(N, true);
      node2In.put(N, v);

      if (functions.hasNodeTransferFunctions()) {
        v = makeNodeVariable(N, false);
        node2Out.put(N, v);
      }

      if (functions.hasEdgeTransferFunctions()) {
        for (T S : Iterator2Iterable.make(G.getSuccNodes(N))) {
          v = makeEdgeVariable(N, S);
          edge2Var.put(Pair.make(N, S), v);
        }
      }
    }
  }

  @Override
  protected void initializeWorkList() {
    buildEquations(true, false);
  }

  public V getOut(Object node) {
    assert node != null;
    V v = node2Out.get(node);
    assert v != null : "no out set for " + node;
    return v;
  }

  public V getIn(Object node) {
    return node2In.get(node);
  }

  public V getEdge(Object key) {
    return edge2Var.get(key);
  }

  public V getEdge(Object src, Object dst) {
    assert src != null;
    assert dst != null;
    V v = getEdge(Pair.make(src, dst));
    assert v != null;
    return v;
  }

  private class UnionFind {
    final IntegerUnionFind uf;

    final ObjectArrayMapping<Object> map;

    boolean didSomething = false;

    final private Object[] allKeys;

    private int mapIt(int i, Object[] allVars, Map<Object, V> varMap) {
      for (Object key : varMap.keySet()) {
        allKeys[i] = key;
        allVars[i++] = varMap.get(key);
      }

      return i;
    }

    UnionFind() {
      allKeys = new Object[node2In.size() + node2Out.size() + edge2Var.size()];
      Object allVars[] = new Object[node2In.size() + node2Out.size() + edge2Var.size()];

      int i = mapIt(0, allVars, node2In);
      i = mapIt(i, allVars, node2Out);
      mapIt(i, allVars, edge2Var);

      uf = new IntegerUnionFind(allVars.length);
      map = new ObjectArrayMapping<>(allVars);
    }

    /**
     * record that variable (n1, in1) is the same as variable (n2,in2), where (x,true) = IN(X) and (x,false) = OUT(X)
     */
    public void union(Object n1, Object n2) {
      assert n1 != null;
      assert n2 != null;
      int x = map.getMappedIndex(n1);
      int y = map.getMappedIndex(n2);
      uf.union(x, y);
      didSomething = true;
    }

    public int size() {
      return map.getSize();
    }

    public int find(int i) {
      return uf.find(i);
    }

    public boolean isIn(int i) {
      return i < node2In.size();
    }

    public boolean isOut(int i) {
      return !isIn(i) && i < (node2In.size() + node2Out.size());
    }

    public Object getKey(int i) {
      return allKeys[i];
    }
  }

  protected void buildEquations(boolean toWorkList, boolean eager) {
    ITransferFunctionProvider<T, V> functions = problem.getTransferFunctionProvider();
    Graph<T> G = problem.getFlowGraph();
    AbstractMeetOperator<V> meet = functions.getMeetOperator();
    UnionFind uf = new UnionFind();
    if (meet.isUnaryNoOp()) {
      shortCircuitUnaryMeets(G, functions, uf);
    }
    shortCircuitIdentities(G, functions, uf);
    fixShortCircuits(uf);

    // add meet operations
    int meetThreshold = (meet.isUnaryNoOp() ? 2 : 1);
    for (T node : G) {
      int nPred = G.getPredNodeCount(node);
      if (nPred >= meetThreshold) {
        // todo: optimize further using unary operators when possible?
        V[] rhs = makeStmtRHS(nPred);
        int i = 0;
        for (Object o : Iterator2Iterable.make(G.getPredNodes(node))) {
          rhs[i++] = (functions.hasEdgeTransferFunctions()) ? getEdge(o, node) : getOut(o);
        }
        newStatement(getIn(node), meet, rhs, toWorkList, eager);
      }
    }

    // add node transfer operations, if requested
    if (functions.hasNodeTransferFunctions()) {
      for (T node : G) {
        UnaryOperator<V> f = functions.getNodeTransferFunction(node);
        if (!f.isIdentity()) {
          newStatement(getOut(node), f, getIn(node), toWorkList, eager);
        }
      }
    }

    // add edge transfer operations, if requested
    if (functions.hasEdgeTransferFunctions()) {
      for (T node : G) {
        for (T succ : Iterator2Iterable.make(G.getSuccNodes(node))) {
          UnaryOperator<V> f = functions.getEdgeTransferFunction(node, succ);
          if (!f.isIdentity()) {
            newStatement(getEdge(node, succ), f, (functions.hasNodeTransferFunctions()) ? getOut(node) : getIn(node), toWorkList,
                eager);
          }
        }
      }
    }
  }

  /**
   * Swap variables to account for identity transfer functions.
   */
  private void shortCircuitIdentities(Graph<T> G, ITransferFunctionProvider<T, V> functions, UnionFind uf) {
    if (functions.hasNodeTransferFunctions()) {
      for (T node : G) {
        UnaryOperator<V> f = functions.getNodeTransferFunction(node);
        if (f.isIdentity()) {
          uf.union(getIn(node), getOut(node));
        }
      }
    }

    if (functions.hasEdgeTransferFunctions()) {
      for (T node : G) {
        for (T succ : Iterator2Iterable.make(G.getSuccNodes(node))) {
          UnaryOperator<V> f = functions.getEdgeTransferFunction(node, succ);
          if (f.isIdentity()) {
            uf.union(getEdge(node, succ), (functions.hasNodeTransferFunctions()) ? getOut(node) : getIn(node));
          }
        }
      }
    }
  }

  /**
   * change the variables to account for short circuit optimizations
   */
  private void fixShortCircuits(UnionFind uf) {
    if (uf.didSomething) {
      for (int i = 0; i < uf.size(); i++) {
        int rep = uf.find(i);
        if (i != rep) {
          Object x = uf.getKey(i);
          Object y = uf.getKey(rep);
          if (uf.isIn(i)) {
            if (uf.isIn(rep)) {
              node2In.put(x, getIn(y));
            } else if (uf.isOut(rep)) {
              node2In.put(x, getOut(y));
            } else {
              node2In.put(x, getEdge(y));
            }
          } else if (uf.isOut(i)) {
            if (uf.isIn(rep)) {
              node2Out.put(x, getIn(y));
            } else if (uf.isOut(rep)) {
              node2Out.put(x, getOut(y));
            } else {
              node2Out.put(x, getEdge(y));
            }
          } else {
            if (uf.isIn(rep)) {
              edge2Var.put(x, getIn(y));
            } else if (uf.isOut(rep)) {
              edge2Var.put(x, getOut(y));
            } else {
              edge2Var.put(x, getEdge(y));
            }
          }
        }
      }
    }
  }

  private void shortCircuitUnaryMeets(Graph<T> G, ITransferFunctionProvider<T,V> functions, UnionFind uf) {
    for (T node : G) {
      assert node != null;
      int nPred = G.getPredNodeCount(node);
      if (nPred == 1) {
        // short circuit by setting IN = OUT_p
        Object p = G.getPredNodes(node).next();
        // if (p == null) {
        // p = G.getPredNodes(node).next();
        // }
        assert p != null;
        uf.union(getIn(node), functions.hasEdgeTransferFunctions() ? getEdge(p, node) : getOut(p));
      }
    }
  }

  public IKilldallFramework<T,V> getProblem() {
    return problem;
  }
}
