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

import com.ibm.wala.automaton.string.*;

public class VState implements IState {
    private IState state;
    private IVariable variable;
    
    public VState(IState state, IVariable v) {
        this.state = state;
        this.variable = v;
    }
    
    public IState getState() {
        return state;
    }
    
    public IVariable getVariable() {
        return variable;
    }

    public String getName() {
        return state.getName();
    }
    
    public int hashCode() {
        return state.hashCode() + variable.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!getClass().equals(obj.getClass())) return false;
        VState vs = (VState) obj;
        return state.equals(vs.state) && variable.equals(vs.variable);
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }

    public IState copy(IStateCopier copier) {
        IState s = copier.copy(this);
        if (s instanceof VState) {
            VState vs = (VState) s;
            vs.state = copier.copyStateReference(vs, vs.state);
        }
        return s;
    }
    
    public String toString() {
        return state.toString() + "(" + variable.toString() + ")";
    }
}
