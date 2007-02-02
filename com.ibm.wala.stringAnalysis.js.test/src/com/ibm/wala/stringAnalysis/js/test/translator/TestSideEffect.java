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
package com.ibm.wala.stringAnalysis.js.test.translator;

import com.ibm.wala.stringAnalysis.grammar.*;
import com.ibm.wala.stringAnalysis.js.translator.*;
import com.ibm.wala.stringAnalysis.translator.*;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.ipa.callgraph.propagation.*;

public class TestSideEffect extends TestJSTranslatorBase {
    private GR gr;
    private GR2CFG gr2cfg;
    private ISimplify approximation;
    
    public void setUp() throws Exception {
        super.setUp();
        SSA2Rule ssa2rule = new JSSSA2Rule();
        BB2GR bb2gr = new BB2GR(ssa2rule);
        IR2GR ir2gr = new IR2GR(bb2gr);
        CG2GR cg2gr = new CG2GR(ir2gr, new FunctionNameCalleeResolver(new JSFunctionNameResolver()));
        gr2cfg = new GR2CFG(new JSTranslatorRepository());
        gr = (GR) cg2gr.translate(getCallGraphBuilder());
        ControlledGrammars.inlineExpansion(gr);
        approximation = new SideEffectSolver(gr, new String[]{"write"}, new String[]{});
        approximation = new RuleAdder(approximation, new IProductionRule[]{
                new ProductionRule(new LexicalVariable("document"), new ISymbol[]{}),
        });
    }
    
    protected PropagationCallGraphBuilder makeCallGraphBuilder() {
        return makeCallGraphBuilder("output.js");
    }
    
    public void testSideEffectSolver1() {
        Trace.println("-- gr:");
        Trace.println(SAUtil.prettyFormat(gr));
        IContextFreeGrammar cfg = gr2cfg.solve(approximation, new LexicalVariable("result"));
        Trace.println(SAUtil.prettyFormat(cfg));
        
        IAutomaton abc = pattern("a(b|B)c");
        verifyCFG(abc, cfg);
    }

}
