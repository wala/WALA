/******************************************************************************
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.callgraph.fieldbased;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.AstContextInsensitiveSSAContextInterpreter;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraphBuilder;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.CallVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.FuncVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.ObjectVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VarVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VertexFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraph;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptConstructTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptFunctionApplyContextInterpreter;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptFunctionApplyTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.JavaScriptFunctionDotCallTargetSelector;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions.JavaScriptConstructor;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.ContextInsensitiveSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultSSAInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * Abstract call graph builder class for building a call graph from a field-based flow graph.
 * The algorithm for building the flow graph is left unspecified, and is implemented differently
 * by subclasses.
 * 
 * @author mschaefer
 *
 */
public abstract class FieldBasedCallGraphBuilder {
	// class hierarchy of the program to be analysed
	protected final IClassHierarchy cha;
	
	// standard call graph machinery
	protected final AnalysisOptions options;
	protected final IAnalysisCacheView cache;
	protected final JavaScriptConstructorFunctions constructors;
	public final MethodTargetSelector targetSelector;
	protected final boolean supportFullPointerAnalysis;
	
	private static final boolean LOG_TIMINGS = true;
	
	public FieldBasedCallGraphBuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView iAnalysisCacheView, boolean supportFullPointerAnalysis) {
		this.cha = cha;
		this.options = options;
		this.cache = iAnalysisCacheView;
		this.constructors = new JavaScriptConstructorFunctions(cha);
		this.targetSelector = setupMethodTargetSelector(constructors, options);
		this.supportFullPointerAnalysis = supportFullPointerAnalysis;
	}

  private static MethodTargetSelector setupMethodTargetSelector(JavaScriptConstructorFunctions constructors2, AnalysisOptions options) {
    MethodTargetSelector result = new JavaScriptConstructTargetSelector(constructors2, options.getMethodTargetSelector());
    if (options instanceof JSAnalysisOptions && ((JSAnalysisOptions)options).handleCallApply()) {
      result = new JavaScriptFunctionApplyTargetSelector(new JavaScriptFunctionDotCallTargetSelector(result));
    }
    return result;
  }
	
  protected FlowGraph flowGraphFactory() {
    FlowGraphBuilder builder = new FlowGraphBuilder(cha, cache, supportFullPointerAnalysis);
    return builder.buildFlowGraph();
  }

	/**
	 * Build a flow graph for the program to be analysed.
	 */
	public abstract FlowGraph buildFlowGraph(IProgressMonitor monitor) throws CancelException;
	
	/**
	 * Main entry point: builds a flow graph, then extracts a call graph and returns it.
	 */
	public Pair<JSCallGraph,PointerAnalysis<ObjectVertex>> buildCallGraph(Iterable<? extends Entrypoint> eps, IProgressMonitor monitor) throws CancelException {
		long fgBegin, fgEnd, cgBegin, cgEnd;
	
		if(LOG_TIMINGS) fgBegin = System.currentTimeMillis();

		MonitorUtil.beginTask(monitor, "flow graph", 1);
		FlowGraph flowGraph = buildFlowGraph(monitor);
		MonitorUtil.done(monitor);
		
		if(LOG_TIMINGS) {
			fgEnd = System.currentTimeMillis();
			System.out.println("flow graph construction took " + (fgEnd-fgBegin)/1000.0 + " seconds");
			cgBegin = System.currentTimeMillis();
		}
		
    MonitorUtil.beginTask(monitor, "extract call graph", 1);
		JSCallGraph cg = extract(flowGraph, eps, monitor);
    MonitorUtil.done(monitor);
		
		if(LOG_TIMINGS) {
			cgEnd = System.currentTimeMillis();
			System.out.println("call graph extraction took " + (cgEnd-cgBegin)/1000.0 + " seconds");
		}
		
		return Pair.make(cg,flowGraph.getPointerAnalysis(cg, cache, monitor));
	}

	/**
	 * Extract a call graph from a given flow graph.
	 */
  public JSCallGraph extract(FlowGraph flowgraph, Iterable<? extends Entrypoint> eps, IProgressMonitor monitor) throws CancelException {
    DelegatingSSAContextInterpreter interpreter = new DelegatingSSAContextInterpreter(new AstContextInsensitiveSSAContextInterpreter(options, cache), new DefaultSSAInterpreter(options, cache));
    return extract(interpreter, flowgraph, eps, monitor);
  }
  
  @SuppressWarnings("deprecation")
	public JSCallGraph extract(SSAContextInterpreter interpreter, FlowGraph flowgraph, Iterable<? extends Entrypoint> eps, IProgressMonitor monitor) throws CancelException {
	  // set up call graph
		final JSCallGraph cg = new JSCallGraph(cha, options, cache);
		cg.init();
		
		// setup context interpreters
    if (options instanceof JSAnalysisOptions && ((JSAnalysisOptions)options).handleCallApply()) {
      interpreter = new DelegatingSSAContextInterpreter(new JavaScriptFunctionApplyContextInterpreter(options, cache), interpreter);
    }
    cg.setInterpreter(interpreter);
	    		
		// set up call edges from fake root to all script nodes
		AbstractRootMethod fakeRootMethod = (AbstractRootMethod)cg.getFakeRootNode().getMethod();
		CGNode fakeRootNode = cg.findOrCreateNode(fakeRootMethod, Everywhere.EVERYWHERE);
		for (Entrypoint ep : eps) {
			CGNode nd = cg.findOrCreateNode(ep.getMethod(), Everywhere.EVERYWHERE);
			SSAAbstractInvokeInstruction invk = ep.addCall(fakeRootMethod);
			fakeRootNode.addTarget(invk.getCallSite(), nd);
		}
		// register the fake root as the "true" entrypoint
		cg.registerEntrypoint(fakeRootNode);
		
		// now add genuine call edges
		Set<Pair<CallVertex, FuncVertex>> edges = extractCallGraphEdges(flowgraph, monitor);

		  for (Pair<CallVertex, FuncVertex> edge : edges) {
		    CallVertex callVertex = edge.fst;
		    FuncVertex targetVertex = edge.snd;
		    IClass kaller = callVertex.getCaller().getConcreteType();
        CGNode caller = cg.findOrCreateNode(kaller.getMethod(AstMethodReference.fnSelector), Everywhere.EVERYWHERE);		    
		    CallSiteReference site = callVertex.getSite();
		    IMethod target = targetSelector.getCalleeTarget(caller, site, targetVertex.getConcreteType());
		    boolean isFunctionPrototypeCall = target != null
		        && target.getName().toString().startsWith(JavaScriptFunctionDotCallTargetSelector.SYNTHETIC_CALL_METHOD_PREFIX);
        boolean isFunctionPrototypeApply = target != null
            && target.getName().toString().startsWith(JavaScriptFunctionApplyTargetSelector.SYNTHETIC_APPLY_METHOD_PREFIX);

		    if (isFunctionPrototypeCall || isFunctionPrototypeApply) {
		      handleFunctionCallOrApplyInvocation(flowgraph, monitor, cg, callVertex, caller, site, target);
		    } else {
	          addEdgeToJSCallGraph(cg, site, target, caller);
		     
	          if (target instanceof JavaScriptConstructor) {
	            IMethod fun = ((JavaScriptConstructor)target).constructedType().getMethod(AstMethodReference.fnSelector);        
	            CGNode ctorCaller = cg.findOrCreateNode(target, Everywhere.EVERYWHERE);
	            
	            CallSiteReference ref = null;
	            Iterator<CallSiteReference> sites = ctorCaller.iterateCallSites();
	            while(sites.hasNext()) {
	              CallSiteReference r = sites.next();
	              if (r.getDeclaredTarget().getSelector().equals(AstMethodReference.fnSelector)) {
	                ref = r;
	                break;
	              }
	            }
	            
	            if (ref != null) {
	              addEdgeToJSCallGraph(cg, ref, fun, ctorCaller);	              
	            }
          } 
		    }
		  }

		return cg;
	}

  public boolean handleFunctionCallOrApplyInvocation(FlowGraph flowgraph, IProgressMonitor monitor, final JSCallGraph cg,
      CallVertex callVertex, CGNode caller, CallSiteReference site,
      IMethod target) throws CancelException {
    // use to get 1-level of call string for Function.prototype.call, to
    // preserve the precision of the field-based call graph
    final nCFAContextSelector functionPrototypeCallSelector = new nCFAContextSelector(1, new ContextInsensitiveSelector());
    Context calleeContext = functionPrototypeCallSelector.getCalleeTarget(caller, site, target, null);
    boolean ret = addCGEdgeWithContext(cg, site, target, caller, calleeContext);
    CGNode functionPrototypeCallNode = cg.findOrCreateNode(target, calleeContext);
    // need to create nodes for reflective targets of call, and then add them
    // as callees of the synthetic method
    OrdinalSet<FuncVertex> reflectiveTargets = getReflectiveTargets(flowgraph, callVertex, monitor);
    System.err.println("adding callees " + reflectiveTargets + " for " + caller);
    // there should only be one call site in the synthetic method
//    CallSiteReference reflectiveCallSite = cache.getIRFactory().makeIR(functionPrototypeCallNode.getMethod(), Everywhere.EVERYWHERE, options.getSSAOptions()).iterateCallSites().next();
    CallSiteReference reflectiveCallSite = functionPrototypeCallNode.getIR().iterateCallSites().next();
    for (FuncVertex f : reflectiveTargets) {
      IMethod reflectiveTgtMethod = targetSelector.getCalleeTarget(functionPrototypeCallNode, reflectiveCallSite, f.getConcreteType());
      ret |= addEdgeToJSCallGraph(cg, reflectiveCallSite, reflectiveTgtMethod, functionPrototypeCallNode);
    }
    return ret;
  }

  public boolean addEdgeToJSCallGraph(final JSCallGraph cg, CallSiteReference site, IMethod target, CGNode caller)
      throws CancelException {
    return addCGEdgeWithContext(cg, site, target, caller, Everywhere.EVERYWHERE);
  }

  Set<IClass> constructedTypes = HashSetFactory.make();
  
  Everywhere targetContext = Everywhere.EVERYWHERE;
  @SuppressWarnings("deprecation")
  private static boolean addCGEdgeWithContext(final JSCallGraph cg, CallSiteReference site, IMethod target, CGNode caller,
      Context targetContext) throws CancelException {
    boolean ret = false;
    if(target != null) {
      CGNode callee = cg.findOrCreateNode(target, targetContext);
    	// add nodes first, to be on the safe side
    	cg.addNode(caller);	
    	cg.addNode(callee);
    	// add callee as successor of caller
    	ret = !cg.getPossibleTargets(caller, site).contains(callee);
    	if (ret) {
    	  cg.addEdge(caller, callee);
    	  caller.addTarget(site, callee);
    	}
    }
    return ret;
  }

  /**
   * Given a callVertex that can invoke Function.prototype.call, get the
   * FuncVertex nodes for the reflectively-invoked methods
   * @throws CancelException 
   */
	private static OrdinalSet<FuncVertex> getReflectiveTargets(FlowGraph flowGraph, CallVertex callVertex, IProgressMonitor monitor) throws CancelException {
	  SSAAbstractInvokeInstruction invoke = callVertex.getInstruction();
	  VarVertex functionParam = flowGraph.getVertexFactory().makeVarVertex(callVertex.getCaller(), invoke.getUse(1));
	  return flowGraph.getReachingSet(functionParam, monitor);
  }

  @SuppressWarnings("unused")
  private static OrdinalSet<FuncVertex> getConstructorTargets(FlowGraph flowGraph, CallVertex callVertex, IProgressMonitor monitor) throws CancelException {
    SSAAbstractInvokeInstruction invoke = callVertex.getInstruction();
    assert invoke.getDeclaredTarget().getName().equals(JavaScriptMethods.ctorAtom);
    VarVertex objectParam = flowGraph.getVertexFactory().makeVarVertex(callVertex.getCaller(), invoke.getUse(0));
    return flowGraph.getReachingSet(objectParam, monitor);
  }
	
  /**
	 * Extract call edges from the flow graph into high-level representation.
	 */
	public Set<Pair<CallVertex, FuncVertex>> extractCallGraphEdges(FlowGraph flowgraph, IProgressMonitor monitor) throws CancelException {
		VertexFactory factory = flowgraph.getVertexFactory();
		final Set<Pair<CallVertex, FuncVertex>> result = HashSetFactory.make();
		
		// find all pairs <call, func> such that call is reachable from func in the flow graph
		for(final CallVertex callVertex : factory.getCallVertices()) {
			for(FuncVertex funcVertex : flowgraph.getReachingSet(callVertex, monitor)) {
			  result.add(Pair.make(callVertex, funcVertex));
			}
		}
		
		return result;
	}
}
