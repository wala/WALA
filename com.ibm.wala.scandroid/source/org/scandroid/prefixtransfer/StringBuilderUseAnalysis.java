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

package org.scandroid.prefixtransfer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

public class StringBuilderUseAnalysis {

	public final Map<ISSABasicBlock, ISSABasicBlock> blockOrdering;
	
	private final CGNode node;
	private final PointerAnalysis<InstanceKey> pa;
	private final Set<LocalPointerKey> localPointerKeys = new HashSet<>();
	private final List<SSAInstruction> instructions;
	
	public StringBuilderUseAnalysis(final InstanceKey ik, final PointerAnalysis<InstanceKey> pa) {
		assert(ik.getConcreteType().getName().toString().equals("Ljava/lang/StringBuilder"));
	
		this.pa = pa;
		this.node = findCGNode(ik, pa);
		this.instructions = findInstructions();
		
		final HashSet<ISSABasicBlock> blockSet = new HashSet<>();
		for (final SSAInstruction inst : instructions) {
			blockSet.add(node.getIR().getBasicBlockForInstruction(inst));
		}
		
		// find the ordering for all of the instructions
		final BlockSearch blockSearch = new BlockSearch(node.getIR());
		final Map<ISSABasicBlock,ISSABasicBlock> blockOrdering = new HashMap<>();
		
		for (final ISSABasicBlock b : blockSet) {
			final ISSABasicBlock target = blockSearch.searchFromBlock(b, blockSet);

			if (target != null) {
				blockOrdering.put(b, target);
			}
		}

		this.blockOrdering = blockOrdering;
	}

	private CGNode findCGNode(final InstanceKey ik, final PointerAnalysis<InstanceKey> pa) {
		CGNode nominatedNode = null;
		
		for (final PointerKey pk : pa.getPointerKeys()) {
			if (pk instanceof LocalPointerKey) {
				final LocalPointerKey lpk = (LocalPointerKey) pk;
				if (!lpk.getNode().getMethod().getReference().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application))	{
					continue;
				}

				for (final InstanceKey k:pa.getPointsToSet(pk)) {
					if (k.equals(ik)) {
						// make sure it's a local pointer key, and make sure that it's in just one cgnode
						localPointerKeys.add(lpk);
						if (nominatedNode == null) {
							nominatedNode = lpk.getNode();
						} else if (nominatedNode != lpk.getNode()) {
							
							return null;
						}
					}
				}
			} else if (!(pk instanceof ReturnValueKey)) {
				// if this pointer key points to our instance key then we have to give up -- we can only analyze local pointer keys
				final OrdinalSet<InstanceKey> pts = pa.getPointsToSet(pk);
				if (pts.contains(ik)) {
					
					return null;
				}
			}
		}
		
		return nominatedNode;
	}

	private List<SSAInstruction> findInstructions() {
		List<SSAInstruction> instructions = new ArrayList<>();
		if (node != null) {
			for (SSAInstruction inst: node.getIR().getInstructions()) {
				if (inst instanceof SSAInvokeInstruction) {
					if (localPointerKeys.contains(new LocalPointerKey(node,inst.getUse(0))))	{
						instructions.add(inst);
					}
				}
			}
		}
		
		return instructions;
	}

	public Set<InstanceKey> getDeps() {
		return null;
	}

	public class StringBuilderToStringInstanceKeySite extends InstanceKeySite {
		
		final ArrayList<Integer> concatenatedInstanceKeys;
		final int instanceID;
		
		StringBuilderToStringInstanceKeySite(final int instanceID, final ArrayList<Integer> concatenatedInstanceKeys) {
			this.concatenatedInstanceKeys = concatenatedInstanceKeys;
			this.instanceID = instanceID;
		}

		@Override
		public PrefixVariable propagate(final PrefixVariable input) {
			// TODO: go through each of the concatenatedInstanceKeys and look up their prefixes in input to figure out the prefix of the string generated by toString()
			final StringBuffer buf = new StringBuffer();
			boolean prefixNew = false;
			boolean prefixFull = true;
			
			for (final Integer i: concatenatedInstanceKeys) {
				final String prefix = input.getPrefix(i);

				if (prefix != null) {
					buf.append(prefix);
					prefixNew = true;
				}
				
				if (!input.fullPrefixKnown.contains(i)) {
					prefixFull = false;
					break;
				}
			}
			
			final PrefixVariable retVal = new PrefixVariable();
			retVal.copyState(input);
			
			if (prefixNew) {
				final String s = buf.toString();
				retVal.update(instanceID, s);

				if (prefixFull) { retVal.include(instanceID); }
			}
			
			return retVal;
		}

		@Override
		public String toString() {
			return ("StringBuilderToString(instanceID = " + instanceID + "; concatenatedInstanceKeys = " + concatenatedInstanceKeys + ")");
		}

		@Override
		public int instanceID() {
			return instanceID;
		}

	}

	public InstanceKeySite getNode(final CallSiteReference csr, final InstanceKey k) {
		final ISSABasicBlock bbs[] = node.getIR().getBasicBlocksForCall(csr);
		
		final OrdinalSetMapping<InstanceKey> mapping = pa.getInstanceKeyMapping();
		final HashSet<ISSABasicBlock> blocksSeen = new HashSet<>();
		final ArrayList<Integer> concatenatedInstanceKeys = new ArrayList<>();

		ISSABasicBlock bPrev = bbs[0];
		ISSABasicBlock bNext = blockOrdering.get(bPrev);
		while (bNext != null) {
			// detect loops
			if (blocksSeen.contains(bNext)) {
				
				
				

				return null;
			}
			
			blocksSeen.add(bNext);
			
			final SSAInvokeInstruction iNext = (SSAInvokeInstruction) bNext.getLastInstruction();
			final MethodReference tgt = iNext.getDeclaredTarget();

			if ("append".equals(tgt.getName().toString())) {
				final LocalPointerKey lpk = new LocalPointerKey(node, iNext.getUse(1));

				for (final InstanceKey ikey: pa.getPointsToSet(lpk)) {
					if (isNonNullConstant(ikey)) {
						concatenatedInstanceKeys.add(0, mapping.getMappedIndex(ikey));
					}
				}
			} else if (isDefaultConstructor(tgt)) {
				final LocalPointerKey lpk = new LocalPointerKey(node, iNext.getUse(1));
				
				for (final InstanceKey ikey: pa.getPointsToSet(lpk)) {
					if (isNonNullConstant(ikey)) {
						concatenatedInstanceKeys.add(0, mapping.getMappedIndex(ikey));
					}
				}
				
				return new StringBuilderToStringInstanceKeySite(mapping.getMappedIndex(k), concatenatedInstanceKeys);
			}
			
			bPrev = bNext;
			bNext = blockOrdering.get(bNext);
		}
		
		
		
		return null;
	}

	private static boolean isDefaultConstructor(final MethodReference mref) {
		return "<init>".equals(mref.getName().toString()) && mref.getNumberOfParameters() == 0;
	}
	
	private static boolean isNonNullConstant(final InstanceKey ik) {
		if (ik instanceof ConstantKey<?>) {
			final ConstantKey<?> ck = (ConstantKey<?>) ik;
			
			return ! "null".equals(ck.getValue().toString());
		}
		
		return false;
	}
	
}
