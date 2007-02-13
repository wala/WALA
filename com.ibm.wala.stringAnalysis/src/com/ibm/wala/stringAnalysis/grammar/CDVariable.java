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
package com.ibm.wala.stringAnalysis.grammar;

import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;

public class CDVariable extends Variable {
    private int valueNumber;
    private CallGraph callGraph;
    private CGNode node;
    private CallSiteReference callSite;
    
    public CDVariable(String name, int v, CallGraph cg, CGNode node, CallSiteReference callSite) {
        super(name);
        this.valueNumber = v;
        this.callGraph = cg;
        this.node = node;
        this.callSite = callSite;
    }
    
    public int getValueNumber() {
        return valueNumber;
    }
    
    public CallGraph getCallGraph() {
        return callGraph;
    }
    
    public CGNode getCGNode() {
        return node;
    }
    
    public CallSiteReference getCallSiteReference() {
        return callSite;
    }
    
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            CDVariable v = (CDVariable) obj;
            return (valueNumber == v.getValueNumber())
                && callGraph.equals(v.getCallGraph())
                && node.equals(v.getCGNode())
                && ((callSite==null) ? (v.getCallSiteReference()==null) : callSite.equals(v.getCallSiteReference()));
        }
        return false;
    }
    
    public String toString() {
        String s = super.toString();
        s = s + "[" + valueNumber + "]" + "[CGNode:" + callGraph.getNumber(node) + "]";
        if (callSite != null) {
            s = s + "[CallSiteReference:" + System.identityHashCode(callSite) + "]";
        }
        return s;
    }
}
