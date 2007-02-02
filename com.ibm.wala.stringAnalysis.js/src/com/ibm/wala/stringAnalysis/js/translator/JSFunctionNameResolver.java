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
package com.ibm.wala.stringAnalysis.js.translator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.GRule;
import com.ibm.wala.stringAnalysis.grammar.InvocationSymbol;
import com.ibm.wala.stringAnalysis.grammar.MemberVariable;
import com.ibm.wala.stringAnalysis.translator.IFunctionNameResolver;

public class JSFunctionNameResolver implements IFunctionNameResolver {
    public Set resolve(PropagationCallGraphBuilder builder, CallGraph cg, CGNode node, GR gr, InvocationSymbol invoke) {
        Set symbols = new HashSet();
        if (invoke.getFunction() instanceof IVariable) {
            Set rs = gr.getRules((IVariable) invoke.getFunction());
            for (Iterator i = rs.iterator(); i.hasNext(); ) {
                GRule r = (GRule) i.next();
                ISymbol s = r.getRight(0);
                if (s instanceof MemberVariable) {
                    MemberVariable mvar = (MemberVariable) s;
                    InvocationSymbol msym = new InvocationSymbol(invoke.getIR(), invoke.getInstruction(), mvar.getMember(), invoke.getParameter(0), invoke.getParameters());
                    symbols.add(msym);
                }
            }
        }
        else {
            symbols.add(invoke);
        }
        return symbols;
    }
}
