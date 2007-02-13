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
package com.ibm.wala.automaton.grammar.tree;

import com.ibm.wala.automaton.grammar.string.IGrammarCopier;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ISymbolCopier;
import com.ibm.wala.automaton.string.ISymbolVisitor;
import com.ibm.wala.automaton.tree.BinaryTreeVariable;
import com.ibm.wala.automaton.tree.IBinaryTree;

public class RTGSymbol implements IBinaryTree {
  private ITreeGrammar tg;

  public RTGSymbol(ITreeGrammar tg) {
    this.tg = tg;
  }

  public ITreeGrammar getTreeGrammar() {
    return tg;
  }

  public String getName() {
    return tg.toString();
  }

  public boolean matches(ISymbol symbol, IMatchContext context) {
    if (symbol instanceof RTGSymbol) {
      ITreeGrammar tg2 = ((RTGSymbol) symbol).getTreeGrammar();
      return RTLComparator.defaultComparator.contains(tg, tg2);
    }
    else if (symbol instanceof IBinaryTree) {
      BinaryTreeVariable v = new BinaryTreeVariable("G");
      ITreeGrammar tg2 = new TreeGrammar(v, new ProductionRule[]{
        new ProductionRule(v, symbol),
      });
      return RTLComparator.defaultComparator.contains(tg, tg2);
    }
    else {
      return false;
    }
  }

  public boolean possiblyMatches(ISymbol symbol, IMatchContext context) {
    return matches(symbol, context) || symbol.matches(this, context);
  }

  public int hashCode() {
    return tg.hashCode();
  }

  public boolean equals(Object obj) {
    if (!getClass().equals(obj.getClass())) return false;
    RTGSymbol tgSym = (RTGSymbol) obj;
    return tg.equals(tgSym.tg);
  }

  public void traverse(ISymbolVisitor visitor) {
    visitor.onVisit(this);
    visitor.onLeave(this);
  }

  public ISymbol copy(ISymbolCopier copier) {
    ISymbol s = copier.copy(this);
    if (s instanceof RTGSymbol) {
      RTGSymbol tgSym = (RTGSymbol) s;
      if (copier instanceof IGrammarCopier) {
        IGrammarCopier gCopier = (IGrammarCopier) copier;
        tgSym.tg = (ITreeGrammar) gCopier.copy(tgSym.tg);
      }
    }
    return s;
  }
  
  public String toString() {
    return "RTGSymbol(" + tg.toString() + ")";
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

  public ISymbol getLabel() {
    return tg.getStartSymbol();
  }
}
