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
package com.ibm.wala.automaton.regex.string;

import com.ibm.wala.automaton.string.*;

public class SymbolPattern extends AbstractPattern implements IPattern {
    private ISymbol symbol;
    
    public SymbolPattern(ISymbol symbol) {
        this.symbol = symbol;
    }
    
    public SymbolPattern(String symbol) {
        this(new StringSymbol(symbol));
    }
    
    public ISymbol getSymbol() {
        return symbol;
    }
    
    public int hashCode() {
        return symbol.hashCode();
    }
    
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!getClass().equals(obj.getClass())) return false;
        SymbolPattern p = (SymbolPattern) obj;
        return getSymbol().equals(p.getSymbol());
    }
    
    public String toString() {
        return symbol.toString();
    }

    public void traverse(IPatternVisitor visitor) {
        visitor.onVisit(this);
        visitor.onLeave(this);
    }
    
    public IPattern copy(IPatternCopier copier) {
        return copier.copy(this, null);
    }
    
    static public IPattern toCharSequencePattern(char cs[]){
        IPattern pat = null;
        for (int i = cs.length-1; i >= 0; i--) {
            SymbolPattern sp = new SymbolPattern(Character.toString(cs[i]));
            if (pat == null) {
                pat = sp;
            }
            else {
                pat = new ConcatenationPattern(sp, pat);
            }
        }
        return pat;
    }
    
    static public IPattern toCharSequencePattern(SymbolPattern symp){
        return toCharSequencePattern(symp.getSymbol().getName().toCharArray());
    }
    
    static public IPattern toCharSequencePattern(String str) {
      return toCharSequencePattern(str.toCharArray());
    }
    
    public IPattern toCharSequencePattern() {
        return toCharSequencePattern(this);
    }
}
