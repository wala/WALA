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
package com.ibm.wala.stringAnalysis.test.grammar;

import java.util.HashMap;
import java.util.Iterator;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.FreshVariableFactory;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.SimpleVariableFactory;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Symbol;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.stringAnalysis.grammar.ControlledGrammars;
import com.ibm.wala.stringAnalysis.grammar.IRegularlyControlledGrammar;
import com.ibm.wala.stringAnalysis.grammar.RegularlyControlledGrammar;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class TestControlledGrammars extends GRJunitBase {
    public void testUseUniqueVariables() {
        IAutomaton automaton1 = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails1[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels1[] = new ISymbol[]{i1, i2, i3, i4};
        IProductionRule rules1[] = new IProductionRule[]{
                new ProductionRule(v0, new ISymbol[]{new Symbol("a")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("b")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("c")}),
                new ProductionRule(v2, new ISymbol[]{v1}),
        };
        RegularlyControlledGrammar g1 = new RegularlyControlledGrammar(automaton1, fails1, labels1, rules1);
        RegularlyControlledGrammar g2 =
          ControlledGrammars.useUniqueVariables(g1,
            new FreshVariableFactory(
              SimpleVariableFactory.defaultFactory,
              SAUtil.set(new String[]{"v0","v1","v2"})), new HashMap());
        for (Iterator i = g2.getNonterminals().iterator(); i.hasNext(); ) {
            IVariable v = (IVariable) i.next();
            assertFalse(g1.getNonterminals().contains(v));
        }
    }

    public void testCreateUnion() {
        IAutomaton automaton1 = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails1[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels1[] = new ISymbol[]{i1, i2, i3, i4};
        IProductionRule rules1[] = new IProductionRule[]{
                new ProductionRule(v0, new ISymbol[]{new Symbol("a")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("b")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("c")}),
                new ProductionRule(v2, new ISymbol[]{v1}),
        };
        RegularlyControlledGrammar g1 = new RegularlyControlledGrammar(automaton1, fails1, labels1, rules1);
        
        IAutomaton automaton2 = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails2[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels2[] = new ISymbol[]{i1, i2, i3, i4};
        IProductionRule rules2[] = new IProductionRule[]{
                new ProductionRule(v0, new ISymbol[]{new Symbol("a")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("b")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("c")}),
                new ProductionRule(v2, new ISymbol[]{v1}),
        };
        IRegularlyControlledGrammar g2 = new RegularlyControlledGrammar(automaton2, fails2, labels2, rules2);

        IRegularlyControlledGrammar g3 = ControlledGrammars.createUnion(g1, g2);
        
        assertEquals(8, g3.getRuleMap().size());
        // TODO: should test this?
        //assertEquals(v0, g3.getStartSymbol());
        assertEquals(8, g3.getFails().size());
    }
    
    public void testCreateConcatenation() {
        IAutomaton automaton1 = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails1[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels1[] = new ISymbol[]{i1, i2, i3, i4};
        IProductionRule rules1[] = new IProductionRule[]{
                new ProductionRule(v0, new ISymbol[]{new Symbol("a")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("b")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("c")}),
                new ProductionRule(v2, new ISymbol[]{v1}),
        };
        RegularlyControlledGrammar g1 = new RegularlyControlledGrammar(automaton1, fails1, labels1, rules1);
        
        IAutomaton automaton2 = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails2[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels2[] = new ISymbol[]{i1, i2, i3, i4};
        IProductionRule rules2[] = new IProductionRule[]{
                new ProductionRule(v0, new ISymbol[]{new Symbol("a")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("b")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("c")}),
                new ProductionRule(v2, new ISymbol[]{v1}),
        };
        IRegularlyControlledGrammar g2 = new RegularlyControlledGrammar(automaton2, fails2, labels2, rules2);

        IRegularlyControlledGrammar g3 = ControlledGrammars.createConcatenation(g1, g2);

        assertEquals(8, g3.getRuleMap().size());
        assertEquals(v0, g3.getStartSymbol());
        assertEquals(8, g3.getFails().size());
    }
    
    public void testCreateClosure() {
        IAutomaton automaton1 = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails1[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels1[] = new ISymbol[]{i1, i2, i3, i4};
        IProductionRule rules1[] = new IProductionRule[]{
                new ProductionRule(v0, new ISymbol[]{new Symbol("a")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("b")}),
                new ProductionRule(v1, new ISymbol[]{v0, new Symbol("c")}),
                new ProductionRule(v2, new ISymbol[]{v1}),
        };
        RegularlyControlledGrammar g1 = new RegularlyControlledGrammar(automaton1, fails1, labels1, rules1);
        
        IRegularlyControlledGrammar g3 = ControlledGrammars.createClosure(g1);

        assertEquals(4, g3.getRuleMap().size());
        assertEquals(v0, g3.getStartSymbol());
        assertEquals(4, g3.getFails().size());
        
        IAutomaton expectedAutomaton = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                    new Transition(s3, s0, i5),
                });
        assertEquals(expectedAutomaton, g3.getAutomaton());
    }
}
