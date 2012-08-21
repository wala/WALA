package com.ibm.wala.util.graph.traverse;

import java.util.Iterator;
import java.util.List;

import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;

/**
 * Extends {@link DFSPathFinder} to discover all paths from a set of root nodes
 * to nodes passing some {@link Filter}.
 * 
 * Note that this code performs work that is potentially exponential in the size
 * of the underlying graph, using exponential space. It most likely won't work
 * even for graphs of moderate size.
 */
public class DFSAllPathsFinder<T> extends DFSPathFinder<T> {

  public DFSAllPathsFinder(Graph<T> G, Iterator<T> nodes, Filter<T> f) {
    super(G, nodes, f);
  }

  public DFSAllPathsFinder(Graph<T> G, T N, Filter<T> f) throws IllegalArgumentException {
    super(G, N, f);
  }

  @Override
  protected Iterator<? extends T> getConnected(T n) {
    final List<T> cp = currentPath();
    return new FilterIterator<T>(G.getSuccNodes(n), new Filter<T>() {
      @Override
      public boolean accepts(T o) {
        return ! cp.contains(o);
      }
    });
  }

  @Override
  protected Iterator<? extends T> getPendingChildren(T n) {
    Pair<List<T>,T> key = Pair.make(currentPath(), n);
    return pendingChildren.get(key);
  }

  @Override
  protected void setPendingChildren(T v, Iterator<? extends T> iterator) {
    Pair<List<T>,T> key = Pair.make(currentPath(), v);
    pendingChildren.put(key, iterator);
  }

  
}
