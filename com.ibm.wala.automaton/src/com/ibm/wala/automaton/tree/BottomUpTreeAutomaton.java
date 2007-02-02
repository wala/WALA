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

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.*;

public class BottomUpTreeAutomaton extends StateTransitionSystem implements ITreeAutomaton {
    /**
     * @param initState
     * @param transitions
     * 
     * Example:
     * IState s1 = new State("s1");
     * IState s2 = new State("s2");
     * IState s3 = new State("s3");
     * IBinaryTreeVariable x = new BinaryTreeVariable("x");
     * IBinaryTreeVariable y = new BinaryTreeVariable("y");
     * IBinaryTree t1 = new BinaryTree("t1", new StateBinaryTree(x, s1), new StateBinaryTree(y, s2));
     * IBinaryTree u1 = new StateBinaryTree(new BinaryTree("u1", x, y), s3);
     * new BottomUpTreeAutomaton(
     *   s1,
     *   new BinaryTreeTransition[]{
     *     new TreeTransition(t1, u1),
     *     ...,
     *   }
     * )
     */
    
    static private IState createInitialState(ITransition transitions[]) {
        return createInitialState(AUtil.set(transitions));
    }
    
    static private IState createInitialState(Set transitions) {
        final Set initStates = new HashSet();
        for (Iterator i = transitions.iterator(); i.hasNext(); ) {
            ITreeTransition t = (ITreeTransition) i.next();
            final RuntimeException notInitState = new RuntimeException();
            try {
                t.getInputSymbol().traverse(new ISymbolVisitor(){
                    public void onVisit(ISymbol symbol) {
                        if (symbol instanceof IVariable) {
                            throw(notInitState);
                        }
                    }
                    public void onLeave(ISymbol symbol) {
                    }
                });
                StateBinaryTree sbt = (StateBinaryTree) t.getOutputSymbols().next();
                initStates.add(sbt.getState());
            }
            catch(RuntimeException e) {
                if (e != notInitState) {
                    throw(e);
                }
            }
        }
        CompositeState cs = new CompositeState("cs", initStates);
        return cs;
    }
    
    private Set finalStates;
    
    public BottomUpTreeAutomaton(IState finalStates[], ITransition transitions[]) {
        super(createInitialState(transitions), transitions);
        this.finalStates = AUtil.set(finalStates);
    }
    
    public BottomUpTreeAutomaton(Set finalStates, Set transitions) {
        super(createInitialState(transitions), transitions);
        this.finalStates = new HashSet(finalStates);
    }
    
    public Set getFinalStates() {
        return finalStates;
    }
    
    private Set translateRec(IBinaryTree tree) {
        if (tree instanceof IParentBinaryTree) {
            IParentBinaryTree ptree = (IParentBinaryTree) tree;
            Set rset = translateRec(ptree.getRight());
            Set lset = translateRec(ptree.getLeft());
            Set s = new HashSet();
            for (Iterator r = rset.iterator(); r.hasNext(); ) {
                StateBinaryTree rbt = (StateBinaryTree) r.next();
                for (Iterator l = lset.iterator(); l.hasNext(); ) {
                    StateBinaryTree lbt = (StateBinaryTree) l.next();
                    IParentBinaryTree bt = (IParentBinaryTree) tree.clone();
                    bt.setLeft(lbt);
                    bt.setRight(rbt);
                    Set ts = translateState(bt);
                    s.addAll(ts);
                }
            }
            //System.err.println(tree + " -> " + s);
            return s;
        }
        else {
            Set s = translateState(tree);
            //System.err.println(tree + " -> " + s);
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
                s.add(bt);
            }
        }
        return s;
    }
    
    public Set translate(IBinaryTree tree) {
        Set trees = translateRec(tree);
        Set result = new HashSet();
        for (Iterator i = trees.iterator(); i.hasNext(); ) {
            StateBinaryTree sbt = (StateBinaryTree) i.next();
            if (finalStates.contains(sbt.getState())) {
                result.add(sbt.getTree());
            }
        }
        return result;
    }
    
    public boolean accept(IBinaryTree tree) {
        return !translate(tree).isEmpty();
    }
}
