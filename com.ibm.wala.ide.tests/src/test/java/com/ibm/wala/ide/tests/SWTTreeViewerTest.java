package com.ibm.wala.ide.tests;

import com.ibm.wala.ide.ui.SWTTreeViewer;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.MapUtil;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.impl.BasicNodeManager;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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
    Graph<String> result =
        new AbstractGraph<String>() {

          final NodeManager<String> nodeManager = new BasicNodeManager<>();
          final EdgeManager<String> edgeManager =
              new EdgeManager<String>() {

                final Map<String, Set<String>> preds = HashMapFactory.make();

                final Map<String, Set<String>> succs = HashMapFactory.make();

                @Override
                public Iterator<String> getPredNodes(String n) {
                  return preds.get(n).iterator();
                }

                @Override
                public int getPredNodeCount(String n) {
                  return preds.get(n).size();
                }

                @Override
                public Iterator<String> getSuccNodes(String n) {
                  return succs.get(n).iterator();
                }

                @Override
                public int getSuccNodeCount(String N) {
                  return succs.get(N).size();
                }

                @Override
                public void addEdge(String src, String dst) {
                  MapUtil.findOrCreateSet(succs, src).add(dst);
                  MapUtil.findOrCreateSet(preds, dst).add(src);
                }

                @Override
                public void removeEdge(String src, String dst)
                    throws UnsupportedOperationException {
                  throw new UnsupportedOperationException();
                }

                @Override
                public void removeAllIncidentEdges(String node)
                    throws UnsupportedOperationException {
                  throw new UnsupportedOperationException();
                }

                @Override
                public void removeIncomingEdges(String node) throws UnsupportedOperationException {
                  throw new UnsupportedOperationException();
                }

                @Override
                public void removeOutgoingEdges(String node) throws UnsupportedOperationException {
                  throw new UnsupportedOperationException();
                }

                @Override
                public boolean hasEdge(String src, String dst) {
                  Set<String> succsForSrc = succs.get(src);
                  return succsForSrc != null && succsForSrc.contains(dst);
                }
              };

          @Override
          protected NodeManager<String> getNodeManager() {
            return nodeManager;
          }

          @Override
          protected EdgeManager<String> getEdgeManager() {

            return edgeManager;
          }
        };
    result.addNode(root);
    result.addNode(leaf);
    result.addEdge(root, leaf);
    return Pair.make(result, Collections.singleton(root));
  }
}
