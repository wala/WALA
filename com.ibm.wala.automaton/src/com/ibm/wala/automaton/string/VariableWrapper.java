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

public class VariableWrapper implements IVariable {
  private IVariable var;

  public VariableWrapper(IVariable v) {
    assert(v != null);
    this.var = v;
  }

  public IVariable getVariable() {
    return var;
  }

  public int hashCode() {
    return var.hashCode();
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof VariableWrapper)) return false;
    if (this == obj) return true;
    VariableWrapper w = (VariableWrapper) obj;
    return var.equals(w.var);
  }

  public String toString() {
    return "$" + var.toString();
  }

  public String getName() {
    return var.getName();
  }

  public boolean matches(ISymbol symbol, IMatchContext context) {
    context.put(this, symbol);
    return var.matches(symbol, context);
  }

  public boolean possiblyMatches(ISymbol symbol, IMatchContext context) {
    context.put(this, symbol);
    return var.possiblyMatches(symbol, context);
  }

  public void traverse(ISymbolVisitor visitor) {
    visitor.onVisit(this);
    //var.traverse(visitor);
    visitor.onLeave(this);
  }

  public ISymbol copy(ISymbolCopier copier) {
    ISymbol s = copier.copy(this);
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
}
