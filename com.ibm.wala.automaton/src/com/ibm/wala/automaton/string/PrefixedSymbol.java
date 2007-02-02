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

public class PrefixedSymbol implements ISymbol {
  static public String SEPARATOR = ":";

  ISymbol prefix;
  ISymbol local;

  public PrefixedSymbol(ISymbol prefix, ISymbol symbol) {
    this.prefix = prefix;
    this.local = symbol;
  }

  public PrefixedSymbol(String prefix, String symbol) {
    this(new StringSymbol(prefix), new StringSymbol(symbol));
  }

  public String getName() {
    return prefix.getName() + SEPARATOR + local.getName();
  }

  public ISymbol getPrefix() {
    return prefix;
  }

  public ISymbol getLocal() {
    return local;
  }

  /*
    public void setName(String name) {
        int sep = name.indexOf(SEPARATOR);
        if (sep >= 0) {
            String prefixStr = name.substring(0, sep);
            String localStr = name.substring(sep+1, name.length());
            getPrefix().setName(prefixStr);
            getLocal().setName(localStr);
        }
        else{
            getLocal().setName(name);
        }
    }
   */

  public boolean matches(ISymbol symbol, IMatchContext ctx) {
    if (!symbol.getClass().equals(this.getClass())) {
      return false;
    }
    PrefixedSymbol psym = (PrefixedSymbol) symbol;
    return prefix.matches(psym.getPrefix(), ctx)
    && local.matches(psym.getLocal(), ctx);
  }

  public boolean possiblyMatches(ISymbol symbol, IMatchContext ctx) {
    if (!symbol.getClass().equals(this.getClass())) {
      return false;
    }
    PrefixedSymbol psym = (PrefixedSymbol) symbol;
    return prefix.possiblyMatches(psym.getPrefix(), ctx)
    && local.possiblyMatches(psym.getLocal(), ctx);
  }

  public void traverse(ISymbolVisitor visitor) {
    visitor.onVisit(this);
    getPrefix().traverse(visitor);
    getLocal().traverse(visitor);
    visitor.onLeave(this);
  }

  public ISymbol copy(ISymbolCopier copier) {
    ISymbol s = copier.copy(this);
    if (s instanceof PrefixedSymbol) {
      PrefixedSymbol ps = (PrefixedSymbol) s;
      ps.local = copier.copySymbolReference(ps, ps.local);
      ps.prefix = copier.copySymbolReference(ps, ps.prefix);
    }
    return s;
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw(new RuntimeException(e));
    }
  }

  public int size() {
    return 0;
  }

  public int hashCode() {
    return prefix.hashCode() + local.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof PrefixedSymbol)) return false;
    PrefixedSymbol psym = (PrefixedSymbol) obj;
    return prefix.equals(psym.getPrefix())
    && local.equals(psym.getLocal());
  }

  public String toString() {
    return "'" + getName() + "'";
  }
}
