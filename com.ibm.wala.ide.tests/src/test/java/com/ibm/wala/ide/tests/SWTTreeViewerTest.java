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
package com.ibm.wala.ide.tests;

import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.impl.BasicGraph;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;

public class SWTTreeViewerTest {

  @Test
  public void testJustOpen() throws WalaException {
    Pair<Graph<String>, Collection<String>> testGraphAndRoots = makeTestGraphAndRoots();
    final SWTTreeViewer<String> v = new SWTTreeViewer<>();
    v.setGraphInput(testGraphAndRoots.fst);
    v.setRootsInput(testGraphAndRoots.snd);
    v.justOpenForTest();
  }

  private static Pair<Graph<String>, Collection<String>> makeTestGraphAndRoots() {
    String root = "root";
    String leaf = "leaf";
    Graph<String> result = new BasicGraph<>();
    result.addNode(root);
    result.addNode(leaf);
    result.addEdge(root, leaf);
    return Pair.make(result, Collections.singleton(root));
  }
}
