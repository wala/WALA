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

import static com.ibm.wala.util.graph.NodeManagerConditions.node;
import static org.assertj.core.api.Assertions.allOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatObject;

import com.ibm.wala.util.graph.impl.BasicGraph;
import org.junit.jupiter.api.Test;

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
    assertThatObject(graph).has(allOf(node(root), node(leaf)));
    assertThat(graph.getPredNodes(leaf)).toIterable().contains(root);
    assertThat(graph.getSuccNodes(root)).toIterable().contains(leaf);
  }
}
