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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.scandroid.prefixtransfer.StringBuilderUseAnalysis.StringBuilderToStringInstanceKeySite;
import org.scandroid.prefixtransfer.modeledAllocations.ConstantString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.graph.Graph;

public class PrefixTransferGraph implements Graph<InstanceKeySite> {
	private static final Logger logger = LoggerFactory.getLogger(PrefixTransferGraph.class);

    private final Map<InstanceKey, InstanceKeySite> nodeMap = new HashMap<InstanceKey, InstanceKeySite>();
    private final List<InstanceKeySite> nodes = new ArrayList<InstanceKeySite>();
    private final Map<InstanceKeySite,Set<InstanceKeySite>> successors = new HashMap<InstanceKeySite, Set<InstanceKeySite>>();
    private final Map<InstanceKeySite,Set<InstanceKeySite>> predecessors = new HashMap<InstanceKeySite, Set<InstanceKeySite>>();
    public final Map<InstanceKey, StringBuilderUseAnalysis> sbuaMap = new HashMap<InstanceKey, StringBuilderUseAnalysis>();

    public PrefixTransferGraph(PointerAnalysis<InstanceKey> pa)
    {
        Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies = new HashMap<InstanceKeySite, Set<InstanceKey>>();
        ArrayList<InstanceKey> instanceKeys = new ArrayList<InstanceKey>();
        instanceKeys.addAll(pa.getInstanceKeys());
        for(InstanceKey k:instanceKeys)
        {
            if(k.getConcreteType().getName().toString().equals("Ljava/lang/StringBuilder"))
            {
                if(k instanceof AllocationSiteInNode)
                {
                    AllocationSiteInNode as = (AllocationSiteInNode)k;
                    if(as.getSite().getDeclaredType().getClassLoader().equals(ClassLoaderReference.Application))
                    {
                        StringBuilderUseAnalysis sbua;
                        try
                        {
                            sbua = new StringBuilderUseAnalysis(k,pa);
                        }
                        catch(Exception e)
                        {
                            logger.error("SBUA failed", e);
                            continue;
                        }
                        for(Entry<ISSABasicBlock, ISSABasicBlock> e : sbua.blockOrdering.entrySet())
                        {
                            logger.debug(e.getKey().toString()+" --> "+e.getValue().toString());
                            SSAInstruction inst = e.getKey().getLastInstruction();
                            if (inst instanceof SSAInvokeInstruction) {
                                logger.debug("Call Site \t" + ((SSAInvokeInstruction) inst).getCallSite());
                            }
                        }
                        sbuaMap.put(k, sbua); // map k to sbua in some global map
                    }
                    continue;
                }
                logger.warn("Skipping StringBuilder InstanceKey: "+k);
                logger.warn("\tClass loader reference: "+k.getConcreteType().getClassLoader().getReference());
            }
        }
        InstanceKeySite node = null;
        for (InstanceKey k:instanceKeys)
        {
            // create a node for each InstanceKey of type string
            if(k.getConcreteType().getName().toString().equals("Ljava/lang/String"))
            {
                if(k instanceof ConstantKey)
                {
                    logger.debug("ConstantKey: "+((ConstantKey<?>)k).getValue());
                    node = new ConstantString(pa.getInstanceKeyMapping().getMappedIndex(k), (String)((ConstantKey<?>)k).getValue());
                    addNode(node);
                    nodeMap.put(k, node);
                }
                else if(k instanceof NormalAllocationInNode)
                {
                    logger.debug("NormalAllocationInNode: "+k);
                    IMethod m = ((NormalAllocationInNode) k).getNode().getMethod();
                    if (m.getSignature().equals("java.lang.StringBuilder.toString()Ljava/lang/String;")) {
                        Context context = ((NormalAllocationInNode) k).getNode().getContext();
                        CGNode caller = (CGNode) context.get(ContextKey.CALLER);
                        CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);
                        InstanceKey receiver = (InstanceKey) context.get(ContextKey.RECEIVER);
                        logger.debug("StringBuilder.toString() csr: "+csr+" context: "+context+" receiver: "+receiver);
                        if (caller != null && caller.getMethod().getReference().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application))
                        {
                            logger.debug("Found StringBuilder receiver for toString call");
                            node = sbuaMap.get(receiver).getNode(csr,k);
                            if(node == null)
                            {
                                continue;
                            }
                            addNode(node);
                            nodeMap.put(k, node);
                            HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
                            for (Integer i: ((StringBuilderToStringInstanceKeySite) node).concatenatedInstanceKeys) {
                                iks.add(pa.getInstanceKeyMapping().getMappedObject(i));
                            }
                            unresolvedDependencies.put(node, iks);
                            // TODO: if this string is created inside the toString function of a string builder, find the StringBuilderUseAnalysis for that string builder and call getNode(k) to get the node for this instance key
                            // - this may have to be done in another phase
//                          NormalAllocationInNode ak = (NormalAllocationInNode)k;
//                          SSAInstruction inst = ak.getNode().getIR().getPEI(ak.getSite());
//                          logger.debug("NormalAllocationInNode inst: "+inst);
//                          logger.debug("NormalAllocationInNode uses:");
//                          for(int i = 0; i < inst.getNumberOfUses(); i++)
//                          {
//                              int use = inst.getUse(i);
//                              OrdinalSet<InstanceKey> useKeys = pa.getPointsToSet(new LocalPointerKey(ak.getNode(), use));
//                              logger.debug("\tUse "+use+": "+useKeys);
//                          }
//                          logger.debug("NormalAllocationInNode defs:");
//                          for(int i = 0; i < inst.getNumberOfDefs(); i++)
//                          {
//                              int def = inst.getDef(i);
//                              OrdinalSet<InstanceKey> useKeys = pa.getPointsToSet(new LocalPointerKey(ak.getNode(), def));
//                              logger.debug("\tDef "+def+": "+useKeys);
//                          }
                        }
                    }
                }
                else if(k instanceof AllocationSite)
                {
                    logger.debug("AllocationSite: "+k);
                }
                else
                {
                    logger.debug("Unknown type: "+k.toString());
                }
                // create an edge for dependencies used in the creation of each instance key
            }
            else
            {
                logger.debug("Got IK of other type "+k);
            }
        }
        for(Entry<InstanceKeySite, Set<InstanceKey>> deps:unresolvedDependencies.entrySet())
        {
            for(InstanceKey dep:deps.getValue())
            {
                InstanceKeySite depSite = nodeMap.get(dep);
                if(depSite == null)
                {
                    throw new IllegalStateException("cannot resolve dependency of "+deps.getKey()+" on "+dep);
                }
                addEdge(depSite, deps.getKey());
            }
        }
    }

    public void removeNodeAndEdges(InstanceKeySite n)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void addNode(InstanceKeySite n) {
        predecessors.put(n,new HashSet<InstanceKeySite>());
        successors.put(n,new HashSet<InstanceKeySite>());
        nodes.add(n);
    }

    public boolean containsNode(InstanceKeySite n) {
        return nodes.contains(n);
    }

    public int getNumberOfNodes() {
        return nodes.size();
    }

    public Iterator<InstanceKeySite> iterator() {
        return nodes.iterator();
    }

    public void removeNode(InstanceKeySite n) {
        throw new UnsupportedOperationException();
    }

    public void addEdge(InstanceKeySite src, InstanceKeySite dst) {
        Set<InstanceKeySite> predSet = predecessors.get(dst);
        if(predSet == null)
        {
            predSet = new HashSet<InstanceKeySite>();
            predecessors.put(dst,predSet);
        }
        predSet.add(src);
        Set<InstanceKeySite> succSet = successors.get(src);
        if(succSet == null)
        {
            succSet = new HashSet<InstanceKeySite>();
            successors.put(src,succSet);
        }
        succSet.add(dst);
    }

    public int getPredNodeCount(InstanceKeySite n) {
        return predecessors.get(n).size();
    }

    public Iterator<InstanceKeySite> getPredNodes(InstanceKeySite n) {
        return predecessors.get(n).iterator();
    }

    public int getSuccNodeCount(InstanceKeySite N) {
        return successors.get(N).size();
    }

    public Iterator<InstanceKeySite> getSuccNodes(InstanceKeySite n) {
        return successors.get(n).iterator();
    }

    public boolean hasEdge(InstanceKeySite src, InstanceKeySite dst) {
        throw new UnsupportedOperationException();
    }

    public void removeAllIncidentEdges(InstanceKeySite node)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void removeEdge(InstanceKeySite src, InstanceKeySite dst)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void removeIncomingEdges(InstanceKeySite node)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void removeOutgoingEdges(InstanceKeySite node)
            throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

}
