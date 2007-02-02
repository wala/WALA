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

public class TestState extends TestCase {
    public void testState(){
        State s = new State("name");
        assertEquals("name", s.getName());
    }
    
    public void testStateSetName(){
        IState s = new State("name");
        s = NameReplaceStateCopier.setName(s, "foo");
        assertEquals("foo", s.getName());
    }
    
    public void testStateEquality(){
        State s1 = new State("name");
        State s2 = new State("name");
        State s3 = new State("foo");
        assertTrue(s1.equals(s2));
        assertFalse(s1.equals(s3));
    }
    
    public void testStateCopy1() {
        IState s1 = new State("s1");
        IState s2 = (IState) s1.copy(SimpleStateCopier.defaultCopier);
        assertEquals(s1, s2);
    }
}
