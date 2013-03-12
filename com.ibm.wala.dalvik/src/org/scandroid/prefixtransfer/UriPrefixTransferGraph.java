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
import org.scandroid.prefixtransfer.modeledAllocations.UriAppendString;
import org.scandroid.prefixtransfer.modeledAllocations.UriParseString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.OrdinalSet;

public class UriPrefixTransferGraph implements Graph<InstanceKeySite> {
	private static final Logger logger = LoggerFactory.getLogger(UriPrefixTransferGraph.class);

    public final Map<InstanceKey, InstanceKeySite> nodeMap = new HashMap<InstanceKey, InstanceKeySite>();
    private final List<InstanceKeySite> nodes = new ArrayList<InstanceKeySite>();
    private final Map<InstanceKeySite,Set<InstanceKeySite>> successors = new HashMap<InstanceKeySite, Set<InstanceKeySite>>();
    private final Map<InstanceKeySite,Set<InstanceKeySite>> predecessors = new HashMap<InstanceKeySite, Set<InstanceKeySite>>();
    public final Map<InstanceKey, StringBuilderUseAnalysis> sbuaMap = new HashMap<InstanceKey, StringBuilderUseAnalysis>();

    public UriPrefixTransferGraph(PointerAnalysis pa)
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
                            logger.warn("SBUA failed", e);
                            continue;
                        }
//                      for(Entry<ISSABasicBlock, ISSABasicBlock> e : sbua.blockOrdering.entrySet())
//                      {
//                          logger.debug(e.getKey().toString()+" --> "+e.getValue().toString());
//                          SSAInstruction inst = e.getKey().getLastInstruction();
//                          if (inst instanceof SSAInvokeInstruction) {
//                              logger.debug("Call Site \t" + ((SSAInvokeInstruction) inst).getCallSite());
//                          }
//                      }
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
            //logger.debug("checking type: " + k.getConcreteType().getName());

            // create a node for each InstanceKey of type string

            if(k.getConcreteType().getName().toString().equals("Ljava/lang/String"))
            {
                if(k instanceof ConstantKey)
                {
                    node = new ConstantString(pa.getInstanceKeyMapping().getMappedIndex(k), (String)((ConstantKey<?>)k).getValue());
                    addNode(node);
//                	logger.debug(node);
                    nodeMap.put(k, node);
                }
                else if(k instanceof NormalAllocationInNode)
                {
//                  logger.debug("NormalAllocationInNode: "+k);
                    IMethod m = ((NormalAllocationInNode) k).getNode().getMethod();
                    if (m.getSignature().equals("java.lang.StringBuilder.toString()Ljava/lang/String;")) {
                        Context context = ((NormalAllocationInNode) k).getNode().getContext();
                        CGNode caller = (CGNode) context.get(ContextKey.CALLER);
                        CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);
                        InstanceKey receiver = (InstanceKey) context.get(ContextKey.RECEIVER);
//                        logger.debug("StringBuilder.toString() caller: " + caller +"\n\tcsr: "+csr+"\n\treceiver: "+receiver);
                        if (caller != null && caller.getMethod().getReference().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application))
                        {
//                        	logger.debug("Found StringBuilder receiver for toString call");
                            if (sbuaMap.get(receiver) != null) {
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
                                logger.debug("adding to UnresolvedDependencies => node: " + node + " => iks: " + iks);
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
                            else {
                                logger.warn("Receiver instancekey is null in UriPrefixTransferGraph, Method: " + ((NormalAllocationInNode) receiver).getNode().getMethod().getSignature());
                            }
                        }
                    }
                }
//              else if(k instanceof AllocationSite)
//              {
//                  logger.debug("AllocationSite: "+k);
//              }
//              else
//              {
//                  logger.debug("Unknown type: "+k.toString());
//              }
                // create an edge for dependencies used in the creation of each instance key
            }
//          else
//          {
//              logger.debug("Got IK of other type "+k);
//          }
        }

        for (Iterator<PointerKey> Ipk = pa.getPointerKeys().iterator(); Ipk.hasNext();) {
        	PointerKey pk = Ipk.next();
        	if (pk instanceof LocalPointerKey) {
        		LocalPointerKey lpk = (LocalPointerKey)pk;
        		IMethod m = lpk.getNode().getMethod();
        		Context context = lpk.getNode().getContext();
        		CGNode caller = (CGNode) context.get(ContextKey.CALLER);
        		CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);
        		if (caller != null && caller.getMethod().getReference().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application)) {
        			if (m.getSignature().equals("android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri;")) {
        				SSAInvokeInstruction invoke = (SSAInvokeInstruction) caller.getIR().getBasicBlocksForCall(csr)[0].getLastInstruction();
        				LocalPointerKey lkey = new LocalPointerKey(caller, invoke.getUse(0));
        				if (pa.getPointsToSet(lkey).iterator().hasNext()) {
        					InstanceKey uriKey = pa.getPointsToSet(lkey).iterator().next();
        					OrdinalSet<InstanceKey> points = pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(1)));
        					if(!points.isEmpty()) {
        						InstanceKey stringKey = points.iterator().next();

        						OrdinalSet<InstanceKey> returnSet = pa.getPointsToSet(new LocalPointerKey(caller, invoke.getReturnValue(0)));
        						logger.debug("Sizeof returnset: " + returnSet.size() +"--"+pk);
        						for (Iterator<InstanceKey> rIK=returnSet.iterator(); rIK.hasNext(); ) {
        							InstanceKey returnIK = rIK.next();
        							node = new UriAppendString(pa.getInstanceKeyMapping().getMappedIndex(returnIK), pa.getInstanceKeyMapping().getMappedIndex(uriKey), pa.getInstanceKeyMapping().getMappedIndex(stringKey));
        							logger.debug("\t Uri.withAppendedPath(): "+ invoke + ", returnIK: " +returnIK+ ", uriKey: " + uriKey + ", stringKey: " + stringKey);
        							logger.debug("\t returnIK_Index:"+pa.getInstanceKeyMapping().getMappedIndex(returnIK)+ ", uriKey_Index: " + pa.getInstanceKeyMapping().getMappedIndex(uriKey) + ", stringKey_Index: " + pa.getInstanceKeyMapping().getMappedIndex(stringKey));
        							
        							if (!nodeMap.containsKey(returnIK)) {
        								addNode(node);
        								nodeMap.put(returnIK, node);
        								HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
        								iks.add(uriKey);
        								iks.add(stringKey);
        								unresolvedDependencies.put(node, iks);
        							}
        						}
        					}
        				}
        			}
        		}
        	}
        }
        
        for (InstanceKey ik: pa.getInstanceKeys()) {
            if (ik instanceof NormalAllocationInNode) {
                IMethod m = ((NormalAllocationInNode) ik).getNode().getMethod();
//                logger.debug("method sig: " + m.getSignature()+", ik " + ik);
                Context context = ((NormalAllocationInNode) ik).getNode().getContext();
                CGNode caller = (CGNode) context.get(ContextKey.CALLER);
                CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);

                if (caller != null && caller.getMethod().getReference().getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Application)) {                	
                    if (m.getSignature().equals("android.net.Uri.parse(Ljava/lang/String;)Landroid/net/Uri;")) {
                        SSAInvokeInstruction invoke = (SSAInvokeInstruction) caller.getIR().getBasicBlocksForCall(csr)[0].getLastInstruction();
                        logger.debug("invoke inst: " + invoke + " getuse: " + invoke.getUse(0));
                        logger.debug("in node: " + caller);
                        OrdinalSet<InstanceKey> points = pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(0)));
//                        logger.debug("Size of pointsParse: " + points.size());
                        if(!points.isEmpty()) {
                            InstanceKey stringKey = points.iterator().next();
                            node = new UriParseString(pa.getInstanceKeyMapping().getMappedIndex(ik), pa.getInstanceKeyMapping().getMappedIndex(stringKey));
                            logger.debug("\t Uri.parse(): "+ invoke + "..." + stringKey);
                            addNode(node);
                            nodeMap.put(ik, node);
                            HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
                            iks.add(stringKey);
                            unresolvedDependencies.put(node, iks);
                        }
                    }
                    //Doesn't seem to be entering this else with the current android jar -- reimplemented above using LocalPointerKey
                    else if (m.getSignature().equals("android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri;")) {
                        logger.debug("android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri call: " + caller);
                        SSAInvokeInstruction invoke = (SSAInvokeInstruction) caller.getIR().getBasicBlocksForCall(csr)[0].getLastInstruction();
                        LocalPointerKey lkey = new LocalPointerKey(caller, invoke.getUse(0));
                        if (pa.getPointsToSet(lkey).iterator().hasNext()) {
                            InstanceKey uriKey = pa.getPointsToSet(lkey).iterator().next();
                            OrdinalSet<InstanceKey> points = pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(1)));
                            if(!points.isEmpty()) {
                                InstanceKey stringKey = points.iterator().next();
                                node = new UriAppendString(pa.getInstanceKeyMapping().getMappedIndex(ik), pa.getInstanceKeyMapping().getMappedIndex(uriKey), pa.getInstanceKeyMapping().getMappedIndex(stringKey));
                                logger.debug("\t Uri.withAppendedPath(): "+ invoke + "..." + uriKey + "..." + stringKey);
                                addNode(node);
                                nodeMap.put(ik, node);
                                HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
                                iks.add(uriKey);
                                iks.add(stringKey);
                                unresolvedDependencies.put(node, iks);
                            }
                        }
                    }
                }
            }
        }


//      int maxInstanceID = pa.getInstanceKeyMapping().getMaximumIndex();
//      HashMap<Integer, InstanceKeySite> uriNodeMap = new HashMap<Integer, InstanceKeySite>();
//
//      for (Entry<Integer,String> s : stringConstants.entrySet()) {
//          logger.debug("Constant: "+ s.getValue());
//          node = new ConstantString(maxInstanceID + s.getKey(), s.getValue());
//          addNode(node);
//          uriNodeMap.put(maxInstanceID + s.getKey(), node);
//      }
//
//      for (Entry<Integer,LocalPointerKey> p : parseMap.entrySet()) {
//          logger.debug("Parse: "+ p.getValue());
//          OrdinalSet<InstanceKey> ikeys = pa.getPointsToSet(p.getValue());
//          if (ikeys.isEmpty()) {
//              node = new UriParseString(maxInstanceID + p.getKey(), maxInstanceID + p.getValue().getValueNumber());
//              //addNode(node);
//              uriNodeMap.put(maxInstanceID + p.getKey(), node);
//              //addEdge(uriNodeMap.get(maxInstanceID + p.getValue().getValueNumber()), node);
//          }
//          else {
//              InstanceKey ikey = ikeys.iterator().next();
//              node = new UriParseString(maxInstanceID + p.getKey(), pa.getInstanceKeyMapping().getMappedIndex(ikey));
//              //addNode(node);
//              uriNodeMap.put(maxInstanceID + p.getKey(), node);
//              //addEdge(nodeMap.get(ikey), node);
//          }
//      }
//
//      for (Entry<Integer,LocalPointerKey> p : appendUriMap.entrySet()) {
//          LocalPointerKey stringLPK = appendStringMap.get(p.getKey());
//          logger.debug("Append: " + p.getValue() + " + " + stringLPK);
//          OrdinalSet<InstanceKey> ikeys = pa.getPointsToSet(stringLPK);
//          if (ikeys.isEmpty()) {
//              node = new UriAppendString(maxInstanceID + p.getKey(), maxInstanceID + p.getValue().getValueNumber(), maxInstanceID + stringLPK.getValueNumber());
//              //addNode(node);
//              uriNodeMap.put(maxInstanceID + p.getKey(), node);
//              //addEdge(uriNodeMap.get(maxInstanceID + p.getValue().getValueNumber()), node);
//              //addEdge(uriNodeMap.get(maxInstanceID + stringLPK.getValueNumber()), node);
//          }
//          else {
//              InstanceKey ikey = ikeys.iterator().next();
//              node = new UriAppendString(maxInstanceID + p.getKey(), maxInstanceID + p.getValue().getValueNumber(), pa.getInstanceKeyMapping().getMappedIndex(ikey));
//              //addNode(node);
//              uriNodeMap.put(maxInstanceID + p.getKey(), node);
//              //addEdge(uriNodeMap.get(maxInstanceID + p.getValue().getValueNumber()), node);
//              //addEdge(nodeMap.get(ikey), node);
//          }
//      }


        for(Entry<InstanceKeySite, Set<InstanceKey>> deps:unresolvedDependencies.entrySet())
        {
            for(InstanceKey dep:deps.getValue())
            {
                //logger.debug("Trying to match dependency: " + dep);
                InstanceKeySite depSite = nodeMap.get(dep);
//              if(depSite == null)
//              {
//                  throw new IllegalStateException("cannot resolve dependency of "+deps.getKey()+" on "+dep);
//              }
//              addEdge(depSite, deps.getKey());
                if (depSite != null)
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
