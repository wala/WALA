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
package com.ibm.wala.automaton.tree;

import java.util.*;

import com.ibm.wala.automaton.string.*;

public class TopDownTreeAutomaton extends StateTransitionSystem implements ITreeAutomaton {
    /**
     * @param initState
     * @param transitions
     * 
     * Example:
     * IState s1 = new State("s1");
     * IState s2 = new State("s2");
     * IState s3 = new State("s3");
     * IVariable x = new BinaryTreeVariable("x");
     * IVariable y = new BinaryTreeVariable("y");
     * IBinaryTree t1 = new StateBinaryTree(new BinaryTree("t1", x, y), s1);
     * IBinaryTree u1 = new BinaryTree("u1", new StateBinaryTree(x, s2), new StateBinaryTree(y, s3));
     * new TopDownTreeAutomaton(
     *   s1,
     *   new BinaryTreeTransition[]{
     *     new TreeTransition(t1, u1),
     *     ...,
     *   }
     * )
     */
    public TopDownTreeAutomaton(IState initState, ITransition transitions[]) {
        super(initState, transitions);
    }
    
    public TopDownTreeAutomaton(IState initState, Set transitions) {
        super(initState, transitions);
    }
    
    private Set translateRec(IBinaryTree tree) {
        if (tree instanceof StateBinaryTree) {
            return translateState(tree);
        }
        else if (tree instanceof IParentBinaryTree) {
            IParentBinaryTree ptree = (IParentBinaryTree) tree;
            Set rset = translateRec(ptree.getRight());
            Set lset = translateRec(ptree.getLeft());
            Set s = new HashSet();
            for (Iterator r = rset.iterator(); r.hasNext(); ) {
                IBinaryTree rbt = (IBinaryTree) r.next();
                for (Iterator l = lset.iterator(); l.hasNext(); ) {
                    IBinaryTree lbt = (IBinaryTree) l.next();
                    IParentBinaryTree bt = (IParentBinaryTree) tree.clone();
                    bt.setLeft(lbt);
                    bt.setRight(rbt);
                    s.add(bt);
                }
            }
            return s;
        }
        else {
            Set s = new HashSet();
            s.add(tree);
            return s;
        }
    }
    
    private Set translateState(IBinaryTree tree) {
        Set s = new HashSet();
        for (Iterator i = getTransitions().iterator(); i.hasNext(); ) {
            ITreeTransition trans = (ITreeTransition) i.next();
            MatchContext ctx = new MatchContext();
            if (trans.accept(tree, ctx)) {
                IBinaryTree bt = trans.transit(tree);
                Set bt2 = translateRec(bt);
                s.addAll(bt2);
            }
        }
        return s;
    }
    
    public Set translate(IBinaryTree tree, IState state) {
        StateBinaryTree sbt = new StateBinaryTree(tree, state);
        return translateState(sbt);
    }
    
    public Set translate(IBinaryTree tree) {
        return translate(tree, getInitialState());
    }
    
    public boolean accept(IBinaryTree tree) {
        return !translate(tree).isEmpty();
    }
}
