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
package com.ibm.wala.fixedpoint.impl;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.fixpoint.IFixedPointStatement;
import com.ibm.wala.fixpoint.IVariable;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.ReverseIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphIntegrity;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.impl.SparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.DFS;

/**
 * 
 * Default implementation of a dataflow graph
 * 
 * @author sfink
 */
public class DefaultFixedPointSystem extends AbstractFixedPointSystem {
  static final boolean DEBUG = false;

  /**
   * A graph which defines the underlying system of statements and variables
   */
  private final NumberedGraph<INodeWithNumber> graph;

  /**
   * We maintain a hash set of equations in order to check for equality with
   * equals() ... the NumberedGraph does not support this. TODO: use a custom
   * NumberedNodeManager to save space
   */
  private Set<GeneralStatement> equations = HashSetFactory.make();

  /**
   * We maintain a hash set of variables in order to check for equality with
   * equals() ... the NumberedGraph does not support this. TODO: use a custom
   * NumberedNodeManager to save space
   */
  private Set<IVariable> variables = HashSetFactory.make();

  /**
   * @param expectedOut number of expected out edges in the "usual" case
   * for constraints .. used to tune graph representation
   */
  public DefaultFixedPointSystem(int expectedOut) {
    super();
    graph = new SparseNumberedGraph<INodeWithNumber>(expectedOut);
  }
  
  /**
   * default constructor ... tuned for one use for each def in
   * dataflow graph.
   */
  public DefaultFixedPointSystem() {
    this(1);
  }
  
  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    return graph.equals(obj);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return graph.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return graph.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#removeEquation(com.ibm.wala.dataflow.fixpoint.AbstractEquation)
   */
  public void removeStatement(IFixedPointStatement s) {
    graph.removeNodeAndEdges(s);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getEquations()
   */
  @SuppressWarnings("unchecked")
  public Iterator<AbstractStatement> getStatements() {
    return new FilterIterator(graph.iterateNodes(), new Filter() {
      public boolean accepts(Object x) {
        return x instanceof AbstractStatement;
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void addStatement(IFixedPointStatement statement) {
    if (statement instanceof UnaryStatement) {
      addStatement((UnaryStatement) statement);
    } else if (statement instanceof NullaryStatement) {
      addStatement((NullaryStatement) statement);
    } else if (statement instanceof GeneralStatement) {
      addStatement((GeneralStatement) statement);
    } else {
      Assertions.UNREACHABLE("unexpected: " + statement.getClass());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#addEquation(com.ibm.wala.dataflow.fixpoint.GeneralEquation)
   */
  public void addStatement(GeneralStatement s) {
    IVariable[] rhs = s.getRHS();
    IVariable lhs = s.getLHS();

    equations.add(s);
    graph.addNode(s);
    if (lhs != null) {
      variables.add(lhs);
      graph.addNode(lhs);
      graph.addEdge(s, lhs);
    }
    for (int i = 0; i < rhs.length; i++) {
      IVariable v = rhs[i];
      if (v != null) {
        variables.add(v);
        graph.addNode(v);
        graph.addEdge(v, s);
      }
    }

    if (DEBUG) {
      checkGraph();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#addEquation(com.ibm.wala.dataflow.fixpoint.UnaryEquation)
   */
  public void addStatement(UnaryStatement s) {
    IVariable lhs = s.getLHS();
    IVariable rhs = s.getRightHandSide();

    graph.addNode(s);
    if (lhs != null) {
      variables.add(lhs);
      graph.addNode(lhs);
      graph.addEdge(s, lhs);
    }
    variables.add(rhs);
    graph.addNode(rhs);
    graph.addEdge(rhs, s);

    if (DEBUG) {
      checkGraph();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#addEquation(com.ibm.wala.dataflow.fixpoint.UnaryEquation)
   */
  public void addStatement(NullaryStatement s) {
    IVariable lhs = s.getLHS();

    graph.addNode(s);
    if (lhs != null) {
      variables.add(lhs);
      graph.addNode(lhs);
      graph.addEdge(s, lhs);
    }

    if (DEBUG) {
      checkGraph();
    }
  }

  public void addVariable(IVariable v) {
    variables.add(v);
    graph.addNode(v);
    if (DEBUG) {
      checkGraph();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getEquation(int)
   */
  public AbstractStatement getStep(int number) {
    return (AbstractStatement) graph.getNode(number);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#reorder()
   */
  public void reorder() {
    if (DEBUG) {
      checkGraph();
    }

    Iterator<INodeWithNumber> finishTime = DFS.iterateFinishTime(graph);
    Iterator<INodeWithNumber> rev = new ReverseIterator<INodeWithNumber>(finishTime);
    // the following statement helps out the GC; note that finishTime holds
    // on to a large array
    finishTime = null;
    Graph<INodeWithNumber> G_T = GraphInverter.invert(graph);
    Iterator<INodeWithNumber> order = DFS.iterateFinishTime(G_T, rev);
    int number = 0;
    while (order.hasNext()) {
      Object elt = order.next();
      if (elt instanceof IVariable) {
        IVariable v = (IVariable) elt;
        v.setOrderNumber(number++);
      }
    }
  }

  /**
   * check that this graph is well-formed
   */
  private void checkGraph() {
    try {
      GraphIntegrity.check(graph);
    } catch (Throwable e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getEquationsThatUse(com.ibm.wala.dataflow.fixpoint.IVariable)
   */
  public Iterator getStatementsThatUse(IVariable v) {
    return (graph.containsNode(v) ? graph.getSuccNodes(v) : EmptyIterator.instance());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getEquationsThatDef(com.ibm.wala.dataflow.fixpoint.IVariable)
   */
  public Iterator getStatementsThatDef(IVariable v) {
    return (graph.containsNode(v) ? graph.getPredNodes(v) : EmptyIterator.instance());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getVariable(int)
   */
  public IVariable getVariable(int n) {
    return (IVariable) graph.getNode(n);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getNumberOfEquationsThatUse(com.ibm.wala.dataflow.fixpoint.AbstractVariable)
   */
  public int getNumberOfStatementsThatUse(IVariable v) {
    return (graph.containsNode(v) ? graph.getSuccNodeCount(v) : 0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getNumberOfEquationsThatUse(com.ibm.wala.dataflow.fixpoint.AbstractVariable)
   */
  public int getNumberOfStatementsThatDef(IVariable v) {
    return (graph.containsNode(v) ? graph.getPredNodeCount(v) : 0);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#getVariables()
   */
  @SuppressWarnings("unchecked")
  public Iterator<IVariable> getVariables() {
    return new FilterIterator(graph.iterateNodes(), new Filter() {
      public boolean accepts(Object x) {
        return x instanceof IVariable;
      }
    });
  }

  public int getNumberOfNodes() {
    return graph.getNumberOfNodes();
  }

  public Iterator<? extends INodeWithNumber> getPredNodes(INodeWithNumber n) {
    return graph.getPredNodes(n);
  }


  public int getPredNodeCount(INodeWithNumber n) {
    return graph.getPredNodeCount(n);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#containsEquations(com.ibm.wala.dataflow.fixpoint.AbstractEquation)
   */
  public boolean containsStatement(IFixedPointStatement s) {
    return equations.contains(s);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.dataflow.fixpoint.DataflowGraph#containsEquations(com.ibm.wala.dataflow.fixpoint.AbstractEquation)
   */
  public boolean containsVariable(IVariable v) {
    return variables.contains(v);
  }

}