package com.ibm.wala.util.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.graph.impl.BasicGraph;
import org.junit.Assert;
import org.junit.Test;

/** Tests for {@link com.ibm.wala.util.graph.impl.BasicGraph}. */
public class BasicGraphTest {

  @Test
  public void testSingleEdge() {
    BasicGraph<String> graph = new BasicGraph<>();
    String root = "root";
    String leaf = "leaf";
    graph.addNode(root);
    graph.addNode(leaf);
    graph.addEdge(root, leaf);
    Assert.assertTrue(graph.containsNode(root));
    Assert.assertTrue(graph.containsNode(leaf));
    assertThat(Iterator2Collection.toList(graph.getPredNodes(leaf)), contains(root));
    assertThat(Iterator2Collection.toList(graph.getSuccNodes(root)), contains(leaf));
  }
}
