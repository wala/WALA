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

public class TestTreeTransition extends TreeJunitBase {
    public void testTreeTransitionPrePostState() {
        StateBinaryTree input =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("t",
                    new BinaryTreeVariable("x"),
                    new BinaryTreeVariable("y")));
        BinaryTree output =
            new BinaryTree("u",
                    new StateBinaryTree(new State("s2"), new BinaryTreeVariable("y")),
                    new StateBinaryTree(new State("s3"), new BinaryTreeVariable("x")));
        TreeTransition t = new TreeTransition(input, output);
        
        IState pre = t.getPreState();
        IState post = t.getPostState();
        assertTrue(pre instanceof CompositeState);
        assertTrue(post instanceof CompositeState);
        
        CompositeState csPre = (CompositeState) pre;
        CompositeState csPost = (CompositeState) post;
        State s2 = new State("s2");
        State s3 = new State("s3");
        
        assertEquals(input.getState(), csPre.getStates().next());
        for (Iterator i = csPost.getStates(); i.hasNext(); ) {
            Object o = i.next();
            assertTrue(o instanceof IState);
            assertTrue(s2.equals(o) || s3.equals(o));
        }
    }
    
    /*
     * <s1>(null) -> (u[null;null])
     */
    public void testTreeTransitionTransitLeaf() {
        IBinaryTree input = new StateBinaryTree(new State("s1"), null);
        IBinaryTree output = new BinaryTree("u");
        TreeTransition t = new TreeTransition(input, output);
        IBinaryTree result = t.transit(input);
        assertEquals(output, result);
    }
    
    /*
     * <s1>($x) -> (u[$x;$x])
     */
    public void testTreeTransitionTransitWithVariableLeaf() {
        BinaryTreeVariable x = new BinaryTreeVariable("x");
        IBinaryTree input = new StateBinaryTree(new State("s1"), x);
        IBinaryTree output = new BinaryTree("u", x, x);
        TreeTransition t = new TreeTransition(input, output);
        IBinaryTree result = t.transit(new StateBinaryTree(new State("s1"), BinaryTree.LEAF));
        assertEquals(new BinaryTree("u"), result);
    }
    
    /*
     * t[null;null] -> u[null;null]
     */
    public void testTreeTransitionTransitSimple() {
        IBinaryTree input = new BinaryTree("t");
        IBinaryTree output = new BinaryTree("u");
        TreeTransition t = new TreeTransition(input, output);
        IBinaryTree result = t.transit(input);
        assertEquals(output, result);
    }

    /*
     * t[$x;$y] -> u[$y;$x]
     */
    public void testTreeTransitionTransitWithVariable1() {
        IBinaryTree input =
            new BinaryTree("t",
                new BinaryTreeVariable("x"),
                new BinaryTreeVariable("y"));
        IBinaryTree output =
            new BinaryTree("u",
                new BinaryTreeVariable("y"),
                new BinaryTreeVariable("x"));
        TreeTransition t = new TreeTransition(input, output);
        
        IBinaryTree tree =
            new BinaryTree("t",
                new BinaryTree("t1"),
                new BinaryTree("t2"));
        IBinaryTree expected =
            new BinaryTree("u",
                new BinaryTree("t2"),
                new BinaryTree("t1"));
        IBinaryTree result = t.transit(tree);
        assertEquals(expected, result);
    }
    
    /*
     * <s1>(t[$x;$y]) -> u[<s2>($y); <s3>($x)]
     */
    public void testTreeTransitionTransitWithStateVariable1() {
        IBinaryTree input =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("t",
                    new BinaryTreeVariable("x"),
                    new BinaryTreeVariable("y")));
        IBinaryTree output =
            new BinaryTree("u",
                new StateBinaryTree(new State("s2"), new BinaryTreeVariable("y")),
                new StateBinaryTree(new State("s3"), new BinaryTreeVariable("x")));
        TreeTransition t = new TreeTransition(input, output);
        
        IBinaryTree tree =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("t",
                    new BinaryTree("t1"),
                    new BinaryTree("t2")));
        IBinaryTree expected =
            new BinaryTree("u",
                new StateBinaryTree(new State("s2"), new BinaryTree("t2")),
                new StateBinaryTree(new State("s3"), new BinaryTree("t1")));
        IBinaryTree result = t.transit(tree);
        assertEquals(expected, result);
    }

    /*
     * u[<s2>($y); <s3>($x)] -> <s1>(t[$x;$y])
     */
    public void testTreeTransitionTransitWithStateVariable2() {
        IBinaryTree output =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("t",
                    new BinaryTreeVariable("x"),
                    new BinaryTreeVariable("y")));
        IBinaryTree input =
            new BinaryTree("u",
                new StateBinaryTree(new State("s2"), new BinaryTreeVariable("y")),
                new StateBinaryTree(new State("s3"), new BinaryTreeVariable("x")));
        TreeTransition t = new TreeTransition(input, output);
        
        IBinaryTree expected =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("t",
                    new BinaryTree("t1"),
                    new BinaryTree("t2")));
        IBinaryTree tree =
            new BinaryTree("u",
                new StateBinaryTree(new State("s2"), new BinaryTree("t2")),
                new StateBinaryTree(new State("s3"), new BinaryTree("t1")));
        IBinaryTree result = t.transit(tree);
        assertEquals(expected, result);
    }

    /*
     * <s1>($t[$x;$y]) -> $t[<s2>($y); <s3>($x)]
     */
    public void testFilteredTreeTransitionTransitWithStateVariable1() {
        IBinaryTree input =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree(new Variable("t"),
                    new BinaryTreeVariable("x"),
                    new BinaryTreeVariable("y")));
        IBinaryTree output =
            new BinaryTree(new Variable("t"),
                new StateBinaryTree(new State("s2"), new BinaryTreeVariable("y")),
                new StateBinaryTree(new State("s3"), new BinaryTreeVariable("x")));
        FilteredTreeTransition t = new FilteredTreeTransition(input, output,
                new FilteredTreeTransition.IFilter(){
                    public List invoke(ISymbol symbol, List outputs) {
                        IParentBinaryTree pbt = (IParentBinaryTree) outputs.get(0);
                        BinaryTree bt2 = new BinaryTree(pbt.getName().toUpperCase(), pbt.getLeft(), pbt.getRight());
                        List result = new ArrayList();
                        result.add(bt2);
                        return result;
                    }
                },
                new FilteredTreeTransition.ICondition(){
                    public boolean accept(ISymbol symbol, IMatchContext ctx) {
                        ISymbol label = (ISymbol) ctx.get(new Variable("t"));
                        if (label.getName().charAt(0) == 'a') {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                }
            );
        
        IBinaryTree tree1 =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("t",
                    new BinaryTree("t1"),
                    new BinaryTree("t2")));
        IBinaryTree tree2 =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("abc",
                    new BinaryTree("t1"),
                    new BinaryTree("t2")));
        IBinaryTree expected =
            new BinaryTree("ABC",
                new StateBinaryTree(new State("s2"), new BinaryTree("t2")),
                new StateBinaryTree(new State("s3"), new BinaryTree("t1")));
        
        assertFalse(t.accept(tree1, new MatchContext()));
        assertTrue(t.accept(tree2, new MatchContext()));
        
        IBinaryTree result = t.transit(tree2);
        assertEquals(expected, result);
    }

    /*
     * $t[<s2>($y); <s3>($x)] -> <s1>($t[$x;$y])
     */
    public void testFilteredTreeTransitionTransitWithStateVariable2() {
        IBinaryTree output =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree(new Variable("t"),
                    new BinaryTreeVariable("x"),
                    new BinaryTreeVariable("y")));
        IBinaryTree input =
            new BinaryTree(new Variable("t"),
                new StateBinaryTree(new State("s2"), new BinaryTreeVariable("y")),
                new StateBinaryTree(new State("s3"), new BinaryTreeVariable("x")));
        FilteredTreeTransition t = new FilteredTreeTransition(input, output,
                new FilteredTreeTransition.IFilter(){
                    public List invoke(ISymbol symbol, List outputs) {
                        StateBinaryTree sbt = (StateBinaryTree) outputs.get(0);
                        IParentBinaryTree pbt = (IParentBinaryTree) sbt.getTree();
                        BinaryTree bt2 = new BinaryTree(pbt.getName().toUpperCase(), pbt.getLeft(), pbt.getRight());
                        StateBinaryTree sbt2 = new StateBinaryTree(sbt.getState(), bt2);
                        List result = new ArrayList();
                        result.add(sbt2);
                        return result;
                    }
                },
                new FilteredTreeTransition.ICondition(){
                    public boolean accept(ISymbol symbol, IMatchContext ctx) {
                        ISymbol label = (ISymbol) ctx.get(new Variable("t"));
                        if (label.getName().charAt(0) == 'a') {
                            return true;
                        }
                        else {
                            return false;
                        }
                    }
                }
            );
        
        IBinaryTree tree1 =
            new BinaryTree("u",
                new StateBinaryTree(new State("s2"), new BinaryTree("t2")),
                new StateBinaryTree(new State("s3"), new BinaryTree("t1")));
        IBinaryTree tree2 =
            new BinaryTree("abc",
                new StateBinaryTree(new State("s2"), new BinaryTree("t2")),
                new StateBinaryTree(new State("s3"), new BinaryTree("t1")));
        IBinaryTree expected =
            new StateBinaryTree(
                new State("s1"),
                new BinaryTree("ABC",
                    new BinaryTree("t1"),
                    new BinaryTree("t2")));
        
        assertFalse(t.accept(tree1, new MatchContext()));
        assertTrue(t.accept(tree2, new MatchContext()));
        
        IBinaryTree result = t.transit(tree2);
        assertEquals(expected, result);
    }
}
