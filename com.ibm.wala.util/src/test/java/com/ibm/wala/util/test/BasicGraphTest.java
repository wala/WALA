/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manu Sridharan - initial API and implementation
 */
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
