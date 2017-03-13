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

import java.util.Set;

import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;
import org.scandroid.domain.StaticFieldElement;
import org.scandroid.flow.types.FlowType;
import org.scandroid.flow.types.StaticFieldFlow;
import org.scandroid.spec.StaticFieldSinkSpec;
import org.scandroid.util.CGAnalysisContext;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.dataflow.IFDS.TabulationResult;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cfg.BasicBlockInContext;
import com.ibm.wala.ssa.analysis.IExplodedBasicBlock;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * @author acfoltzer
 * 
 */
public class StaticFieldSinkPoint implements ISinkPoint {
//	private static final Logger logger = LoggerFactory
//			.getLogger(StaticFieldSinkPoint.class);

	private final IField field;
	private final FlowType<IExplodedBasicBlock> flow;
	private final BasicBlockInContext<IExplodedBasicBlock> block;

	public StaticFieldSinkPoint(StaticFieldSinkSpec spec,
			BasicBlockInContext<IExplodedBasicBlock> block) {
		this.field = spec.getField();
		this.block = block;
		this.flow = new StaticFieldFlow<>(block, field, false);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.scandroid.flow.ISinkPoint#findSources(org.scandroid.util.
	 * CGAnalysisContext, com.ibm.wala.dataflow.IFDS.TabulationResult,
	 * org.scandroid.domain.IFDSTaintDomain)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Set<FlowType<IExplodedBasicBlock>> findSources(
			CGAnalysisContext<IExplodedBasicBlock> ctx,
			TabulationResult<BasicBlockInContext<IExplodedBasicBlock>, CGNode, DomainElement> flowResult,
			IFDSTaintDomain<IExplodedBasicBlock> domain) {
		Set<FlowType<IExplodedBasicBlock>> sources = HashSetFactory.make();

		for (DomainElement de : domain
				.getPossibleElements(new StaticFieldElement(field
						.getReference()))) {
			if (de.taintSource instanceof StaticFieldFlow<?>) {
				StaticFieldFlow<IExplodedBasicBlock> source = (StaticFieldFlow<IExplodedBasicBlock>) de.taintSource;
				if (source.getField().equals(field)) {
					continue;
				}
			} else if (flowResult.getResult(block).contains(domain.getMappedIndex(de))) {
				sources.add(de.taintSource);
			}
		}

		return sources;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.scandroid.flow.ISinkPoint#getFlow()
	 */
	@Override
	public FlowType<IExplodedBasicBlock> getFlow() {
		return flow;
	}

}
