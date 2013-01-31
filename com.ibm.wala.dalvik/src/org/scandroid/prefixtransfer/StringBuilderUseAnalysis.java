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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

public class StringBuilderUseAnalysis {
	private static final Logger logger = LoggerFactory.getLogger(StringBuilderUseAnalysis.class);

	final InstanceKey sbik;
	final CGNode node;
	final PointerAnalysis pa;
	final Set<LocalPointerKey> localPointerKeys = new HashSet<LocalPointerKey>();
	final List<SSAInstruction> instructions;
	public Map<ISSABasicBlock, ISSABasicBlock> blockOrdering;
	public StringBuilderUseAnalysis(InstanceKey ik, PointerAnalysis pa) throws Exception
	{
		assert(ik.getConcreteType().getName().toString().equals("Ljava/lang/StringBuilder"));
//		System.out.println("Analyzing StringBuilder key "+ik);
		this.sbik = ik;
		this.pa = pa;
		node = findCGNode(ik, pa);
//		System.out.println("Found node: "+node);
//		System.out.println("Local pointer count: "+localPointerKeys.size());
//		for(LocalPointerKey lpk:localPointerKeys)
//		{
//			System.out.println("LPK: "+lpk);
//		}
		instructions = findInstructions();
//		System.out.println("Instruction count: "+instructions.size());
		HashSet<ISSABasicBlock> blockSet = new HashSet<ISSABasicBlock>();
		for(SSAInstruction inst:instructions)
		{
			blockSet.add(node.getIR().getBasicBlockForInstruction(inst));
//			System.out.println("Got string builder inst: "+inst);
//			System.out.println("Block Associated with inst: " + node.getIR().getBasicBlockForInstruction(inst));
		}
		// find the ordering for all of the instructions
		BlockSearch blockSearch = new BlockSearch(node.getIR());
		Map<ISSABasicBlock,ISSABasicBlock> blockOrdering = new HashMap<ISSABasicBlock, ISSABasicBlock>();
		for(ISSABasicBlock b:blockSet)
		{
//			System.out.println("Trying to order block: " + b);
			ISSABasicBlock target = blockSearch.searchFromBlock(b, blockSet);

			if(target == null)
			{
				//If target is null, means that this is the first block
				//or that there are multiple predecessor blocks that can be targets
//				System.out.println("failed to order blocks!!!");
				//                blockOrdering.put(b, target);
			}
			else
			{
//				System.out.println("Mapping b: " + b + "\t to target: " + target);                
				blockOrdering.put(b, target);
			}
		}

		this.blockOrdering = blockOrdering;
	}

	private CGNode findCGNode(InstanceKey ik, PointerAnalysis pa) throws Exception
	{
		CGNode nominatedNode = null;
		for(PointerKey pk:pa.getPointerKeys())
		{
			if(pk instanceof LocalPointerKey)
			{
				LocalPointerKey lpk = (LocalPointerKey) pk;
				if(!lpk.getNode().getMethod().getReference().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application))
				{
					//                  System.out.println("Skipping non-application pointer key "+lpk);
					continue;
				}
				//                int nonPrimordialCount = 0;
				//              boolean interestingKey = false;
				for(InstanceKey k:pa.getPointsToSet(pk))
				{
					/*
                    if(k instanceof AllocationSiteInNode)
                    {
                        if(((AllocationSiteInNode) k).getSite().getDeclaredType().getClassLoader().equals(ClassLoaderReference.Application))
                        {
                            nonPrimordialCount++;
                        }
                    }
					 */
					if(k.equals(ik))
					{
						//                      interestingKey = true;
						//                      System.out.println("Found interesting key: "+lpk);
						// make sure it's a local pointer key, and make sure that it's in just one cgnode
						localPointerKeys.add(lpk);
						if(nominatedNode == null)
							nominatedNode = lpk.getNode();
						else if(nominatedNode != lpk.getNode())
						{
							logger.warn("got conflicting nodes: "+nominatedNode+" <> "+lpk.getNode());
							return null;
						}
					}
				}
				//              if(interestingKey && nonPrimordialCount > 1)
				//              {
				//                  System.out.println("too many ("+nonPrimordialCount+") non-primordial instance keys for pointer key "+pk);
				//                  for(InstanceKey localKey:pa.getPointsToSet(pk))
				//                  {
				//                      System.out.println("\t"+localKey);
				//                  }
				//                  throw new Exception("can't determine which string builder to analyze for this local pointer key");
				//              }
			}
			else if(pk instanceof ReturnValueKey)
			{
				//              System.out.println("Ignoring return value key "+pk);
			}
			else
			{
				// if this pointer key points to our instance key then we have to give up -- we can only analyze local pointer keys
				if(pa.getPointsToSet(pk).contains(ik))
				{
					logger.warn("Found non LocalPointerKey refering to our ik: "+pk);
					return null;
				}
			}
		}
		return nominatedNode;
	}

	private List<SSAInstruction> findInstructions()
	{
		List<SSAInstruction> instructions = new ArrayList<SSAInstruction>();
		if (node != null) {
			for(SSAInstruction inst: node.getIR().getInstructions())
			{
				if(inst instanceof SSAInvokeInstruction)
				{
					if(localPointerKeys.contains(new LocalPointerKey(node,inst.getUse(0))))
					{
						instructions.add(inst);
					}
				}
			}
		}
		return instructions;
	}

	public Set<InstanceKey> getDeps()
	{
		return null;
	}

	public class StringBuilderToStringInstanceKeySite extends InstanceKeySite
	{
		ArrayList<Integer> concatenatedInstanceKeys;
		int instanceID;
		StringBuilderToStringInstanceKeySite(int instanceID, ArrayList<Integer> concatenatedInstanceKeys)
		{
			this.concatenatedInstanceKeys = concatenatedInstanceKeys;
			this.instanceID = instanceID;
		}

		@Override
		public PrefixVariable propagate(PrefixVariable input) {
			// TODO: go through each of the concatenatedInstanceKeys and look up their prefixes in input to figure out the prefix of the string generated by toString()
			StringBuffer buf = new StringBuffer();
			boolean prefixNew = false;
			boolean prefixFull = true;
			for (Integer i: concatenatedInstanceKeys) {
				String prefix = input.getPrefix(i);
				if (prefix != null) {
					buf.append(prefix);
					prefixNew = true;
				}
				if (!input.fullPrefixKnown.contains(i)) {
					prefixFull = false;
					break;
				}
			}
			String s = buf.toString();
			PrefixVariable retVal = new PrefixVariable();
			retVal.copyState(input);
			if (prefixNew) {
				retVal.update(instanceID, s);
				if (prefixFull) retVal.include(instanceID);
			}
			return retVal;
		}

		public String toString() {
			return ("StringBuilderToString(instanceID = " + instanceID + "; concatenatedInstanceKeys = " + concatenatedInstanceKeys + ")");
		}

		@Override
		public int instanceID() {
			return instanceID;
		}

	}

	public InstanceKeySite getNode(CallSiteReference csr, InstanceKey k)
	{
		ArrayList<Integer> concatenatedInstanceKeys = new ArrayList<Integer>();
		SSAInvokeInstruction iNext = null;
		ISSABasicBlock bNext = null;
		LocalPointerKey lpk = null;
		ISSABasicBlock bbs[] = node.getIR().getBasicBlocksForCall(csr);
		if(bbs.length != 1)
		{
			logger.warn("Got wrong number of basic blocks for call site: "+node.getMethod().getSignature() + " blocks:" +bbs.length);
		}
		bNext = bbs[0];
		ISSABasicBlock bPrev = bNext;
		bNext = blockOrdering.get(bNext);
		HashSet<ISSABasicBlock> blocksSeen = new HashSet<ISSABasicBlock>();
//		System.out.println("start of bnext");      
//		SSAInvokeInstruction iPrev = (SSAInvokeInstruction)bPrev.getLastInstruction();
//		if (iPrev.getDeclaredTarget().getName().toString().equals("toString")) {
//			lpk = new LocalPointerKey(node,iPrev.getReturnValue(0));
//			for (InstanceKey ikey: pa.getPointsToSet(lpk))
//				System.out.println("toString() return id: " + pa.getInstanceKeyMapping().getMappedIndex(ikey));
//		}
		while(bNext != null)
		{
//			System.out.println("bPrev: " + bPrev);
//			System.out.println("bNext: " + bNext);
			// detect loops
			if(blocksSeen.contains(bNext))
			{
				logger.warn("Loop detected in string builder use analysis for "+sbik+"!");
				logger.warn("bPrev: "+bPrev);
				logger.warn("bNext: "+bNext);
				return null;
			}
			blocksSeen.add(bNext);
			iNext = (SSAInvokeInstruction)bNext.getLastInstruction();
//			System.out.println("iNext: " + iNext);
			if (iNext.getDeclaredTarget().getName().toString().equals("append")) {
				lpk = new LocalPointerKey(node,iNext.getUse(1));
//				System.out.println("lpk " + lpk);

				for (InstanceKey ikey: pa.getPointsToSet(lpk)) {
					if (!((ikey instanceof ConstantKey<?>) && (((ConstantKey<?>) ikey).getValue().toString().equals("null")))) {
//						System.out.println("Adding " + pa.getInstanceKeyMapping().getMappedIndex(ikey) + ": " + ikey);
						concatenatedInstanceKeys.add(0,pa.getInstanceKeyMapping().getMappedIndex(ikey));
					}
				}
			}
			else if(iNext.getDeclaredTarget().getName().toString().equals("<init>") &&
					iNext.getDeclaredTarget().getDescriptor().getNumberOfParameters() != 0)
			{
				lpk = new LocalPointerKey(node, iNext.getUse(1));
				for (InstanceKey ikey: pa.getPointsToSet(lpk)) {
					if (!((ikey instanceof ConstantKey<?>) && (((ConstantKey<?>) ikey).getValue().toString().equals("null")))) {
//						System.out.println("Adding " + pa.getInstanceKeyMapping().getMappedIndex(ikey) + ": " + ikey);
						concatenatedInstanceKeys.add(0,pa.getInstanceKeyMapping().getMappedIndex(ikey));
					}
				}
				//              System.out.println("Concats: " + concatenatedInstanceKeys);
				return new StringBuilderToStringInstanceKeySite(pa.getInstanceKeyMapping().getMappedIndex(k), concatenatedInstanceKeys);
			}
			bPrev = bNext;
			bNext = blockOrdering.get(bNext);
		}
		logger.warn("Ran out of parents before getting to <init> on SB: "+csr+ " with builder "+sbik);
		return null;
		//      for(Entry<ISSABasicBlock, ISSABasicBlock> e : this.blockOrdering.entrySet())
		//      {
		//          CallSiteReference csrkey = ((SSAInvokeInstruction) e.getKey().getLastInstruction()).getCallSite();
		//          if (csrkey.equals(csr)) {
		//              iNext = (SSAInvokeInstruction) e.getValue().getLastInstruction();
		//          }
		//      }
		//      while (!iNext.getDeclaredTarget().getName().toString().equals("<init>")) {
		//          for(Entry<ISSABasicBlock, ISSABasicBlock> e : this.blockOrdering.entrySet())
		//          {
		//              SSAInvokeInstruction key = (SSAInvokeInstruction) e.getKey().getLastInstruction();
		//              CallSiteReference csrkey = key.getCallSite();
		//              if (csrkey.equals(iNext.getCallSite())) {
		//                  iNext = (SSAInvokeInstruction) e.getValue().getLastInstruction();
		//                  if (key.getDeclaredTarget().getName().toString().equals("append")) {
		//                      lpk = new LocalPointerKey(node,key.getUse(1));
		//                      for (InstanceKey ikey: pa.getPointsToSet(lpk)) {
		//                          if (!((ikey instanceof ConstantKey<?>) && (((ConstantKey<?>) ikey).getValue().toString().equals("null")))) concatenatedInstanceKeys.add(pa.getInstanceKeyMapping().getMappedIndex(ikey));
		//                      }
		//                  }
		//              }
		//          }
		//      }
		//      lpk = new LocalPointerKey(node, iNext.getUse(1));
		//      for (InstanceKey ikey: pa.getPointsToSet(lpk)) {
		//          if (!((ikey instanceof ConstantKey<?>) && (((ConstantKey<?>) ikey).getValue().toString().equals("null")))) concatenatedInstanceKeys.add(0,pa.getInstanceKeyMapping().getMappedIndex(ikey));
		//      }
		//      for(Entry<ISSABasicBlock, ISSABasicBlock> e : this.blockOrdering.entrySet())
		//      {
		//          SSAInstruction key = e.getKey().getLastInstruction();
		//          SSAInstruction value = e.getValue().getLastInstruction();
		//          CallSiteReference csrkey = ((SSAInvokeInstruction) key).getCallSite();
		//          System.out.println("From \t" + csrkey);
		//          System.out.println("\t"+key);
		//          //if (csrkey.equals(csr)) System.out.println("The one!");
		//          System.out.println("To \t" + ((SSAInvokeInstruction) value).getCallSite());
		//          System.out.println("\t"+value);
		//      }
		//      System.out.println("CSR: \t" + csr);
		//      System.out.println(pa.getPointsToSet(new LocalPointerKey(node, 4)));
		//      System.out.println(pa.getPointsToSet(new LocalPointerKey(node, 7)));
		// find the call to toString()
		// track back to the call to init
		// keep track of the appends along the way
		//      System.out.println("Concats: " + concatenatedInstanceKeys);
		//      return new StringBuilderToStringInstanceKeySite(pa.getInstanceKeyMapping().getMappedIndex(k), concatenatedInstanceKeys);
	}
}
