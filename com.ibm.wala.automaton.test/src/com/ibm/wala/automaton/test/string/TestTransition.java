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

import junit.framework.TestCase;

public class TestTransition extends TestCase {
    public void testEquality() { 
        Transition t1 = new Transition(new State("s1"), new State("s2"), new Symbol("a"), new ISymbol[]{new Symbol("b")});
        Transition t2 = new Transition(new State("s1"), new State("s2"), new Symbol("a"), new ISymbol[]{new Symbol("b")});
        assertEquals(t1, t2);
        assertEquals(t2, t1);
    }
    
    public void testEquality2() {
        Transition t1 = new Transition(new State("s1"), new State("s2"), new Symbol("a"), new ISymbol[]{new Symbol("b")});
        Transition t2 = new Transition(new State("s1"), new State("s2"), new Symbol("a"), new ISymbol[]{new Symbol("b"), new Symbol("c")});
        assertFalse(t1.equals(t2));
        assertFalse(t2.equals(t1));
    }
    
    public void testEquality3() {
        Transition t1 = new Transition(new State("s1"), new State("s2"), new Symbol("a"), new ISymbol[]{new Symbol("b")});
        Transition t2 = new Transition(new State("s1"), new State("s3"), new Symbol("a"), new ISymbol[]{new Symbol("b")});
        assertFalse(t1.equals(t2));
        assertFalse(t2.equals(t1));
    }

    public void testEquality4() {
        Transition t1 = new Transition(new State("s1"), new State("s2"), new Symbol("a"));
        Transition t2 = new Transition(new State("s1"), new State("s2"), new Symbol("a"), new ISymbol[]{});
        assertTrue(t1.equals(t2));
        assertTrue(t2.equals(t1));
    }
    
    public void testTransit(){
        Transition t = new Transition(new State("s1"), new State("s2"), new Symbol("a"), new ISymbol[]{new Symbol("b")});
        
        assertTrue(t.accept(new Symbol("a"), new MatchContext()));
        List l = new ArrayList();
        l.add(new Symbol("b"));
        assertEquals(l, t.transit(new Symbol("a")));
    }
    
    public void testTransitWithVariable(){
        State s1 = new State("s1");
        State s2 = new State("s2");
        Variable v = new Variable("v");
        Symbol a = new Symbol("a");
        
        Transition t = new Transition(s1, s2, v, new ISymbol[]{v});
        List l = new ArrayList();
        l.add(a);
        assertEquals(l, t.transit(a));
    }
    
    public void testFilteredTransition1() {
        State s1 = new State("s1");
        State s2 = new State("s2");
        Variable v = new Variable("v");
        Symbol a = new Symbol("a");
        Symbol A = new Symbol("A");
        
        Transition t = new FilteredTransition(s1, s2, v, new ISymbol[]{v},
                new FilteredTransition.IFilter(){
                    public List invoke(ISymbol symbol, List outputs) {
                        List result = new ArrayList();
                        for (Iterator i = outputs.iterator(); i.hasNext(); ) {
                            ISymbol s = (ISymbol) i.next();
                            s = NameReplaceSymbolCopier.setName(s, s.getName().toUpperCase());
                            result.add(s);
                        }
                        return result;
                    }
                });
        List l = new ArrayList();
        l.add(A);
        assertEquals(l, t.transit(a));
    }
    
    public void testFilteredTransition2() {
        State s1 = new State("s1");
        State s2 = new State("s2");
        Variable v = new Variable("v");
        Symbol a = new Symbol("a");
        Symbol A = new Symbol("A");
        Symbol B = new Symbol("B");
        Symbol C = new Symbol("C");
        
        Transition t = new FilteredTransition(s1, s2, v, new ISymbol[]{v},
                new FilteredTransition.IFilter(){
                    public List invoke(ISymbol symbol, List outputs) {
                        List result = new ArrayList();
                        for (Iterator i = outputs.iterator(); i.hasNext(); ) {
                            ISymbol s = (ISymbol) i.next();
                            s = NameReplaceSymbolCopier.setName(s, s.getName().toUpperCase());
                            result.add(s);
                        }
                        return result;
                    }
                });
        t.appendOutputSymbols(AUtil.list(new Object[]{B, C}));
        List l = AUtil.list(new Object[]{A, B, C});
        assertEquals(l, t.transit(a));
    }
    
    public void testFilteredTransition3() {
        State s1 = new State("s1");
        State s2 = new State("s2");
        Variable v = new Variable("v");
        Symbol a = new Symbol("a");
        Symbol A = new Symbol("A");
        Symbol B = new Symbol("B");
        Symbol C = new Symbol("C");
        
        Transition t = new FilteredTransition(s1, s2, v, new ISymbol[]{v},
                new FilteredTransition.IFilter(){
                    public List invoke(ISymbol symbol, List outputs) {
                        List result = new ArrayList();
                        for (Iterator i = outputs.iterator(); i.hasNext(); ) {
                            ISymbol s = (ISymbol) i.next();
                            s = NameReplaceSymbolCopier.setName(s, s.getName().toUpperCase());
                            result.add(s);
                        }
                        return result;
                    }
                });
        t.prependOutputSymbols(AUtil.list(new Object[]{C, B}));
        List l = AUtil.list(new Object[]{C, B, A});
        assertEquals(l, t.transit(a));
    }
    
    public void testComplementTransition1() {
        State s1 = new State("s1");
        State s2 = new State("s2");
        Variable v = new Variable("v");
        Symbol a = new Symbol("a");
        Symbol A = new Symbol("A");
        Symbol b = new Symbol("b");
        
        Transition t1 = new Transition(s1, s2, a, new ISymbol[]{A});
        Transition t2 = new Transition(s1, s2, A, new ISymbol[]{a});
        Transition ct = new ComplementTransition(s1, s2, v, new ISymbol[]{v}, null, new ITransition[]{t1, t2});
        assertEquals(A, t1.transit(a).get(0));
        assertEquals(a, t2.transit(A).get(0));
        assertFalse(ct.accept(a, new MatchContext()));
        assertFalse(ct.accept(A, new MatchContext()));
        assertTrue(ct.accept(b, new MatchContext()));
        assertEquals(b, ct.transit(b).get(0));
    }
    
    public void testIntersectionTransition1() {
        State s1 = new State("s1");
        State s2 = new State("s2");
        Variable v = new Variable("v");
        Symbol a = new Symbol("a");
        Symbol a2 = new Symbol("a");
        Symbol b = new Symbol("b");
        
        Transition t1 = new Transition(s1, s2, a, new ISymbol[]{a});
        Transition t2 = new Transition(s1, s2, a2, new ISymbol[]{a2});
        Transition t3 = new Transition(s1, s2, b, new ISymbol[]{b});
        Transition t12 = new IntersectionTransition(s1, s2, v, new ISymbol[]{v}, null, new ITransition[]{t1, t2});
        Transition t13 = new IntersectionTransition(s1, s2, v, new ISymbol[]{v}, null, new ITransition[]{t1, t3});
        
        assertEquals(a, t12.transit(a).get(0));
        assertFalse(t13.accept(a, new MatchContext()));
    }
}
