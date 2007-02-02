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
package com.ibm.wala.stringAnalysis.translator;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.stringAnalysis.grammar.*;

public class FunctionNameCalleeResolver implements ICalleeResolver {
    private IFunctionNameResolver functionNameResolver;
    
    public FunctionNameCalleeResolver(IFunctionNameResolver resolver) {
        functionNameResolver = resolver;
    }
    
    public Set<GR> resolve(PropagationCallGraphBuilder builder, CallGraph cg, CGNode node, GR gr, GRule rule, CalleeMap calleeMap) {
        Set<GR> grammars = new HashSet<GR>();
        ISymbol sym = (ISymbol) rule.getRight(0);
        if (!(sym instanceof InvocationSymbol)) {
            Set<GRule> rules = new HashSet<GRule>();
            rules.add(rule);
            GR g = GR.createGR(gr.getIR(), rules);
            grammars.add(g);
            return grammars;
        }
        InvocationSymbol isym = (InvocationSymbol) sym;
        Set<InvocationSymbol> msyms = functionNameResolver.resolve(builder, cg, node, gr, isym);
        for (InvocationSymbol msym : msyms) {
            Set<GRule> rules = new HashSet<GRule>();
            GRule newRule = new GRule(gr.getIR(), msym.getInstruction(), rule.getLeft(), new ISymbol[]{msym});
            rules.add(newRule);
            GR g = GR.createGR(gr.getIR(), rules);
            grammars.add(g);
        }
        return grammars;
    }

}
