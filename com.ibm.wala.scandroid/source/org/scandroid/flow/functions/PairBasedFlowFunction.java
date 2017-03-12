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
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>)
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
package org.scandroid.flow.functions;

import java.util.List;

import org.scandroid.domain.CodeElement;
import org.scandroid.domain.DomainElement;
import org.scandroid.domain.IFDSTaintDomain;

import com.ibm.wala.dataflow.IFDS.IUnaryFlowFunction;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;


final class PairBasedFlowFunction <E extends ISSABasicBlock> implements IUnaryFlowFunction {
//	private static final Logger logger = 
//			LoggerFactory.getLogger(PairBasedFlowFunction.class);
	
    private final List<UseDefPair> useToDefList;
	private final IFDSTaintDomain<E> domain;

    public PairBasedFlowFunction(IFDSTaintDomain<E> domain, List<UseDefPair> useToDefList) {
    	this.domain = domain;
        this.useToDefList = useToDefList;
    }
    
    @Override
    public IntSet getTargets(int d) {
    	//logger.debug("getTargets("+d+")");
    	if (0 == d) {
    		//logger.debug("getTargets("+d+"): {0}");
    		return TaintTransferFunctions.ZERO_SET;
    	}
    	
        MutableSparseIntSet set = MutableSparseIntSet.makeEmpty();

        DomainElement de = domain.getMappedObject(d);
        // Here we list what facts we pass through. If a fact was true
        // before executing this instruction, it'll be true after,
        // unless we created a new definition of its associated
        // CodeElement.
    	
    	// see if D is still true; if so, pass it through:
    	// (this corresponds to the vertical 'pass through' arrows in the RHS paper)
    	// we actually assume that D passes through, unless there 
    	// is evidence to the contrary.  Because of this, instructions will
    	// 'default' to propagating taints that were not relevant to that 
    	// instruction, which is what we want.
    	set.add(d);
    	for (UseDefPair udPair : useToDefList) {
			CodeElement def = udPair.getDef();
			
			if (def.equals(de.codeElement)) {
				// this instruction redefined D, so we 
				// do *not* pass it through - this conditional has 
				// contradicted our assumption that D should be passed through,
				// so remove it from the set:
				set.remove(d);
				break;
			}
		}
    	
    	////////////////////////////////////////////////////////////////
    	// see if the taints associated with D also flow through to any 
    	// other domain elements:
    	
    	for (UseDefPair udPair : useToDefList) {
			CodeElement use = udPair.getUse();
			
			if (use.equals(de.codeElement)) {
				// ok, the d element flows to the def, so we add that def
				// and keep looking.
				DomainElement newDE = 
				     new DomainElement(udPair.getDef(), de.taintSource);
				set.add(domain.getMappedIndex(newDE));
			}
    	}	
    	// logger.debug("getTargets("+d+"): "+set);
        return set;
    }
}
