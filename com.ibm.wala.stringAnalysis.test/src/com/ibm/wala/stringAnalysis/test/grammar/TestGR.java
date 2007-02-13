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

import com.ibm.wala.automaton.grammar.string.ContextFreeGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.string.Automaton;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Symbol;
import com.ibm.wala.automaton.string.Transition;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.GRule;
import com.ibm.wala.stringAnalysis.grammar.RegularlyControlledGrammar;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class TestGR extends GRJunitBase {
    public void testNew1() {
        IAutomaton automaton = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels[] = new ISymbol[]{i1, i2, i3, i4};
        GRule rules[] = new GRule[]{
                new GRule(
                        null,
                        new DummySSAInstruction(0, 'a'),
                        v0, new ISymbol[]{new Symbol("a")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'b'),
                        v1, new ISymbol[]{v0, new Symbol("b")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'c'),
                        v1, new ISymbol[]{v0, new Symbol("c")}),
                new GRule(
                        null,
                        new DummySSAInstruction(2, 1),
                        v2, new ISymbol[]{v1}),
        };
        GR g = new GR(null, null, null, automaton, fails, labels, rules);
        assertEquals(automaton, g.getAutomaton());
        assertEquals(SAUtil.set(fails), g.getFails());
        assertEquals(SAUtil.map(labels,rules), g.getRuleMap());
    }
    
    public void testEquality1() {
        IAutomaton automaton = new Automaton(
                s0, new State[]{s3},
                new ITransition[]{
                    new Transition(s0, s1, i1),
                    new Transition(s1, s2, i2),
                    new Transition(s2, s3, i3),
                    new Transition(s3, s4, i4),
                });
        ISymbol fails[] = new ISymbol[]{i1, i2, i3, i4};
        ISymbol labels[] = new ISymbol[]{i1, i2, i3, i4};
        GRule rules[] = new GRule[]{
                new GRule(
                        null,
                        new DummySSAInstruction(0, 'a'),
                        v0, new ISymbol[]{new Symbol("a")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'b'),
                        v1, new ISymbol[]{v0, new Symbol("b")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'c'),
                        v1, new ISymbol[]{v0, new Symbol("c")}),
                new GRule(
                        null,
                        new DummySSAInstruction(2, 1),
                        v2, new ISymbol[]{v1}),
        };
        GR g1 = new GR(null, null, null, automaton, fails, labels, rules);
        GR g2 = new GR(g1);
        assertTrue(g1.equals(g2));
        assertTrue(g2.equals(g1));
    }
    
    public void testEquality2() {
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
        GRule rules1[] = new GRule[]{
                new GRule(
                        null,
                        new DummySSAInstruction(0, 'a'),
                        v0, new ISymbol[]{new Symbol("a")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'b'),
                        v1, new ISymbol[]{v0, new Symbol("b")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'c'),
                        v1, new ISymbol[]{v0, new Symbol("c")}),
                new GRule(
                        null,
                        new DummySSAInstruction(2, 1),
                        v2, new ISymbol[]{v1}),
        };
        GR g1 = new GR(null, null, null, automaton1, fails1, labels1, rules1);
        
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
        GRule rules2[] = new GRule[]{
                new GRule(
                        null,
                        new DummySSAInstruction(0, 'a'),
                        v0, new ISymbol[]{new Symbol("a")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'b'),
                        v1, new ISymbol[]{v0, new Symbol("b")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'z'),
                        v1, new ISymbol[]{v0, new Symbol("z")}),
                new GRule(
                        null,
                        new DummySSAInstruction(2, 1),
                        v2, new ISymbol[]{v1}),
        };
        GR g2 = new GR(null, null, null, automaton2, fails2, labels2, rules2);

        assertFalse(g1.equals(g2));
        assertFalse(g2.equals(g1));
    }
    
    public void testCFG() {
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
                new GRule(
                        null,
                        new DummySSAInstruction(0, 'a'),
                        v0, new ISymbol[]{new Symbol("a")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'b'),
                        v1, new ISymbol[]{v0, new Symbol("b")}),
                new GRule(
                        null,
                        new DummySSAInstruction(1, 0, 'c'),
                        v1, new ISymbol[]{v0, new Symbol("c")}),
                new GRule(
                        null,
                        new DummySSAInstruction(2, 1),
                        v2, new ISymbol[]{v1}),
        };
        RegularlyControlledGrammar g1 = new RegularlyControlledGrammar(automaton1, fails1, labels1, rules1);

        ContextFreeGrammar cfg = new ContextFreeGrammar(
                v0,
                new IProductionRule[]{
                    new GRule(
                            null,
                            new DummySSAInstruction(0, 'a'),
                            v0, new ISymbol[]{new Symbol("a")}),
                    new GRule(
                            null,
                            new DummySSAInstruction(1, 0, 'b'),
                            v1, new ISymbol[]{v0, new Symbol("b")}),
                    new GRule(
                            null,
                            new DummySSAInstruction(1, 0, 'c'),
                            v1, new ISymbol[]{v0, new Symbol("c")}),
                    new GRule(
                            null,
                            new DummySSAInstruction(2, 1),
                            v2, new ISymbol[]{v1}),
                }
        );
        
        assertEquals(cfg, g1.toSimple());
    }
}
