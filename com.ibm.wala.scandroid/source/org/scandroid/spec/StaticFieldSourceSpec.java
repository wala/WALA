/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, 
 *                Rogan Creswick <creswick@galois.com>, 
 *                Adam Foltzer <acfoltzer@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
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

package org.scandroid.spec;

import java.util.Map;
import java.util.Set;

import org.scandroid.domain.CodeElement;
import org.scandroid.domain.InstanceKeyElement;
import org.scandroid.domain.StaticFieldElement;
import org.scandroid.flow.InflowAnalysis;
import org.scandroid.flow.types.FlowType;
import org.scandroid.flow.types.StaticFieldFlow;
import org.scandroid.util.CGAnalysisContext;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.dataflow.IFDS.ISupergraph;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * @author creswick
 *
 */
public class StaticFieldSourceSpec extends SourceSpec {

	private final IField field;

	public StaticFieldSourceSpec(IField field) {
		this.field = field;
		argNums = null;
	}

	/* (non-Javadoc)
	 * @see org.scandroid.spec.SourceSpec#addDomainElements(java.util.Map, com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.cfg.BasicBlockInContext, com.ibm.wala.ssa.SSAInvokeInstruction, int[], com.ibm.wala.dataflow.IFDS.ISupergraph, com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis, com.ibm.wala.ipa.callgraph.CallGraph)
	 */
	@Override
	public <E extends ISSABasicBlock> void addDomainElements(
			CGAnalysisContext<E> ctx,
			Map<BasicBlockInContext<E>, Map<FlowType<E>, Set<CodeElement>>> taintMap,
			IMethod im, 
			BasicBlockInContext<E> block,
			SSAInvokeInstruction invInst, 
			int[] newArgNums,
			ISupergraph<BasicBlockInContext<E>, CGNode> graph,
			PointerAnalysis<InstanceKey> pa, 
			CallGraph cg) {

		Set<CodeElement> valueElements = HashSetFactory.make();
		valueElements.add(new StaticFieldElement(field.getReference()));
		FlowType<E> flow = new StaticFieldFlow<>(block, field, true);
		
		TypeReference typeRef = field.getFieldTypeReference();
		
		if (typeRef.isPrimitiveType()) {
			InflowAnalysis.addDomainElements(taintMap, block, flow, valueElements);
			return;
		}
		
		// else, handle reference types:
		
		PointerKey pk = pa.getHeapModel().getPointerKeyForStaticField(field);
		OrdinalSet<InstanceKey> pointsToSet = pa.getPointsToSet(pk);
		
		if (pointsToSet.isEmpty()) {
			IClassHierarchy cha = im.getClassHierarchy();
			if (null == cha.lookupClass(typeRef)) {
				
				return;
			}
			if (cha.isInterface(typeRef)) {
				// TODO we could find all implementations of the interface, and add a concrete type key for each.
				// we aren't doing that yet.
				InflowAnalysis.addDomainElements(taintMap, block, flow, valueElements);
				return;
			}
			
			IClass clazz = cha.lookupClass(typeRef);
			if (null == clazz) {
				
			} else {
				InstanceKey ik = new ConcreteTypeKey(clazz);
				valueElements.add(new InstanceKeyElement(ik));
			}					
		}
		
		for (InstanceKey ik : pointsToSet) {
			valueElements.add(new InstanceKeyElement(ik));
		}
		InflowAnalysis.addDomainElements(taintMap, block, flow, valueElements);
	}

	@Override
	public String toString() {
		return "StaticFieldSourceSpec [field=" + field + "]";
	}
}
