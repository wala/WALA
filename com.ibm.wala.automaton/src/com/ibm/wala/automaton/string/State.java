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

public class State implements IState {
    private String name;
    
    public State(String name){
        setName(name);
    }
    
    public String getName(){
        return name;
    }
    protected void setName(String name){
        this.name = name;
    }
    
    public int hashCode(){
        return name.hashCode();
    }
    
    public boolean equals(Object obj){
        if ((obj != null) && getClass().equals(obj.getClass())) {
            State state = (State) obj;
            return getName().equals(state.getName());
        }
        else{
            return false;
        }
    }
    
    public String toString(){
        return "<" + getName() + ">";
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
        if (s instanceof State) {
            State ss = (State) s;
            ss.setName(copier.copyStateName(name));
        }
        return s;
    }
}
