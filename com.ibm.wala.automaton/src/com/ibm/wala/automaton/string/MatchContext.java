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

public class MatchContext implements IMatchContext {
    private Map map;
    
    public MatchContext() {
        this.map = new HashMap();
    }
    
    public MatchContext(Map map) {
        this.map = new HashMap(map);
    }

    public void put(ISymbol key, ISymbol val) {
        map.put(key, val);
    }

    public ISymbol get(ISymbol key) {
        return (ISymbol) map.get(key);
    }

    public Iterator iterator() {
        return map.keySet().iterator();
    }
    
    public Map toMap() {
        return new HashMap(map);
    }
}
