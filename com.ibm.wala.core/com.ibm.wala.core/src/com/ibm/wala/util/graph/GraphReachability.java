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

import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionConstant;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * 
 * A dataflow system that computes, for each graph node, the set of
 * "interesting" nodes that are reachable
 * 
 * @author sfink
 */
public class GraphReachability <T>{

  /**
   * Governing graph
   */
  private final Graph<T> g;

  /**
   * Killdall-style dataflow solver
   */
  private DataflowSolver solver;

  /**
   * set of "interesting" CGNodes
   */
  final OrdinalSetMapping<T> domain;

  /**
   * @param g
   *          call graph to analyze
   * @param filter
   *          "interesting" node definition
   */
  public GraphReachability(Graph<T> g, Filter filter) {
    this.g = g;
    Iterator<T> i = new FilterIterator<T>(g.iterateNodes(), filter);
    domain = new MutableMapping<T>((new Iterator2Collection<T>(i)).toArray());
  }

  /**
   * @param n
   * @return the set of interesting nodes reachable from n
   */
  public OrdinalSet<T> getReachableSet(Object n) {
    BitVectorVariable v = (BitVectorVariable) solver.getOut(n);
    if (Assertions.verifyAssertions) {
      if (v == null) {
        Assertions._assert(v != null, "null variable for node " + n);
      }
    }
    if (v.getValue() == null) {
      return OrdinalSet.empty();
    } else {
      return new OrdinalSet<T>(v.getValue(), domain);
    }
  }

  /**
   * @return true iff the evaluation of some equation caused a change in the
   *         value of some variable.
   */
  public boolean solve() {

    ITransferFunctionProvider<T> functions = new ITransferFunctionProvider<T>() {

      /* (non-Javadoc)
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#getNodeTransferFunction(java.lang.Object)
       */
      public UnaryOperator getNodeTransferFunction(T n) {
        int index = domain.getMappedIndex(n);
        if (index > -1) {
          return new BitVectorUnionConstant(index);
        } else {
          return BitVectorIdentity.instance();
        }
      }

      /* (non-Javadoc)
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#hasNodeTransferFunctions()
       */
      public boolean hasNodeTransferFunctions() {
        return true;
      }

      /* (non-Javadoc)
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#getEdgeTransferFunction(java.lang.Object, java.lang.Object)
       */
      public UnaryOperator getEdgeTransferFunction(Object from, Object to) {
        Assertions.UNREACHABLE();
        return null;
      }

      /* (non-Javadoc)
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#hasEdgeTransferFunctions()
       */
      public boolean hasEdgeTransferFunctions() {
        return false;
      }

      /* (non-Javadoc)
       * @see com.ibm.wala.dataflow.graph.ITransferFunctionProvider#getMeetOperator()
       */
      public AbstractMeetOperator getMeetOperator() {
        return BitVectorUnion.instance();
      }
    };

    BitVectorFramework<T,T> f = new BitVectorFramework<T,T>(GraphInverter.invert(g), functions, domain);
    solver = new BitVectorSolver<T>(f);
    return solver.solve();
  }

}