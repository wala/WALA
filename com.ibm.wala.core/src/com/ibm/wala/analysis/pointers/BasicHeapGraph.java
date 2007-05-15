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

import com.ibm.wala.analysis.reflection.Malleable;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CompoundIterator;
import com.ibm.wala.util.IntFunction;
import com.ibm.wala.util.IntMapIterator;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.AbstractNumberedGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.graph.impl.NumberedNodeIterator;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * @author sfink
 */
public class BasicHeapGraph extends HeapGraph {

  private final static boolean VERBOSE = false;

  private final static int VERBOSE_INTERVAL = 10000;

  /**
   * Pointer analysis solution
   */
  private final PointerAnalysis pointerAnalysis;

  /**
   * The backing graph
   */
  private final NumberedGraph<Object> G;

  /**
   * governing call graph
   */
  private final CallGraph callGraph;

  /**
   * @param P
   *          governing pointer analysis
   * @throws NullPointerException  if P is null
   */
  public BasicHeapGraph(final PointerAnalysis P, final CallGraph callGraph) throws NullPointerException {
    super(P.getHeapModel());
    this.pointerAnalysis = P;
    this.callGraph = callGraph;

    final OrdinalSetMapping<PointerKey> pointerKeys = getPointerKeys(P);
    final NumberedNodeManager<Object> nodeMgr = new NumberedNodeManager<Object>() {
      public Iterator<Object> iterator() {
        return new CompoundIterator<Object>(pointerKeys.iterator(), P.getInstanceKeyMapping().iterator());
      }

      public int getNumberOfNodes() {
        return pointerKeys.getMappingSize() + P.getInstanceKeyMapping().getMappingSize();
      }

      public void addNode(Object n) {
        Assertions.UNREACHABLE();
      }

      public void removeNode(Object n) {
        Assertions.UNREACHABLE();
      }

      public int getNumber(Object N) {
        if (N instanceof PointerKey) {
          return pointerKeys.getMappedIndex((PointerKey) N);
        } else {
          if (Assertions.verifyAssertions) {
            if (!(N instanceof InstanceKey)) {
              Assertions.UNREACHABLE(N.getClass().toString());
            }
          }
          int inumber = P.getInstanceKeyMapping().getMappedIndex((InstanceKey) N);
          return (inumber == -1) ? -1 : inumber + pointerKeys.getMappingSize();
        }
      }

      public Object getNode(int number) {
        if (number >= pointerKeys.getMappingSize()) {
          return P.getInstanceKeyMapping().getMappedObject(number - pointerKeys.getMappingSize());
        } else {
          return pointerKeys.getMappedObject(number);
        }
      }

      public int getMaxNumber() {
        return getNumberOfNodes() - 1;
      }

      public boolean containsNode(Object n) {
        return getNumber(n) != -1;
      }

      public Iterator<Object> iterateNodes(IntSet s) {
        return new NumberedNodeIterator<Object>(s, this);
      }
    };

    final IBinaryNaturalRelation pred = computePredecessors(nodeMgr);
    final IntFunction<Object> toNode = new IntFunction<Object>() {
      public Object apply(int i) {
        return nodeMgr.getNode(i);
      }
    };

    this.G = new AbstractNumberedGraph<Object>() {
      private final EdgeManager<Object> edgeMgr = new EdgeManager<Object>() {
        public Iterator<Object> getPredNodes(Object N) {
          int n = nodeMgr.getNumber(N);
          IntSet p = pred.getRelated(n);
          if (p == null) {
            return EmptyIterator.instance();
          } else {
            return new IntMapIterator<Object>(p.intIterator(), toNode);
          }
        }

        public int getPredNodeCount(Object N) {
          int n = nodeMgr.getNumber(N);
          return pred.getRelatedCount(n);
        }

        public Iterator<? extends Object> getSuccNodes(Object N) {
          int[] succ = computeSuccNodeNumbers(N, nodeMgr);
          if (succ == null) {
            return EmptyIterator.instance();
          }
          SparseIntSet s = new MutableSparseIntSet(succ);
          return new IntMapIterator<Object>(s.intIterator(), toNode);
        }

        public int getSuccNodeCount(Object N) {
          int[] succ = computeSuccNodeNumbers(N, nodeMgr);
          return succ == null ? 0 : succ.length;
        }

        public void addEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
        }
        
        public void removeEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
        }

        public void removeAllIncidentEdges(Object node) {
          Assertions.UNREACHABLE();
        }

        public void removeIncomingEdges(Object node) {
          Assertions.UNREACHABLE();
        }

        public void removeOutgoingEdges(Object node) {
          Assertions.UNREACHABLE();
        }

        public boolean hasEdge(Object src, Object dst) {
          Assertions.UNREACHABLE();
          return false;
        }
      };

      protected NodeManager<Object> getNodeManager() {
        return nodeMgr;
      }

      protected EdgeManager<Object> getEdgeManager() {
        return edgeMgr;
      }
    };
  }

  /**
   * 
   */
  private OrdinalSetMapping<PointerKey> getPointerKeys(PointerAnalysis pointerAnalysis) {
    MutableMapping<PointerKey> result = new MutableMapping<PointerKey>();

    for (Iterator it = pointerAnalysis.getPointerKeys().iterator(); it.hasNext();) {
      PointerKey p = (PointerKey) it.next();
      result.add(p);
    }
    return result;

  }

  private int[] computeSuccNodeNumbers(Object N, NumberedNodeManager<Object> nodeManager) {
    if (N instanceof PointerKey) {
      PointerKey P = (PointerKey) N;
      OrdinalSet S = pointerAnalysis.getPointsToSet(P);
      int[] result = new int[S.size()];
      int i = 0;
      for (Iterator it = S.iterator(); it.hasNext();) {
        result[i] = nodeManager.getNumber(it.next());
        i++;
      }
      return result;
    } else if (N instanceof InstanceKey) {
      InstanceKey I = (InstanceKey) N;
      TypeReference T = I.getConcreteType().getReference();

      if (Assertions.verifyAssertions) {
        if (T == null) {
          Assertions._assert(T != null, "null concrete type from " + I.getClass());
        }
      }
      if (T.isArrayType()) {
        PointerKey p = getHeapModel().getPointerKeyForArrayContents(I);
        if (p == null || !nodeManager.containsNode(p)) {
          return null;
        } else {
          return new int[] { nodeManager.getNumber(p) };
        }
      } else if (!Malleable.isMalleable(T)) {
        IClass klass = getHeapModel().getClassHierarchy().lookupClass(T);
        if (Assertions.verifyAssertions) {
          if (klass == null) {
            Assertions._assert(klass != null, "null klass for type " + T);
          }
        }
        MutableSparseIntSet result = new MutableSparseIntSet();
        try {
          for (Iterator<IField> it = klass.getAllInstanceFields().iterator(); it.hasNext();) {
            IField f = it.next();
            if (!f.getReference().getFieldType().isPrimitiveType()) {
              PointerKey p = getHeapModel().getPointerKeyForInstanceField(I, f);
              if (p != null && nodeManager.containsNode(p)) {
                result.add(nodeManager.getNumber(p));
              }
            }
          }
        } catch (ClassHierarchyException e) {
          // uh oh. skip it for now.
        }
        return result.toIntArray();
      } else {
        Assertions._assert(Malleable.isMalleable(T));
        return null;
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
    BasicNaturalRelation R = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.SIMPLE },
        BasicNaturalRelation.SIMPLE);

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
          for (int z = 0; z < succ.length; z++) {
            int j = succ[z];
            R.add(j, i);
          }
        }
      }
    }
  }

  /**
   * traverse locals in order, first by node, then by value number: attempt to
   * improve locality
   */
  private void computePredecessorsForLocals(NumberedNodeManager<Object> nodeManager, BasicNaturalRelation R) {

    ArrayList<LocalPointerKey> list = new ArrayList<LocalPointerKey>();
    for (Iterator it = nodeManager.iterator(); it.hasNext();) {
      Object n = it.next();
      if (n instanceof LocalPointerKey) {
        list.add((LocalPointerKey) n);
      }
    }
    Object[] arr =  list.toArray();
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
        for (int z = 0; z < succ.length; z++) {
          int j = succ[z];
          R.add(j, num);
        }
      }
    }
  }

  /**
   * sorts local pointers by node, then value number
   */
  private final class LocalPointerComparator implements Comparator<Object> {
    public int compare(Object arg1, Object arg2) {
      LocalPointerKey o1 = (LocalPointerKey)arg1;
      LocalPointerKey o2 = (LocalPointerKey)arg2;
      if (o1.getNode().equals(o2.getNode())) {
        return o1.getValueNumber() - o2.getValueNumber();
      } else {
        return callGraph.getNumber(o1.getNode()) - callGraph.getNumber(o2.getNode());
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNumber(com.ibm.wala.util.graph.Node)
   */
  public int getNumber(Object N) {
    return G.getNumber(N);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getNode(int)
   */
  public Object getNode(int number) {
    return G.getNode(number);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NumberedNodeManager#getMaxNumber()
   */
  public int getMaxNumber() {
    return G.getMaxNumber();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#iterateNodes()
   */
  public Iterator<Object> iterator() {
    return G.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#getNumberOfNodes()
   */
  public int getNumberOfNodes() {
    return G.getNumberOfNodes();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<? extends Object> getPredNodes(Object N) {
    return G.getPredNodes(N);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getPredNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getPredNodeCount(Object N) {
    return G.getPredNodeCount(N);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodes(com.ibm.wala.util.graph.Node)
   */
  public Iterator<? extends Object> getSuccNodes(Object N) {
    return G.getSuccNodes(N);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.EdgeManager#getSuccNodeCount(com.ibm.wala.util.graph.Node)
   */
  public int getSuccNodeCount(Object N) {
    return G.getSuccNodeCount(N);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#addNode(com.ibm.wala.util.graph.Node)
   */
  public void addNode(Object n) {
    Assertions.UNREACHABLE();

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  public void removeNode(Object n) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void addEdge(Object from, Object to) {
    Assertions.UNREACHABLE();
  }
  
  public void removeEdge(Object from, Object to) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   */
  public boolean hasEdge(Object from, Object to) {
    Assertions.UNREACHABLE();
    return false;
  }

  public void removeAllIncidentEdges(Object node) {
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(com.ibm.wala.util.graph.Node)
   */
  public boolean containsNode(Object N) {
    return G.containsNode(N);
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
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
        for (Iterator it = getSuccNodes(node); it.hasNext();) {
          Object s = it.next();
          result.append(getNumber(s)).append(" ");
        }
        result.append("\n");
      }
    }

    return result.toString();
  }

  /*
   * (non-Javadoc)
   * 
   */
  public void removeIncomingEdges(Object node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }

  /*
   * (non-Javadoc)
   */
  public void removeOutgoingEdges(Object node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
  }

  public IntSet getSuccNodeNumbers(Object node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  public IntSet getPredNodeNumbers(Object node) {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }
}
