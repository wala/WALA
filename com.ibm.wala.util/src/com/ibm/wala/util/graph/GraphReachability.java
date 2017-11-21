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
package com.ibm.wala.util.graph;

import java.util.Iterator;
import java.util.function.Predicate;

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionConstant;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * A dataflow system that computes, for each graph node, the set of "interesting" nodes that are reachable
 */
public class GraphReachability<T, S> {

  /**
   * Governing graph
   */
  private final Graph<T> g;

  /**
   * Killdall-style dataflow solver
   */
  private DataflowSolver<T, BitVectorVariable> solver;

  /**
   * set of "interesting" CGNodes
   */
  final OrdinalSetMapping<S> domain;

  /**
   * @param g call graph to analyze
   * @param filter "interesting" node definition
   * @throws IllegalArgumentException if g is null
   */
  public GraphReachability(Graph<T> g, Predicate<? super T> filter) {
    if (g == null) {
      throw new IllegalArgumentException("g is null");
    }
    this.g = g;
    Iterator<T> i = new FilterIterator<>(g.iterator(), filter);
    domain = new MutableMapping<>((Iterator2Collection.toSet(i)).toArray());
  }

  /**
   * @param n
   * @return the set of interesting nodes reachable from n
   */
  public OrdinalSet<S> getReachableSet(Object n) throws IllegalStateException {
    if (solver == null) {
      throw new IllegalStateException("must call solve() before calling getReachableSet()");
    }
    BitVectorVariable v = solver.getOut(n);
    assert v != null : "null variable for node " + n;
    if (v.getValue() == null) {
      return OrdinalSet.empty();
    } else {
      return new OrdinalSet<>(v.getValue(), domain);
    }
  }

  /**
   * @return true iff the evaluation of some equation caused a change in the value of some variable.
   */
  public boolean solve(IProgressMonitor monitor) throws CancelException {

    ITransferFunctionProvider<T, BitVectorVariable> functions = new ITransferFunctionProvider<T, BitVectorVariable>() {

      /*
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#getNodeTransferFunction(java.lang.Object)
       */
      @Override
      public UnaryOperator<BitVectorVariable> getNodeTransferFunction(T n) {
        int index = domain.getMappedIndex(n);
        if (index > -1) {
          return new BitVectorUnionConstant(index);
        } else {
          return BitVectorIdentity.instance();
        }
      }

      /*
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#hasNodeTransferFunctions()
       */
      @Override
      public boolean hasNodeTransferFunctions() {
        return true;
      }

      /*
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#getEdgeTransferFunction(java.lang.Object, java.lang.Object)
       */
      @Override
      public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(Object from, Object to) {
        Assertions.UNREACHABLE();
        return null;
      }

      /*
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#hasEdgeTransferFunctions()
       */
      @Override
      public boolean hasEdgeTransferFunctions() {
        return false;
      }

      /*
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#getMeetOperator()
       */
      @Override
      public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
        return BitVectorUnion.instance();
      }
    };

    BitVectorFramework<T, S> f = new BitVectorFramework<>(GraphInverter.invert(g), functions, domain);
    solver = new BitVectorSolver<>(f);
    return solver.solve(monitor);
  }

}
