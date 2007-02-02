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

public class Symbol implements ISymbol {
    private String name;
    
    public Symbol(String name){
        setName(name);
    }
    
    public String getName() {
        return name;
    }
    
    protected void setName(String name) {
        if (name == null) throw(new AssertionError("the symbol name should not be null."));
        this.name = name;
    }

    public int hashCode(){
        return name.hashCode();
    }
    
    public boolean equals(Object obj){
        if (obj != null && getClass().equals(obj.getClass())) {
            Symbol sym = (Symbol) obj;
            return name.equals(sym.name);
        }
        else{
            return false;
        }
    }
    
    public boolean matches(ISymbol symbol, IMatchContext ctx){
        if (equals(symbol)) {
            ctx.put(this, symbol);
            return true;
        }
        else {
            return false;
        }
    }
    
    public boolean possiblyMatches(ISymbol symbol, IMatchContext ctx) {
      return matches(symbol, ctx);
    }
    
    public void traverse(ISymbolVisitor visitor){
        visitor.onVisit(this);
        visitor.onLeave(this);
    }
    
    public ISymbol copy(ISymbolCopier copier) {
        ISymbol s = copier.copy(this);
        if (s instanceof Symbol) {
            Symbol sym = (Symbol) s;
            sym.setName(copier.copyName(sym.name));
        }
        return s;
    }

    public int size(){
        return 0;
    }
    
    public String toString(){
        return "'" + getName() + "'";
    }
    
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw(new RuntimeException(e));
        }
    }
}
