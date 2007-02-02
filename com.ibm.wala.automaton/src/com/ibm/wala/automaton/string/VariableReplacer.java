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

public class VariableReplacer extends DeepSymbolCopier {
    Map map;
    
    public VariableReplacer(IMatchContext ctx) {
        this(ctx.toMap());
    }
    
    public VariableReplacer(Map map) {
        this.map = map;
    }
    
    public ISymbol copy(ISymbol symbol) {
        if (symbol instanceof IVariable) {
            if (map.containsKey(symbol)) {
                ISymbol s = (ISymbol) map.get(symbol);
                return s;
            }
            else {
                return symbol;
            }
        }
        else {
            return super.copy(symbol);
        }
    }
    
    public String copyName(String name) {
        return name;
    }
    
    static public ISymbol replace(ISymbol symbol, IMatchContext ctx) {
        return symbol.copy((new VariableReplacer(ctx)));
    }
}
