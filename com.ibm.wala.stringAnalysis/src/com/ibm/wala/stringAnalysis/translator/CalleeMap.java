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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.stringAnalysis.grammar.GR;

public class CalleeMap {
    private Map cnode2gr;
    private Map gr2cnodes;
    private Set entryGRs;
    
    public CalleeMap() {
        cnode2gr = new HashMap();
        gr2cnodes = new HashMap();
        entryGRs = new HashSet();
    }
    
    public void put(CalleeNode cnode, GR gr) {
        cnode2gr.put(cnode, gr);
        Set s = (Set) gr2cnodes.get(gr);
        if (s == null) {
            s = new HashSet();
            gr2cnodes.put(gr, s);
        }
        s.add(cnode);
        
        if (cnode.caller == null) {
            entryGRs.add(gr);
        }
    }
    
    public GR get(CalleeNode cnode) {
        return (GR) cnode2gr.get(cnode);
    }
    
    public Set get(GR gr) {
        return (Set) gr2cnodes.get(gr);
    }
    
    public Set getGRs() {
        return gr2cnodes.keySet();
    }
    
    public Set getCalleeNodes() {
        return cnode2gr.keySet();
    }
    
    public Set getEntryGRs() {
        return entryGRs;
    }
}