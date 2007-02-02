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

public class Automaton extends StateTransitionSystem implements IAutomaton {
    private Set finalStates;

    public Automaton(IState initialState, Set finalStates, Set transitions){
        super(initialState, transitions);
        this.finalStates = new HashSet(finalStates);
    }
    
    public Automaton(IState initialState, IState[] finalStates, ITransition[] transitions) {
        super(initialState, transitions);
        this.finalStates = AUtil.set(finalStates);
    }

    public Automaton(IAutomaton automaton) {
        this(automaton.getInitialState(), automaton.getFinalStates(), automaton.getTransitions());
    }
    
    public Automaton(){
        super();
        finalStates = new HashSet();
    }
    
    public Set getFinalStates() {
        return finalStates;
    }

    public Set getStates() {
        Set states = super.getStates();
        states.addAll(finalStates);
        return states;
    }
    
    private boolean isFinalState(IState state){
        return getFinalStates().contains(state);
    }
    
    public List translate(List symbols) {
        return translate(getInitialState(), symbols);
    }
    
    public boolean accept(List symbols) {
        return accept(getInitialState(), symbols);
    }
    
    public boolean accept(IState state, List symbols){
        return !translate(state, symbols).isEmpty();
    }
    
    protected List translate(IState state, List symbols){
        if (symbols.isEmpty()) {
            if (isFinalState(state)) {
                List l = new ArrayList();
                l.add(new ArrayList());
                return l;
            }
        }
        
        List output = new ArrayList();
        
        Set epsilons = getEpsilonTransitions(state);
        for (Iterator i = epsilons.iterator(); i.hasNext(); ) {
            ITransition transition = (ITransition) i.next();
            if (transition.getPostState().equals(state)) {
              continue;
            }
            List results = null;
            results = translate(transition.getPostState(), symbols);
            if (results != null) {
                for (Iterator j = results.iterator(); j.hasNext(); ) {
                    List l = (List) j.next();
                    l.addAll(0, AUtil.list(transition.getOutputSymbols()));
                }
                output.addAll(results);
            }
        }
        
        if (symbols.isEmpty()) {
            if (output.isEmpty()) {
                return new ArrayList();
            }
            else {
                return output;
            }
        }
        
        List tail = new ArrayList(symbols);
        ISymbol symbol = (ISymbol) tail.get(0);
        tail.remove(0);

        Set transitions = getAcceptTransitions(state, symbol);
        for (Iterator i = transitions.iterator(); i.hasNext(); ) {
            ITransition transition = (ITransition) i.next();
            List results = null;
            results = translate(transition.getPostState(), tail);
            if (results != null) {
                for (Iterator j = results.iterator(); j.hasNext(); ) {
                    List l = (List) j.next();
                    l.addAll(0, transition.transit(symbol));
                }
                output.addAll(results);
            }
        }
        
        return output;
    }
    
    public void traverseStates(final IStateVisitor visitor) {
        super.traverseStates(visitor);
        for (Iterator i = getFinalStates().iterator(); i.hasNext(); ) {
            IState state = (IState) i.next();
            visitor.onVisit(state);
        }
    }
    
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            Automaton a = (Automaton) obj;
            return finalStates.equals(a.finalStates);
        }
        else {
            return false;
        }
    }
    
    public IStateTransitionSystem copy(ISTSCopier copier) {
        Automaton a = (Automaton) super.copy(copier);
        a.finalStates = new HashSet(copier.copyStates(a.finalStates));
        return a;
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
        StringBuffer finalStates = new StringBuffer();
        for (Iterator i = getFinalStates().iterator(); i.hasNext(); ) {
            IState state = (IState) i.next();
            finalStates.append(state.toString());
            if (i.hasNext()) {
                finalStates.append("; ");
            }
        }
        return "{init:" + getInitialState() + ", final:{" + finalStates + "}, transitions:{" + transitions + "}}";
    }
}
