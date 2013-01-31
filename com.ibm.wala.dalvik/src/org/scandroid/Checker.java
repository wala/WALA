package org.scandroid;
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.scandroid.flow.types.FlowType;
import org.scandroid.flow.types.IKFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.NormalAllocationInNode;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;



public class Checker {
	private static final Logger logger = LoggerFactory.getLogger(Checker.class);

    private static boolean addFlow(IKFlow source, IKFlow dest, Map<IKFlow, Set<IKFlow>> flow)
    {
        Set<IKFlow> dests = flow.get(source);
        if(dests == null)
        {
            dests = new HashSet<IKFlow>();
            flow.put(source, dests);
        }
        return dests.add(dest);
    }

    private static boolean addFlow(FlowType source, FlowType dest, Map<FlowType, Set<FlowType>> flow)
    {
        Set<FlowType> dests = flow.get(source);
        if(dests == null)
        {
            dests = new HashSet<FlowType>();
            flow.put(source, dests);
        }
        return dests.add(dest);
    }

    private static Map<IKFlow, Set<IKFlow>> computeClosure(Map<FlowType, Set<FlowType>> permissionFlow)
    {
        Map<IKFlow, Set<IKFlow>> uriFlow = new HashMap<IKFlow, Set<IKFlow>>();
        boolean changed = true;
        Map<FlowType, Set<FlowType>> growingFlow = new HashMap<FlowType,Set<FlowType>>(permissionFlow);
        Map<FlowType, Set<FlowType>> newFlow = new HashMap<FlowType,Set<FlowType>>();
        while(changed)
        {
            changed = false;
            newFlow.clear();
            for(Entry<FlowType, Set<FlowType>> e: growingFlow.entrySet())
            {
                if(e.getKey() instanceof IKFlow)
                {
                    for(FlowType v:e.getValue())
                    {
                        /*
                        if(v instanceof IKFlow)
                            addFlow((IKFlow)e.getKey(),(IKFlow)v,uriFlow);
                        else
                        {
                            // find all of the possible targets
                            if(v instanceof ActivityCallFlow || v instanceof ServiceCallFlow)
                            {
                                // find all instances of InputFlow
                                for(Entry<FlowType, Set<FlowType>> e2:permissionFlow.entrySet())
                                {
                                    if(e2.getKey() instanceof InputFlow)
                                    {
                                        for(FlowType v2: e2.getValue())
                                        {
                                            addFlow(e.getKey(),v2,newFlow);
                                        }
                                    }
                                }
                            }
                            else if(v instanceof ReturnFlow)
                            {
                                for(Entry<FlowType, Set<FlowType>> e2:permissionFlow.entrySet())
                                {
                                    if(e2.getKey() instanceof ReturnFlow)
                                    {
                                        for(FlowType v2: e2.getValue())
                                        {
                                            addFlow(e.getKey(),v2,newFlow);
                                        }
                                    }
                                }
                            }
                            else if(v instanceof BinderFlow)
                            {
                                for(Entry<FlowType, Set<FlowType>> e2:permissionFlow.entrySet())
                                {
                                    if(e2.getKey() instanceof BinderFlow)
                                    {
                                        for(FlowType v2: e2.getValue())
                                        {
                                            addFlow(e.getKey(),v2,newFlow);
                                        }
                                    }
                                }
                            }
                        }
                        */
                    }
                }
            }
            for(Entry<FlowType,Set<FlowType>> e: newFlow.entrySet())
            {
                for(FlowType v:e.getValue())
                {
                    changed = addFlow(e.getKey(),v,growingFlow) || changed;
                }
            }
        }

        return uriFlow;
    }

    public static void check(
            Map<FlowType, Set<FlowType>> permissionOutflow,
            Permissions perms, Map<InstanceKey, String> prefixes) {

        // compute the transitive closure of permissionOutflow

        Map<IKFlow,Set<IKFlow>> uriFlow = computeClosure(permissionOutflow);

        HashMap<InstanceKey, HashSet<String>> readPerms = new HashMap<InstanceKey, HashSet<String>>();
        HashMap<InstanceKey, HashSet<String>> writePerms = new HashMap<InstanceKey, HashSet<String>>();
        for (Entry<InstanceKey, String> prefix: prefixes.entrySet()) {
            if (prefix.getKey() instanceof NormalAllocationInNode) {
                NormalAllocationInNode ik = (NormalAllocationInNode) prefix.getKey();
                if (ik.getConcreteType().getName().toString().contains("Landroid/net/Uri")) {
//                  logger.debug(pa.getInstanceKeyMapping().getMappedIndex(ik) + " << " + perms.readPerms(prefix.getValue()));
                    readPerms.put(ik, perms.readPerms(prefix.getValue()));
//                  logger.debug(pa.getInstanceKeyMapping().getMappedIndex(ik) + " >> " + perms.writePerms(prefix.getValue()));
                    writePerms.put(ik, perms.writePerms(prefix.getValue()));
                }
            }
        }
        logger.debug("*********************");
        logger.debug("*    Constraints    *");
        logger.debug("*********************");

        /*
        for (Entry<IKFlow, Set<IKFlow>> e: uriFlow.entrySet()) {
            if(e.getKey() instanceof IKFlow)
            {
                InstanceKey sourceIK = ((IKFlow)e.getKey()).ik;
                if (readPerms.containsKey(sourceIK)) {
                    for (FlowType f: e.getValue()) {
                        if(f instanceof IKFlow)
                        {
                            if (readPerms.containsKey(((IKFlow)f).ik))
                                logger.debug(readPerms.get(((IKFlow)f).ik) + " can read " + readPerms.get(sourceIK));
                        }
                    }
                }
                if (writePerms.containsKey(sourceIK)) {
                    for (FlowType f: e.getValue()) {
                        if(f instanceof IKFlow)
                            if (writePerms.containsKey(((IKFlow)f).ik)) logger.debug(writePerms.get(sourceIK) + " can write " + writePerms.get(((IKFlow)f).ik));
                    }
                }
            }
        }
        */
    }

    private static Map<InstanceKey, Set<InstanceKey>> fake(PointerAnalysis pa) {
        Map<InstanceKey, Set<InstanceKey>> permissionOutflow = new HashMap<InstanceKey, Set<InstanceKey>>();
        HashSet<InstanceKey> iks = new HashSet<InstanceKey>();
        iks.add(pa.getInstanceKeyMapping().getMappedObject(25));
        permissionOutflow.put(pa.getInstanceKeyMapping().getMappedObject(29), iks);

        return permissionOutflow;
    }

    public static void fakeCheck(PointerAnalysis pa,
            Map<InstanceKey, Set<InstanceKey>> permissionOutflow,
            Permissions perms, Map<InstanceKey, String> prefixes) {
        permissionOutflow = fake(pa);
        HashMap<InstanceKey, HashSet<String>> readPerms = new HashMap<InstanceKey, HashSet<String>>();
        HashMap<InstanceKey, HashSet<String>> writePerms = new HashMap<InstanceKey, HashSet<String>>();
        for (Entry<InstanceKey, String> prefix: prefixes.entrySet()) {
            if (prefix.getKey() instanceof NormalAllocationInNode) {
                NormalAllocationInNode ik = (NormalAllocationInNode) prefix.getKey();
                if (ik.getConcreteType().getName().toString().equals("Landroid/net/Uri")) {
                    logger.debug(pa.getInstanceKeyMapping().getMappedIndex(ik) + " << " + perms.readPerms(prefix.getValue()));
                    readPerms.put(ik, perms.readPerms(prefix.getValue()));
                    logger.debug(pa.getInstanceKeyMapping().getMappedIndex(ik) + " >> " + perms.writePerms(prefix.getValue()));
                    writePerms.put(ik, perms.writePerms(prefix.getValue()));
                }
            }
        }
        logger.debug("*********************");
        logger.debug("*    Constraints    *");
        logger.debug("*********************");
        for (Entry<InstanceKey, Set<InstanceKey>> e: permissionOutflow.entrySet()) {
            if (readPerms.containsKey(e.getKey())) {
                for (InstanceKey k: e.getValue()) {
                    if (readPerms.containsKey(k)) logger.debug(readPerms.get(k) + " can read " + readPerms.get(e.getKey()));
                }
            }
            if (writePerms.containsKey(e.getKey())) {
                for (InstanceKey k: e.getValue()) {
                    if (writePerms.containsKey(k)) logger.debug(writePerms.get(e.getKey()) + " can write " + writePerms.get(k));
                }
            }
        }

    }




}
