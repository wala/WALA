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

import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;

public class TestBinaryTree extends TreeJunitBase {
    public void testBTreeEquality(){
        BinaryTree t1 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        BinaryTree t2 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        assertEquals(t1, t2);
    }

    public void testBTreeEquality2(){
        BinaryTree t1 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        BinaryTree t2 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("b12"));
        assertFalse(t1.equals(t2));
    }

    public void testBTreeSetLeft(){
        BinaryTree t1 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        BinaryTree t2 = new BinaryTree("a1", new BinaryTree("b11"), new BinaryTree("a12"));
        t2.setLeft(new BinaryTree("a11"));
        assertEquals(t1, t2);
    }

    public void testBTreeSetRight(){
        BinaryTree t1 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        BinaryTree t2 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("b12"));
        t2.setRight(new BinaryTree("a12"));
        assertEquals(t1, t2);
    }

    public void testBTreeSize(){
        BinaryTree t = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        assertEquals(2, t.size());
    }
    
    public void testBTreeVariableMatches(){
        BinaryTree t1 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        BinaryTree t2 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTreeVariable("v"));
        MatchContext ctx = new MatchContext();
        
        boolean result = t2.matches(t1, ctx);
        assertTrue(result);
        
        IBinaryTree t3 = (IBinaryTree) ctx.get(new BinaryTreeVariable("v"));
        assertEquals(new BinaryTree("a12"), t3);
        
        IBinaryTree t4 = (IBinaryTree) VariableReplacer.replace(t2, ctx);
        assertEquals(t1, t4);
    }

    public void testBTreeVariableMatches2(){
        BinaryTree t1 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTree("a12"));
        BinaryTree t2 = new BinaryTree("a1", new BinaryTree("a11"), new BinaryTreeVariable("v"));
        MatchContext ctx = new MatchContext();
        boolean result = t1.matches(t2, ctx);
        assertFalse(result);
    }
}
