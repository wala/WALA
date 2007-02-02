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

import com.ibm.wala.automaton.regex.string.ComplementPattern;
import com.ibm.wala.automaton.regex.string.ConcatenationPattern;
import com.ibm.wala.automaton.regex.string.EmptyPattern;
import com.ibm.wala.automaton.regex.string.IPattern;
import com.ibm.wala.automaton.regex.string.IntersectionPattern;
import com.ibm.wala.automaton.regex.string.IterationPattern;
import com.ibm.wala.automaton.regex.string.SymbolPattern;
import com.ibm.wala.automaton.regex.string.UnionPattern;
import com.ibm.wala.automaton.regex.string.VariableBindingPattern;
import com.ibm.wala.automaton.regex.string.VariableReferencePattern;
import com.ibm.wala.automaton.string.Symbol;
import com.ibm.wala.automaton.string.Variable;

import junit.framework.TestCase;

public class TestPattern extends TestCase {
    public void testEmptyPatternEquality() {
        EmptyPattern empty1 = new EmptyPattern();
        EmptyPattern empty2 = new EmptyPattern();
        assertTrue(empty1.equals(empty2));
        assertTrue(empty2.equals(empty1));
    }

    public void testSymbolPatternEquality() {
        IPattern p1 = new SymbolPattern(new Symbol("a"));
        IPattern p2 = new SymbolPattern(new Symbol("a"));
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testSymbolPatternEquality2() {
        IPattern p1 = new SymbolPattern(new Symbol("a"));
        IPattern p2 = new SymbolPattern(new Symbol("b"));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }

    public void testIterationPatternEquality() {
        IterationPattern p1 = new IterationPattern(new EmptyPattern(), true);
        IterationPattern p2 = new IterationPattern(new EmptyPattern(), true);
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testIterationPatternEquality2() {
        IterationPattern p1 = new IterationPattern(new SymbolPattern(new Symbol("a")));
        IterationPattern p2 = new IterationPattern(new SymbolPattern(new Symbol("b")));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }
    
    public void testIterationPatternEquality3() {
        IterationPattern p1 = new IterationPattern(new SymbolPattern(new Symbol("a")), true);
        IterationPattern p2 = new IterationPattern(new SymbolPattern(new Symbol("a")), false);
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }
    
    public void testComplementPatternEquality() {
        ComplementPattern p1 = new ComplementPattern(new SymbolPattern(new Symbol("a")));
        ComplementPattern p2 = new ComplementPattern(new SymbolPattern(new Symbol("a")));
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testComplementPatternEquality2() {
        ComplementPattern p1 = new ComplementPattern(new SymbolPattern(new Symbol("a")));
        ComplementPattern p2 = new ComplementPattern(new SymbolPattern(new Symbol("b")));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }
    
    public void testConcatenationPatternEquality() {
        ConcatenationPattern p1 = new ConcatenationPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new EmptyPattern());
        ConcatenationPattern p2 = new ConcatenationPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new EmptyPattern());
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testConcatenationPatternEquality2() {
        ConcatenationPattern p1 = new ConcatenationPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new EmptyPattern());
        ConcatenationPattern p2 = new ConcatenationPattern(
                                    new SymbolPattern(new Symbol("b")),
                                    new EmptyPattern());
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }

    public void testUnionPatternEquality() {
        UnionPattern p1 = new UnionPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new EmptyPattern());
        UnionPattern p2 = new UnionPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new EmptyPattern());
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testUnionPatternEquality2() {
        UnionPattern p1 = new UnionPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new EmptyPattern());
        UnionPattern p2 = new UnionPattern(
                                    new SymbolPattern(new Symbol("b")),
                                    new EmptyPattern());
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }
    
    public void testVariableBindingPatternEquality() {
        VariableBindingPattern p1 = new VariableBindingPattern(new SymbolPattern(new Symbol("a")), new Variable("v"));
        VariableBindingPattern p2 = new VariableBindingPattern(new SymbolPattern(new Symbol("a")), new Variable("v"));
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testVariableBindingPatternEquality2() {
        VariableBindingPattern p1 = new VariableBindingPattern(new SymbolPattern(new Symbol("a")), new Variable("v"));
        VariableBindingPattern p2 = new VariableBindingPattern(new SymbolPattern(new Symbol("a")), new Variable("w"));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }

    public void testVariableBindingPatternEquality3() {
        VariableBindingPattern p1 = new VariableBindingPattern(new SymbolPattern(new Symbol("a")), new Variable("v"));
        VariableBindingPattern p2 = new VariableBindingPattern(new SymbolPattern(new Symbol("b")), new Variable("v"));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }
    
    public void testVariableReferencePatternEquality() {
        VariableReferencePattern p1 = new VariableReferencePattern(new Variable("v"));
        VariableReferencePattern p2 = new VariableReferencePattern(new Variable("v"));
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testVariableReferencePatternEquality2() {
        VariableReferencePattern p1 = new VariableReferencePattern(new Variable("v"));
        VariableReferencePattern p2 = new VariableReferencePattern(new Variable("w"));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }

    public void testIntersectionPatternEquality() {
        IntersectionPattern p1 = new IntersectionPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new SymbolPattern(new Symbol("a")));
        IntersectionPattern p2 = new IntersectionPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new SymbolPattern(new Symbol("a")));
        assertTrue(p1.equals(p2));
        assertTrue(p2.equals(p1));
    }

    public void testIntersectionPatternEquality2() {
        IntersectionPattern p1 = new IntersectionPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new SymbolPattern(new Symbol("a")));
        IntersectionPattern p2 = new IntersectionPattern(
                                    new SymbolPattern(new Symbol("a")),
                                    new SymbolPattern(new Symbol("b")));
        assertFalse(p1.equals(p2));
        assertFalse(p2.equals(p1));
    }
}
