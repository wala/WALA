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
package com.ibm.wala.util.graph.impl;

import java.util.Iterator;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NumberedNodeManager;
import com.ibm.wala.util.intset.IntSet;

/**
 * Basic implementation of a numbered graph -- this implementation relies on nodes that carry numbers and edges.
 * 
 * The management of node numbers is a bit fragile, but designed this way for efficiency. Use this class with care.
 */
public class DelegatingNumberedNodeManager<T extends INodeWithNumber> implements NumberedNodeManager<T> {

  private final double BUFFER_FACTOR = 1.5;

  private INodeWithNumber[] nodes = new INodeWithNumber[20];

  private int maxNumber = -1;

  private int numberOfNodes = 0;

  /*
   * @see com.ibm.wala.util.graph.NumberedGraph#getNumber(com.ibm.wala.util.graph.Node)
   */
  @Override
  public int getNumber(T N) {
    if (N == null) {
      throw new IllegalArgumentException("N is null");
    }
    INodeWithNumber n = N;
    return n.getGraphNodeId();
  }

  @Override
  @SuppressWarnings("unchecked")
  public T getNode(int number) {
    try {
      return (T) nodes[number];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("Invalid number " + number, e);
    }
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedGraph#getMaxNumber()
   */
  @Override
  public int getMaxNumber() {
    return maxNumber;
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#iterateNodes()
   */
  @Override
  public Iterator<T> iterator() {
    final INodeWithNumber[] arr = nodes;
    return new Iterator<T>() {
      int nextCounter = -1;
      {
        advance();
      }

      void advance() {
        for (int i = nextCounter + 1; i < arr.length; i++) {
          if (arr[i] != null) {
            nextCounter = i;
            return;
          }
        }
        nextCounter = -1;
      }

      @Override
      public boolean hasNext() {
        return nextCounter != -1;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T next() {
        if (hasNext()) {
          int r = nextCounter;
          advance();
          return (T) arr[r];
        } else {
          return null;
        }
      }

      @Override
      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /*
   * @see com.ibm.wala.util.graph.Graph#getNumberOfNodes()
   */
  @Override
  public int getNumberOfNodes() {
    return numberOfNodes;
  }

  /**
   * If N.getNumber() == -1, then set N.number and insert this node in the graph. Use with extreme care.
   * 
   * @see com.ibm.wala.util.graph.NodeManager#addNode(java.lang.Object)
   * @throws IllegalArgumentException if n is null
   */
  @Override
  public void addNode(T n) {
    if (n == null) {
      throw new IllegalArgumentException("n is null");
    }
    INodeWithNumber N = n;
    int number = N.getGraphNodeId();
    if (number == -1) {
      maxNumber++;
      N.setGraphNodeId(maxNumber);
      number = maxNumber;
    } else {
      if (number > maxNumber) {
        maxNumber = number;
      }
    }
    ensureCapacity(number);
    if (nodes[number] != null && nodes[number] != N) {
      Assertions.UNREACHABLE("number: " + number + " N: " + N + " nodes[number]: " + nodes[number]);
    }
    nodes[number] = N;
    numberOfNodes++;
  }

  /**
   * @param number
   */
  private void ensureCapacity(int number) {
    if (nodes.length < number + 1) {
      int newLength = (int) ((number + 1) * BUFFER_FACTOR);
      INodeWithNumber[] old = nodes;
      nodes = new INodeWithNumber[newLength];
      System.arraycopy(old, 0, nodes, 0, old.length);
    }
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#remove(com.ibm.wala.util.graph.Node)
   */
  @Override
  public void removeNode(T n) {
    if (n == null) {
      throw new IllegalArgumentException("n is null");
    }
    INodeWithNumber N = n;
    int number = N.getGraphNodeId();
    if (number == -1) {
      throw new IllegalArgumentException("Cannot remove node, not in graph");
    }
    if (nodes[number] != null) {
      nodes[number] = null;
      numberOfNodes--;
    }
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("Nodes:\n");
    for (int i = 0; i <= maxNumber; i++) {
      result.append(i).append(" ");
      if (nodes[i] != null) {
        result.append(nodes[i].toString());
      }
      result.append("\n");
    }
    return result.toString();
  }

  /*
   * @see com.ibm.wala.util.graph.NodeManager#containsNode(com.ibm.wala.util.graph.Node)
   */
  @Override
  public boolean containsNode(T n) {
    if (n == null) {
      throw new IllegalArgumentException("n is null");
    }
    INodeWithNumber N = n;
    int number = N.getGraphNodeId();
    if (number == -1) {
      return false;
    }
    if (number >= nodes.length) {
      throw new IllegalArgumentException(
          "node already has a graph node id, but is not registered there in this graph (number too big)\n"
              + "this graph implementation is fragile and won't support this kind of test\n" + n.getClass() + " : " + n);
    }
    if (nodes[number] != N) {
      throw new IllegalArgumentException("node already has a graph node id, but is not registered there in this graph\n"
          + "this graph implementation is fragile and won't support this kind of test\n" + n.getClass() + " : " + n);
    }
    return true;
    // return (nodes[number] == N);
  }

  /*
   * @see com.ibm.wala.util.graph.NumberedNodeManager#iterateNodes(com.ibm.wala.util.intset.IntSet)
   */
  @Override
  public Iterator<T> iterateNodes(IntSet s) {
    return new NumberedNodeIterator<>(s, this);
  }

}
