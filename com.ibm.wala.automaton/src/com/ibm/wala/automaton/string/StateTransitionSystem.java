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
package com.ibm.wala.automaton.string;

import java.util.*;

import com.ibm.wala.automaton.*;

public class StateTransitionSystem implements IStateTransitionSystem {
    private IState initialState;
    private Set transitions;

    public StateTransitionSystem(IState initialState, Set transitions){
        this();
        setInitialState(initialState);
        this.transitions.addAll(transitions);
    }
    
    public StateTransitionSystem(IState initialState, ITransition[] transitions) {
        this(initialState, AUtil.set(transitions));
    }
    
    public StateTransitionSystem(IStateTransitionSystem automaton) {
        this(automaton.getInitialState(), automaton.getTransitions());
    }
    
    public StateTransitionSystem(){
        initialState = null;
        transitions = new HashSet();
    }
    
    public IState getInitialState() {
        return initialState;
    }
    
    public void setInitialState(IState state){
        this.initialState = state;
    }

    public Set getTransitions() {
        return transitions;
    }
    
    public Set getTransitions(IState preState){
        Set l = new HashSet();
        for (Iterator i = getTransitions().iterator(); i.hasNext(); ){
            ITransition transition = (ITransition) i.next();
            if (transition.getPreState().equals(preState)) {
                l.add(transition);
            }
        }
        return l;
    }

    /**
     * Select transitions that have the same input symbol as 'symbol'.
     */
    public Set getTransitions(IState preState, ISymbol symbol){
        Set l = getTransitions(preState);
        for (Iterator i = l.iterator(); i.hasNext(); ){
            ITransition transition = (ITransition) i.next();
            if (!transition.getInputSymbol().equals(symbol)) {
                i.remove();
            }
        }
        return l;
    }
    
    /**
     * Select transitions that have the input symbol matched by 'symbol'.
     */
    public Set getAcceptTransitions(IState preState, ISymbol symbol){
        Set l = new HashSet();
        for (Iterator i = getTransitions(preState).iterator(); i.hasNext(); ){
            ITransition transition = (ITransition) i.next();
            if (transition.accept(symbol, new MatchContext())) {
                l.add(transition);
            }
        }
        return l;
    }
    
    public Set getEpsilonTransitions(IState preState) {
        Set l = getTransitions(preState);
        for (Iterator i = l.iterator(); i.hasNext(); ){
            ITransition transition = (ITransition) i.next();
            if (!transition.isEpsilonTransition()) {
                i.remove();
            }
        }
        return l;
    }
    
    public Set getStates() {
        Set states = new HashSet();
        if (initialState != null) {
            states.add(initialState);
        }
        for (Iterator i = getTransitions().iterator(); i.hasNext(); ) {
            ITransition transition = (ITransition) i.next();
            states.add(transition.getPreState());
            states.add(transition.getPostState());
        }
        return states;
    }
    
    public void traverseTransitions(ITransitionVisitor visitor) {
        for (Iterator i = getTransitions().iterator(); i.hasNext(); ) {
            ITransition transition = (ITransition) i.next();
            visitor.onVisit(transition);
        }
    }
    
    public void traverseStates(final IStateVisitor visitor) {
        visitor.onVisit(getInitialState());
        traverseTransitions(new ITransitionVisitor(){
            public void onVisit(ITransition transition) {
                visitor.onVisit(transition.getPreState());
                visitor.onVisit(transition.getPostState());
            }
        });
    }
    
    public void traverse(IAutomatonVisitor visitor) {
        traverseStates(visitor);
        traverseTransitions(visitor);
    }
    
    public int hashCode() {
        return getClass().hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        StateTransitionSystem sts = (StateTransitionSystem) obj;
        return initialState.equals(sts.initialState)
            && transitions.equals(sts.transitions);
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }
    
    public IStateTransitionSystem copy(ISTSCopier copier) {
        IStateTransitionSystem ists = copier.copy(this);
        if (ists instanceof StateTransitionSystem) {
            StateTransitionSystem sts = (StateTransitionSystem) ists; 
            sts.initialState = copier.copy(sts.initialState);
            sts.transitions = new HashSet(copier.copyTransitions(sts.transitions));
        }
        return ists;
    }
    
    public String toString() {
        StringBuffer transitions = new StringBuffer();
        for (Iterator i = AUtil.sort(getTransitions()).iterator(); i.hasNext(); ) {
            ITransition t = (ITransition) i.next();
            transitions.append(t.toString());
            if (i.hasNext()) {
                transitions.append(", ");
            }
        }
        return "{init:" + getInitialState() + ", transitions:{" + transitions + "}}";
    }
}
