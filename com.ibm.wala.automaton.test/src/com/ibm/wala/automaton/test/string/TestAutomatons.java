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
package com.ibm.wala.automaton.test.string;

import java.util.*;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.test.AutomatonJunitBase;

public class TestAutomatons extends AutomatonJunitBase {
    public void testUseUniqueStates() {
        IAutomaton a1 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                }
        );
        Map stateMap = new HashMap();
        IAutomaton a2 = Automatons.useUniqueStates(a1, a1, stateMap);
        Set states = AUtil.set(new State[]{s4, s5, s6});
        assertEquals(states, a2.getStates());
        assertTrue(states.contains(a2.getInitialState()));
        assertTrue(states.containsAll(a2.getFinalStates()));
    }
    
    public void testUseUniqueInputSymbols() {
        IAutomaton a1 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                }
        );
        Map symMap = new HashMap();
        IAutomaton a2 = Automatons.useUniqueInputSymbols(a1, a1, symMap);
        Set syms = AUtil.set(new Symbol[]{i1, i2});
        assertEquals(syms, Automatons.collectInputSymbols(a2));
        assertEquals(2, symMap.size());
        assertTrue(symMap.get(a).equals(i1)
                || symMap.get(a).equals(i2));
        assertTrue(symMap.get(b).equals(i1)
                || symMap.get(b).equals(i2));
    }
    
    public void testCreateConcatenation() {
        IAutomaton a1 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                }
        );
        IAutomaton a2 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                }
        );
        Map stateMap = new HashMap();
        IAutomaton a3 = Automatons.createConcatenation(a1, a2, stateMap);
        assertEquals(
                AUtil.set(new State[]{s1, s2, s3, s4, s5, s6}),
                a3.getStates());
        assertEquals(5, a3.getTransitions().size());
        assertTrue(a3.accept(AUtil.list(new Symbol[]{a, b, a, b})));
        assertFalse(a3.accept(AUtil.list(new Symbol[]{a, b, a})));
    }

    public void testCreateUnion() {
        IAutomaton a1 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                }
        );
        IAutomaton a2 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, A),
                    new Transition(s2, s3, B),
                }
        );
        Map stateMap = new HashMap();
        IAutomaton a3 = Automatons.createUnion(a1, a2, stateMap);
        assertEquals(
                AUtil.set(new State[]{s1, s2, s3, s4, s5, s6, s7}),
                a3.getStates());
        assertEquals(6, a3.getTransitions().size());
        Automatons.eliminateEpsilonTransitions(a3);
        Automatons.eliminateNonDeterministics(a3);
        assertTrue(a3.accept(AUtil.list(new Symbol[]{a, b})));
        assertTrue(a3.accept(AUtil.list(new Symbol[]{A, B})));
        assertFalse(a3.accept(AUtil.list(new Symbol[]{A, b})));
    }

    public void testCreateComplement() {
        IAutomaton a1 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                }
        );
        IAutomaton a2 = Automatons.createComplement(a1);
        assertFalse(a2.accept(AUtil.list(new Symbol[]{a, b})));
        assertTrue(a2.accept(AUtil.list(new Symbol[]{a, B})));
        assertTrue(a2.accept(AUtil.list(new Symbol[]{A, B})));
    }
    
    public void testCreateIntersection() {
        IAutomaton a1 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                }
        );
        IAutomaton a2 = new Automaton(
                s2,
                new State[]{s4},
                new Transition[]{
                    new Transition(s2, s3, a),
                    new Transition(s3, s4, b),
                    new Transition(s3, s4, c),
                }
        );
        
        IAutomaton a3 = Automatons.createIntersection(a1, a2);
        assertTrue(a3.accept(AUtil.list(new Symbol[]{a, b})));
        assertFalse(a3.accept(AUtil.list(new Symbol[]{a, c})));

        IAutomaton a4 = Automatons.createIntersection(a2, a1);
        assertTrue(a4.accept(AUtil.list(new Symbol[]{a, b})));
        assertFalse(a4.accept(AUtil.list(new Symbol[]{a, c})));
    }
    
    public void testCreateSubtraction() {
        IAutomaton a1 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, a),
                    new Transition(s2, s3, b),
                    new Transition(s1, s2, A),
                    new Transition(s2, s3, B),
                }
        );
        IAutomaton a2 = new Automaton(
                s1,
                new State[]{s3},
                new Transition[]{
                    new Transition(s1, s2, A),
                    new Transition(s2, s3, B),
                }
        );
        
        IAutomaton a3 = Automatons.createSubtraction(a1, a2);
        //Automatons.expand(a3, AUtil.set(new ISymbol[]{a, b, c, A, B}));
        //System.err.println(Automatons.toGraphviz(a3));

        assertTrue(a3.accept(AUtil.list(new Symbol[]{a, b})));
        assertTrue(a3.accept(AUtil.list(new Symbol[]{A, b})));
        assertTrue(a3.accept(AUtil.list(new Symbol[]{a, B})));
        assertFalse(a3.accept(AUtil.list(new Symbol[]{A, B})));
    }

    public void testEliminateEpsilonTransitions() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), Transition.EpsilonSymbol, new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                }
        ); 
        Automaton fst2 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("A"), new Symbol("B")}),
                    new Transition(new State("s1"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("A"), new Symbol("0")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                }
        );
        Automatons.eliminateEpsilonTransitions(fst1);
        assertEquals(fst2, fst1);
    }
    
    public void testEliminateEpsilonTransitions2() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), Transition.EpsilonSymbol, new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), Transition.EpsilonSymbol, new Symbol[]{new Symbol("0")}),
                }
        ); 
        Automaton fst2 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("A"), new Symbol("B")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                }
        );
        Automatons.eliminateEpsilonTransitions(fst1);
        assertEquals(fst2, fst1);
    }
    
    public void testEliminateEpsilonTransitions3() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), Transition.EpsilonSymbol, new Symbol[]{}),
                    new Transition(new State("s3"), new State("s4"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s4"), new State("s3"), Transition.EpsilonSymbol, new Symbol[]{}),
                }
        ); 
        Automaton fst2 = new Automaton(
                new State("s1"),
                new State[]{new State("s2"), new State("s3"), new State("s4")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s4"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s3"), new State("s4"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s4"), new State("s4"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                }
        );
        Automatons.eliminateEpsilonTransitions(fst1);
        assertEquals(fst2, fst1);
    }
    
    public void testEliminateEpsilonTransitions4() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), Transition.EpsilonSymbol, new Symbol[]{new CharSymbol("0")}),
                    new FilteredTransition(new State("s2"), new State("s3"), new Variable("x"), new Symbol[]{new Variable("x")},
                            new FilteredTransition.IFilter(){
                                public List invoke(ISymbol symbol, List outputs) {
                                    String s = ((CharSymbol)outputs.get(0)).getName().toUpperCase();
                                    return AUtil.list(new ISymbol[]{new CharSymbol(s)});
                                }},
                            new FilteredTransition.ICondition(){
                                private CharSymbol c = new CharSymbol("c");
                                public boolean accept(ISymbol symbol, IMatchContext ctx) {
                                    return c.equals(symbol);
                                }}),
                }
        ); 
        Automatons.eliminateEpsilonTransitions(fst1);
        
        List r1 = fst1.translate(StringSymbol.toCharSymbols("a"));
        assertTrue(r1.isEmpty());

        List r2 = fst1.translate(StringSymbol.toCharSymbols("c"));
        assertEquals(1, r2.size());
        assertEquals(StringSymbol.toCharSymbols("0C"), r2.get(0));

        List r3 = fst1.translate(StringSymbol.toCharSymbols("cc"));
        assertTrue(r3.isEmpty());
    }
    
    public void testEliminateEpsilonTransitions5() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new FilteredTransition(new State("s1"), new State("s2"), new Variable("x"), new Symbol[]{new Variable("x")},
                            new FilteredTransition.IFilter(){
                                public List invoke(ISymbol symbol, List outputs) {
                                    String s = ((CharSymbol)outputs.get(0)).getName().toUpperCase();
                                    return AUtil.list(new ISymbol[]{new CharSymbol(s)});
                                }
                            },
                            new FilteredTransition.ICondition(){
                                private CharSymbol c = new CharSymbol("c");
                                public boolean accept(ISymbol symbol, IMatchContext ctx) {
                                    return c.equals(symbol);
                                }
                            }),
                    new Transition(new State("s2"), new State("s3"), Transition.EpsilonSymbol, new Symbol[]{}),
                    new Transition(new State("s3"), new State("s1"), Transition.EpsilonSymbol, new Symbol[]{}),
                }
        ); 
        Automatons.eliminateEpsilonTransitions(fst1);
        
        List r1 = fst1.translate(StringSymbol.toCharSymbols("a"));
        assertTrue(r1.isEmpty());

        List r2 = fst1.translate(StringSymbol.toCharSymbols("c"));
        assertEquals(1, r2.size());
        assertEquals(StringSymbol.toCharSymbols("C"), r2.get(0));

        // TODO: should fix this.
        /*
        List r3 = fst1.translate(StringSymbol.toCharSymbols("ccc"));
        assertEquals(1, r2.size());
        assertEquals(StringSymbol.toCharSymbols("CCC"), r2.get(0));
        */
    }

    public void testEliminateNonDeterministics() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("b"), new Symbol[]{}),
                }
        ); 
        Automaton fst2 = new Automaton(
                new State("s1"),
                new State[]{new State("s4")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{}),
                    new Transition(new State("s2"), new State("s4"), new Symbol("b"), new Symbol[]{}),
                    new Transition(new State("s4"), new State("s4"), new Symbol("b"), new Symbol[]{}),
                }
        );
        Automatons.eliminateNonDeterministics(fst1);
        assertEquals(fst2, fst1);
    }
    
    public void testEliminateNonDeterministics2() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s4")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{}),
                    new Transition(new State("s3"), new State("s4"), new Symbol("c"), new Symbol[]{}),
                    new Transition(new State("s1"), new State("s5"), new Symbol("a"), new Symbol[]{}),
                    new Transition(new State("s5"), new State("s6"), new Symbol("b"), new Symbol[]{}),
                    new Transition(new State("s6"), new State("s7"), new Symbol("c"), new Symbol[]{}),
                }
        ); 
        Automaton fst2 = new Automaton(
                new State("s1"),
                new State[]{new State("s10")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s8"), new Symbol("a"), new Symbol[]{}),
                    new Transition(new State("s8"), new State("s9"), new Symbol("b"), new Symbol[]{}),
                    new Transition(new State("s9"), new State("s10"), new Symbol("c"), new Symbol[]{}),
                }
        );
        Automatons.eliminateNonDeterministics(fst1);
        assertEquals(fst2, fst1);
    }
}
