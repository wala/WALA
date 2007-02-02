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

public class TestBottomUpTreeAutomaton extends TreeJunitBase {
    /*
     * null -> <s1>(null)
     * 
     * u2[<s1>(x), <s1>(y)] -> <s2>(t2[x,y])
     * 
     * u1[<s1>(x), <s1>(y)] -> <s1>(u1[x, y])
     * u1[<s1>(x), <s2>(y)] -> <s3>(t1[x, y])
     * u1[<s1>(x), <s3>(y)] -> <s3>(y)
     *
     * u1[<s2>(x), <s1>(y)] -> <s3>(t1[x, y])
     * u1[<s2>(x), <s2>(y)] -> <s3>(t1[x, y])
     * u1[<s2>(x), <s3>(y)] -> <s3>(y)
     * 
     * u1[<s3>(x), <s1>(y)] -> <s3>(x)
     * u1[<s3>(x), <s2>(y)] -> <s3>(x)
     * u1[<s3>(x), <s3>(y)] -> <s3>(x)
     * u1[<s3>(x), <s3>(y)] -> <s3>(y)
     */
    public void testUndeterministicBottomUpTreeAutomatonTranslate() {
        State s1 = new State("s1");
        State s2 = new State("s2");
        State s3 = new State("s3");
        BinaryTreeVariable x = new BinaryTreeVariable("x");
        BinaryTreeVariable y = new BinaryTreeVariable("y");
        BottomUpTreeAutomaton butt = new BottomUpTreeAutomaton(
            new State[]{s3},
            new TreeTransition[]{
                    new TreeTransition(
                            BinaryTree.LEAF,
                            new StateBinaryTree(s1, BinaryTree.LEAF)
                    ),
                    
                    new TreeTransition(
                            new BinaryTree("u2", new StateBinaryTree(s1, x), new StateBinaryTree(s1, y)),
                            new StateBinaryTree(s2, new BinaryTree("t2", x, y))
                    ),
                    
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s1, x), new StateBinaryTree(s1, y)),
                            new StateBinaryTree(s1, new BinaryTree("u1", x, y))
                    ),
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s1, x), new StateBinaryTree(s2, y)),
                            new StateBinaryTree(s3, new BinaryTree("t1", x, y))
                    ),
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s1, x), new StateBinaryTree(s3, y)),
                            new StateBinaryTree(s3, y)
                    ),
                    
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s2, x), new StateBinaryTree(s1, y)),
                            new StateBinaryTree(s3, new BinaryTree("t1", x, y))
                    ),
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s2, x), new StateBinaryTree(s2, y)),
                            new StateBinaryTree(s3, new BinaryTree("t1", x, y))
                    ),
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s2, x), new StateBinaryTree(s3, y)),
                            new StateBinaryTree(s3, y)
                    ),
                    
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s3, x), new StateBinaryTree(s1, y)),
                            new StateBinaryTree(s3, x)
                    ),
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s3, x), new StateBinaryTree(s2, y)),
                            new StateBinaryTree(s3, x)
                    ),
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s3, x), new StateBinaryTree(s3, y)),
                            new StateBinaryTree(s3, x)
                    ),
                    new TreeTransition(
                            new BinaryTree("u1", new StateBinaryTree(s3, x), new StateBinaryTree(s3, y)),
                            new StateBinaryTree(s3, y)
                    ),

            }
        );
        
        BinaryTree t =
            new BinaryTree("u1",
                    new BinaryTree("u1",
                            new BinaryTree("u2"),
                            new BinaryTree("u1")),
                    new BinaryTree("u1"));
        assertTrue(butt.accept(t));
        
        BinaryTree ex1 =
            new BinaryTree("t1",
                    new BinaryTree("t2"),
                    new BinaryTree("u1"));
        Set ex = AUtil.set(new BinaryTree[]{ex1});
        Set result = butt.translate(t);
        assertEquals(ex, result);
        
        BinaryTree tt =
            new BinaryTree("u1",
                    new BinaryTree("u1"),
                    new BinaryTree("u1"));
        assertFalse(butt.accept(tt));
        assertFalse(butt.accept(null));
    }
}
