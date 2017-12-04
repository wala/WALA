/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.pointers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.IntFunction;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.IntMapIterator;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * Basic implementation of {@link HeapGraph}
 */
public class BasicHeapGraph<T extends InstanceKey> extends HeapGraphImpl<T> {

  private final static boolean VERBOSE = false;

  private final static int VERBOSE_INTERVAL = 10000;

  private final static MutableSparseIntSetFactory factory = new MutableSparseIntSetFactory();
  
  /**
   * The backing graph
   */
  private final NumberedGraph<Object> G;

  /**
   * governing call graph
   */
  private final CallGraph callGraph;

  /**
   * @param P governing pointer analysis
   * @throws NullPointerException if P is null
   */
  public BasicHeapGraph(final PointerAnalysis<T> P, final CallGraph callGraph) throws NullPointerException {
    super(P);
    this.callGraph = callGraph;

    final OrdinalSetMapping<PointerKey> pointerKeys = getPointerKeys();
    final NumberedNodeManager<Object> nodeMgr = new NumberedNodeManager<Object>() {
      @Override
      public Iterator<Object> iterator() {
        return new CompoundIterator<>(pointerKeys.iterator(), P.getInstanceKeyMapping().iterator());
      }

      @Override
      public int getNumberOfNodes() {
        return pointerKeys.getSize() + P.getInstanceKeyMapping().getSize();
      }

      @Override
      public void addNode(Object n) {
        Assertions.UNREACHABLE();
      }

      @Override
      public void removeNode(Object n) {
        Assertions.UNREACHABLE();
      }

      @Override
      public int getNumber(Object N) {
        if (N instanceof PointerKey) {
          return pointerKeys.getMappedIndex(N);
        } else {
          if (!(N instanceof InstanceKey)) {
            Assertions.UNREACHABLE(N.getClass().toString());
          }
          int inumber = P.getInstanceKeyMapping().getMappedIndex(N);
          return (inumber == -1) ? -1 : inumber + pointerKeys.getMaximumIndex() + 1;
        }
      }

      @Override
      public Object getNode(int number) {
        if (number > pointerKeys.getMaximumIndex()) {
          return P.getInstanceKeyMapping().getMappedObject(number - pointerKeys.getSize());
        } else {
          return pointerKeys.getMappedObject(number);
        }
      }

      @Override
      public int getMaxNumber() {
        return getNumberOfNodes() - 1;
      }

      @Override
      public boolean containsNode(Object n) {
        return getNumber(n) != -1;
      }

      @Override
      public Iterator<Object> iterateNodes(IntSet s) {
        return new NumberedNodeIterator<>(s, this);
      }
    };

    final IBinaryNaturalRelation pred = computePredecessors(nodeMgr);
    final IntFunction<Object> toNode = nodeMgr::getNode;

    this.G = new AbstractNumberedGraph<Object>() {
      private final NumberedEdgeManager<Object> edgeMgr = new NumberedEdgeManager<Object>() {
        @Override
        public Iterator<Object> getPredNodes(Object N) {
          int n = nodeMgr.getNumber(N);
          IntSet p = pred.getRelated(n);
          if (p == null) {
            return EmptyIterator.instance();
          } else {
            return new IntMapIterator<>(p.intIterator(), toNode);
          }
        }

        @Override
        public IntSet getPredNodeNumbers(Object N) {
          int n = nodeMgr.getNumber(N);
          IntSet p = pred.getRelated(n);
          if (p != null) {
            return p;
          } else {
            return IntSetUtil.make();
          }
        }

        @Override
        public int getPredNodeCount(Object N) {
          int n = nodeMgr.getNumber(N);
          return pred.getRelatedCount(n);
        }

        @Override
        public Iterator<Object> getSuccNodes(Object N) {
          int[] succ = computeSuccNodeNumbers(N, nodeMgr);
          if (succ == null) {
            return EmptyIterator.instance();
          }
          SparseIntSet s = factory.make(succ);
          return new IntMapIterator<>(s.intIterator(), toNode);
        }

        @Override
        public IntSet getSuccNodeNumbers(Object N) {
          int[] succ = computeSuccNodeNumbers(N, nodeMgr);
          if (succ == null) {
            return IntSetUtil.make();
          } else {
            return IntSetUtil.make(succ);
          }
        }
        
        @Override
        public int getSuccNodeCount(Object N) {
          int[] succ = computeSuccNodeNumbers(N, nodeMgr);
          return succ == null ? 0 : succ.length;
        }

        @Override
        public void addEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
        }

        @Override
        public void removeEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
        }

        @Override
        public void removeAllIncidentEdges(Object node) {
          Assertions.UNREACHABLE();
        }

        @Override
        public void removeIncomingEdges(Object node) {
          Assertions.UNREACHABLE();
        }

        @Override
        public void removeOutgoingEdges(Object node) {
          Assertions.UNREACHABLE();
        }

        @Override
        public boolean hasEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
          return false;
        }
      };

      @Override
      protected NumberedNodeManager<Object> getNodeManager() {
        return nodeMgr;
      }

      @Override
      protected NumberedEdgeManager<Object> getEdgeManager() {
        return edgeMgr;
      }
    };
  }

  private OrdinalSetMapping<PointerKey> getPointerKeys() {
    MutableMapping<PointerKey> result = MutableMapping.make();

    for (PointerKey p : getPointerAnalysis().getPointerKeys()) {
      result.add(p);
    }
    return result;

  }

  private int[] computeSuccNodeNumbers(Object N, NumberedNodeManager<Object> nodeManager) {
    if (N instanceof PointerKey) {
      PointerKey P = (PointerKey) N;
      OrdinalSet<T> S = getPointerAnalysis().getPointsToSet(P);
      int[] result = new int[S.size()];
      int i = 0;
      for (T t : S) {
        result[i] = nodeManager.getNumber(t);
        i++;
      }
      return result;
    } else if (N instanceof InstanceKey) {
      InstanceKey I = (InstanceKey) N;
      TypeReference T = I.getConcreteType().getReference();

      assert T != null : "null concrete type from " + I.getClass();
      if (T.isArrayType()) {
        PointerKey p = getHeapModel().getPointerKeyForArrayContents(I);
        if (p == null || !nodeManager.containsNode(p)) {
          return null;
        } else {
          return new int[] { nodeManager.getNumber(p) };
        }
      } else {
        IClass klass = getHeapModel().getClassHierarchy().lookupClass(T);
        assert klass != null : "null klass for type " + T;
        MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
        for (IField f : klass.getAllInstanceFields()) {
          if (!f.getReference().getFieldType().isPrimitiveType()) {
            PointerKey p = getHeapModel().getPointerKeyForInstanceField(I, f);
            if (p != null && nodeManager.containsNode(p)) {
              result.add(nodeManager.getNumber(p));
            }
          }
        }
        return result.toIntArray();
      }
    } else {
      Assertions.UNREACHABLE("Unexpected type: " + N.getClass());
      return null;
    }
  }

  /**
   * @return R, y \in R(x,y) if the node y is a predecessor of node x
   */
  private IBinaryNaturalRelation computePredecessors(NumberedNodeManager<Object> nodeManager) {
    BasicNaturalRelation R = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.SIMPLE }, BasicNaturalRelation.SIMPLE);

    // we split the following loops to improve temporal locality,
    // particularly for locals
    computePredecessorsForNonLocals(nodeManager, R);
    computePredecessorsForLocals(nodeManager, R);

    return R;
  }

  private void computePredecessorsForNonLocals(NumberedNodeManager<Object> nodeManager, BasicNaturalRelation R) {
    // Note: we run this loop backwards on purpose, to avoid lots of resizing of
    // bitvectors
    // in the backing relation. i.e., we will add the biggest bits first.
    // pretty damn tricky.
    for (int i = nodeManager.getMaxNumber(); i >= 0; i--) {
      if (VERBOSE) {
        if (i % VERBOSE_INTERVAL == 0) {
          System.err.println("Building HeapGraph: " + i);
        }
      }
      Object n = nodeManager.getNode(i);
      if (!(n instanceof LocalPointerKey)) {
        int[] succ = computeSuccNodeNumbers(n, nodeManager);
        if (succ != null) {
          for (int j : succ) {
            R.add(j, i);
          }
        }
      }
    }
  }

  /**
   * traverse locals in order, first by node, then by value number: attempt to improve locality
   */
  private void computePredecessorsForLocals(NumberedNodeManager<Object> nodeManager, BasicNaturalRelation R) {

    ArrayList<LocalPointerKey> list = new ArrayList<>();
    for (Object n : nodeManager) {
      if (n instanceof LocalPointerKey) {
        list.add((LocalPointerKey) n);
      }
    }
    Object[] arr = list.toArray();
    Arrays.sort(arr, new LocalPointerComparator());

    for (int i = 0; i < arr.length; i++) {
      if (VERBOSE) {
        if (i % VERBOSE_INTERVAL == 0) {
          System.err.println("Building HeapGraph: " + i + " of " + arr.length);
        }
      }
      LocalPointerKey n = (LocalPointerKey) arr[i];
      int num = nodeManager.getNumber(n);
      int[] succ = computeSuccNodeNumbers(n, nodeManager);
      if (succ != null) {
        for (int j : succ) {
          R.add(j, num);
        }
      }
    }
  }

  /**
   * sorts local pointers by node, then value number
   */
  private final class LocalPointerComparator implements Comparator<Object> {
    @Override
    public int compare(Object arg1, Object arg2) {
      LocalPointerKey o1 = (LocalPointerKey) arg1;
      LocalPointerKey o2 = (LocalPointerKey) arg2;
      if (o1.getNode().equals(o2.getNode())) {
        return o1.getValueNumber() - o2.getValueNumber();
      } else {
        return callGraph.getNumber(o1.getNode()) - callGraph.getNumber(o2.getNode());
      }
    }
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNumber(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getNumber(Object N) {
    return G.getNumber(N);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNode(int)
   */
  @Override
  public Object getNode(int number) {
    return G.getNode(number);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getMaxNumber()
   */
  @Override
  public int getMaxNumber() {
    return G.getMaxNumber();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  @Override
  public Iterator<Object> iterator() {
    return G.iterator();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  @Override
  public int getNumberOfNodes() {
    return G.getNumberOfNodes();
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  @Override
  public Iterator<Object> getPredNodes(Object N) {
    return G.getPredNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getPredNodeCount(Object N) {
    return G.getPredNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  @Override
  public Iterator<Object> getSuccNodes(Object N) {
    return G.getSuccNodes(N);
  }

  /*
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getSuccNodeCount(Object N) {
    return G.getSuccNodeCount(N);
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#addNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void addNode(Object n) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNode(Object n) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  @Override
  public void addEdge(Object from, Object to) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeEdge(Object from, Object to) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  @Override
  public boolean hasEdge(Object from, Object to) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  @Override
  public void removeAllIncidentEdges(Object node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public boolean containsNode(Object N) {
    return G.containsNode(N);
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("Nodes:\n");
    for (int i = 0; i <= getMaxNumber(); i++) {
      Object node = getNode(i);
      if (node != null) {
        result.append(i).append("  ").append(node).append("\n");
      }
    }
    result.append("Edges:\n");
    for (int i = 0; i <= getMaxNumber(); i++) {
      Object node = getNode(i);
      if (node != null) {
        result.append(i).append(" -> ");
        for (Object s : Iterator2Iterable.make(getSuccNodes(node))) {
          result.append(getNumber(s)).append(" ");
        }
        result.append("\n");
      }
    }

    return result.toString();
  }

  @Override
  public void removeIncomingEdges(Object node) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  @Override
  public void removeOutgoingEdges(Object node) throws UnimplementedError {
    Assertions.UNREACHABLE();
  }

  @Override
  public IntSet getSuccNodeNumbers(Object node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public IntSet getPredNodeNumbers(Object node) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }
}
