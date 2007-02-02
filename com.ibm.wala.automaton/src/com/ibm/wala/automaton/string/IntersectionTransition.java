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

public class IntersectionTransition extends FilteredTransition {
    static private class Condition implements ICondition {
        private Collection transitions;
        
        public Condition(Collection transitions) {
            this.transitions = transitions;
        }
        
        public Condition(ITransition transitions[]) {
            this(AUtil.list(transitions));
        }
        
        public boolean accept(ISymbol symbol, IMatchContext ctx) {
            for (Iterator i = transitions.iterator(); i.hasNext(); ) {
                ITransition t = (ITransition) i.next();
                if (!t.accept(symbol, ctx)) {
                    return false;
                }
            }
            return true;
        }
    }
    
    public IntersectionTransition(IState preState, IState postState, ISymbol inputSymbol, List outputSymbols, IFilter filter, Set transitions) {
        super(preState, postState, inputSymbol, outputSymbols, filter, new Condition(transitions));
    }
    
    public IntersectionTransition(IState preState, IState postState, ISymbol inputSymbol, ISymbol outputSymbols[], IFilter filter, ITransition transitions[]) {
        super(preState, postState, inputSymbol, outputSymbols, filter, new Condition(transitions));
    }
    
    public IntersectionTransition(IState preState, IState postState, ISymbol inputSymbol, Iterator outputSymbols, IFilter filter, ITransition transitions[]) {
        super(preState, postState, inputSymbol, AUtil.list(outputSymbols), filter, new Condition(transitions));
    }
    
    public IntersectionTransition(IState preState, IState postState, ISymbol inputSymbol, ITransition transitions[]) {
        super(preState, postState, inputSymbol, new Condition(transitions));
    }
}
