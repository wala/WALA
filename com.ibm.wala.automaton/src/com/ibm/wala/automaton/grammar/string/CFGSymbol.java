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

import com.ibm.wala.automaton.regex.string.StringPatternSymbol;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ISymbolCopier;
import com.ibm.wala.automaton.string.ISymbolVisitor;
import com.ibm.wala.automaton.string.StringSymbol;

public class CFGSymbol implements IGrammarSymbol<IContextFreeGrammar> {
  private IContextFreeGrammar cfg;

  public CFGSymbol(IContextFreeGrammar cfg) {
    this.cfg = cfg;
  }

  public IContextFreeGrammar getGrammar() {
    return cfg;
  }

  public String getName() {
    return cfg.toString();
  }

  // TODO: improve CFGSymbol#matches()
  public boolean matches(ISymbol symbol, IMatchContext context) {
    if (symbol instanceof StringPatternSymbol) {
      return false;
    }
    else if (symbol instanceof StringSymbol) {
      StringSymbol ss = (StringSymbol) symbol;
      return CFLReachability.containsSome(cfg, ss.toCharSymbols());
    }
    else if (symbol instanceof CFGSymbol) {
      CFGSymbol cfgSym = (CFGSymbol) symbol;
      if (cfg.equals(cfgSym.getGrammar())) {
        context.put(this, symbol);
        return true;
      }
      else {
        return false;
      }
    }
    else {
      return false;
    }
  }

  public boolean possiblyMatches(ISymbol symbol, IMatchContext context) {
    if (symbol instanceof StringPatternSymbol) {
      StringPatternSymbol sps = (StringPatternSymbol) symbol;
      if (CFLReachability.containsSome(cfg, sps.getCompiledPattern())) {
        return true;
      }
      IAutomaton fst = Grammars.toAutomaton(cfg);
      if (CFLReachability.containsSome(Grammars.toCFG(sps.getCompiledPattern()), fst)) {
        return true;
      }
      return false;
    }
    else if (symbol instanceof StringSymbol) {
      StringSymbol ss = (StringSymbol) symbol;
      return CFLReachability.containsSome(cfg, ss.toCharSymbols());
    }
    else if (symbol instanceof CFGSymbol) {
      CFGSymbol cfg2 = (CFGSymbol) symbol;
      IAutomaton fst2 = Grammars.toAutomaton(cfg2.getGrammar());
      if (CFLReachability.containsSome(cfg, fst2)) {
        return true;
      }
      IAutomaton fst = Grammars.toAutomaton(cfg);
      if (CFLReachability.containsSome(cfg2.getGrammar(), fst)) {
        return true;
      }
      return false;
    }
    else {
      return false;
    }
  }

  public int hashCode() {
    return cfg.hashCode();
  }

  public boolean equals(Object obj) {
    if (!getClass().equals(obj.getClass())) return false;
    CFGSymbol cfgSym = (CFGSymbol) obj;
    return cfg.equals(cfgSym.cfg);
  }

  public void traverse(ISymbolVisitor visitor) {
    visitor.onVisit(this);
    visitor.onLeave(this);
  }

  public ISymbol copy(ISymbolCopier copier) {
    ISymbol s = copier.copy(this);
    if (s instanceof CFGSymbol) {
      CFGSymbol cfgSym = (CFGSymbol) s;
      if (copier instanceof IGrammarCopier) {
        IGrammarCopier gCopier = (IGrammarCopier) copier;
        cfgSym.cfg = (IContextFreeGrammar) gCopier.copy(cfgSym.cfg);
      }
    }
    return s;
  }
  
  public String toString() {
    return "CFGSymbol(" + cfg.toString() + ")";
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
