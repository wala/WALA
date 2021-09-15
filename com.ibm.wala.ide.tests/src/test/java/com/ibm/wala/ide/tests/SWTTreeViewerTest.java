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
    Pair<Graph<String>, Collection<String>> testGraphAndRoots = makeTestGraphAndRoot();
    final SWTTreeViewer<String> v = new SWTTreeViewer<>();
    v.setGraphInput(testGraphAndRoots.fst);
    v.setRootsInput(testGraphAndRoots.snd);
    v.justOpenForTest();
  }

  private static Pair<Graph<String>, Collection<String>> makeTestGraphAndRoot() {
    String root = "root";
    String leaf = "leaf";
    Graph<String> result = new BasicGraph<>();
    result.addNode(root);
    result.addNode(leaf);
    result.addEdge(root, leaf);
    return Pair.make(result, Collections.singleton(root));
  }
}
