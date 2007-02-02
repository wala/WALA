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
package com.ibm.wala.automaton.test;

import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.string.Symbol;
import com.ibm.wala.automaton.string.Variable;

import junit.framework.TestCase;

abstract public class AutomatonJunitBase extends TestCase {
    protected State
        s0 = new State("s0"),
        s1 = new State("s1"),
        s2 = new State("s2"),
        s3 = new State("s3"),
        s4 = new State("s4"),
        s5 = new State("s5"),
        s6 = new State("s6"),
        s7 = new State("s7"),
        s8 = new State("s8");
    protected Symbol
        a = new Symbol("a"),
        b = new Symbol("b"),
        c = new Symbol("c"),
        A = new Symbol("A"),
        B = new Symbol("B"),
        C = new Symbol("C"),
        i0 = new Symbol("i0"),
        i1 = new Symbol("i1"),
        i2 = new Symbol("i2"),
        i3 = new Symbol("i3"),
        i4 = new Symbol("i4"),
        i5 = new Symbol("i5"),
        i6 = new Symbol("i6");
    protected Variable
        v0 = new Variable("v0"),
        v1 = new Variable("v1"),
        v2 = new Variable("v2"),
        v3 = new Variable("v3"),
        v4 = new Variable("v4"),
        v5 = new Variable("v5");
}
