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
package com.ibm.wala.automaton.test.regex.string;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.regex.string.StringPatternSymbol;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.test.AutomatonJunitBase;

public class TestPatternSymbol extends AutomatonJunitBase {
    public void testStringPatternSymbol1() {
        StringPatternSymbol pat = new StringPatternSymbol("abc|ABC");
        StringSymbol str1 = new StringSymbol("abc");
        StringSymbol str2 = new StringSymbol("ABC");
        StringSymbol str3 = new StringSymbol("aBC");
        MatchContext ctx = new MatchContext();

        assertTrue(pat.matches(str1, ctx));
        assertEquals(str1, ctx.get(pat));
        
        assertTrue(pat.matches(str2, ctx));
        assertEquals(str2, ctx.get(pat));
        
        assertFalse(pat.matches(str3, ctx));
    }
    
    public void testStringPatternSymbol2() {
        ContextFreeGrammar cfg1 = new ContextFreeGrammar(
                new Variable("A"),
                new ProductionRule[]{
                    new ProductionRule(new Variable("A"), StringSymbol.toCharSymbols("abc")),
                    new ProductionRule(new Variable("A"), StringSymbol.toCharSymbols("ABC")),
                }
            );
        CFGSymbol cs1 = new CFGSymbol(cfg1);
        
        ContextFreeGrammar cfg2 = new ContextFreeGrammar(
                new Variable("A"),
                new ProductionRule[]{
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("a"), new Variable("B")}),
                    new ProductionRule(new Variable("A"), new ISymbol[]{new Symbol("A"), new Variable("B")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("b"), new Variable("C")}),
                    new ProductionRule(new Variable("B"), new ISymbol[]{new Symbol("B"), new Variable("C")}),
                    new ProductionRule(new Variable("C"), new ISymbol[]{new Symbol("c")}),
                    new ProductionRule(new Variable("C"), new ISymbol[]{new Symbol("C")}),
                }
            );
        CFGSymbol cs2 = new CFGSymbol(cfg2);
        
        StringPatternSymbol ps = new StringPatternSymbol("abc|ABC");
        
        IMatchContext ctx = new MatchContext();
        
        assertTrue(ps.matches(cs1, ctx));
        assertFalse(cs1.matches(ps, ctx));
        
        assertFalse(ps.matches(cs2, ctx));
        assertFalse(cs2.matches(ps, ctx));
    }
}
