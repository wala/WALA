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

import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;

public class TestTree extends TreeJunitBase {
    public void testTreeEquality(){
        Tree a1 = new Tree("a1");
        Tree a11 = new Tree("a11");
        Tree a12 = new Tree("a12");
        a1.addChild(a11);
        a1.addChild(a12);
        Tree t1 = new Tree("a1", new Tree[]{
                new Tree("a11"),
                new Tree("a12"),
        });
        Tree t2 = new Tree("a1", new Tree[]{
                new Tree("a11"),
                new Tree("b12"),
        });
        assertEquals(t1, a1);
        assertFalse(t2.equals(a1));
    }

    public void testTreeAddChildren(){
        Tree a1 = new Tree("a1");
        Tree a11 = new Tree("a11");
        Tree a12 = new Tree("a12");
        List l = new ArrayList();
        l.add(a11);
        l.add(a12);
        a1.addChildren(l);
        Tree t = new Tree("a1", new Tree[]{
                new Tree("a11"),
                new Tree("a12"),
        });
        assertEquals(t, a1);
    }
    
    public void testTreeSize(){
        Tree t = new Tree("a1", new Tree[]{
                new Tree("a11"),
                new Tree("a12"),
        });
        assertEquals(2, t.size());
    }

    public void testTreeClearChildren(){
        Tree t = new Tree("a1", new ITree[]{
                new Tree("a11"),
                new Tree("a12"),
        });
        t.clearChildren();
        assertEquals(0, t.size());
    }
    
    public void testTreeVariableMatches(){
        Tree t1 = new Tree("a1", new ITree[]{
                new Tree("a11"),
                new Tree("a12"),
        });
        Tree t2 = new Tree("a1", new ITree[]{
                new Tree("a11"),
                new TreeVariable("v"),
        });
        MatchContext ctx = new MatchContext();
        boolean result = t2.matches(t1, ctx);
        assertTrue(result);
        
        ITree t3 = (ITree) ctx.get(new TreeVariable("v"));
        assertEquals(new Tree("a12"), t3);
        
        ITree t4 = (ITree) VariableReplacer.replace(t2, ctx);
        assertEquals(t1, t4);
    }

    public void testTreeVariableMatches2(){
        Tree t1 = new Tree("a1", new ITree[]{
                new Tree("a11"),
                new Tree("a12"),
        });
        Tree t2 = new Tree("a1", new ITree[]{
                new Tree("a11"),
                new TreeVariable("v"),
        });
        MatchContext ctx = new MatchContext();
        boolean result = t1.matches(t2, ctx);
        assertFalse(result);
    }
}
