/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.flow.functions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.scandroid.domain.CodeElement;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.FieldElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.domain.InstanceKeyElement;
import org.scandroid.domain.LocalElement;
import org.scandroid.domain.ReturnElement;
import org.scandroid.flow.types.FlowType;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.IFlowFunction;
import com.ibm.wala.dataflow.IFDS.IFlowFunctionMap;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.OrdinalSet;


/**
 * @deprecated Replaced by TaintTransferFunctions.
 */
@Deprecated
public class IFDSTaintFlowFunctionProvider<E extends ISSABasicBlock>
implements IFlowFunctionMap<BasicBlockInContext<E>> {

	private final IFDSTaintDomain<E> domain;
	private final ISupergraph<BasicBlockInContext<E>,CGNode> graph;
	private final PointerAnalysis<InstanceKey> pa;

	public IFDSTaintFlowFunctionProvider(IFDSTaintDomain<E> domain,
			ISupergraph<BasicBlockInContext<E>, CGNode> graph, 
			PointerAnalysis<InstanceKey> pa)
	{
		this.domain = domain;
		this.graph = graph;
		this.pa = pa;
	}

	// instruction has a valid def set
	private static boolean inFlow(SSAInstruction instruction) {
		return  (instruction instanceof SSAArrayLoadInstruction) ||
				(instruction instanceof SSAGetInstruction);
	}

	// instruction's def is getUse(0)
	private static boolean outFlow(SSAInstruction instruction) {
		return (instruction instanceof SSAArrayStoreInstruction) ||
			   (instruction instanceof SSAPutInstruction) ||
			   (instruction instanceof SSAInvokeInstruction);
	}

	// instruction is a return instruction
	private static boolean returnFlow(SSAInstruction instruction) {
		return (instruction instanceof SSAReturnInstruction);
	}

	private static class UseDefSetPair
	{
		public Set<CodeElement> uses = HashSetFactory.make();
		public Set<CodeElement> defs = HashSetFactory.make();
	}

	private class DefUse implements IUnaryFlowFunction {
	
		private final List<UseDefSetPair> useToDefList = new ArrayList<>();

		private final BasicBlockInContext<E> bb;

		public DefUse(final BasicBlockInContext<E> inBlock) {
			
			this.bb = inBlock;

			for (SSAInstruction instruction : bb) {				
				handleInstruction(instruction);
			}
		}

		private void handleInstruction(SSAInstruction instruction) {
//			System.out.println("handle instruction: "+instruction);
			
			UseDefSetPair p = new UseDefSetPair();
			boolean thisToResult = false;
			if(instruction instanceof SSAInvokeInstruction)
			{
				thisToResult = handleInvokeInstruction(instruction, p);
			}
			if (thisToResult) {
				useToDefList.add(p);
				p = new UseDefSetPair();
			}
				
			IClassHierarchy ch = bb.getNode().getClassHierarchy();

			if (inFlow(instruction)) {
				handleInflowInstruction(instruction, p, ch);
			}
			else if (outFlow(instruction)) {
				handleOutflowInstruction(instruction, p, ch);
			}
			else if(returnFlow(instruction))
			{
				handleReturnFlowInstruction(instruction, p);
			}
			else
			{
				for (int i = 0; i < instruction.getNumberOfUses(); i++) {
					p.uses.addAll(CodeElement.valueElements(instruction.getUse(i)));
				}
				for (int j = 0; j < instruction.getNumberOfDefs(); j++) {
					p.defs.addAll(CodeElement.valueElements(instruction.getDef(j)));
				}
			}
			
			useToDefList.add(p);
		}

		private void handleReturnFlowInstruction(SSAInstruction instruction,
				UseDefSetPair p) {
			SSAReturnInstruction retInst = (SSAReturnInstruction)instruction;
			if(retInst.getNumberOfUses() > 0)
			{
				/* TODO: why not add instance keys, too? */
				for(int i = 0; i < instruction.getNumberOfUses(); i++)
				{
					//p.uses.add(new LocalElement(instruction.getUse(i)));
					p.uses.addAll(
						CodeElement.valueElements(instruction.getUse(i)));
					
				}
				p.defs.add(new ReturnElement());
			}
		}

		private boolean handleInvokeInstruction(SSAInstruction instruction,
				UseDefSetPair p) {
			boolean thisToResult;
			SSAInvokeInstruction invInst = (SSAInvokeInstruction)instruction;
			if(!invInst.isSpecial() && !invInst.isStatic() && instruction.getNumberOfDefs() > 0)
			{
				//System.out.println("adding receiver flow in "+this+" for "+invInst);
				//System.out.println("\tadding local element "+invInst.getReceiver());
				//getReceiver() == getUse(0) == param[0] == this
				p.uses.addAll(CodeElement.valueElements(invInst.getReceiver()));
				for(int i = 0; i < invInst.getNumberOfDefs(); i++)
				{
					//System.out.println("\tadding def local element "+invInst.getDef(i));
					//return valuenumber of invoke instruction
					p.defs.addAll(CodeElement.valueElements(invInst.getDef(i)));
				}
			}
			thisToResult = true;
			return thisToResult;
		}

		private void handleInflowInstruction(SSAInstruction instruction,
				UseDefSetPair p, IClassHierarchy ch) {
			if (instruction instanceof SSAGetInstruction) {
				handleInflowGetInstruction(instruction, p, ch);
			}
			else if (instruction instanceof SSAArrayLoadInstruction){
				handleInflowArrayLoadInstruction(instruction, p);
			}
		}

		private void handleOutflowInstruction(SSAInstruction instruction,
				UseDefSetPair p, IClassHierarchy ch) {
			if (instruction instanceof SSAPutInstruction) {
				handleOutflowPutInstruction(instruction, p, ch);
			}
			else if (instruction instanceof SSAArrayStoreInstruction){						
				handleOutflowArrayStoreInstruction(instruction, p);
			}
			else if (instruction instanceof SSAInvokeInstruction){
				
				handleOutflowInvokeInstruction(instruction, p);
			}
		}

		private void handleOutflowInvokeInstruction(SSAInstruction instruction,
				UseDefSetPair p) {
			MethodReference targetMethod = ((SSAInvokeInstruction) instruction).getCallSite().getDeclaredTarget();
			if (methodExcluded(targetMethod)) {
				// TODO make all parameters flow into all other 
				// parameters, which could happen in the static case as well.
				if (!((SSAInvokeInstruction) instruction).isStatic()) {
					// These loops cause all parameters flow into the 
					// 'this' param (due to instruction.getUse(0))
					for (int i = 1; i < instruction.getNumberOfUses(); i++) {
						p.uses.addAll(CodeElement.valueElements(instruction.getUse(i)));
					}
				

					if (instruction.getNumberOfUses() > 0) {
						p.defs.addAll(CodeElement.valueElements(instruction.getUse(0)));
					}
				}
			}
		}

		private void handleOutflowArrayStoreInstruction(
				SSAInstruction instruction, UseDefSetPair p) {
			p.uses.addAll(CodeElement.valueElements(instruction.getUse(2)));
			p.defs.addAll(CodeElement.valueElements(instruction.getUse(0)));
		}

		private void handleOutflowPutInstruction(SSAInstruction instruction,
				UseDefSetPair p, IClassHierarchy ch) {
			SSAPutInstruction pi = (SSAPutInstruction)instruction;
			PointerKey pk;
			Set<CodeElement> elements = HashSetFactory.make();
			if (pi.isStatic()) {
			    p.uses.addAll(CodeElement.valueElements(instruction.getUse(0)));
			    FieldReference declaredField = pi.getDeclaredField();
			    IField staticField = getStaticIField(ch, declaredField);
			    if (staticField == null) {
			    	pk = null;
			    } else {
			    	pk = new StaticFieldKey(staticField);
			    }
			} else {
			    p.uses.addAll(
			    		CodeElement.valueElements(instruction.getUse(1)));

			    // this value number seems to be the object referenced in this instruction (?)
				int valueNumber = instruction.getUse(0);
				pk = new LocalPointerKey(bb.getNode(), valueNumber);
				
				//MyLogger.log(LogLevel.DEBUG, " instruction: "+instruction);
				
				// add the object that holds the field that was modified
				// to the list of things tainted by this flow:
				p.defs.addAll(CodeElement.valueElements(valueNumber));
			}	
			// now add the field keys to the defs list so that they
			// are also tainted:
			if (pk!=null) {
				OrdinalSet<InstanceKey> m = pa.getPointsToSet(pk);
				if (m != null) {
					for (InstanceKey instanceKey : m) {
						elements.add(new FieldElement(instanceKey, pi.getDeclaredField()));
						elements.add(new InstanceKeyElement(instanceKey));
					}
				}
				p.defs.addAll(elements);
			}
		}

		private void handleInflowArrayLoadInstruction(
				SSAInstruction instruction, UseDefSetPair p) {
			p.uses.addAll(CodeElement.valueElements(instruction.getUse(0)));
			p.defs.addAll(CodeElement.valueElements(instruction.getDef()));
		}

		private void handleInflowGetInstruction(SSAInstruction instruction,
				UseDefSetPair p, IClassHierarchy ch) {
			SSAGetInstruction gi = (SSAGetInstruction)instruction;
			
			PointerKey pk;
			FieldReference declaredField = gi.getDeclaredField();
			if ( gi.isStatic()) {
			    IField staticField =
			            getStaticIField(ch, declaredField);
			    
			    if (staticField == null) {
			    	pk = null;
			    } else {
			    	pk = new StaticFieldKey(staticField);
			    }
			} else {
			    int valueNumber = instruction.getUse(0);
			    pk = new LocalPointerKey(bb.getNode(), valueNumber);
			}
			
			if (pk!=null) {
				Set<CodeElement> elements = HashSetFactory.make();
				OrdinalSet<InstanceKey> m = pa.getPointsToSet(pk);
				if(m != null) {
					for (InstanceKey instanceKey : m) {
						elements.add(new FieldElement(instanceKey, declaredField));
						elements.add(new InstanceKeyElement(instanceKey));
					}
				}
				p.uses.addAll(elements);
				//getinstruction only has 1 def
				p.defs.add(new LocalElement(instruction.getDef(0)));
			}
		}

		/**
		 * Determines if the provide method is in the exclusions by checking the supergraph.
		 * @param method
		 * @return True if the method can not be found in the supergraph.
		 */
        private boolean methodExcluded(MethodReference method) {
        	Collection<IMethod> iMethods = pa.getClassHierarchy().getPossibleTargets(method);
			return 0 == iMethods.size();
		}

		private IField getStaticIField(IClassHierarchy ch,
                FieldReference declaredField) {
            TypeReference staticTypeRef = declaredField.getDeclaringClass();
            
            IClass staticClass = ch.lookupClass(staticTypeRef);
            
            //referring to a static field which we don't have loaded in the class hierarchy
            //possibly ignored in the exclusions file or just not included in the scope
            if (staticClass == null)
            	return null;

            IField staticField = 
                    staticClass.getField(declaredField.getName());
            return staticField;
        }

		private void addTargets(CodeElement d1, MutableIntSet set, FlowType<E> taintType)
		{
			//System.out.println(this.toString()+".addTargets("+d1+"...)");
			for(UseDefSetPair p: useToDefList)
			{
				if(p.uses.contains(d1))
				{
					//System.out.println("\t\tfound pair that uses "+d1);
					for(CodeElement i:p.defs)
					{
						//System.out.println("\t\tadding outflow "+i);
						set.add(domain.getMappedIndex(new DomainElement(i,taintType)));
					}
				}
			}
		}	

		@Override
		@SuppressWarnings("unchecked")
		public IntSet getTargets(int d1) {
			//System.out.println(this.toString()+".getTargets("+d1+") "+bb);
			//BitVectorIntSet set = new BitVectorIntSet();
			MutableSparseIntSet set = MutableSparseIntSet.makeEmpty();
			set.add(d1);
			DomainElement de = domain.getMappedObject(d1);
			if (de != null) {
				addTargets(de.codeElement, set, de.taintSource);
			}
			return set;
		}
	}

	@Override
	public IUnaryFlowFunction getCallFlowFunction(
			BasicBlockInContext<E> src,
			BasicBlockInContext<E> dest,
			BasicBlockInContext<E> ret) {
		assert graph.isCall(src);

		final SSAInvokeInstruction instruction = (SSAInvokeInstruction) src.getLastInstruction();
		
//		String signature = dest.getMethod().getSignature();
//		if ( dest.getMethod().isSynthetic() ) { 
//			System.out.println("Synthetic: "+signature);
//		} else {
//			System.err.println(signature);
//		}
		
		
//		if ( LoaderUtils.fromLoader(src.getNode(), ClassLoaderReference.Application)
//		  && LoaderUtils.fromLoader(dest.getNode(), ClassLoaderReference.Primordial)) {
//			System.out.println("Call to system: "+signature);
//		}
		
//		if (! dest.getMethod().isSynthetic() 
//		    && LoaderUtils.fromLoader(dest.getNode(), ClassLoaderReference.Primordial)) {
//		    
//            MyLogger.log(DEBUG,"Primordial and No Summary! (getCallFlowFunction) - " + dest.getMethod().getReference());
//		}

		final Map<CodeElement,CodeElement> parameterMap = HashMapFactory.make();
		for (int i = 0; i < instruction.getNumberOfParameters(); i++) {
			Set<CodeElement> elements = CodeElement.valueElements(instruction.getUse(i));
			for(CodeElement e: elements) {
				parameterMap.put(e, new LocalElement(i+1));
			}
		}

		return d1 -> {
			BitVectorIntSet set = new BitVectorIntSet();
			if(d1 == 0 || !(domain.getMappedObject(d1).codeElement instanceof LocalElement)) {
				set.add(d1);
			}
			DomainElement de = domain.getMappedObject(d1);
			if(de!=null && parameterMap.containsKey(de.codeElement))
				set.add(domain.getMappedIndex(new DomainElement(parameterMap.get(de.codeElement),de.taintSource)));
			return set;
		};
	}

	@Override
	public IUnaryFlowFunction getCallNoneToReturnFlowFunction(
			BasicBlockInContext<E> src,
			BasicBlockInContext<E> dest) {
		//I Believe this method is called only if there are no callees of src in the supergraph
		//if supergraph included all primordials, this method can still be called if it calls a 		
		//method that wasn't included in the scope
		
		//Assertions.UNREACHABLE();
		// TODO: Look up summary for this method, or warn if it doesn't exist.
		assert (src.getNode().equals(dest.getNode()));
		
//		final SSAInvokeInstruction instruction = (SSAInvokeInstruction) src.getLastInstruction();

//		System.out.println("call to return(no callee) method inside call graph: " + src.getNode()+"--" + instruction.getDeclaredTarget());
		// System.out.println("call to system: " + instruction.getDeclaredTarget());
		return new DefUse(dest);
	}

	@Override
	public IUnaryFlowFunction getCallToReturnFlowFunction(
			BasicBlockInContext<E> src,
			BasicBlockInContext<E> dest) {
		assert (src.getNode().equals(dest.getNode()));
		//final SSAInvokeInstruction instruction = (SSAInvokeInstruction) src.getLastInstruction();
		//System.out.println("call to return method inside call graph: " + instruction.getDeclaredTarget());

		return new DefUse(dest);
	}

	@Override
	public IUnaryFlowFunction getNormalFlowFunction(
			BasicBlockInContext<E> src,
			BasicBlockInContext<E> dest) {
		assert (src.getNode().equals(dest.getNode()));
		//System.out.println("getNormalFlowFuntion");
		//System.out.println("\tSrc " + src.getLastInstruction());
		//System.out.println("\tDest " + dest.getLastInstruction());
		return new DefUse(dest);
	}

	public class ReturnDefUse extends DefUse
	{
		CodeElement callSet;
		Set<CodeElement> receivers = new HashSet<>();

		public ReturnDefUse(BasicBlockInContext<E> dest,
				BasicBlockInContext<E> call) {
			super(dest);
			
			// TODO: look into exception handling through getDef(1)
			if(call.getLastInstruction() instanceof SSAInvokeInstruction) {
				SSAInvokeInstruction invInst = (SSAInvokeInstruction) call.getLastInstruction();
				if(!invInst.isSpecial()) {// && !invInst.isStatic()) {
//					for (int i = 0; i < invInst.getNumberOfReturnValues(); i++) {
//						
//					}
					if (invInst.hasDef()) {
						callSet = new LocalElement(invInst.getReturnValue(0));

						if ( !invInst.isStatic() ) {
							//used to be invInst.getReceiver(), but I believe that was incorrect.
							receivers.addAll(CodeElement.valueElements(invInst.getReceiver()));
							//receivers.addAll(CodeElement.valueElements(pa, call.getNode(), invInst.getReturnValue(0)));
						}
					}
				}				
			}
			else {
				callSet = null;
			}
//			// TODO: look into exception handling through getDef(1)
//			if(call.getLastInstruction().getNumberOfDefs() == 1)
//			{
//				//System.out.println("\treturn defines something: "+call.getLastInstruction());
//				callSet = new LocalElement(call.getLastInstruction().getDef(0));
//				if(call.getLastInstruction() instanceof SSAInvokeInstruction)
//				{
//					SSAInvokeInstruction invInst = (SSAInvokeInstruction) call.getLastInstruction();
//					if(!invInst.isSpecial() && !invInst.isStatic()) {
//						receivers.addAll(CodeElement.valueElements(pa, call.getNode(), invInst.getReceiver()));
//					}
//				}
//			}
//			else
//				callSet = null;
		}

		@Override
		public IntSet getTargets(int d1)
		{
			if(d1 != 0 && domain.getMappedObject(d1).codeElement instanceof ReturnElement)
			{
				BitVectorIntSet set = new BitVectorIntSet();
				if(callSet != null) {
//					System.out.println("callset: " + callSet);
					set.add(domain.getMappedIndex(new DomainElement(callSet,domain.getMappedObject(d1).taintSource)));
				}
				return set;
			}
			else if(d1 != 0 && domain.getMappedObject(d1).codeElement instanceof LocalElement)
			{
				return new BitVectorIntSet();
			}
			else if(d1 != 0 && receivers.contains(domain.getMappedObject(d1).codeElement))
			{
				BitVectorIntSet set = new BitVectorIntSet();
				if(callSet != null)
					set.add(domain.getMappedIndex(new DomainElement(callSet,domain.getMappedObject(d1).taintSource)));
				set.addAll(super.getTargets(d1));
				return set;
			}
			else
			{
				return super.getTargets(d1);
			}
		}
	}

	@Override
	public IFlowFunction getReturnFlowFunction(BasicBlockInContext<E> call,
			BasicBlockInContext<E> src,
			BasicBlockInContext<E> dest) {
		assert (graph.isCall(call) && graph.isReturn(dest) && call.getNode().equals(dest.getNode()));
		//final SSAInvokeInstruction instruction = (SSAInvokeInstruction) call.getLastInstruction();

		//System.out.println("Return from call to method inside call graph: " + instruction.getDeclaredTarget());

		return new ReturnDefUse(dest,call);
	}

}
