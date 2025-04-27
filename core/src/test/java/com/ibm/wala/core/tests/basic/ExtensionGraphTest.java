/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.core.tests.basic;

import static org.assertj.core.api.Assertions.assertThat;

import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.ExtensionGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.SCCIterator;
import org.junit.jupiter.api.Test;

public class ExtensionGraphTest {

  private static NumberedGraph<String> makeBaseGraph() {
    NumberedGraph<String> base = SlowSparseNumberedGraph.make();

    base.addNode("A");
    base.addNode("B");
    base.addNode("C");
    base.addNode("D");
    base.addNode("E");
    base.addNode("F");
    base.addNode("G");
    base.addNode("H");

    base.addEdge("A", "B");
    base.addEdge("B", "C");
    base.addEdge("A", "D");
    base.addEdge("D", "E");
    base.addEdge("A", "F");
    base.addEdge("F", "G");
    base.addEdge("C", "H");
    base.addEdge("E", "H");
    base.addEdge("G", "H");

    return base;
  }

  private static void augmentA(NumberedGraph<String> base) {
    base.addEdge("C", "B");
    base.addEdge("E", "D");
    base.addEdge("G", "F");
  }

  private static void augmentB(NumberedGraph<String> base) {
    base.addNode("I");
    base.addNode("J");

    base.addEdge("I", "J");
    base.addEdge("A", "I");
    base.addEdge("H", "J");
  }

  private static void augmentC(NumberedGraph<String> base) {
    base.addEdge("H", "A");
  }

  @Test
  public void testAugment() {
    NumberedGraph<String> base = makeBaseGraph();
    assertThat(IteratorUtil.count(new SCCIterator<>(base)))
        .withFailMessage("base has 8 SCCs")
        .isEqualTo(8);

    NumberedGraph<String> x = new ExtensionGraph<>(base);
    augmentA(x);
    assertThat(IteratorUtil.count(new SCCIterator<>(x)))
        .withFailMessage("base+A has 5 SCCs")
        .isEqualTo(5);
    assertThat(IteratorUtil.count(new SCCIterator<>(base)))
        .withFailMessage("base has 8 SCCs")
        .isEqualTo(8);

    NumberedGraph<String> y = new ExtensionGraph<>(x);
    augmentB(y);
    assertThat(IteratorUtil.count(new SCCIterator<>(y)))
        .withFailMessage("base+A+B has 7 SCCs")
        .isEqualTo(7);
    assertThat(IteratorUtil.count(new SCCIterator<>(x)))
        .withFailMessage("base+A has 5 SCCs")
        .isEqualTo(5);
    assertThat(IteratorUtil.count(new SCCIterator<>(base)))
        .withFailMessage("base has 8 SCCs")
        .isEqualTo(8);

    NumberedGraph<String> z = new ExtensionGraph<>(y);
    augmentC(z);
    assertThat(IteratorUtil.count(new SCCIterator<>(z)))
        .withFailMessage("base+A+B+C has 3 SCCs")
        .isEqualTo(3);
    assertThat(IteratorUtil.count(new SCCIterator<>(y)))
        .withFailMessage("base+A+B has 7 SCCs")
        .isEqualTo(7);
    assertThat(IteratorUtil.count(new SCCIterator<>(x)))
        .withFailMessage("base+A has 5 SCCs")
        .isEqualTo(5);
    assertThat(IteratorUtil.count(new SCCIterator<>(base)))
        .withFailMessage("base has 8 SCCs")
        .isEqualTo(8);
  }
}
