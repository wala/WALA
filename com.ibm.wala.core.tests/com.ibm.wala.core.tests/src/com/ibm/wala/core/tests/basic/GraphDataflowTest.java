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

import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.dataflow.graph.AbstractMeetOperator;
import com.ibm.wala.dataflow.graph.BitVectorFilter;
import com.ibm.wala.dataflow.graph.BitVectorFramework;
import com.ibm.wala.dataflow.graph.BitVectorIdentity;
import com.ibm.wala.dataflow.graph.BitVectorSolver;
import com.ibm.wala.dataflow.graph.BitVectorUnion;
import com.ibm.wala.dataflow.graph.BitVectorUnionConstant;
import com.ibm.wala.dataflow.graph.DataflowSolver;
import com.ibm.wala.dataflow.graph.ITransferFunctionProvider;
import com.ibm.wala.fixedpoint.impl.UnaryOperator;
import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * 
 * Simple Regression test for a graph-based dataflow problem
 * 
 * @author Donald P Pazel
 * @author sfink
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class GraphDataflowTest extends WalaTestCase {

  public static final String nodeNames = "ABCDEFGH";
  protected final static String[] nodes = new String[nodeNames.length()];

  public GraphDataflowTest() {
    super("GraphDataflowTest");
  }

  /**
   * A simple test of the GraphBitVectorDataflow system
   */
  public void testSolverNodeEdge() {
    Graph<String> G = buildGraph();
    String result = solveNodeEdge(G);
    System.err.println(result);
    if (!result.equals(expectedStringNodeEdge())) {
      System.err.println("Uh oh.");
      System.err.println(expectedStringNodeEdge());
    }
    assertEquals(expectedStringNodeEdge(), result);
  }

  public void testSolverNodeOnly() {
    Graph<String> G = buildGraph();
    String result = solveNodeOnly(G);
    System.err.println(result);
    assertEquals(expectedStringNodeOnly(), result);
  }

  /**
   * @return the expected dataflow result as a String
   */
  public static String expectedStringNodeOnly() {
    StringBuffer result = new StringBuffer("------\n");
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
    StringBuffer result = new StringBuffer("------\n");
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
  private static Graph<String> buildGraph() {
    Graph<String> G = new SlowSparseNumberedGraph<String>();
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
   */
  private String solveNodeOnly(Graph<String> G) {
    final OrdinalSetMapping<String> values = new MutableMapping<String>(nodes);
    ITransferFunctionProvider<String> functions = new ITransferFunctionProvider<String>() {

      public UnaryOperator getNodeTransferFunction(String node) {
        return new BitVectorUnionConstant(values.getMappedIndex(node));
      }

      public boolean hasNodeTransferFunctions() {
        return true;
      }

      public UnaryOperator getEdgeTransferFunction(String from, String to) {
        Assertions.UNREACHABLE();
        return null;
      }

      public boolean hasEdgeTransferFunctions() {
        return false;
      }

      public AbstractMeetOperator getMeetOperator() {
        return BitVectorUnion.instance();
      }

    };

    BitVectorFramework<String,String> F = new BitVectorFramework<String,String>(G, functions, values);
    DataflowSolver<String> s = new BitVectorSolver<String>(F);
    s.solve();
    return result2String(s);
  }

  private String solveNodeEdge(Graph<String> G) {
    final OrdinalSetMapping<String> values = new MutableMapping<String>(nodes);
    ITransferFunctionProvider<String> functions = new ITransferFunctionProvider<String>() {

      public UnaryOperator getNodeTransferFunction(String node) {
        return new BitVectorUnionConstant(values.getMappedIndex(node));
      }

      public boolean hasNodeTransferFunctions() {
        return true;
      }

      private BitVector zero() {
        BitVector b = new BitVector();
        b.set(0);
        return b;
      }

      private BitVector one() {
        BitVector b = new BitVector();
        b.set(1);
        return b;
      }

      public UnaryOperator getEdgeTransferFunction(String from, String to) {
        if (from == nodes[1] && to == nodes[3])
          return new BitVectorFilter(zero());
        else if (from == nodes[1] && to == nodes[2])
          return new BitVectorFilter(one());
        else {
          return BitVectorIdentity.instance();
        }
      }

      public boolean hasEdgeTransferFunctions() {
        return true;
      }

      public AbstractMeetOperator getMeetOperator() {
        return BitVectorUnion.instance();
      }

    };

    BitVectorFramework<String,String> F = new BitVectorFramework<String,String>(G, functions, values);
    DataflowSolver<String> s = new BitVectorSolver<String>(F);
    s.solve();
    return result2String(s);
  }

  public static String result2String(DataflowSolver<String> solver) {
    StringBuffer result = new StringBuffer("------\n");
    for (int i = 0; i < nodes.length; i++) {
      String n = nodes[i];
      BitVectorVariable varI = (BitVectorVariable) solver.getOut(n);
      String s = varI.toString();
      result.append("Node " + n + "(" + i + ") = " + s + "\n");
    }
    return result.toString();
  }
}