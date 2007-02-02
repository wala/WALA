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

import com.ibm.wala.automaton.AUtil;

public class Transition implements ITransition {
    private IState preState;
    private IState postState;
    private ISymbol inputSymbol;
    private List outputSymbols;
    
    static final public ISymbol EpsilonSymbol = null;
    
    private Transition() {
        preState = null;
        postState = null;
        inputSymbol = null;
        outputSymbols = new ArrayList();
    }
    
    public Transition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols){
        this();
        setPreState(preState);
        setPostState(postState);
        setInputSymbol(inputSymbol);
        if (outputSymbols != null) {
            appendOutputSymbols(outputSymbols);
        }
    }
    
    public Transition(IState preState, IState postState, ISymbol inputSymbol, Iterator outputSymbols) {
        this(preState, postState, inputSymbol, AUtil.list(outputSymbols));
    }

    public Transition(IState preState, IState postState, ISymbol inputSymbol, ISymbol[] outputSymbols){
        this(preState, postState, inputSymbol, AUtil.list(outputSymbols));
    }
    
    public Transition(IState preState, IState postState, ISymbol inputSymbol) {
        this(preState, postState, inputSymbol, (List) null);
    }
    
    public Transition(IState preState, IState postState) {
        this(preState, postState, EpsilonSymbol);
    }
    
    public Transition(ITransition trans) {
        this(trans.getPreState(), trans.getPostState(), trans.getInputSymbol(), AUtil.list(trans.getOutputSymbols()));
    }
    
    public IState getPreState() {
        return preState;
    }
    
    public void setPreState(IState state){
        preState = state;
    }

    public IState getPostState() {
        return postState;
    }
    
    public void setPostState(IState state){
        postState = state;
    }

    public ISymbol getInputSymbol() {
        return inputSymbol;
    }
    
    public void setInputSymbol(ISymbol symbol){
        inputSymbol = symbol;
    }

    public Iterator getOutputSymbols() {
        return outputSymbols.iterator();
    }
    
    public boolean hasOutputSymbols() {
        return !outputSymbols.isEmpty();
    }
    
    public void appendOutputSymbols(List symbols){
        outputSymbols.addAll(symbols);
    }
    
    public void prependOutputSymbols(List symbols) {
        outputSymbols.addAll(0, symbols);
    }
    
    public boolean isEpsilonTransition() {
        return inputSymbol == EpsilonSymbol;
    }
    
    public boolean accept(ISymbol symbol, IMatchContext ctx){
        return (inputSymbol != null)
            && (!(symbol instanceof IVariable))
            && inputSymbol.matches(symbol, ctx);
    }
    
    public List transit(ISymbol symbol){
        IMatchContext ctx = new MatchContext();
        if (accept(symbol, ctx)) {
            ctx.put(new Variable("_"), symbol);
            return rewrite(outputSymbols, ctx);
        }
        else{
            return null;
        }
    }
    
    private List rewrite(List symbols, IMatchContext context){
        List result = new ArrayList();
        for (Iterator i = symbols.iterator(); i.hasNext(); ) {
            ISymbol symbol = (ISymbol) i.next();
            result.add(VariableReplacer.replace(symbol,context));
        }
        return result;
    }
    
    public int hashCode() {
        return getClass().hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        Transition t = (Transition) obj;
        return ((inputSymbol==null) ? (t.inputSymbol==null) : inputSymbol.equals(t.inputSymbol))
            && outputSymbols.equals(t.outputSymbols)
            && postState.equals(t.postState)
            && preState.equals(t.preState);
    }
    
    public String toString(){
        StringBuffer buff = new StringBuffer();
        for (Iterator i = getOutputSymbols(); i.hasNext(); ) {
            ISymbol s = (ISymbol) i.next();
            buff.append(s.toString());
            if (i.hasNext()) {
                buff.append(", ");
            }
        }
        return getPreState().toString() + "(" + getInputSymbol() + ")"
            + " -> "
            + getPostState().toString() + "(" + buff.toString() + ")"; 
    }

    public Object clone() {
        try {
            Transition t = (Transition) super.clone();
            t.outputSymbols = new ArrayList(t.outputSymbols);
            return t;
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }
    
    public ITransition copy(ITransitionCopier copier) {
        ITransition t = copier.copy(this);
        if (t instanceof Transition) {
            Transition tt = (Transition) t;
            tt.preState = copier.copy(tt.preState);
            tt.postState = copier.copy(tt.postState);
            tt.inputSymbol = copier.copy(tt.inputSymbol);
            tt.outputSymbols = new ArrayList(copier.copySymbols(tt.outputSymbols));
        }
        return t;
    }
}
