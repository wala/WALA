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
package org.scandroid.flow;

import java.util.Collections;
import java.util.Set;

import org.scandroid.domain.CodeElement;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.domain.LocalElement;
import org.scandroid.flow.types.FlowType;
import org.scandroid.util.CGAnalysisContext;

import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;

public class LocalSinkPoint implements ISinkPoint {
	
	private final BasicBlockInContext<IExplodedBasicBlock> block;
	private final int ssaVal;
	private final FlowType<IExplodedBasicBlock> sinkFlow;

	public LocalSinkPoint(BasicBlockInContext<IExplodedBasicBlock> block,
			int ssaVal, FlowType<IExplodedBasicBlock> sinkFlow) {
		this.block = block;
		this.ssaVal = ssaVal;
		this.sinkFlow = sinkFlow;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Set<FlowType<IExplodedBasicBlock>> findSources(CGAnalysisContext<IExplodedBasicBlock> ctx,
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain) {
		Set<FlowType<IExplodedBasicBlock>> sources = HashSetFactory.make();

		final CodeElement localElt = new LocalElement(ssaVal);
		Set<CodeElement> elts = HashSetFactory.make(Collections.singleton(localElt));

		final CGNode node = block.getNode();
		PointerKey pk = ctx.pa.getHeapModel().getPointerKeyForLocal(node,
				ssaVal);
		OrdinalSet<InstanceKey> iks = ctx.pa.getPointsToSet(pk);

		for (InstanceKey ik : iks) {
			elts.addAll(ctx.codeElementsForInstanceKey(ik));
		}

		for (CodeElement elt : elts) {
			for (DomainElement de : domain.getPossibleElements(elt)) {
				if (flowResult.getResult(block).contains(
						domain.getMappedIndex(de))) {
					sources.add(de.taintSource);
				}
			}
		}
		return sources;
	}
	
	@Override
	public FlowType<IExplodedBasicBlock> getFlow() {
		return sinkFlow;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((block == null) ? 0 : block.hashCode());
		result = prime * result
				+ ((sinkFlow == null) ? 0 : sinkFlow.hashCode());
		result = prime * result + ssaVal;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		LocalSinkPoint other = (LocalSinkPoint) obj;
		if (block == null) {
			if (other.block != null)
				return false;
		} else if (!block.equals(other.block))
			return false;
		if (sinkFlow == null) {
			if (other.sinkFlow != null)
				return false;
		} else if (!sinkFlow.equals(other.sinkFlow))
			return false;
		if (ssaVal != other.ssaVal)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SinkPoint [block=" + block + ", ssaVal=" + ssaVal
				+ ", sinkFlow=" + sinkFlow + "]";
	}

}
