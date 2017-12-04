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
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.cast.ipa.callgraph.AstHeapModel;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.AbstractVertexVisitor;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.CreationSiteVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.FuncVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.PropVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.PrototypeFieldVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.PrototypeFieldVertex.PrototypeField;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.UnknownVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VarVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.Vertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VertexFactory;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.SetPrototype;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.TypeFilter;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.CompoundIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphReachability;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.NumberedGraph;
import com.ibm.wala.util.graph.impl.ExtensionGraph;
import com.ibm.wala.util.graph.impl.InvertedGraph;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.DFS;
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
		this.graph = new SlowSparseNumberedGraph<>(1);
		this.factory = new VertexFactory();
	}
	
	// (re-)compute optimistic_closure
	private void compute_optimistic_closure(IProgressMonitor monitor) throws CancelException {
		if(optimistic_closure != null)
			return;
		
		optimistic_closure = computeClosure(graph, monitor, FuncVertex.class);
	}
	
	private static <T> GraphReachability<Vertex, T> computeClosure(NumberedGraph<Vertex> graph, IProgressMonitor monitor, final Class<?> type) throws CancelException {
		// prune flowgraph by taking out 'unknown' vertex
		Graph<Vertex> pruned_flowgraph = GraphSlicer.prune(graph, t -> t.accept(new AbstractVertexVisitor<Boolean>() {
    	@Override
    	public Boolean visitVertex() {
    		return true;
    	}
    	
    	@Override
    	public Boolean visitUnknownVertex(UnknownVertex unknownVertex) {
    		return false;
    	}
    }));
		
		// compute transitive closure
		GraphReachability<Vertex, T> optimistic_closure = 
		    new GraphReachability<>(
		      new InvertedGraph<>(pruned_flowgraph),
		      type::isInstance
		    );
		
		optimistic_closure.solve(monitor);
		
		return optimistic_closure;
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
  
  public PointerAnalysis<ObjectVertex> getPointerAnalysis(final CallGraph cg, final IAnalysisCacheView cache, final IProgressMonitor monitor) throws CancelException {
    return new PointerAnalysis<ObjectVertex>() {
      
      private final Map<Pair<PrototypeField,ObjectVertex>,PrototypeFieldVertex> proto = HashMapFactory.make();
      
      private GraphReachability<Vertex,ObjectVertex> pointerAnalysis = computeClosure(graph, monitor, ObjectVertex.class);

      private final ExtensionGraph<Vertex> dataflow = new ExtensionGraph<>(graph);

      protected IR getIR(final IAnalysisCacheView cache, FuncVertex func) {
        return cache.getIR(func.getConcreteType().getMethod(AstMethodReference.fnSelector));
      }

      private PointerKey propertyKey(String property, ObjectVertex o) {
        if ("__proto__".equals(property) || "prototype".equals(property)) {
          return get(PrototypeField.valueOf(property), o);
        } else {
          return factory.makePropVertex(property);
        }
      }
            
      {
        PropVertex proto = factory.makePropVertex("prototype");
        if (graph.containsNode(proto)) {
          for(Vertex p : Iterator2Iterable.make(graph.getPredNodes(proto))) {
            if (p instanceof VarVertex) {
              int rval = ((VarVertex) p).getValueNumber();
              FuncVertex func = ((VarVertex) p).getFunction();
              DefUse du = cache.getDefUse(getIR(cache, func));
              for(SSAInstruction inst : Iterator2Iterable.make(du.getUses(rval))) {
                if (inst instanceof JavaScriptPropertyWrite) {
                  int obj = ((JavaScriptPropertyWrite) inst).getObjectRef();
                  VarVertex object = factory.makeVarVertex(func, obj);
                  for(ObjectVertex o : getPointsToSet(object)) {
                    PrototypeFieldVertex prototype = get(PrototypeField.prototype, o);
                    if (! dataflow.containsNode(prototype)) {
                      dataflow.addNode(prototype);
                    }
                    System.err.println("adding " + p + " --> " + prototype);
                    dataflow.addEdge(p, prototype);
                  }
                }
              }
            }
          }
        }
        
        pointerAnalysis = computeClosure(dataflow, monitor, ObjectVertex.class);
      }
      
      private PrototypeFieldVertex get(PrototypeField f, ObjectVertex o) {
        Pair<PrototypeField,ObjectVertex> key = Pair.make(f, o);
        if (! proto.containsKey(key)) {
          proto.put(key, new PrototypeFieldVertex(f, o));
        }
        return proto.get(key);
      }

      private FuncVertex getVertex(CGNode n) {
        IMethod m = n.getMethod();
        if (m.getSelector().equals(AstMethodReference.fnSelector)) {
          IClass fun = m.getDeclaringClass();
          return factory.makeFuncVertex(fun);
        } else {
          return null;
        }       
      }

      @Override
      public OrdinalSet<ObjectVertex> getPointsToSet(PointerKey key) {
        if (dataflow.containsNode((Vertex)key)) {
          return pointerAnalysis.getReachableSet(key);
        } else {
          return OrdinalSet.empty();
        }
      }

      @Override
      public Collection<ObjectVertex> getInstanceKeys() {
        Set<ObjectVertex> result = HashSetFactory.make();
        for(CreationSiteVertex cs : factory.creationSites()) {
          if (cg.getNode(cs.getMethod(), Everywhere.EVERYWHERE) != null) {
            result.add(cs);
          }
        }
        result.addAll(factory.getFuncVertices());
        result.add(factory.global());
        return result;
      }

      @Override
      public boolean isFiltered(PointerKey pk) {
         return false;
      }

      @Override
      public OrdinalSetMapping<ObjectVertex> getInstanceKeyMapping() {
        assert false;
        return null;
      }

      @Override
      public Iterable<PointerKey> getPointerKeys() {
        return () -> new CompoundIterator<>(factory.getArgVertices().iterator(),
            new CompoundIterator<>(factory.getRetVertices().iterator(), 
                new CompoundIterator<PointerKey>(factory.getVarVertices().iterator(),
                    factory.getPropVertices().iterator())));
      }
      
      @Override
      public HeapModel getHeapModel() {
        return new AstHeapModel() {
          
          @Override
          public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
            FuncVertex function = getVertex(node);
            if (function != null) {
              return factory.makeVarVertex(function, valueNumber);
            } else {
              assert false;
              return null;
            }
          }

          @Override
          public PointerKey getPointerKeyForReturnValue(CGNode node) {
            FuncVertex function = getVertex(node);
            if (function != null) {
              return factory.makeRetVertex(function);
            } else {
              assert false;
              return null;
            }
          }

          @Override
          public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
            String f = field.getName().toString();
            if ("__proto__".equals(f) || "prototype".equals(f)) {
              return get(PrototypeField.valueOf(f), (ObjectVertex)I);
            } else {
              return factory.makePropVertex(f);
            }
          }

          @Override
          public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, TypeFilter filter) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public PointerKey getPointerKeyForStaticField(IField f) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Iterator<PointerKey> iteratePointerKeys() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public IClassHierarchy getClassHierarchy() {
            assert false;
            return null;
          }

          @Override
          public PointerKey getPointerKeyForArrayLength(InstanceKey I) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Iterator<PointerKey> getPointerKeysForReflectedFieldRead(InstanceKey I, InstanceKey F) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Iterator<PointerKey> getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F) {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public PointerKey getPointerKeyForObjectCatalog(InstanceKey I) {
            // TODO Auto-generated method stub
            return null;
          }        
        };
      }

      private HeapGraph<ObjectVertex> heapGraph;
      
      @Override
      public HeapGraph<ObjectVertex> getHeapGraph() {
        if (heapGraph == null) {

          final PointerAnalysis<ObjectVertex> pa = this;
          class FieldBasedHeapGraph extends SlowSparseNumberedGraph<Object> implements HeapGraph<ObjectVertex> {

            private static final long serialVersionUID = -3544629644808422215L;

            private <X> X ensureNode(X n) {
              if (!containsNode(n)) {
                addNode(n);
              }

              return n;
            }

            private PropVertex getCoreProto(TypeReference coreType) {
              if (coreType.equals(JavaScriptTypes.Object)) {
                return factory.makePropVertex("Object$proto$__WALA__");
              } else if (coreType.equals(JavaScriptTypes.Function)) {
                return factory.makePropVertex("Function$proto$__WALA__");
              } else if (coreType.equals(JavaScriptTypes.Number) || coreType.equals(JavaScriptTypes.NumberObject)) {
                return factory.makePropVertex("Number$proto$__WALA__");
              } else if (coreType.equals(JavaScriptTypes.Array)) {
                return factory.makePropVertex("Array$proto$__WALA__");
              } else if (coreType.equals(JavaScriptTypes.String) || coreType.equals(JavaScriptTypes.StringObject)) {
                return factory.makePropVertex("String$proto$__WALA__");
              } else if (coreType.equals(JavaScriptTypes.Date)) {
                return factory.makePropVertex("Date$proto$__WALA__");
              } else if (coreType.equals(JavaScriptTypes.RegExp) || coreType.equals(JavaScriptTypes.RegExpObject)) {
                return factory.makePropVertex("RegExp$proto$__WALA__");
              } else {
                return null;
              }
            }
            
            {
              for(PropVertex property : factory.getPropVertices()) {

                // edges from objects to properties assigned to them
                for(Vertex p : Iterator2Iterable.make(dataflow.getPredNodes(property))) {
                  if (p instanceof VarVertex) {
                    int rval = ((VarVertex) p).getValueNumber();
                    FuncVertex func = ((VarVertex) p).getFunction();
                    DefUse du = cache.getDefUse(getIR(cache, func));
                    for(SSAInstruction inst : Iterator2Iterable.make(du.getUses(rval))) {
                      if (inst instanceof JavaScriptPropertyWrite) {
                        int obj = ((JavaScriptPropertyWrite) inst).getObjectRef();
                        VarVertex object = factory.makeVarVertex(func, obj);
                        for(ObjectVertex o : getPointsToSet(object)) {
                          addEdge(ensureNode(o), ensureNode(propertyKey(property.getPropName(), o)));
                          for(ObjectVertex v : getPointsToSet(property)) {
                            addEdge(ensureNode(propertyKey(property.getPropName(), o)), ensureNode(v));
                          }
                        }
                      } else if (inst instanceof AstGlobalWrite) {
                        addEdge(ensureNode(factory.global()), ensureNode(property));
                        for(ObjectVertex v : getPointsToSet(property)) {
                          addEdge(ensureNode(property), ensureNode(v));                        
                        }
                      } else if (inst instanceof SetPrototype) {
                        int obj = inst.getUse(0);
                        for(ObjectVertex o : getPointsToSet(factory.makeVarVertex(func, obj))) {
                          for(ObjectVertex v : getPointsToSet(property)) {
                            addEdge(ensureNode(o), ensureNode(get(PrototypeField.prototype, o)));
                            addEdge(ensureNode(get(PrototypeField.prototype, o)), ensureNode(v));
                          }
                        }
                      } else {
                        System.err.println("ignoring " + inst);
                      }
                    }
                  }
                }
              } 
                                   
              // prototype dataflow for function creations
              for(FuncVertex f : factory.getFuncVertices()) {
                ensureNode(get(PrototypeField.__proto__, f));
                addEdge(
                  ensureNode(getCoreProto(JavaScriptTypes.Function)),
                  ensureNode(get(PrototypeField.prototype, f))
                );
              }

              // prototype dataflow for object creations
              for(CreationSiteVertex cs : factory.creationSites()) {
                if (cg.getNode(cs.getMethod(), Everywhere.EVERYWHERE) != null) {
                for(Pair<CGNode, NewSiteReference> site : Iterator2Iterable.make(cs.getCreationSites(cg))) {
                  IR ir = site.fst.getIR();
                  SSAInstruction creation = ir.getInstructions()[ site.snd.getProgramCounter() ];
                  if (creation instanceof JavaScriptInvoke) {
                    for(ObjectVertex f : getPointsToSet(factory.makeVarVertex(getVertex(site.fst), creation.getUse(0)))) {
                      for(ObjectVertex o : getPointsToSet(factory.makeVarVertex(getVertex(site.fst), creation.getDef(0)))) {
                        addEdge(
                            ensureNode(get(PrototypeField.prototype, f)),                    
                            ensureNode(get(PrototypeField.__proto__, o)));                      
                      }
                    }
                  } else if (creation instanceof SSANewInstruction) {
                    PointerKey proto = getCoreProto(((SSANewInstruction) creation).getConcreteType());
                    if (proto != null) {
                      for(ObjectVertex f : getPointsToSet(proto)) {
                        for(ObjectVertex o : getPointsToSet(factory.makeVarVertex(getVertex(site.fst), creation.getDef(0)))) {
                          addEdge(
                            ensureNode(get(PrototypeField.__proto__, o)),
                            ensureNode(f));
                        }
                      }
                    }
                  }
                }
              }
              }
            }

            @Override
            public Collection<Object> getReachableInstances(Set<Object> roots) {
              return DFS.getReachableNodes(this, roots, ObjectVertex.class::isInstance);
            }

            @Override
            public HeapModel getHeapModel() {
              return pa.getHeapModel();
            }

            @Override
            public PointerAnalysis<ObjectVertex> getPointerAnalysis() {
              return pa;
            }
          }

          heapGraph = new FieldBasedHeapGraph();
        }

        return heapGraph;
      }

      @Override
      public IClassHierarchy getClassHierarchy() {
        assert false;
        return null;
      }
      
    };
  }
}
