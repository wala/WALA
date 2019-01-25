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
package com.ibm.wala.core.tests.basic;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFilter;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionConstant;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.UnaryOperator;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Simple Regression test for a graph-based dataflow problem.
 */
public class GraphDataflowTest extends WalaTestCase {

  public static final String nodeNames = "ABCDEFGH";
  protected final static String[] nodes = new String[nodeNames.length()];

  private static BitVector zero() {
    BitVector b = new BitVector();
    b.set(0);
    return b;
  }

  private static BitVector one() {
    BitVector b = new BitVector();
    b.set(1);
    return b;
  }

  /**
   * A simple test of the GraphBitVectorDataflow system
   * @throws CancelException 
   */
  @Test public void testSolverNodeEdge() throws CancelException {
    Graph<String> G = buildGraph();
    String result = solveNodeEdge(G);
    System.err.println(result);
    if (!result.equals(expectedStringNodeEdge())) {
      System.err.println("Uh oh.");
      System.err.println(expectedStringNodeEdge());
    }
    Assert.assertEquals(expectedStringNodeEdge(), result);
  }

  @Test public void testSolverNodeOnly() throws CancelException {
    Graph<String> G = buildGraph();
    String result = solveNodeOnly(G);
    System.err.println(result);
    Assert.assertEquals(expectedStringNodeOnly(), result);
  }

  /**
   * @return the expected dataflow result as a String
   */
  public static String expectedStringNodeOnly() {
    StringBuilder result = new StringBuilder("------\n");
    result.append("Node A(0) = { 0 }\n");
    result.append("Node B(1) = { 0 1 }\n");
    result.append("Node C(2) = { 0 1 2 }\n");
    result.append("Node D(3) = { 0 1 3 }\n");
    result.append("Node E(4) = { 0 1 2 3 4 }\n");
    result.append("Node F(5) = { 0 1 2 3 4 5 }\n");
    result.append("Node G(6) = { 6 }\n");
    result.append("Node H(7) = { 7 }\n");
    return result.toString();
  }

  public static String expectedStringNodeEdge() {
    StringBuilder result = new StringBuilder("------\n");
    result.append("Node A(0) = { 0 }\n");
    result.append("Node B(1) = { 0 1 }\n");
    result.append("Node C(2) = { 0 2 }\n");
    result.append("Node D(3) = { 1 3 }\n");
    result.append("Node E(4) = { 0 1 2 3 4 }\n");
    result.append("Node F(5) = { 0 1 2 3 4 5 }\n");
    result.append("Node G(6) = { 6 }\n");
    result.append("Node H(7) = { 7 }\n");
    return result.toString();
  }

  /**
   * @return a graph with the expected structure
   */
  public static Graph<String> buildGraph() {
    Graph<String> G = SlowSparseNumberedGraph.make();
    for (int i = 0; i < nodeNames.length(); i++) {
      String n = nodeNames.substring(i, i + 1);
      G.addNode(n);
      nodes[i] = n;
    }
    G.addEdge(nodes[0], nodes[1]);
    G.addEdge(nodes[1], nodes[2]);
    G.addEdge(nodes[1], nodes[3]);
    G.addEdge(nodes[2], nodes[4]);
    G.addEdge(nodes[3], nodes[4]);
    G.addEdge(nodes[4], nodes[5]);
    return G;
  }

  /**
   * Solve the dataflow system and return the result as a string
   * @throws CancelException 
   */
  public static String solveNodeOnly(Graph<String> G) throws CancelException {
    final OrdinalSetMapping<String> values = new MutableMapping<>(nodes);
    ITransferFunctionProvider<String, BitVectorVariable> functions = new ITransferFunctionProvider<String, BitVectorVariable>() {
      
      @Override
      public UnaryOperator<BitVectorVariable> getNodeTransferFunction(String node) {
        return new BitVectorUnionConstant(values.getMappedIndex(node));
      }

      @Override
      public boolean hasNodeTransferFunctions() {
        return true;
      }

      @Override
      public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(String from, String to) {
        Assertions.UNREACHABLE();
        return null;
      }

      @Override
      public boolean hasEdgeTransferFunctions() {
        return false;
      }

      @Override
      public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
        return BitVectorUnion.instance();
      }

    };

    BitVectorFramework<String,String> F = new BitVectorFramework<>(G, functions, values);
    BitVectorSolver<String> s = new BitVectorSolver<>(F);
    s.solve(null);
    return result2String(s);
  }

  public static String solveNodeEdge(Graph<String> G) throws CancelException {
    final OrdinalSetMapping<String> values = new MutableMapping<>(nodes);
    ITransferFunctionProvider<String, BitVectorVariable> functions = new ITransferFunctionProvider<String, BitVectorVariable>() {

      @Override
      public UnaryOperator<BitVectorVariable> getNodeTransferFunction(String node) {
        return new BitVectorUnionConstant(values.getMappedIndex(node));
      }

      @Override
      public boolean hasNodeTransferFunctions() {
        return true;
      }

      @Override
      public UnaryOperator<BitVectorVariable> getEdgeTransferFunction(String from, String to) {
        if (from == nodes[1] && to == nodes[3])
          return new BitVectorFilter(zero());
        else if (from == nodes[1] && to == nodes[2])
          return new BitVectorFilter(one());
        else {
          return BitVectorIdentity.instance();
        }
      }

      @Override
      public boolean hasEdgeTransferFunctions() {
        return true;
      }

      @Override
      public AbstractMeetOperator<BitVectorVariable> getMeetOperator() {
        return BitVectorUnion.instance();
      }

    };

    BitVectorFramework<String,String> F = new BitVectorFramework<>(G, functions, values);
    BitVectorSolver<String> s = new BitVectorSolver<>(F);
    s.solve(null);
    return result2String(s);
  }

  public static String result2String(BitVectorSolver<String> solver) {
    StringBuilder result = new StringBuilder("------\n");
    for (int i = 0; i < nodes.length; i++) {
      String n = nodes[i];
      BitVectorVariable varI = solver.getOut(n);
      String s = varI.toString();
      result.append("Node " + n + "(" + i + ") = " + s + "\n");
    }
    return result.toString();
  }
}
