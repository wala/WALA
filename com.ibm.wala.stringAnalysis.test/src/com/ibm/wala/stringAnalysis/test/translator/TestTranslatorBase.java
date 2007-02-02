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
package com.ibm.wala.stringAnalysis.test.translator;

import java.util.*;

import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.ssa.*;
import com.ibm.wala.stringAnalysis.test.*;
import com.ibm.wala.stringAnalysis.util.*;

abstract public class TestTranslatorBase extends SAJunitBase {
    private List irs = new ArrayList();
    private List nodes = new ArrayList();
    private PropagationCallGraphBuilder callGraphBuilder = null;
    
    protected abstract PropagationCallGraphBuilder makeCallGraphBuilder();
    
    protected void setUp() throws Exception {
        super.setUp();
        callGraphBuilder = makeCallGraphBuilder( );
        
        ExplicitCallGraph cg = callGraphBuilder.getCallGraph();
        //cg.registerEntrypoint(cg.getFakeRootNode());
        int n = cg.getNumberOfNodes();
        for (int i = 0; i < n; i++) {
            CGNode node = (CGNode) cg.getNode(i);
            IR ir = SAUtil.Domo.getIR(cg, node);
            nodes.add(node);
            irs.add(ir);
        }
    }
    
    public List getIRs() {
        return irs;
    }
    
    public List getCGNodes() {
        return nodes;
    }
    
    public PropagationCallGraphBuilder getCallGraphBuilder() {
        return callGraphBuilder;
    }
    
    
}
