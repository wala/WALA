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
package com.ibm.wala.dataflow.IFDS;

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;

/**
 * 
 * A view of a supergraph as an exploded supergraph.
 * 
 * Nodes are ExplodedSupergraphNodes. Edges are edges as realized by IFDS flow
 * functions;
 * 
 * Note: not terribly efficient, use with care.
 * 
 * @author sfink
 */
public class ExplodedSupergraph<T> implements Graph<ExplodedSupergraphNode<T>> {

  private final ISupergraph<T,?> supergraph;

  private final IFlowFunctionMap<T> flowFunctions;

  public ExplodedSupergraph(ISupergraph<T,?> supergraph, IFlowFunctionMap<T> flowFunctions) {
    this.supergraph = supergraph;
    this.flowFunctions = flowFunctions;
  }

  public void removeNodeAndEdges(ExplodedSupergraphNode N) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public Iterator<ExplodedSupergraphNode<T>> iterator() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  public int getNumberOfNodes() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return 0;
  }

  public void addNode(ExplodedSupergraphNode n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();

  }

  public void removeNode(ExplodedSupergraphNode n) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public boolean containsNode(ExplodedSupergraphNode N) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return false;
  }

  public Iterator<ExplodedSupergraphNode<T>> getPredNodes(ExplodedSupergraphNode<T> node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    T dest = node.getSupergraphNode();
    HashSet<ExplodedSupergraphNode<T>> result = HashSetFactory.make(supergraph.getPredNodeCount(dest));
    for (Iterator<? extends T> it = supergraph.getPredNodes(dest); it.hasNext();) {
      T src = it.next();
      if (supergraph.classifyEdge(src, dest) != ISupergraph.RETURN_EDGE) {
        IFlowFunction f = getFlowFunction(src, dest);
        if (f instanceof IReversibleFlowFunction) {
          IReversibleFlowFunction rf = (IReversibleFlowFunction) f;
          IntSet sources = rf.getSources(node.getFact());
          if (sources != null) {
            for (IntIterator ii = sources.intIterator(); ii.hasNext();) {
              int t = ii.next();
              result.add(new ExplodedSupergraphNode<T>(src, t));
            }
          }
        } else {
          Assertions.UNREACHABLE("need to implement for non-reversible flow function " + f.getClass());
        }
      } else {
        // special logic for a return edge.  dest is a return site
        for (Iterator<? extends T> it2 = supergraph.getCallSites(dest); it2.hasNext(); ) {
          T callBlock = it2.next();
          IFlowFunction f = flowFunctions.getReturnFlowFunction(callBlock,src,dest);
          if (f instanceof IReversibleFlowFunction) {
            IReversibleFlowFunction rf = (IReversibleFlowFunction) f;
            IntSet sources = rf.getSources(node.getFact());
            if (sources != null) {
              for (IntIterator ii = sources.intIterator(); ii.hasNext();) {
                int t = ii.next();
                result.add(new ExplodedSupergraphNode<T>(src, t));
              }
            }
          } else {
            Assertions.UNREACHABLE("need to implement for non-reversible flow function " + f.getClass());
          }
        }
      }
    }
    return result.iterator();
  }

  public int getPredNodeCount(ExplodedSupergraphNode<T> node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    T dest = node.getSupergraphNode();
    int count = 0;
    for (Iterator<? extends T> it = supergraph.getPredNodes(dest); it.hasNext();) {
      T src = it.next();
      if (supergraph.classifyEdge(src, dest) != ISupergraph.RETURN_EDGE) {
        IFlowFunction f = getFlowFunction(src, dest);
        if (f instanceof IReversibleFlowFunction) {
          IReversibleFlowFunction rf = (IReversibleFlowFunction) f;
          IntSet sources = rf.getSources(node.getFact());
          if (sources != null) {
            count += sources.size();
          }
        } else {
          Assertions.UNREACHABLE("need to implement for non-reversible flow function");
        }
      } else {
        // special logic for a return edge
        Assertions.UNREACHABLE("TODO: Implement me!");
      }
    }
    return count;
  }

  public Iterator<ExplodedSupergraphNode<T>> getSuccNodes(ExplodedSupergraphNode<T>  node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    T src = node.getSupergraphNode();
    HashSet<ExplodedSupergraphNode<T>> result = HashSetFactory.make(supergraph.getSuccNodeCount(src));
    for (Iterator<? extends T> it = supergraph.getSuccNodes(src); it.hasNext();) {
      T dest = it.next();
      if (supergraph.classifyEdge(src, dest) != ISupergraph.RETURN_EDGE) {
        IUnaryFlowFunction f = (IUnaryFlowFunction) getFlowFunction(src, dest);
        IntSet targets = f.getTargets(node.getFact());
        if (targets != null) {
          for (IntIterator ii = targets.intIterator(); ii.hasNext();) {
            int t = ii.next();
            result.add(new ExplodedSupergraphNode<T>(dest, t));
          }
        }
      } else {
        // special logic for a return edge.  dest is a return site
        for (Iterator<? extends T> it2 = supergraph.getCallSites(dest); it2.hasNext(); ) {
          T callBlock = it2.next();
          IUnaryFlowFunction f = (IUnaryFlowFunction) flowFunctions.getReturnFlowFunction(callBlock,src,dest);
          IntSet targets = f.getTargets(node.getFact());
          if (targets != null) {
            for (IntIterator ii = targets.intIterator(); ii.hasNext();) {
              int t = ii.next();
              result.add(new ExplodedSupergraphNode<T>(dest, t));
            }
          }
        }
      }
    }
    return result.iterator();
  }

  private IFlowFunction getFlowFunction(T src, T dest) {
    switch (supergraph.classifyEdge(src, dest)) {
    case ISupergraph.CALL_EDGE:
      return flowFunctions.getCallFlowFunction(src, dest);
    case ISupergraph.CALL_TO_RETURN_EDGE:
      Iterator callees = supergraph.getCalledNodes(src);
      if (callees.hasNext()) {
        return flowFunctions.getCallToReturnFlowFunction(src, dest);
      } else {
        return flowFunctions.getCallNoneToReturnFlowFunction(src, dest);
      }
    case ISupergraph.RETURN_EDGE:
      Assertions.UNREACHABLE();
      return null;
    // return flowFunctions.getReturnFlowFunction(src, dest);
    case ISupergraph.OTHER:
      return flowFunctions.getNormalFlowFunction(src, dest);
    default:
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public int getSuccNodeCount(ExplodedSupergraphNode<T> node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    T src = node.getSupergraphNode();
    int count = 0;
    for (Iterator<? extends T> it = supergraph.getSuccNodes(src); it.hasNext();) {
      T dest = it.next();
      IUnaryFlowFunction f = (IUnaryFlowFunction) getFlowFunction(src, dest);
      IntSet targets = f.getTargets(node.getFact());
      if (targets != null) {
        count += targets.size();
      }
    }
    return count;
  }

  public void addEdge(ExplodedSupergraphNode src, ExplodedSupergraphNode dst) {
    Assertions.UNREACHABLE();

  }
  
  public void removeEdge(ExplodedSupergraphNode src, ExplodedSupergraphNode dst) {
    throw new UnsupportedOperationException();
  }

  public void removeAllIncidentEdges(ExplodedSupergraphNode node) {
    Assertions.UNREACHABLE();throw new UnsupportedOperationException();
  }

  public void removeIncomingEdges(ExplodedSupergraphNode node) {
    throw new UnsupportedOperationException();
  }

  public void removeOutgoingEdges(ExplodedSupergraphNode node) throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  public boolean hasEdge(ExplodedSupergraphNode<T> src, ExplodedSupergraphNode<T> dst) {
    for (Iterator it = getSuccNodes(src); it.hasNext();) {
      if (it.next().equals(dst)) {
        return true;
      }
    }
    return false;
  }

  public ISupergraph<T,?> getSupergraph() {
    return supergraph;
  }

  /**
   * @return Returns the flowFunctions.
   */
  public IFlowFunctionMap getFlowFunctions() {
    return flowFunctions;
  }
}
