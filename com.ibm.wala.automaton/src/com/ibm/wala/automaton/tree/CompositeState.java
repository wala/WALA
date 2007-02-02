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
/**
 * 
 */
package com.ibm.wala.automaton.tree;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.automaton.AUtil;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.IStateCopier;
import com.ibm.wala.automaton.string.State;

public class CompositeState extends State {
    Set states;
    
    public CompositeState(String name, IState states[]) {
        this(name, AUtil.set(states));
    }
    
    public CompositeState(String name, Set states) {
        super(name);
        this.states = new HashSet(states);
    }
    
    public IState copy(IStateCopier copier) {
        IState s = super.copy(copier);
        if (s instanceof CompositeState) {
            CompositeState cs = (CompositeState) s;
            cs.states = (Set) copier.copyStates(cs.states);
        }
        return s;
    }
    
    public Iterator getStates() {
        return states.iterator();
    }
    
    public int hashCode() {
        return super.hashCode() + states.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            CompositeState cs = (CompositeState) obj;
            return states.equals(cs.states);
        }
        else {
            return false;
        }
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Iterator i = states.iterator(); i.hasNext(); ) {
            IState s = (IState) i.next();
            buff.append(s);
            if (i.hasNext()) {
                buff.append(",");
            }
        }
        return super.toString() + "(" + buff + ")";
    }
}