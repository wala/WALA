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
package com.ibm.wala.automaton.test.grammar.string;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;

import junit.framework.TestCase;

public class TestCFLTranslator extends TestCase {
    public void testCFLTranslator1() {
        Automaton transducer = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                }
        );
        ContextFreeGrammar cfg = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")}),
                }
        );
        ContextFreeGrammar expected = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("0"), new Variable("B")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("0"), new Symbol("2")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("2")}),
                }
        );
        CFLTranslator translator = new CFLTranslator(transducer);
        IContextFreeGrammar result = translator.translate(cfg);
        assertEquals(expected, result);
    }
    
    public void testCFLTranslator2() {
        Automaton transducer = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
                    new FilteredTransition(new State("s2"), new State("s2"), new Variable("v"), new Symbol[]{new Variable("v")},
                        new FilteredTransition.IFilter(){
                            public List invoke(ISymbol s, List outputs) {
                                List l = new ArrayList();
                                for (Iterator i = outputs.iterator(); i.hasNext(); ) {
                                    ISymbol sym = (ISymbol) i.next();
                                    int ival = Integer.parseInt(s.getName());
                                    sym = new Symbol(Integer.toString(ival+1));
                                    l.add(sym);
                                }
                                return l;
                            }
                        },
                        new FilteredTransition.ICondition(){
                            public boolean accept(ISymbol symbol, IMatchContext ctx) {
                                if (symbol instanceof IVariable) {
                                    return false;
                                }
                                try {
                                    Integer.parseInt(symbol.getName());
                                    return true;
                                } catch(Exception e) {
                                    return false;
                                }
                            }
                        }),
                }
        );
        ContextFreeGrammar cfg = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("2"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("z"), new Variable("B")}),
                }
        );
        ContextFreeGrammar expected = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("1"), new Variable("B")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("1"), new Symbol("2")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("2")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("3"), new Variable("B")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("3"), new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1"), new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("3"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("3"), new Symbol("2")}),
                }
        );
        CFLTranslator translator = new CFLTranslator(transducer);
        IContextFreeGrammar result = translator.translate(cfg);
        assertEquals(expected, result);
    }

    public void testCFLTranslator3() {
        Automaton transducer = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
                    new FilteredTransition(new State("s2"), new State("s2"), new Variable("A"), new Symbol[]{new Variable("A"), new Symbol("z")},
                        new FilteredTransition.IFilter(){
                            public List invoke(ISymbol s, List outputs) {
                                List l = new ArrayList();
                                ISymbol sym0 = (ISymbol) outputs.get(0);
                                int ival = Integer.parseInt(s.getName());
                                sym0 = new Symbol(Integer.toString(ival+1));
                                l.add(sym0);
                                ISymbol sym1 = (ISymbol) outputs.get(1);
                                l.add(sym1);
                                return l;
                            }
                        },
                        new FilteredTransition.ICondition(){
                            public boolean accept(ISymbol symbol, IMatchContext ctx) {
                                if (symbol instanceof IVariable) {
                                    return false;
                                }
                                try {
                                    Integer.parseInt(symbol.getName());
                                    return true;
                                } catch(Exception e) {
                                    return false;
                                }
                            }
                        }),
                }
        );
        ContextFreeGrammar cfg = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("2"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("z"), new Variable("B")}),
                }
        );
        ContextFreeGrammar expected = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("1"), new Symbol("z"), new Variable("B")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("1"), new Symbol("z"), new Symbol("2")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("2")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("3"), new Symbol("z"), new Variable("B")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("1"), new Symbol("3"), new Symbol("z"), new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1"), new Symbol("z"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1"), new Symbol("z"), new Symbol("2")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("3"), new Symbol("z"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("3"), new Symbol("z"), new Symbol("2")}),
                }
        );
        CFLTranslator translator = new CFLTranslator(transducer);
        IContextFreeGrammar result = translator.translate(cfg);
        assertEquals(expected, result);
    }

    public void testCFLTranslator4() {
        Automaton transducer = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("1")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("2")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                }
        );
        ContextFreeGrammar cfg = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a")}),
                }
        );
        ContextFreeGrammar expected = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[0]),
                }
        );
        CFLTranslator translator = new CFLTranslator(transducer);
        IContextFreeGrammar result = translator.translate(cfg);
        assertEquals(expected, result);
    }
}
