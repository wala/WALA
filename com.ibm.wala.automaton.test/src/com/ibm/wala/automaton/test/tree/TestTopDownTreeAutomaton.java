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
package com.ibm.wala.automaton.test.tree;

import java.util.*;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;

public class TestTopDownTreeAutomaton extends TreeJunitBase {
    public void testUndeterministicTopDownTreeAutomatonTranslate() {
        State s1 = new State("s1");
        State s2 = new State("s2");
        State s3 = new State("s3");
        BinaryTreeVariable x = new BinaryTreeVariable("x");
        BinaryTreeVariable y = new BinaryTreeVariable("y");
        TopDownTreeAutomaton tdtt = new TopDownTreeAutomaton(
            s1,
            new TreeTransition[]{
                new TreeTransition(
                    new StateBinaryTree(s1, new BinaryTree("t1", x, y)),
                    new BinaryTree("u1", new StateBinaryTree(s3, y), new StateBinaryTree(s2, x))
                ),
                new TreeTransition(
                    new StateBinaryTree(s2, new BinaryTree("t2", x, y)),
                    new BinaryTree("u2", x, y)
                ),
                new TreeTransition(
                    new StateBinaryTree(s3, new BinaryTree("t3", x, y)),
                    new BinaryTree("u3", x, y)
                ),
                new TreeTransition(
                    new StateBinaryTree(s3, new BinaryTree("t3", x, y)),
                    new BinaryTree("v3", x, y)
                ),
            }
        );
        
        BinaryTree t =
            new BinaryTree("t1",
                    new BinaryTree("t2"),
                    new BinaryTree("t3"));
        assertTrue(tdtt.accept(t));
        
        BinaryTree ex1 =
            new BinaryTree("u1",
                    new BinaryTree("u3"),
                    new BinaryTree("u2"));
        BinaryTree ex2 =
            new BinaryTree("u1",
                    new BinaryTree("v3"),
                    new BinaryTree("u2"));
        Set ex = AUtil.set(new BinaryTree[]{ex1, ex2});
        Set result = tdtt.translate(t);
        assertEquals(ex, result);
        
        BinaryTree tt =
            new BinaryTree("t1",
                    new BinaryTree("t3"),
                    new BinaryTree("t3"));
        assertFalse(tdtt.accept(tt));
        assertFalse(tdtt.accept(null));
    }
}
