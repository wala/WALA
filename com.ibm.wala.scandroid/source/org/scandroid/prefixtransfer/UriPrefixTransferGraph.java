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
import java.util.Collection;
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

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
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
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

public class UriPrefixTransferGraph implements Graph<InstanceKeySite> {
	
	private static final Logger logger = LoggerFactory.getLogger(UriPrefixTransferGraph.class);

    public final Map<InstanceKey, InstanceKeySite> nodeMap = new HashMap<InstanceKey, InstanceKeySite>();
    public final Map<InstanceKey, StringBuilderUseAnalysis> sbuaMap =
    	new HashMap<InstanceKey, StringBuilderUseAnalysis>();

    private final List<InstanceKeySite> nodes = new ArrayList<InstanceKeySite>();
    private final Map<InstanceKeySite,Set<InstanceKeySite>> successors =
    	new HashMap<InstanceKeySite, Set<InstanceKeySite>>();
    private final Map<InstanceKeySite,Set<InstanceKeySite>> predecessors =
    	new HashMap<InstanceKeySite, Set<InstanceKeySite>>();

    public UriPrefixTransferGraph(final PointerAnalysis<InstanceKey> pa) {
        final Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies =
        	new HashMap<InstanceKeySite, Set<InstanceKey>>();
        final OrdinalSetMapping<InstanceKey> mapping = pa.getInstanceKeyMapping();
        final Collection<InstanceKey> instanceKeys = pa.getInstanceKeys();
        
        for (final InstanceKey k : instanceKeys) {
        	handleStringBuilder(k, pa, mapping, unresolvedDependencies);
        }
        
        for (final InstanceKey k : instanceKeys) {
        	handleString(k, pa, mapping, unresolvedDependencies);
        }

        for (final PointerKey pk : pa.getPointerKeys()) {
        	if (pk instanceof LocalPointerKey) {
        		final LocalPointerKey lpk = (LocalPointerKey) pk;
        		handleUriWitAppendPath(lpk, pa, mapping, unresolvedDependencies);
        	}
        }
        
        for (final InstanceKey ik: instanceKeys) {
            if (ik instanceof NormalAllocationInNode) {
            	final NormalAllocationInNode naik = (NormalAllocationInNode) ik;
            	handleUriParse(naik, pa, mapping, unresolvedDependencies);
            	handleUriWitAppendPath(naik, pa, mapping, unresolvedDependencies);
            }
        }

        for (final Entry<InstanceKeySite, Set<InstanceKey>> deps : unresolvedDependencies.entrySet()) {
            for (final InstanceKey dep : deps.getValue()) {
                final InstanceKeySite depSite = nodeMap.get(dep);
                if (depSite != null) {
                    addEdge(depSite, deps.getKey());
                }
            }
        }
    }
    
    private void handleString(final InstanceKey ik, final PointerAnalysis<InstanceKey> pa,
    		final OrdinalSetMapping<InstanceKey> mapping,
    		final Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies) {
        if (isOfType(ik, "Ljava/lang/String")) {
            if (ik instanceof ConstantKey) {
            	final String value = (String) ((ConstantKey<?>) ik).getValue(); 
                final InstanceKeySite node = new ConstantString(mapping.getMappedIndex(ik), value);
                addNode(node);
                nodeMap.put(ik, node);
            } else if (ik instanceof NormalAllocationInNode) {
            	final NormalAllocationInNode nain = (NormalAllocationInNode) ik;
            	handleStringBuilderToString(nain, pa, mapping, unresolvedDependencies);
            }
        }
    }
    
    private void handleStringBuilder(final InstanceKey ik, final PointerAnalysis<InstanceKey> pa,
    		final OrdinalSetMapping<InstanceKey> mapping,
    		final Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies) {
    	
        if (isOfType(ik, "Ljava/lang/StringBuilder")) {
            if (ik instanceof AllocationSiteInNode) {
                final AllocationSiteInNode as = (AllocationSiteInNode) ik;
                if (isApplicationCode(as.getSite().getDeclaredType())) {
                    final StringBuilderUseAnalysis sbua;
                    try {
                        sbua = new StringBuilderUseAnalysis(ik, pa);
                    } catch(Exception e) {
                        logger.warn("SBUA failed", e);
                        return;
                    }

                    sbuaMap.put(ik, sbua); // map ik to sbua in some global map
                }
            } else {
                logger.warn("Skipping StringBuilder InstanceKey: " + ik);
                logger.warn("\tClass loader reference: " + ik.getConcreteType().getClassLoader().getReference());
            }
        }
    }
    
    private void handleStringBuilderToString(final NormalAllocationInNode nain, final PointerAnalysis<InstanceKey> pa,
    		final OrdinalSetMapping<InstanceKey> mapping,
    		final Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies) {
        if (hasSignature(nain, "java.lang.StringBuilder.toString()Ljava/lang/String;")) {
            final Context context = nain.getNode().getContext();
            final CGNode caller = (CGNode) context.get(ContextKey.CALLER);

            if (caller != null && isApplicationCode(caller.getMethod())) {
                final InstanceKey receiver = (InstanceKey) context.get(ContextKey.RECEIVER);

                if (sbuaMap.get(receiver) != null) {
                    final CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);
                	final InstanceKeySite node = sbuaMap.get(receiver).getNode(csr, nain);

                	if (node != null) {
                        addNode(node);
                        nodeMap.put(nain, node);
                        
                        final StringBuilderToStringInstanceKeySite s2si =
                        		(StringBuilderToStringInstanceKeySite) node;
                        final HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
                        
                        for (final Integer i: s2si.concatenatedInstanceKeys) {
                            iks.add(mapping.getMappedObject(i));
                        }
                        
                        logger.debug("adding to UnresolvedDependencies => node: " + node + " => iks: " + iks);
                        unresolvedDependencies.put(node, iks);
                        // TODO: if this string is created inside the toString function of a string builder,
                        // find the StringBuilderUseAnalysis for that string builder and call getNode(k) to
                        // get the node for this instance key
                        // - this may have to be done in another phase
                    }
                } else {
                    logger.warn("Receiver instancekey is null in UriPrefixTransferGraph, Method: "
                    	+ ((NormalAllocationInNode) receiver).getNode().getMethod().getSignature());
                }
            }
        }
    }

    private void handleUriWitAppendPath(final LocalPointerKey lpk, final PointerAnalysis<InstanceKey> pa,
    		final OrdinalSetMapping<InstanceKey> mapping,
    		final Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies) {
		final Context context = lpk.getNode().getContext();
		final CGNode caller = (CGNode) context.get(ContextKey.CALLER);
		
		if (caller != null && isApplicationCode(caller.getMethod())
				&& hasSignature(lpk, "android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri;")) {
    		final CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);
			final SSAInvokeInstruction invoke =
				(SSAInvokeInstruction) caller.getIR().getBasicBlocksForCall(csr)[0].getLastInstruction();
			final OrdinalSet<InstanceKey> ptsUri =
				pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(0)));
			
			if (!ptsUri.isEmpty()) {
				final InstanceKey uriKey = ptsUri.iterator().next();
				final OrdinalSet<InstanceKey> points =
						pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(1)));
				
				if (!points.isEmpty()) {
					final InstanceKey stringKey = points.iterator().next();

					final OrdinalSet<InstanceKey> returnSet =
							pa.getPointsToSet(new LocalPointerKey(caller, invoke.getReturnValue(0)));
					logger.debug("Sizeof returnset: " + returnSet.size() + "--" + lpk);
					
					for (final Iterator<InstanceKey> rIK = returnSet.iterator(); rIK.hasNext(); ) {
						final InstanceKey returnIK = rIK.next();
						final UriAppendString node = new UriAppendString(mapping.getMappedIndex(returnIK),
							mapping.getMappedIndex(uriKey), mapping.getMappedIndex(stringKey));
						
						logger.debug("\t Uri.withAppendedPath(): "+ invoke + ", returnIK: " + returnIK
							+ ", uriKey: " + uriKey + ", stringKey: " + stringKey);
						logger.debug("\t returnIK_Index: " + mapping.getMappedIndex(returnIK)
							+ ", uriKey_Index: " + mapping.getMappedIndex(uriKey) + ", stringKey_Index: "
							+ mapping.getMappedIndex(stringKey));
						
						if (!nodeMap.containsKey(returnIK)) {
							addNode(node);
							nodeMap.put(returnIK, node);
							final HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
							iks.add(uriKey);
							iks.add(stringKey);
							unresolvedDependencies.put(node, iks);
						}
					}
				}
			}
		}
    }
    
    private void handleUriWitAppendPath(final NormalAllocationInNode ik, final PointerAnalysis<InstanceKey> pa,
    		final OrdinalSetMapping<InstanceKey> mapping,
    		final Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies) {
    	final CGNode allocNode = ik.getNode();
        final Context context = allocNode.getContext();
        final CGNode caller = (CGNode) context.get(ContextKey.CALLER);
        
        if (hasSignature(allocNode, "android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri;")) {
            //Doesn't seem to be entering this else with the current android jar -- reimplemented above using LocalPointerKey
            logger.debug("android.net.Uri.withAppendedPath(Landroid/net/Uri;Ljava/lang/String;)Landroid/net/Uri call: " + caller);
            final CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);
            final SSAInvokeInstruction invoke =
            		(SSAInvokeInstruction) caller.getIR().getBasicBlocksForCall(csr)[0].getLastInstruction();
            final OrdinalSet<InstanceKey> ptsUri =
            	pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(0)));

            if (!ptsUri.isEmpty()) {
                final InstanceKey uriKey = ptsUri.iterator().next();
                final OrdinalSet<InstanceKey> points =
                	pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(1)));

                if (!points.isEmpty()) {
                    final InstanceKey stringKey = points.iterator().next();
                    final UriAppendString node =
                    	new UriAppendString(mapping.getMappedIndex(ik),
                    			mapping.getMappedIndex(uriKey),
                    			mapping.getMappedIndex(stringKey));
                    
                    logger.debug("\t Uri.withAppendedPath(): "+ invoke + "..." + uriKey + "..." + stringKey);
                    addNode(node);
                    nodeMap.put(ik, node);
                    final HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
                    iks.add(uriKey);
                    iks.add(stringKey);
                    unresolvedDependencies.put(node, iks);
                }
            }
        }
    }
    
    private void handleUriParse(final NormalAllocationInNode ik, final PointerAnalysis<InstanceKey> pa,
    		final OrdinalSetMapping<InstanceKey> mapping,
    		final Map<InstanceKeySite, Set<InstanceKey>> unresolvedDependencies) {
    	final CGNode allocNode = ik.getNode();
        final Context context = allocNode.getContext();
        final CGNode caller = (CGNode) context.get(ContextKey.CALLER);
    	
    	if (hasSignature(allocNode, "android.net.Uri.parse(Ljava/lang/String;)Landroid/net/Uri;")) {
            final CallSiteReference csr = (CallSiteReference) context.get(ContextKey.CALLSITE);
            final SSAInvokeInstruction invoke =
            		(SSAInvokeInstruction) caller.getIR().getBasicBlocksForCall(csr)[0].getLastInstruction();
            logger.debug("invoke inst: " + invoke + " getuse: " + invoke.getUse(0));
            logger.debug("in node: " + caller);
            final OrdinalSet<InstanceKey> points =
            	pa.getPointsToSet(new LocalPointerKey(caller, invoke.getUse(0)));

            if (!points.isEmpty()) {
                final InstanceKey stringKey = points.iterator().next();
                final UriParseString node = new UriParseString(
                	mapping.getMappedIndex(ik),
                	mapping.getMappedIndex(stringKey));
                
                logger.debug("\t Uri.parse(): "+ invoke + "..." + stringKey);
                addNode(node);
                nodeMap.put(ik, node);
                final HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
                iks.add(stringKey);
                unresolvedDependencies.put(node, iks);
            }
    	}
    }

    public void removeNodeAndEdges(InstanceKeySite n) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    public void addNode(final InstanceKeySite n) {
        predecessors.put(n, new HashSet<InstanceKeySite>());
        successors.put(n, new HashSet<InstanceKeySite>());
        nodes.add(n);
    }

    public boolean containsNode(final InstanceKeySite n) {
        return nodes.contains(n);
    }

    public int getNumberOfNodes() {
        return nodes.size();
    }

    public Iterator<InstanceKeySite> iterator() {
        return nodes.iterator();
    }

    public void removeNode(final InstanceKeySite n) {
        throw new UnsupportedOperationException();
    }

    public void addEdge(final InstanceKeySite src, final InstanceKeySite dst) {
        Set<InstanceKeySite> predSet = predecessors.get(dst);
        if (predSet == null) {
            predSet = new HashSet<InstanceKeySite>();
            predecessors.put(dst, predSet);
        }
        predSet.add(src);

        Set<InstanceKeySite> succSet = successors.get(src);
        if (succSet == null) {
            succSet = new HashSet<InstanceKeySite>();
            successors.put(src, succSet);
        }
        succSet.add(dst);
    }

    public int getPredNodeCount(final InstanceKeySite n) {
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

    private static boolean isApplicationCode(final IMethod im) {
    	return isApplicationCode(im.getReference());
    }
    
    private static boolean isApplicationCode(final MethodReference mref) {
    	return isApplicationCode(mref.getDeclaringClass());
    }
    
    private static boolean isApplicationCode(final TypeReference tref) {
    	return tref.getClassLoader().equals(ClassLoaderReference.Application);
    }
    
    private static boolean isOfType(final InstanceKey ik, final String typeName) {
    	return typeName.equals(ik.getConcreteType().getName().toString());
    }
    
    private static boolean hasSignature(final CGNode n, final String signature) {
    	return hasSignature(n.getMethod(), signature);
    }
    
    private static boolean hasSignature(final IMethod im, final String signature) {
    	return signature.equals(im.getSignature());
    }
    
    private static boolean hasSignature(final LocalPointerKey pk, final String signature) {
    	return hasSignature(pk.getNode(), signature);
    }
    
    private static boolean hasSignature(final InstanceKeyWithNode ik, final String signature) {
    	return hasSignature(ik.getNode(), signature);
    }
}
