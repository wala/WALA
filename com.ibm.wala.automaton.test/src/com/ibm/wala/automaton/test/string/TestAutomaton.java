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

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.string.*;

import junit.framework.TestCase;

public class TestAutomaton extends TestCase {
    public void testCreate(){
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                }
        ); 
        assertEquals(new State("s1"), fst1.getInitialState());
        assertEquals(1, fst1.getFinalStates().size());
        assertEquals(3, fst1.getTransitions().size());
    }

    public void testEquality(){
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                }
        ); 
        Automaton fst2 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Symbol("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                }
        );
        assertEquals(fst1, fst2);
    }
    
    public void testGetTransitions() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Variable("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Variable("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("1"), new Symbol[]{new Symbol("1")}),
                }
        );
        assertEquals(3, fst1.getTransitions(new State("s2")).size());
        assertEquals(1, fst1.getTransitions(new State("s2"), new Variable("b")).size());
        assertEquals(1, fst1.getTransitions(new State("s2"), new Symbol("0")).size());

        assertNotNull(fst1.getTransitions(new State("s3")));
        assertTrue(fst1.getTransitions(new State("s3")).isEmpty());
    }
    
    public void testGetStates() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s1")},
                new Transition[]{}
                );
        Set states = AUtil.set(new State[]{new State("s1")});
        assertEquals(states, fst1.getStates());

        assertTrue(fst1.getTransitions().isEmpty());
        assertNotNull(fst1.getTransitions(new State("s1")));
        assertTrue(fst1.getTransitions(new State("s1")).isEmpty());
    }

    public void testGetTransitionsModified() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Variable("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Variable("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("1"), new Symbol[]{new Symbol("1")}),
                }
        );
        Set expected = AUtil.set(new Transition[]{
                new Transition(new State("s1"), new State("s2"), new Variable("a"), new Symbol[]{new Symbol("A")}),
                new Transition(new State("s2"), new State("s3"), new Variable("b"), new Symbol[]{new Symbol("B")}),
                new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                new Transition(new State("s2"), new State("s2"), new Symbol("1"), new Symbol[]{new Symbol("1")}),
                new Transition(new State("s3"), new State("s4"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
        });
        fst1.getTransitions().add(new Transition(new State("s3"), new State("s4"), new Symbol("0"), new Symbol[]{new Symbol("0")}));
        assertEquals(expected, fst1.getTransitions());
    }

    public void testGetAcceptTransitions() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Variable("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Variable("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("0")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("1"), new Symbol[]{new Symbol("1")}),
                }
        );
        assertEquals(3, fst1.getTransitions(new State("s2")).size());
        assertEquals(0, fst1.getAcceptTransitions(new State("s2"), new Variable("b")).size());
        assertEquals(2, fst1.getAcceptTransitions(new State("s2"), new Symbol("0")).size());
    }
    
    public void testTranslate() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Variable("a"), new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), new Variable("b"), new Symbol[]{new Symbol("B")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("0"), new Symbol[]{new Symbol("1")}),
                    new Transition(new State("s2"), new State("s2"), new Symbol("1"), new Symbol[]{new Symbol("0")}),
                }
        );
        List l1 = AUtil.list(new Symbol[]{
                new Symbol("a"),
                new Symbol("0"),
                new Symbol("b"),
        });
        List l2 = AUtil.list(
                new List[]{
                        AUtil.list(new Symbol[]{
                            new Symbol("A"),
                            new Symbol("1"),
                            new Symbol("B"),
                        }),
                    });
        assertEquals(l2, fst1.translate(l1));
        assertTrue(fst1.accept(l1));
        
        List l3 = AUtil.list(new Symbol[]{
                new Symbol("a"),
                new Symbol("b"),
        });
        List l4 = AUtil.list(
                new List[]{
                        AUtil.list(new Symbol[]{
                            new Symbol("A"),
                            new Symbol("B"),
                        }),
                    });
        assertEquals(l4, fst1.translate(l3));
        assertTrue(fst1.accept(l3));
        
        List l5 = AUtil.list(new Symbol[]{
                new Symbol("a"),
                new Symbol("b"),
                new Symbol("1"),
        });
        assertFalse(fst1.accept(l5));
    }
    
    public void testAcceptEmpty1() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s2"), new State("s3")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), Transition.EpsilonSymbol, new Symbol[]{new Symbol("A")}),
                    new Transition(new State("s2"), new State("s3"), Transition.EpsilonSymbol, new Symbol[]{new Symbol("B")}),
                }
        );

        assertTrue(fst1.accept(AUtil.list(new Symbol[0])));
        assertEquals(1, fst1.translate(AUtil.list(new Symbol[0])).size());
        assertEquals(
                AUtil.list(new Symbol[]{new Symbol("A")}),
                fst1.translate(AUtil.list(new Symbol[0])).get(0));
    }
    
    public void testAutomatonCopy1() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s2")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                }
        );
        
        Automaton fst2 = (Automaton) fst1.copy(SimpleSTSCopier.defaultCopier);
        assertEquals(fst1, fst2);
        assertTrue(System.identityHashCode(fst1.getInitialState())
                == System.identityHashCode(fst2.getInitialState()));
        assertFalse(System.identityHashCode(fst1.getFinalStates())
                == System.identityHashCode(fst2.getFinalStates()));
        assertTrue(System.identityHashCode((new ArrayList(fst1.getFinalStates())).get(0))
                == System.identityHashCode((new ArrayList(fst2.getFinalStates())).get(0)));
        assertTrue(System.identityHashCode((new ArrayList(fst1.getTransitions())).get(0))
                == System.identityHashCode((new ArrayList(fst2.getTransitions())).get(0)));
    }

    public void testAutomatonCopy2() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s2")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                }
        );
        
        Automaton fst2 = (Automaton) fst1.copy(new DeepSTSCopier(SimpleTransitionCopier.defaultCopier));
        assertEquals(fst1, fst2);
        assertTrue(System.identityHashCode(fst1.getInitialState())
                == System.identityHashCode(fst2.getInitialState()));
        assertFalse(System.identityHashCode(fst1.getFinalStates())
                == System.identityHashCode(fst2.getFinalStates()));
        assertTrue(System.identityHashCode((new ArrayList(fst1.getFinalStates())).get(0))
                == System.identityHashCode((new ArrayList(fst2.getFinalStates())).get(0)));
        assertFalse(System.identityHashCode((new ArrayList(fst1.getTransitions())).get(0))
                == System.identityHashCode((new ArrayList(fst2.getTransitions())).get(0)));
    }
    
    public void testAutomatonCopy3() {
        Automaton fst1 = new Automaton(
                new State("s1"),
                new State[]{new State("s2")},
                new Transition[]{
                    new Transition(new State("s1"), new State("s2"), new Symbol("a"), new Symbol[]{new Symbol("A")}),
                }
        );
        
        Automaton fst2 = (Automaton) fst1.copy(new DeepSTSCopier(new DeepTransitionCopier()));
        assertEquals(fst1, fst2);
        assertFalse(System.identityHashCode(fst1.getInitialState())
                == System.identityHashCode(fst2.getInitialState()));
        assertFalse(System.identityHashCode(fst1.getFinalStates())
                == System.identityHashCode(fst2.getFinalStates()));
        assertFalse(System.identityHashCode((new ArrayList(fst1.getFinalStates())).get(0))
                == System.identityHashCode((new ArrayList(fst2.getFinalStates())).get(0)));
        assertFalse(System.identityHashCode((new ArrayList(fst1.getTransitions())).get(0))
                == System.identityHashCode((new ArrayList(fst2.getTransitions())).get(0)));
    }
}
