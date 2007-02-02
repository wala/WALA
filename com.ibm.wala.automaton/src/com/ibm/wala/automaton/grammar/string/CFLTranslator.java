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
package com.ibm.wala.automaton.grammar.string;

import java.util.*;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.*;

public class CFLTranslator {    
    private IAutomaton transducer;
    private IAutomaton lastResult;
    
    public CFLTranslator(IAutomaton transducer) {
        this.transducer = transducer;
    }
    
    public IContextFreeGrammar translate(IContextFreeGrammar cfg) {
        CFLReachability.TraceableTransitionFactory factory
            = new CFLReachability.TraceableTransitionFactory(
                    new CFLReachability.ProductionRuleTransitionFactory());
        lastResult = CFLReachability.analyze(transducer, cfg, factory);
        Set paths = factory.getCorrespondingTransitions();
        Set transitions = CFLReachability.selectConnectedTransitions(paths, transducer.getInitialState(), transducer.getFinalStates(), cfg.getStartSymbol());
        Set rules = new HashSet();
        for (Iterator i = transitions.iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            if ((t instanceof CFLReachability.ProductionRuleTransition) /* && (t.getInputSymbol() instanceof IVariable) */) {
                IProductionRule rule = new ProductionRule((IVariable)t.getInputSymbol(), AUtil.list(t.getOutputSymbols()));
                rules.add(rule);
            }
        }
        if (rules.isEmpty()) {
            IProductionRule rule = new ProductionRule(cfg.getStartSymbol(), new ISymbol[0]);
            rules.add(rule);
        }
        IContextFreeGrammar cfg2 = new ContextFreeGrammar(cfg.getStartSymbol(), rules);
        return cfg2;
    }
    
    static public IContextFreeGrammar translate(IAutomaton transducer, IContextFreeGrammar cfg) {
        return (new CFLTranslator(transducer)).translate(cfg);
    }
    
    public IAutomaton getLastResult() {
        return lastResult;
    }
}
