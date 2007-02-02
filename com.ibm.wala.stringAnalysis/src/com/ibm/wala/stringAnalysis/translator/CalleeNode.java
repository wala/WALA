/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/**
 * 
 */
package com.ibm.wala.stringAnalysis.translator;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

public class CalleeNode {
    public CallSiteReference callSite; // null is ok.
    public CGNode callee; // target
    public CGNode caller;
    public InstanceKey ikey;
    public CallGraph cg;
    
    public CalleeNode(CallGraph cg, CGNode caller, CGNode callee, CallSiteReference callSite, InstanceKey ikey) {
        this.cg = cg;
        this.callee = callee;
        this.caller = caller;
        this.callSite = callSite;
        this.ikey = ikey;
    }
    
    public int hashCode() {
        return cg.hashCode()
            + ((caller==null) ? 0 : caller.hashCode())
            + callee.hashCode()
            + ((callSite==null) ? 0 : callSite.hashCode())
            + ((ikey==null) ? 0 : ikey.hashCode());
    }
    
    public String toString() {
        int callSiteId = 0;
        int callerId = 0;
        int calleeId = 0;
        if (callSite != null) {
            callSiteId = System.identityHashCode(callSite);
        }
        callerId = ((caller==null) ? -1 : cg.getNumber(caller));
        calleeId = ((callee==null) ? -1 : cg.getNumber(callee));
        return "{caller=" + callerId + ", callee=" + calleeId + ", callSite=" + callSiteId + ", ikey=" + ikey + "}";
    }
    
    public boolean equals(Object obj) {
        if (getClass().equals(obj.getClass())) {
            CalleeNode other = (CalleeNode) obj;
            return cg.equals(other.cg)
                && ((caller==null) ? (other.caller==null) : caller.equals(other.caller))
                && callee.equals(other.callee)
                && ((ikey==null) ? (other.ikey==null) : ikey.equals(other.ikey))
                && ((callSite==null) ? (other.callSite==null) : callSite.equals(other.callSite));
        }
        else {
            return false;
        }
    }
}