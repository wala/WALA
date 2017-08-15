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
package com.ibm.wala.cast.js.callgraph.fieldbased;

import java.util.Set;

import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.CallVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.FuncVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VarVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VertexFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Optimistic call graph builder that propagates inter-procedural data flow iteratively as
 * call edges are discovered. Slower, but potentially more sound than {@link PessimisticCallGraphBuilder}.
 * 
 * @author mschaefer
 *
 */
public class OptimisticCallgraphBuilder extends FieldBasedCallGraphBuilder {
	/** The maximum number of iterations to perform. */
	public int ITERATION_CUTOFF = Integer.MAX_VALUE;
	
	private final boolean handleCallApply;
	
	public OptimisticCallgraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView iAnalysisCacheView, boolean supportFullPointerAnalysis) {
		super(cha, options, iAnalysisCacheView, supportFullPointerAnalysis);
		handleCallApply = options instanceof JSAnalysisOptions && ((JSAnalysisOptions)options).handleCallApply();
	}

	@Override
	public FlowGraph buildFlowGraph(IProgressMonitor monitor) throws CancelException {
	   FlowGraph flowgraph = flowGraphFactory();
		
		// keep track of which call edges we already know about
		Set<Pair<CallVertex, FuncVertex>> knownEdges = HashSetFactory.make();
		
		// flag for fixpoint iteration
		boolean changed = true;
		int iter = 0;
		
		while(iter++ < ITERATION_CUTOFF && changed) {
			MonitorUtil.throwExceptionIfCanceled(monitor);
			changed = false;
			
			// extract all call edges from the flow graph
			Set<Pair<CallVertex, FuncVertex>> newEdges = this.extractCallGraphEdges(flowgraph, monitor);
			
			for(Pair<CallVertex, FuncVertex> edge : newEdges) {
				MonitorUtil.throwExceptionIfCanceled(monitor);
				
				// set changed to true if this is a new edge
				boolean newEdge = knownEdges.add(edge);
				changed = changed || newEdge;
				
				if(newEdge) {
					// handle it
					addEdge(flowgraph, edge.fst, edge.snd);
				
					// special handling of invocations of Function.prototype.call
					// TODO: since we've just added some edges to the flow graph, its transitive closure will be
					//       recomputed here, which is slow and unnecessary
					if(handleCallApply && 
					    (edge.snd.getFullName().equals("Lprologue.js/Function_prototype_call") ||
					     edge.snd.getFullName().equals("Lprologue.js/Function_prototype_apply"))) {
						addReflectiveCallEdge(flowgraph, edge.fst, monitor);
					}
				}
			}
		}
		
		return flowgraph;
	}

	// add flow corresponding to a new call edge
	private static void addEdge(FlowGraph flowgraph, CallVertex c, FuncVertex callee) {
	  VertexFactory factory = flowgraph.getVertexFactory();
	  JavaScriptInvoke invk = c.getInstruction();
	  FuncVertex caller = c.getCaller();

	  int offset = 0;
    if (invk.getDeclaredTarget().getSelector().equals(JavaScriptMethods.ctorReference.getSelector())) {
      offset = 1;
    }
    
    for(int i=0;i<invk.getNumberOfParameters();++i) {
      // only flow receiver into 'this' if invk is, in fact, a method call
      flowgraph.addEdge(factory.makeVarVertex(caller, invk.getUse(i)), factory.makeArgVertex(callee));
      //if(i != 1 || !invk.getDeclaredTarget().getSelector().equals(AstMethodReference.fnSelector))
        flowgraph.addEdge(factory.makeVarVertex(caller, invk.getUse(i)), factory.makeParamVertex(callee, i+offset));
    }

	  // flow from return vertex to result vertex
	  flowgraph.addEdge(factory.makeRetVertex(callee), factory.makeVarVertex(caller, invk.getDef()));			
	}
	
	// add data flow corresponding to a reflective invocation via Function.prototype.call
	// NB: for f.call(...), f will _not_ appear as a call target, but the appropriate argument and return data flow will be set up
	private static void addReflectiveCallEdge(FlowGraph flowgraph, CallVertex c, IProgressMonitor monitor) throws CancelException {
	  VertexFactory factory = flowgraph.getVertexFactory();
	  FuncVertex caller = c.getCaller();
	  JavaScriptInvoke invk = c.getInstruction();

	  VarVertex receiverVertex = factory.makeVarVertex(caller, invk.getUse(1));
	  OrdinalSet<FuncVertex> realCallees = flowgraph.getReachingSet(receiverVertex, monitor);
	  System.err.println("callees " + realCallees + " for " + caller);
	  for(FuncVertex realCallee: realCallees) {
	    // flow from arguments to parameters
	    for(int i=2;i<invk.getNumberOfParameters();++i)
	      flowgraph.addEdge(factory.makeVarVertex(caller, invk.getUse(i)), factory.makeParamVertex(realCallee, i-1));

	    // flow from return vertex to result vertex
	    flowgraph.addEdge(factory.makeRetVertex(realCallee), factory.makeVarVertex(caller, invk.getDef()));
	  }
	}
}
