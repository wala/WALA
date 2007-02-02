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

public class TestCFG extends TestCase {
    public void testEquality(){
        ContextFreeGrammar cfg1 = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{}),
                }
            );
        ContextFreeGrammar cfg2 = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
                }
            );
        assertTrue(cfg1.equals(cfg2));
        assertTrue(cfg2.equals(cfg1));
    }

    public void testEquality2(){
        ContextFreeGrammar cfg1 = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{}),
                }
            );
        ContextFreeGrammar cfg2 = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("("), new Variable("A"), new Symbol(")")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("0")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("1")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("-"), new Variable("A")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("B")}),
                }
            );
        assertFalse(cfg1.equals(cfg2));
        assertFalse(cfg2.equals(cfg1));
    }
    
    public void testCFGCopy1() {
        ContextFreeGrammar cfg1 = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
                });
        ContextFreeGrammar cfg2 = (ContextFreeGrammar) cfg1.copy(SimpleGrammarCopier.defaultCopier);
        assertEquals(cfg1, cfg2);
        
        assertTrue(System.identityHashCode(cfg1.getStartSymbol())
                == System.identityHashCode(cfg2.getStartSymbol()));
        assertTrue(System.identityHashCode((new ArrayList(cfg1.getRules())).get(0))
                == System.identityHashCode((new ArrayList(cfg2.getRules())).get(0)));
    }
    
    public void testCFGCopy2() {
        ContextFreeGrammar cfg1 = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
                });
        ContextFreeGrammar cfg2 = (ContextFreeGrammar) cfg1.copy(new DeepGrammarCopier(new SimpleRuleCopier()));
        assertEquals(cfg1, cfg2);
        
        assertTrue(System.identityHashCode(cfg1.getStartSymbol())
                == System.identityHashCode(cfg2.getStartSymbol()));
        assertFalse(System.identityHashCode((new ArrayList(cfg1.getRules())).get(0))
                == System.identityHashCode((new ArrayList(cfg2.getRules())).get(0)));
    }
    
    public void testCFGCopy3() {
        ContextFreeGrammar cfg1 = new ContextFreeGrammar(
                new Variable("A"),
                new IProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Variable("A"), new Symbol("+"), new Variable("A")}),
                });
        ContextFreeGrammar cfg2 = (ContextFreeGrammar) cfg1.copy(new DeepGrammarCopier(new DeepRuleCopier(new SimpleSymbolCopier())));
        assertEquals(cfg1, cfg2);
        
        assertFalse(System.identityHashCode(cfg1.getStartSymbol())
                == System.identityHashCode(cfg2.getStartSymbol()));
        assertFalse(System.identityHashCode((new ArrayList(cfg1.getRules())).get(0))
                == System.identityHashCode((new ArrayList(cfg2.getRules())).get(0)));
    }
}