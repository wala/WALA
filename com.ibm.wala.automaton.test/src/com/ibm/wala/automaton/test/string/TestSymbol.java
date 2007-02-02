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

import com.ibm.wala.automaton.string.*;

import junit.framework.TestCase;

public class TestSymbol extends TestCase {
    public void testSymbolName(){
        Symbol s = new Symbol("name");
        assertEquals("name", s.getName());
    }
    
    /*
    public void testSymbolSetName(){
        Symbol s = new Symbol("name");
        s.setName("foo");
        assertEquals("foo", s.getName());
    }
    */
    
    public void testSymbolSize(){
        Symbol s = new Symbol("name");
        assertEquals(0, s.size());
    }
    
    public void testSymbolEquality(){
        Symbol s1 = new Symbol("name");
        Symbol s2 = new Symbol("name");
        Symbol s3 = new Symbol("foo");
        assertTrue(s1.equals(s2));
        assertFalse(s1.equals(s3));
    }
    
    public void testVariableMatches(){
        Symbol v = new Variable("v");
        Symbol s = new Symbol("name");
        MatchContext ctx = new MatchContext();
        assertTrue(v.matches(s, ctx));
        ISymbol r = VariableReplacer.replace(v, ctx);
        assertEquals(s, r);
        assertTrue(System.identityHashCode(s) == System.identityHashCode(r));
    }

    public void testVariableMatches2(){
        Symbol v = new Variable("v");
        Symbol s = new Symbol("name");
        MatchContext ctx = new MatchContext(); 
        assertFalse(s.matches(v, ctx));
    }
    
    public void testPrefixNew() {
        PrefixedSymbol s = new PrefixedSymbol(new Symbol("p"), new Symbol("a"));
        assertEquals("p:a", s.getName());
        assertEquals("p", s.getPrefix().getName());
        assertEquals("a", s.getLocal().getName());
    }
    
    /*
    public void testPrefixSetName() {
        PrefixedSymbol s = new PrefixedSymbol(new StringSymbol("p"), new StringSymbol("a"));
        s.setName("q:a");
        assertEquals(new PrefixedSymbol(new StringSymbol("q"), new StringSymbol("a")), s);
    }
    
    public void testPrefixSetName2() {
        PrefixedSymbol s = new PrefixedSymbol(new StringSymbol("p"), new StringSymbol("a"));
        s.setName("b");
        assertEquals(new PrefixedSymbol(new StringSymbol("p"), new StringSymbol("b")), s);
    }

    public void testPrefixSetName3() {
        PrefixedSymbol s = new PrefixedSymbol(new CharSymbol("p"), new CharSymbol("a"));
        s.setName("b");
        assertEquals(new PrefixedSymbol(new CharSymbol("p"), new CharSymbol("b")), s);
        assertNotSame(new PrefixedSymbol(new CharSymbol("p"), new Symbol("b")), s);
        assertNotSame(new PrefixedSymbol(new CharSymbol("p"), new StringSymbol("b")), s);
    }
    */
    
    public void testPrefixEquality() {
        ISymbol s1 = new PrefixedSymbol(new Symbol("p"), new Symbol("name"));
        ISymbol s2 = new PrefixedSymbol(new Symbol("p"), new Symbol("name"));
        assertTrue(s1.equals(s2));
    }

    public void testPrefixEquality2() {
        ISymbol s1 = new PrefixedSymbol(new Symbol("p"), new Symbol("name"));
        ISymbol s2 = new PrefixedSymbol(new Symbol("q"), new Symbol("name"));
        assertFalse(s1.equals(s2));
    }
    
    public void testPrefixEquality3() {
        ISymbol s1 = new PrefixedSymbol(new Symbol("p"), new Symbol("name"));
        ISymbol s2 = new PrefixedSymbol(new Symbol("p"), new Symbol("foo"));
        assertFalse(s1.equals(s2));
    }

    public void testPrefixMatches() {
        ISymbol s1 = new PrefixedSymbol(new Variable("x"), new Symbol("name"));
        ISymbol s2 = new PrefixedSymbol(new Symbol("p"), new Symbol("name"));
        MatchContext ctx = new MatchContext();
        assertTrue(s1.matches(s2, ctx));
        assertEquals(new Symbol("p"), ctx.get(new Variable("x")));
    }

    public void testPrefixMatches2() {
        ISymbol s1 = new Variable("x");
        ISymbol s2 = new PrefixedSymbol(new Symbol("p"), new Symbol("name"));
        MatchContext ctx = new MatchContext();
        assertTrue(s1.matches(s2, ctx));
        assertEquals(s2, ctx.get(new Variable("x")));
    }
    
    public void testPrefixMatches3() {
        ISymbol s1 = new PrefixedSymbol(new Symbol("x"), new Symbol("name"));
        ISymbol s2 = new PrefixedSymbol(new Symbol("p"), new Symbol("foo"));
        MatchContext ctx = new MatchContext();
        assertFalse(s1.matches(s2, ctx));
    }
    
    public void testSimpleSymbolCopier() {
        PrefixedSymbol s1 = new PrefixedSymbol(new Symbol("x"), new Symbol("name"));
        PrefixedSymbol s2 = (PrefixedSymbol) s1.copy(SimpleSymbolCopier.defaultCopier);
        assertEquals(s1, s2);
        assertFalse(System.identityHashCode(s1) == System.identityHashCode(s2));
        assertTrue(System.identityHashCode(s1.getPrefix()) == System.identityHashCode(s2.getPrefix()));
        assertTrue(System.identityHashCode(s1.getLocal()) == System.identityHashCode(s2.getLocal()));
    }
    
    public void testDeepSymbolCopier() {
        PrefixedSymbol s1 = new PrefixedSymbol(new Symbol("x"), new Symbol("name"));
        PrefixedSymbol s2 = (PrefixedSymbol) s1.copy(DeepSymbolCopier.defaultCopier);
        assertEquals(s1, s2);
        assertFalse(System.identityHashCode(s1) == System.identityHashCode(s2));
        assertFalse(System.identityHashCode(s1.getPrefix()) == System.identityHashCode(s2.getPrefix()));
        assertFalse(System.identityHashCode(s1.getLocal()) == System.identityHashCode(s2.getLocal()));
    }
    
    public void testVariableReplacer1() {
        PrefixedSymbol s1 = new PrefixedSymbol(new Variable("x"), new Symbol("name"));
        IMatchContext ctx = new MatchContext();
        ctx.put(new Variable("x"), new Symbol("p"));
        PrefixedSymbol s2 = (PrefixedSymbol) s1.copy(new VariableReplacer(ctx));
        PrefixedSymbol expected = new PrefixedSymbol(new Symbol("p"), new Symbol("name"));
        assertEquals(expected, s2);
        assertFalse(System.identityHashCode(s1) == System.identityHashCode(s2));
        assertFalse(System.identityHashCode(s1.getPrefix()) == System.identityHashCode(s2.getPrefix()));
        assertFalse(System.identityHashCode(s1.getLocal()) == System.identityHashCode(s2.getLocal()));
    }

    public void testVariableReplacer2() {
        PrefixedSymbol s1 = new PrefixedSymbol(new Variable("x"), new Symbol("name"));
        IMatchContext ctx = new MatchContext();
        PrefixedSymbol s2 = (PrefixedSymbol) s1.copy(new VariableReplacer(ctx));
        assertEquals(s1, s2);
        assertFalse(System.identityHashCode(s1) == System.identityHashCode(s2));
        assertTrue(System.identityHashCode(s1.getPrefix()) == System.identityHashCode(s2.getPrefix()));
        assertFalse(System.identityHashCode(s1.getLocal()) == System.identityHashCode(s2.getLocal()));
    }
    
    public void testVariableReplacer3() {
        Variable x = new Variable("x");
        IMatchContext ctx = new MatchContext();
        ctx.put(x, null);
        
        ISymbol result = VariableReplacer.replace(x, ctx);
        assertEquals(null, result);
    }
}
