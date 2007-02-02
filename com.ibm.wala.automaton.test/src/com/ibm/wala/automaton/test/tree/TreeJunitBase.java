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

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.grammar.tree.*;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.tree.*;

import junit.framework.TestCase;

abstract public class TreeJunitBase extends TestCase {
    public ITreeVariable V(int n) {
        return new TreeVariable("v" + n);
    }
    
    public ITreeVariable V(String name) {
        return new TreeVariable(name);
    }
    
    public ITree T(String name, List children) {
        return new Tree(name, children);
    }
    
    public ITree T(String name, ITree children[]) {
        return new Tree(name, children);
    }
    
    public ITree T(String name, ITree t1) {
        return new Tree(name, new ITree[]{t1});
    }

    public ITree T(String name, ITree t1, ITree t2) {
        return new Tree(name, new ITree[]{t1, t2});
    }

    public ITree T(String name, ITree t1, ITree t2, ITree t3) {
        return new Tree(name, new ITree[]{t1, t2, t3});
    }

    public ITree T(String name, ITree t1, ITree t2, ITree t3, ITree t4) {
        return new Tree(name, new ITree[]{t1, t2, t3, t4});
    }
    
    public IBinaryTreeVariable BV(int n) {
        return new BinaryTreeVariable("bv" + n);
    }
    
    public IBinaryTreeVariable BV(String name) {
        return new BinaryTreeVariable(name);
    }
    
    public IBinaryTree BT(String name, IBinaryTree l, IBinaryTree r) {
        return new BinaryTree(name, l, r);
    }
    
    public IBinaryTree BT(ISymbol label, IBinaryTree l, IBinaryTree r) {
        return new BinaryTree(label, l, r);
    }
    
    public IBinaryTree BT(String name) {
        return new BinaryTree(name);
    }

    public IBinaryTree BT(ISymbol label) {
        return new BinaryTree(label);
    }
    
    public ITreeGrammar TG(IBinaryTreeVariable start, Object rules[]) {
        Set l = new HashSet();
        for (int i = 0; i < rules.length; i += 2) {
            IBinaryTreeVariable left = (IBinaryTreeVariable) rules[i];
            IBinaryTree right = (IBinaryTree) rules[i+1];
            if (right == null) {
                right = BinaryTree.LEAF;
            }
            IProductionRule rule = new ProductionRule(left, right);
            l.add(rule);
        }
        return new TreeGrammar(start, l);
    }

    public void assertContains(ITreeGrammar g1, ITreeGrammar g2) {
        assertTrue(RTLComparator.defaultComparator.contains(g1, g2));
    }
    
    public void assertNotContains(ITreeGrammar g1, ITreeGrammar g2) {
        assertFalse(RTLComparator.defaultComparator.contains(g1, g2));
    }
}
