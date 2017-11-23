/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.util.graph.impl;

import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

public class ExtensionGraph<T> implements NumberedGraph<T> {
  private final NumberedGraph<T> original;
  private final NumberedNodeManager<T> additionalNodes = new SlowNumberedNodeManager<>();
  private final NumberedEdgeManager<T> edgeManager = new NumberedEdgeManager<T>() {
    private final Map<T, MutableIntSet> inEdges = HashMapFactory.make();
    private final Map<T, MutableIntSet> outEdges = HashMapFactory.make();

    private Iterator<T> nodes(final T node, final Map<T,? extends IntSet> extra) {
      if (extra.containsKey(node)) {
        return new Iterator<T>() {
          private final IntIterator i = extra.get(node).intIterator();
          @Override
          public boolean hasNext() {
            return i.hasNext();
          }

          @Override
          public T next() {
            return ExtensionGraph.this.getNode(i.next());
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      } else {
        return EmptyIterator.instance();
      }
    }
    
    @Override
    public Iterator<T> getPredNodes(T n) {
      Iterator<T> orig = (original.containsNode(n)? original.getPredNodes(n): EmptyIterator.<T>instance());
      return new CompoundIterator<>(orig, nodes(n, inEdges));
    }

    @Override
    public int getPredNodeCount(T n) {
      return
          (original.containsNode(n)? original.getPredNodeCount(n): 0) +
          (inEdges.containsKey(n)? inEdges.get(n).size(): 0);
    }

    @Override
    public Iterator<T> getSuccNodes(T n) {
      Iterator<T> orig = (original.containsNode(n)? original.getSuccNodes(n): EmptyIterator.<T>instance());
      return new CompoundIterator<>(orig, nodes(n, outEdges));
    }

    @Override
    public int getSuccNodeCount(T n) {
      return
          (original.containsNode(n)? original.getSuccNodeCount(n): 0) +
          (outEdges.containsKey(n)? outEdges.get(n).size(): 0);
    }

    @Override
    public void addEdge(T src, T dst) {
      assert !original.hasEdge(src, dst);
      assert containsNode(src) && containsNode(dst);
      if (! inEdges.containsKey(dst)) { inEdges.put(dst, IntSetUtil.make()); }
      inEdges.get(dst).add(getNumber(src));
      if (! outEdges.containsKey(src)) { outEdges.put(src, IntSetUtil.make()); }
      outEdges.get(src).add(getNumber(dst));
    }

    @Override
    public void removeEdge(T src, T dst) throws UnsupportedOperationException {
      assert hasEdge(src, dst);
      assert !original.hasEdge(src, dst);
      assert containsNode(src) && containsNode(dst);
      inEdges.get(dst).remove(getNumber(src));
      outEdges.get(src).remove(getNumber(dst));
    }

    @Override
    public void removeAllIncidentEdges(T node) throws UnsupportedOperationException {
      removeIncomingEdges(node);
      removeOutgoingEdges(node);
    }

    @Override
    public void removeIncomingEdges(T node) throws UnsupportedOperationException {
      assert !original.containsNode(node) || original.getPredNodeCount(node) == 0;
      inEdges.remove(node);
    }

    @Override
    public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
      assert !original.containsNode(node) || original.getSuccNodeCount(node) == 0;
      outEdges.remove(node);
    }

    @Override
    public boolean hasEdge(T src, T dst) {
      return original.hasEdge(src, dst) || 
          (outEdges.containsKey(src) && outEdges.get(src).contains(getNumber(dst)));
    }

    @Override
    public IntSet getSuccNodeNumbers(T node) {
      if (original.containsNode(node)) {
        if (outEdges.containsKey(node)) {
          MutableIntSet x = IntSetUtil.makeMutableCopy(original.getSuccNodeNumbers(node));
          x.addAll(outEdges.get(node));
          return x;
        } else {
          return original.getSuccNodeNumbers(node);
        }
      } else {
        if (outEdges.containsKey(node)) {
          return outEdges.get(node);
        } else {
          return EmptyIntSet.instance;
        }
      }
    }

    @Override
    public IntSet getPredNodeNumbers(T node) {
      if (original.containsNode(node)) {
        if (inEdges.containsKey(node)) {
          MutableIntSet x = IntSetUtil.makeMutableCopy(original.getPredNodeNumbers(node));
          x.addAll(inEdges.get(node));
          return x;
        } else {
          return original.getPredNodeNumbers(node);
        }
      } else {
        if (inEdges.containsKey(node)) {
          return inEdges.get(node);
        } else {
          return EmptyIntSet.instance;
        }
      }
    }
    
  };
  
  public ExtensionGraph(NumberedGraph<T> original) {
    this.original = original;
  }

  @Override
  public Iterator<T> iterator() {
    return new CompoundIterator<>(original.iterator(), additionalNodes.iterator());
  }

  @Override
  public int getNumberOfNodes() {
    return original.getNumberOfNodes() + additionalNodes.getNumberOfNodes();
  }

  @Override
  public void addNode(T n) {
    assert !original.containsNode(n);
    additionalNodes.addNode(n);
  }

  @Override
  public void removeNode(T n) throws UnsupportedOperationException {
    assert !original.containsNode(n);
    additionalNodes.removeNode(n);
  }

  @Override
  public boolean containsNode(T n) {
    return original.containsNode(n) || additionalNodes.containsNode(n);
  }

  @Override
  public int getNumber(T N) {
    if (original.containsNode(N)) {
      return original.getNumber(N);
    } else {
      return additionalNodes.getNumber(N) + original.getMaxNumber() + 1;
    }
  }

  @Override
  public T getNode(int number) {
    if (number <= original.getMaxNumber()) {
      return original.getNode(number);
    } else {
      return additionalNodes.getNode(number - original.getMaxNumber() - 1);
    }
  }

  @Override
  public int getMaxNumber() {
    if (additionalNodes.iterator().hasNext()) {
      return original.getMaxNumber() + 1 + additionalNodes.getMaxNumber();
    } else {
      return original.getMaxNumber();
    }
  }

  @Override
  public Iterator<T> iterateNodes(IntSet s) {
    final MutableIntSet os = IntSetUtil.make();
    final MutableIntSet es = IntSetUtil.make();
    s.foreach(x -> {
      if (x <= original.getMaxNumber()) {
        os.add(x);
      } else {
        es.add(x - original.getMaxNumber() - 1);
      }
    });
    return new CompoundIterator<>(original.iterateNodes(os), additionalNodes.iterateNodes(es));
  }

  @Override
  public Iterator<T> getPredNodes(T n) {
    return edgeManager.getPredNodes(n);
  }

  @Override
  public int getPredNodeCount(T n) {
    return edgeManager.getPredNodeCount(n);
  }

  @Override
  public IntSet getPredNodeNumbers(T node) {
    return edgeManager.getPredNodeNumbers(node);
  }

  @Override
  public Iterator<T> getSuccNodes(T n) {
    return edgeManager.getSuccNodes(n);
  }

  @Override
  public int getSuccNodeCount(T N) {
    return edgeManager.getSuccNodeCount(N);
  }

  @Override
  public IntSet getSuccNodeNumbers(T node) {
    return edgeManager.getSuccNodeNumbers(node);
  }

  @Override
  public void addEdge(T src, T dst) {
    assert !original.hasEdge(src, dst);
    edgeManager.addEdge(src, dst);
  }

  @Override
  public void removeEdge(T src, T dst) throws UnsupportedOperationException {
    assert !original.hasEdge(src, dst);
    edgeManager.removeEdge(src, dst);
  }

  @Override
  public void removeAllIncidentEdges(T node) throws UnsupportedOperationException {
    assert !original.containsNode(node);
    edgeManager.removeAllIncidentEdges(node);
   }

  @Override
  public void removeIncomingEdges(T node) throws UnsupportedOperationException {
    assert !original.containsNode(node);
    edgeManager.removeIncomingEdges(node);
  }

  @Override
  public void removeOutgoingEdges(T node) throws UnsupportedOperationException {
    assert !original.containsNode(node);
    edgeManager.removeOutgoingEdges(node);
  }

  @Override
  public boolean hasEdge(T src, T dst) {
    return edgeManager.hasEdge(src, dst);
  }

  @Override
  public void removeNodeAndEdges(T n) throws UnsupportedOperationException {
    assert !original.containsNode(n);
    edgeManager.removeAllIncidentEdges(n);
    additionalNodes.removeNode(n);
  }

}
