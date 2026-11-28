package com.ibm.wala.util.graph.impl;

import static com.google.common.base.Preconditions.checkArgument;

import com.ibm.wala.util.graph.EdgeManager;
import java.util.Iterator;
import org.jspecify.annotations.Nullable;

public class SelfLoopAddedEdgeManager<T> implements EdgeManager<T> {
  private class PrependIterator implements Iterator<T> {
    private final Iterator<T> original;
    private @Nullable T first;

    private PrependIterator(Iterator<T> original, T first) {
      this.original = original;
      this.first = first;
    }

    @Override
    public boolean hasNext() {
      return first != null || original.hasNext();
    }

    @Override
    public T next() {
      T next = first;
      if (next != null) {
        first = null;
        return next;
      }
      return original.next();
    }

    @Override
    public void remove() {
      assert false;
    }
  }

  private final EdgeManager<T> original;

  public SelfLoopAddedEdgeManager(EdgeManager<T> original) {
    checkArgument(original != null, "original is null");
    this.original = original;
  }

  @Override
  public Iterator<T> getPredNodes(T n) {
    if (original.hasEdge(n, n)) {
      return original.getPredNodes(n);
    } else {
      return new PrependIterator(original.getPredNodes(n), n);
    }
  }

  @Override
  public int getPredNodeCount(T n) {
    if (original.hasEdge(n, n)) {
      return original.getPredNodeCount(n);
    } else {
      return original.getPredNodeCount(n) + 1;
    }
  }

  @Override
  public Iterator<T> getSuccNodes(T n) {
    if (original.hasEdge(n, n)) {
      return original.getSuccNodes(n);
    } else {
      return new PrependIterator(original.getSuccNodes(n), n);
    }
  }

  @Override
  public int getSuccNodeCount(T n) {
    if (original.hasEdge(n, n)) {
      return original.getSuccNodeCount(n);
    } else {
      return original.getSuccNodeCount(n) + 1;
    }
  }

  @Override
  public void addEdge(T src, T dst) {
    original.addEdge(src, dst);
  }

  @Override
  public void removeEdge(T src, T dst) throws UnsupportedOperationException {
    original.removeEdge(src, dst);
  }

  @Override
  public void removeAllIncidentEdges(T node) throws UnsupportedOperationException {
    original.removeAllIncidentEdges(node);
  }

  @Override
  public void removeIncomingEdges(T node) throws UnsupportedOperationException {
    original.removeIncomingEdges(node);
  }

  @Override
  public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
    original.removeOutgoingEdges(node);
  }

  /** Returns {@code true} for equal non-null endpoints and delegates other lookups. */
  @Override
  public boolean hasEdge(@Nullable T src, @Nullable T dst) {
    return (src != null && src.equals(dst)) || original.hasEdge(src, dst);
  }
}
