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
package com.ibm.wala.automaton.grammar.string;

import java.util.*;

import com.ibm.wala.automaton.string.*;

public class ContextFreeGrammar<T extends IProductionRule> extends SimpleGrammar<T> implements IContextFreeGrammar<T> {
    public ContextFreeGrammar(){
        super();
    }
    
    public ContextFreeGrammar(IVariable startSymbol, Collection<T> rules){
        super(startSymbol, rules);
    }

    public ContextFreeGrammar(IVariable startSymbol, T rules[]){
        super(startSymbol, rules);
    }
    
    public ContextFreeGrammar(SimpleGrammar<T> g) {
        super(g);
    }
}
