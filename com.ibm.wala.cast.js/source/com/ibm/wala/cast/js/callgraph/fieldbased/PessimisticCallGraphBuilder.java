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

import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.FlowGraph;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.FuncVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VertexFactory;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.MonitorUtil.IProgressMonitor;
import com.ibm.wala.util.collections.Iterator2Iterable;

/**
 * Call graph builder for building pessimistic call graphs, where inter-procedural flows are not
 * tracked except in the trivial case of local calls. This builder is fast, but in general less
 * sound than {@link OptimisticCallgraphBuilder}.
 * 
 * @author mschaefer
 *
 */
public class PessimisticCallGraphBuilder extends FieldBasedCallGraphBuilder {
	public PessimisticCallGraphBuilder(IClassHierarchy cha,	AnalysisOptions options, IAnalysisCacheView iAnalysisCacheView, boolean supportFullPointerAnalysis) {
		super(cha, options, iAnalysisCacheView, supportFullPointerAnalysis);
	}

	@Override
	public FlowGraph buildFlowGraph(IProgressMonitor monitor) {
	  FlowGraph flowgraph = flowGraphFactory();
	  resolveLocalCalls(flowgraph);
		return flowgraph;
	}

	protected boolean filterFunction(IMethod function) {
	  return function.getDescriptor().equals(AstMethodReference.fnDesc);
	}
	
	// add inter-procedural flow for local calls
	private void resolveLocalCalls(FlowGraph flowgraph) {
		for(IClass klass : cha) {
			for(IMethod method : klass.getDeclaredMethods()) {
				if (filterFunction(method)) {
					IR ir = cache.getIR(method);
					ir.visitAllInstructions(new LocalCallSSAVisitor(method, ir.getSymbolTable(), cache.getDefUse(ir), flowgraph));
				}
			}
		}
	}

	/**
	 * This visitor looks for calls where the callee can be determined locally by a def-use graph, and adds
	 * inter-procedural edges.
	 * 
	 * @author mschaefer
	 *
	 */
	private class LocalCallSSAVisitor extends JSMethodInstructionVisitor {
		private final FlowGraph flowgraph;
		private final VertexFactory factory;
		private final FuncVertex caller;
		
		public LocalCallSSAVisitor(IMethod method, SymbolTable symtab, DefUse du, FlowGraph flowgraph) {
			super(method, symtab, du);
			this.flowgraph = flowgraph;
			this.factory = flowgraph.getVertexFactory();
			this.caller = this.factory.makeFuncVertex(method.getDeclaringClass());
		}
		
		@Override
		public void visitJavaScriptInvoke(JavaScriptInvoke invk) {
			// check whether this instruction corresponds to a function expression/declaration
			if(isFunctionConstructorInvoke(invk)) {
				int defn = invk.getDef();
				
				// the name of the function
				String fnName = symtab.getStringValue(invk.getUse(1));
				IClass fnClass = cha.lookupClass(TypeReference.findOrCreate(JavaScriptTypes.jsLoader, fnName));
        if (fnClass == null) {
          System.err.println("cannot find " + fnName + " at " +  ((AstMethod)method).getSourcePosition());
          return;
        }
				IMethod fn = fnClass.getMethod(AstMethodReference.fnSelector);
				FuncVertex callee = factory.makeFuncVertex(fnClass);
				
				// look at all uses
				for(SSAInstruction use : Iterator2Iterable.make(du.getUses(defn))) {
					
					// check whether this is a local call
					if(use instanceof JavaScriptInvoke && ((JavaScriptInvoke)use).getFunction() == defn) {
						JavaScriptInvoke use_invk = (JavaScriptInvoke)use;
						
						// yes, so add edges from arguments to parameters...
						for(int i=2;i<use_invk.getNumberOfParameters();++i)
							flowgraph.addEdge(factory.makeVarVertex(caller, use_invk.getUse(i)), factory.makeParamVertex(callee, i));
						
						// ...and from return to result
						flowgraph.addEdge(factory.makeRetVertex(callee), factory.makeVarVertex(caller, use.getDef()));
						
						// note: local calls are never qualified, so there is no flow into the receiver vertex
					} else {
						// no, it's a more complicated use, so add flows from/to unknown
						for(int i=1;i<fn.getNumberOfParameters();++i)
							flowgraph.addEdge(factory.makeUnknownVertex(), factory.makeParamVertex(callee, i));
						flowgraph.addEdge(factory.makeRetVertex(callee), factory.makeUnknownVertex());
					}
				}
			} else {
				// this is a genuine function call; find out where the function came from
				SSAInstruction def = du.getDef(invk.getFunction());
				
				// if it's not a local call, add flows from/to unknown
				if(!(def instanceof JavaScriptInvoke) || !isFunctionConstructorInvoke((JavaScriptInvoke)def)) {
					for(int i=1;i<invk.getNumberOfParameters();++i)
						flowgraph.addEdge(factory.makeVarVertex(caller, invk.getUse(i)), factory.makeUnknownVertex());
					flowgraph.addEdge(factory.makeUnknownVertex(), factory.makeVarVertex(caller, invk.getDef()));
				}
			}
		}
	}
}
