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

public class FilteredTransition extends Transition {
    public interface IFilter {
        public List invoke(ISymbol symbol, List outputs);
    }
    
    public interface ICondition {
        public boolean accept(ISymbol symbol, IMatchContext ctx);
    }
    
    static private IFilter DEFAULT_FILTER = new IFilter() {
        public List invoke(ISymbol symbol, List outputs) {
            return outputs;
        }
    };
    
    static private ICondition DEFAULT_CONDITION = new ICondition(){
        public boolean accept(ISymbol symbol, IMatchContext ctx) {
            return true;
        }
    };
    
    private IFilter filter;
    private ICondition condition;
    
    public FilteredTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IFilter filter, ICondition condition) {
        super(preState, postState, inputSymbol, outputSymbols);
        this.filter = (filter == null) ? DEFAULT_FILTER : filter;
        this.condition = (condition == null) ? DEFAULT_CONDITION : condition;
    }

    public FilteredTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IFilter filter) {
        super(preState, postState, inputSymbol, outputSymbols);
        this.filter = (filter == null) ? DEFAULT_FILTER : filter;
        this.condition = DEFAULT_CONDITION;
    }
    
    public FilteredTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[], IFilter filter, ICondition condition) {
        super(preState, postState, inputSymbol, outputSymbols);
        this.filter = (filter == null) ? DEFAULT_FILTER : filter;
        this.condition = (condition == null) ? DEFAULT_CONDITION : condition;
    }

    public FilteredTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[], ICondition condition) {
        super(preState, postState, inputSymbol, outputSymbols);
        this.filter = DEFAULT_FILTER;
        this.condition = (condition == null) ? DEFAULT_CONDITION : condition;
    }
    
    public FilteredTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[], IFilter filter) {
        super(preState, postState, inputSymbol, outputSymbols);
        this.filter = (filter == null) ? DEFAULT_FILTER : filter;
        this.condition = DEFAULT_CONDITION;
    }

    public FilteredTransition(IState preState, IState postState, ISymbol inputSymbol, ICondition condition) {
        super(preState, postState, inputSymbol);
        this.filter = DEFAULT_FILTER;
        this.condition = (condition == null) ? DEFAULT_CONDITION : condition;
    }
    
    public void appendOutputSymbols(List outputs) {
        super.appendOutputSymbols(outputs);
        final int len = outputs.size();
        final IFilter oldFilter = filter;
        filter = new IFilter(){
            public List invoke(ISymbol symbol, List outputs) {
                List l = oldFilter.invoke(symbol, outputs.subList(0, outputs.size()-len));
                l.addAll(outputs.subList(len-1, outputs.size()));
                return l;
            }
        };
    }
    
    public void prependOutputSymbols(List outputs) {
        super.prependOutputSymbols(outputs);
        final int len = outputs.size();
        final IFilter oldFilter = filter;
        filter = new IFilter(){
            public List invoke(ISymbol symbol, List outputs) {
                List l = oldFilter.invoke(symbol, outputs.subList(len, outputs.size()));
                l.addAll(0, outputs.subList(0, len));
                return l;
            }
        };
    }

    public boolean accept(ISymbol s, IMatchContext ctx) {
        return (!(s instanceof IVariable)) // don't accept variables.
            && super.accept(s, ctx)
            && condition.accept(s, ctx);
    }
    
    public List transit(ISymbol s) {
        List outputs = super.transit(s);
        if (outputs != null) {
            return filter.invoke(s, outputs);
        }
        else {
            return null;
        }
    }
    
    public IFilter getFilter() {
        return filter;
    }
    
    public ICondition getCondition() {
        return condition;
    }
    
    public int hashCode() {
        return super.hashCode() + filter.hashCode() + condition.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (super.equals(obj)) {
            FilteredTransition t = (FilteredTransition) obj;
            return filter.equals(t.filter) && condition.equals(t.condition);
        }
        else {
            return false;
        }
    }
}
