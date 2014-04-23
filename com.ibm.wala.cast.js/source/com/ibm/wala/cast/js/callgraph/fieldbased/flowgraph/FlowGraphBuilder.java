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
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph;

import java.util.Iterator;

import com.ibm.wala.cast.ir.ssa.AstGlobalRead;
import com.ibm.wala.cast.ir.ssa.AstGlobalWrite;
import com.ibm.wala.cast.ir.ssa.AstLexicalAccess.Access;
import com.ibm.wala.cast.ir.ssa.AstLexicalRead;
import com.ibm.wala.cast.ir.ssa.AstLexicalWrite;
import com.ibm.wala.cast.js.callgraph.fieldbased.JSMethodInstructionVisitor;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.FuncVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VarVertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.Vertex;
import com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices.VertexFactory;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyRead;
import com.ibm.wala.cast.js.ssa.JavaScriptPropertyWrite;
import com.ibm.wala.cast.js.ssa.PrototypeLookup;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.js.util.Util;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

/**
 * Class for building intra-procedural flow graphs for a given class hierarchy.
 * 
 * @author mschaefer
 */
public class FlowGraphBuilder {
	private final IClassHierarchy cha;
	private final AnalysisCache cache;
	
	public FlowGraphBuilder(IClassHierarchy cha, AnalysisCache cache) {
		this.cha = cha;
		this.cache = cache;
	}
	
	/**
	 * This is the main entry point of the flow graph builder.
	 * 
	 * <p>
	 * It creates a new, empty flow graph, adds nodes for a small number of special primitive
	 * functions such as <code>Object</code> and <code>Function</code> and sets up flow
	 * edges to make them flow into the corresponding global variables. Then it iterates over
	 * all functions in the class hierarchy and all their IR instructions, and adds the
	 * flow edges induced by these instructions.
	 * </p>
	 * 
	 * @return the completed flow graph
	 */
	public FlowGraph buildFlowGraph() {
		FlowGraph flowgraph = new FlowGraph();
		
		addPrimitives(flowgraph);
		
		visitProgram(flowgraph);
					
		return flowgraph;
	}

  protected void visitProgram(FlowGraph flowgraph) {
    for(IClass klass : cha) {
			for(IMethod method : klass.getDeclaredMethods()) {
				if(method.getDescriptor().equals(AstMethodReference.fnDesc))
          visitFunction(flowgraph, method);
			}
		}
  }

  protected void visitFunction(FlowGraph flowgraph, IMethod method) {
    {
    	IR ir = cache.getIR(method);
    	FlowGraphSSAVisitor visitor = new FlowGraphSSAVisitor(ir, flowgraph);

    	// first visit normal instructions
    	SSAInstruction[] normalInstructions = ir.getInstructions();
    	for(int i=0;i<normalInstructions.length;++i)
    		if(normalInstructions[i] != null) {
    			visitor.instructionIndex  = i;
    			normalInstructions[i].visit(visitor);
    		}
    	
    	// now visit phis and catches
    	visitor.instructionIndex = -1;
    	for(Iterator<? extends SSAInstruction> iter=ir.iteratePhis();iter.hasNext();)
    		iter.next().visit(visitor);
    	
    	for(Iterator<SSAInstruction> iter=ir.iterateCatchInstructions();iter.hasNext();)
    		iter.next().visit(visitor);
    }
  }
	
	// primitive functions that are treated specially
	private static String[] primitiveFunctions = { "Object", "Function", "Array", "StringObject", "NumberObject", "BooleanObject", "RegExp" };
	
	/**
	 * Add flows from the special primitive functions to the corresponding global variables.
	 * 
	 * @param flowgraph the flow graph under construction
	 */
	private void addPrimitives(FlowGraph flowgraph) {
		VertexFactory factory = flowgraph.getVertexFactory();
		for(String pf : primitiveFunctions) {
			TypeReference typeref = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, "L" + pf);
			IClass klass = cha.lookupClass(typeref);
			String prop = pf.endsWith("Object")? pf.substring(0, pf.length() - 6): pf;
			flowgraph.addEdge(factory.makeFuncVertex(klass), factory.makePropVertex(prop));
		}
	}
	
	/**
	 * Visitor class that does the heavy lifting (such as it is) of flow graph construction, adding flow graph
	 * edges for every instruction in the method IR.
	 * 
	 * <p>
	 * The only slightly tricky thing are assignments to exposed variables inside their defining function. In
	 * the IR, they initially appear as normal SSA variable assignments, without any indication of their lexical
	 * nature. The normal call graph construction logic does something convoluted to fix this up later when
	 * an actual lexical access is encountered.
	 * </p>
	 * 
	 * <p>
	 * We use a much simpler approach. Whenever we see an assignment <code>v<sub>i</sub> = e</code>, we ask the
	 * enclosing function whether <code>v<sub>i</sub></code> is an exposed variable. If it is, we determine its
	 * source-level names <code>x<sub>1</sub>, x<sub>2</sub>, ..., x<sub>n</sub></code>, and then add edges
	 * corresponding to lexical writes of <code>v<sub>i</sub></code> into all the <code>x<sub>j</sub></code>.
	 * </p>
	 * 
	 * @author mschaefer
	 */
	private class FlowGraphSSAVisitor extends JSMethodInstructionVisitor {
		// index of the instruction currently visited; -1 if the instruction isn't a normal instruction
		public int instructionIndex = -1;
		
		// flow graph being built
		private final FlowGraph flowgraph;
		
		// vertex factory to use for constructing new vertices
		private final VertexFactory factory;
		
		// lexical information about the current function
		private final LexicalInformation lexicalInfo;
		
		// the set of SSA variables in the current function that are accessed by nested functions
		private final IntSet exposedVars;
		
		// the IR of the current function
		private final IR ir;
		
		// the function vertex corresponding to the current function
		private final FuncVertex func;
		
		public FlowGraphSSAVisitor(IR ir, FlowGraph flowgraph) {
			super(ir.getMethod(), ir.getSymbolTable(), cache.getDefUse(ir));
			this.ir = ir;
			this.flowgraph = flowgraph;
			this.factory = flowgraph.getVertexFactory();
			this.func = this.factory.makeFuncVertex(ir.getMethod().getDeclaringClass());
			if(method instanceof AstMethod) {
				this.lexicalInfo = ((AstMethod)method).lexicalInfo();
				this.exposedVars = lexicalInfo.getAllExposedUses();
			} else {
				this.lexicalInfo = null;
				this.exposedVars = EmptyIntSet.instance;
			}
		}
		
		// add extra flow from v_def to every lexical variable it may correspond to at source-level
		private void handleLexicalDef(int def) {
			assert def != -1;
			if(instructionIndex != -1 && exposedVars.contains(def)) {
				VarVertex v = factory.makeVarVertex(func, def);
				for(String localName : ir.getLocalNames(instructionIndex, def))
					flowgraph.addEdge(v, factory.makeLexicalAccessVertex(lexicalInfo.getScopingName(), localName));
			}
		}
		
		@Override
		public void visitPhi(SSAPhiInstruction phi) {
			int n = phi.getNumberOfUses();
			VarVertex w = factory.makeVarVertex(func, phi.getDef());
			for(int i=0;i<n;++i) {
				VarVertex v = factory.makeVarVertex(func, phi.getUse(i));
				flowgraph.addEdge(v, w);
			}
		}
		
		@Override
		public void visitPrototypeLookup(PrototypeLookup proto) {
			// treat it simply as an assignment
			flowgraph.addEdge(factory.makeVarVertex(func, proto.getUse(0)),
							          factory.makeVarVertex(func, proto.getDef()));
			handleLexicalDef(proto.getDef());
		}
		
		@Override
		public void visitPut(SSAPutInstruction put) {
			String propName = put.getDeclaredField().getName().toString();
			
			// hack to account for global variables
			if(propName.startsWith("global "))
				propName = propName.substring("global ".length());
			
			Vertex v = factory.makeVarVertex(func, put.getVal()),
			       w = factory.makePropVertex(propName);
			flowgraph.addEdge(v, w);
		}
		
		@Override
		public void visitAstGlobalWrite(AstGlobalWrite instruction) {
			visitPut(instruction);
		}
		
		@Override
		public void visitJavaScriptPropertyWrite(JavaScriptPropertyWrite pw) {
			int p = pw.getMemberRef();
			if(symtab.isConstant(p)) {
				String pn = symtab.getValue(p)+"";
				if(pn.charAt(0) == '#')
				  pn = pn.substring(1);
				
				Vertex v = factory.makeVarVertex(func, pw.getValue()),
				       w = factory.makePropVertex(pn);
				flowgraph.addEdge(v, w);
			}
		}
		
		@Override
		public void visitAstLexicalWrite(AstLexicalWrite lw) {
			for(Access acc : lw.getAccesses()) {
				Vertex v = factory.makeVarVertex(func, acc.valueNumber),
					   w = factory.makeLexicalAccessVertex(acc.variableDefiner, acc.variableName);
				flowgraph.addEdge(v, w);
			}
		}
		
		@Override
		public void visitGet(SSAGetInstruction get) {
			String propName = get.getDeclaredField().getName().toString();
			if(propName.startsWith("global "))
				propName = propName.substring("global ".length());

			Vertex v = factory.makePropVertex(propName),
				   w = factory.makeVarVertex(func, get.getDef());
			flowgraph.addEdge(v, w);
			handleLexicalDef(get.getDef());
		}
		
		@Override
		public void visitAstGlobalRead(AstGlobalRead instruction) {
			visitGet(instruction);
		}
		
		@Override
		public void visitJavaScriptPropertyRead(JavaScriptPropertyRead pr) {
			int p = pr.getMemberRef();
			if(symtab.isConstant(p)) {
				String pn = symtab.getValue(p)+"";
				if(pn.charAt(0) == '#')
				  pn = pn.substring(1);
				Vertex v = factory.makePropVertex(pn),
					   w = factory.makeVarVertex(func, pr.getDef());
				flowgraph.addEdge(v, w);
			}
			
			IntSet argVns = Util.getArgumentsArrayVns(ir, du);
			if (argVns.contains(pr.getObjectRef())) {
			  Vertex v = factory.makeArgVertex(func),
            w = factory.makeVarVertex(func, pr.getDef());
       flowgraph.addEdge(v, w);
			}
			
			handleLexicalDef(pr.getDef());
		}
		
		@Override
		public void visitAstLexicalRead(AstLexicalRead lr) {
			for(Access acc : lr.getAccesses()) {
				Vertex v = factory.makeLexicalAccessVertex(acc.variableDefiner, acc.variableName),
					   w = factory.makeVarVertex(func, acc.valueNumber);
				flowgraph.addEdge(v, w);
				handleLexicalDef(acc.valueNumber);
			}
		}
		
		@Override
		public void visitReturn(SSAReturnInstruction ret) {
			Vertex v = factory.makeVarVertex(func, ret.getResult()),
				   w = factory.makeRetVertex(func);
			flowgraph.addEdge(v, w);
		}
		
		@Override
		public void visitThrow(SSAThrowInstruction thr) {
			Vertex v = factory.makeVarVertex(func, thr.getException()),
				   w = factory.makeUnknownVertex();
			flowgraph.addEdge(v, w);
		}
		
		@Override
		public void visitGetCaughtException(SSAGetCaughtExceptionInstruction katch) {
			Vertex v = factory.makeUnknownVertex(),
				   w = factory.makeVarVertex(func, katch.getDef());
			flowgraph.addEdge(v, w);
		}
		
		@Override
		public void visitJavaScriptInvoke(JavaScriptInvoke invk) {
			flowgraph.addEdge(factory.makeUnknownVertex(),
							  factory.makeVarVertex(func, invk.getException()));
			
			// check whether this invoke corresponds to a function expression/declaration
			if(isFunctionConstructorInvoke(invk)) {
				// second parameter is function name
				String fn_name = symtab.getStringValue(invk.getUse(1));
				
				// find the function being defined here
				IClass klass = cha.lookupClass(TypeReference.findOrCreate(JavaScriptTypes.jsLoader, fn_name));
				if (klass == null) {
				  System.err.println("cannot find " + fn_name + " at " +  ((AstMethod)ir.getMethod()).getSourcePosition(ir.getCallInstructionIndices(invk.getCallSite()).intIterator().next()));
				  return;
				}
				IMethod fn = klass.getMethod(AstMethodReference.fnSelector);
				FuncVertex fnVertex = factory.makeFuncVertex(klass);
				
				// function flows into its own v1 variable 
				flowgraph.addEdge(fnVertex, factory.makeVarVertex(fnVertex, 1));
				
				// flow parameters into local variables
				for(int i=0;i<fn.getNumberOfParameters()-1;++i)
					flowgraph.addEdge(factory.makeParamVertex(fnVertex, i), factory.makeVarVertex(fnVertex, i+2));
				
				// flow function into result variable
				flowgraph.addEdge(fnVertex, factory.makeVarVertex(func, invk.getDef()));
				
				// flow callee variable into callee vertex
				flowgraph.addEdge(factory.makeVarVertex(func, invk.getFunction()),
								          factory.makeCallVertex(func, invk));
			} else {
				// check whether it is a method call
				if(invk.getDeclaredTarget().equals(JavaScriptMethods.dispatchReference)) {
					// we only handle method calls with constant names
					if(symtab.isStringConstant(invk.getFunction()))
						// flow callee property into callee vertex
						flowgraph.addEdge(factory.makePropVertex(symtab.getStringValue(invk.getFunction())),
										  factory.makeCallVertex(func, invk));
				} else {
					// this case is simpler: just flow callee variable into callee vertex
					flowgraph.addEdge(factory.makeVarVertex(func, invk.getFunction()),
									  factory.makeCallVertex(func, invk));
				}
			}
			handleLexicalDef(invk.getDef());
		}
	}
}
