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
package com.ibm.wala.automaton.test.grammar.tree;

import com.ibm.wala.automaton.grammar.tree.ITreeGrammar;
import com.ibm.wala.automaton.grammar.tree.RTLTopDownTranslator;
import com.ibm.wala.automaton.string.State;
import com.ibm.wala.automaton.test.tree.TreeJunitBase;
import com.ibm.wala.automaton.tree.BinaryTree;
import com.ibm.wala.automaton.tree.BinaryTreeVariable;
import com.ibm.wala.automaton.tree.StateBinaryTree;
import com.ibm.wala.automaton.tree.TopDownTreeAutomaton;
import com.ibm.wala.automaton.tree.TreeTransition;

public class TestRTLTopDownTranslator extends TreeJunitBase {
    public void testRTLTopDownTranslator1() {
        ITreeGrammar tg = TG(
            BV(1),
            new Object[]{
              BV(1), BT("a", BV(2), BT("c")),
              BV(2), BT("b"),
            }
        );
        
        State s1 = new State("s1");
        State s2 = new State("s2");
        State s3 = new State("s3");
        BinaryTreeVariable x = new BinaryTreeVariable("x");
        BinaryTreeVariable y = new BinaryTreeVariable("y");
        TopDownTreeAutomaton tdtt = new TopDownTreeAutomaton(
            s1,
            new TreeTransition[]{
                new TreeTransition(
                    new StateBinaryTree(s1, new BinaryTree("a", x, y)),
                    new BinaryTree("A", new StateBinaryTree(s3, y), new StateBinaryTree(s2, x))
                ),
                new TreeTransition(
                    new StateBinaryTree(s2, new BinaryTree("b", x, y)),
                    new BinaryTree("B")
                ),
                new TreeTransition(
                    new StateBinaryTree(s3, new BinaryTree("c", x, y)),
                    new BinaryTree("c")
                ),
                new TreeTransition(
                    new StateBinaryTree(s3, new BinaryTree("c", x, y)),
                    new BinaryTree("C")
                ),
            }
        );
        
        ITreeGrammar spec1 = TG(
                BV("A"),
                new Object[]{
                    BV("A"), BT("A", BV("C"), BV("B")),
                    BV("B"), BT("B"),
                    BV("C"), BT("c"),
                    BV("C"), BT("C"),
                }
            );
        ITreeGrammar spec2 = TG(
                BV("A"),
                new Object[]{
                    BV("A"), BT("A", BV("C"), BV("B")),
                    BV("B"), BT("B"),
                    BV("C"), BT("C"),
                }
            );
        RTLTopDownTranslator translator = new RTLTopDownTranslator(tdtt);
        ITreeGrammar result = translator.translate(tg);
        assertContains(spec1, result);
        assertContains(result, spec1);
        assertNotContains(spec2, result);
        assertContains(result, spec2);
    }

}
