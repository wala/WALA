package com.ibm.wala.cast.ir.toSource;

import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.ReverseIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.graph.traverse.Topological;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

public class IRToCAst {

  private static <T> Map<T, Integer> computeFinishTimes(T entry, Graph<T> ipcfg) {
    int dfsNumber = 0;
    Map<T, Integer> dfsFinish = HashMapFactory.make();
    Iterator<T> search = DFS.iterateFinishTime(ipcfg, Collections.singleton(entry).iterator());
    while (search.hasNext()) {
      T n = search.next();
      assert !dfsFinish.containsKey(n) : n;
      dfsFinish.put(n, dfsNumber++);
    }
    return dfsFinish;
  }

  private static <T> Map<T, Integer> computeStartTimes(T entry, Graph<T> ipcfg) {
    int reverseDfsNumber = 0;
    Map<T, Integer> dfsStart = HashMapFactory.make();
    Iterator<T> reverseSearch = DFS.iterateDiscoverTime(ipcfg, entry);
    while (reverseSearch.hasNext()) {
      dfsStart.put(reverseSearch.next(), reverseDfsNumber++);
    }
    return dfsStart;
  }

  public static Map<ISSABasicBlock, Set<ISSABasicBlock>> toCAst(IR ir, boolean isDebug) {
    SSACFG cfg = ir.getControlFlowGraph();

    Map<ISSABasicBlock, Integer> dfsFinish = computeFinishTimes(cfg.entry(), cfg);
    Map<ISSABasicBlock, Integer> dfsStart = computeStartTimes(cfg.entry(), cfg);

    BiPredicate<ISSABasicBlock, ISSABasicBlock> isForwardEdge =
        (p, s) -> dfsFinish.get(s) >= dfsFinish.get(p) && dfsStart.get(p) <= dfsStart.get(s);

    Graph<ISSABasicBlock> forward = GraphSlicer.prune(cfg, isForwardEdge);

    Graph<ISSABasicBlock> ordered =
        new AbstractGraph<>() {
          @Override
          protected NodeManager<ISSABasicBlock> getNodeManager() {
            return forward;
          }

          @Override
          protected EdgeManager<ISSABasicBlock> getEdgeManager() {
            return new EdgeManager<>() {

              @Override
              public Iterator<ISSABasicBlock> getPredNodes(ISSABasicBlock n) {
                return forward.getPredNodes(n);
              }

              @Override
              public int getPredNodeCount(ISSABasicBlock n) {
                return forward.getPredNodeCount(n);
              }

              @Override
              public Iterator<ISSABasicBlock> getSuccNodes(ISSABasicBlock n) {
                Iterator<ISSABasicBlock> ss = forward.getSuccNodes(n);
                return new CompoundIterator<>(
                    new FilterIterator<>(ss, s -> getPredNodeCount(s) == 1),
                    new FilterIterator<>(ss, s -> getPredNodeCount(s) > 1));
              }

              @Override
              public int getSuccNodeCount(ISSABasicBlock n) {
                return forward.getSuccNodeCount(n);
              }

              @Override
              public void addEdge(ISSABasicBlock src, ISSABasicBlock dst) {
                Assertions.UNREACHABLE();
              }

              @Override
              public void removeEdge(ISSABasicBlock src, ISSABasicBlock dst)
                  throws UnsupportedOperationException {
                Assertions.UNREACHABLE();
              }

              @Override
              public void removeAllIncidentEdges(ISSABasicBlock node)
                  throws UnsupportedOperationException {
                Assertions.UNREACHABLE();
              }

              @Override
              public void removeIncomingEdges(ISSABasicBlock node)
                  throws UnsupportedOperationException {
                Assertions.UNREACHABLE();
              }

              @Override
              public void removeOutgoingEdges(ISSABasicBlock node)
                  throws UnsupportedOperationException {
                Assertions.UNREACHABLE();
              }

              @Override
              public boolean hasEdge(ISSABasicBlock src, ISSABasicBlock dst) {
                return forward.hasEdge(src, dst);
              }
            };
          }
        };

    Iterable<ISSABasicBlock> top = Topological.makeTopologicalIter(ordered);
    Map<ISSABasicBlock, Set<ISSABasicBlock>> groups = HashMapFactory.make();
    Set<ISSABasicBlock> group = HashSetFactory.make();
    boolean fresh = true;
    for (Iterator<ISSABasicBlock> bbs = ReverseIterator.reverse(top.iterator()); bbs.hasNext(); ) {
      ISSABasicBlock bb = bbs.next();
      if (isDebug) System.err.println("looking at " + bb);
      if (fresh || cfg.getPredNodeCount(bb) == 1) {
        fresh = false;
      } else {
        fresh = true;
        group = HashSetFactory.make();
      }
      if (!bb.isEntryBlock() && !bb.isExitBlock()) {
        group.add(bb);
        groups.put(bb, group);
      }
    }

    return groups;
  }
}
