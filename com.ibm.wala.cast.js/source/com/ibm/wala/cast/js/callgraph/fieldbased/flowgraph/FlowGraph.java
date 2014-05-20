/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph;

import java.util.Collection;
import java.util.Iterator;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.AbstractVertexVisitor;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.FuncVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.UnknownVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VarVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.Vertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VertexFactory;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphReachability;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.InvertedGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * A flow graph models data flow between vertices representing local variables, properties,
 * return values, and so forth.
 * 
 * @author mschaefer
 */
public class FlowGraph implements Iterable<Vertex> {
	// the actual flow graph representation
	private final NumberedGraph<Vertex> graph;
	
	// a factory that allows us to build canonical vertices
	private final VertexFactory factory;
	
	// the transitive closure of the inverse of this.graph, 
	// but without paths going through the Unknown vertex
	private GraphReachability<Vertex,FuncVertex> optimistic_closure;
	
	public FlowGraph() {
		this.graph = new SlowSparseNumberedGraph<Vertex>(1);
		this.factory = new VertexFactory();
	}
	
	// (re-)compute optimistic_closure
	private void compute_optimistic_closure(IProgressMonitor monitor) throws CancelException {
		if(optimistic_closure != null)
			return;
		
		// prune flowgraph by taking out 'unknown' vertex
		Graph<Vertex> pruned_flowgraph = GraphSlicer.prune(graph, new Predicate<Vertex>() {
			@Override
			public boolean test(Vertex t) {
				return t.accept(new AbstractVertexVisitor<Boolean>() {
					@Override
					public Boolean visitVertex(Vertex vertex) {
						return true;
					}
					
					@Override
					public Boolean visitUnknownVertex(UnknownVertex unknownVertex) {
						return false;
					}
				});
			}
		});
		
		// compute transitive closure
		optimistic_closure = 
		    new GraphReachability<Vertex,FuncVertex>(
		      new InvertedGraph<Vertex>(pruned_flowgraph),
		      new Filter<Vertex>() {
		        @Override
		        public boolean accepts(Vertex o) {
		          return o instanceof FuncVertex;
		        } 
		      }
		    );
		
		optimistic_closure.solve(monitor);
	}
	
	public VertexFactory getVertexFactory() {
		return factory;
	}
	
	/**
	 * Adds an edge from vertex <code>from</code> to vertex <code>to</code>, adding the vertices
	 * to the graph if they are not in there yet.
	 */
	public void addEdge(Vertex from, Vertex to) {
		if(!graph.containsNode(from))
			graph.addNode(from);
		if(!graph.containsNode(to))
			graph.addNode(to);
		
		if(!graph.hasEdge(from, to)) {
		    optimistic_closure = null;
		    graph.addEdge(from, to);
		}
	}

	/**
	 * Computes the set of vertices that may reach <code>dest</code> along paths not containing an
	 * {@link UnknownVertex}.
	 */
	public OrdinalSet<FuncVertex> getReachingSet(Vertex dest, IProgressMonitor monitor) throws CancelException {
		if(!graph.containsNode(dest))
			return OrdinalSet.empty();
		
		compute_optimistic_closure(monitor);
		return optimistic_closure.getReachableSet(dest);
	}
	
	public Iterator<Vertex> getSucc(Vertex v) {
	  return graph.getSuccNodes(v);
	}
	
  @Override
  public Iterator<Vertex> iterator() {
    return graph.iterator();
  }
  
  public PointerAnalysis<FuncVertex> getPointerAnalysis(final IProgressMonitor monitor) {
    return new PointerAnalysis<FuncVertex>() {

      @Override
      public OrdinalSet<FuncVertex> getPointsToSet(PointerKey key) {
        if (key instanceof LocalPointerKey) {
          CGNode node = ((LocalPointerKey)key).getNode();
          FuncVertex fn = factory.makeFuncVertex(node.getMethod().getDeclaringClass());
          int vn = ((LocalPointerKey)key).getValueNumber();
          VarVertex v = factory.makeVarVertex(fn, vn);
          try {
            return getReachingSet(v, monitor);
          } catch (CancelException e) {
            return null;
          }
        } else {
          return null;
        }
      }

      @Override
      public OrdinalSetMapping<InstanceKey> getInstanceKeyMapping() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Iterable<PointerKey> getPointerKeys() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Collection<InstanceKey> getInstanceKeys() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public boolean isFiltered(PointerKey pk) {
         return false;
      }

      @Override
      public HeapModel getHeapModel() {
        assert false;
        return null;
      }

      @Override
      public HeapGraph getHeapGraph() {
        assert false;
        return null;
      }

      @Override
      public IClassHierarchy getClassHierarchy() {
        assert false;
        return null;
      }
      
    };
  }
}
