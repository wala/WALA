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
package com.ibm.wala.stringAnalysis.grammar;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.IGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.IRuleVisitor;
import com.ibm.wala.automaton.grammar.string.ISimplify;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;

public class GInvocationRule extends GRule {
  private Set<GR> grammars;
  private Set<IProductionRule> aliasRules;
  private Map<ISymbol, ISymbol> aliasMap;

  public GInvocationRule(Set<GR> grammars, IR ir, SSAInstruction ssai, IVariable left, ISymbol right[]) {
    super(ir, ssai, left, right);
    this.grammars = new HashSet<GR>(grammars);
    this.aliasRules = new HashSet<IProductionRule>();
    this.aliasMap = new HashMap<ISymbol,ISymbol>();
  }

  public GInvocationRule(Set<GR> grammars, IR ir, SSAInstruction ssai, IVariable left, List right) {
    super(ir, ssai, left, right);
    this.grammars = new HashSet<GR>(grammars);
    this.aliasRules = new HashSet<IProductionRule>();
    this.aliasMap = new HashMap<ISymbol,ISymbol>();
  }

  public GInvocationRule(Set<GR> grammars, GRule instruction) {
    super(instruction);
    this.grammars = new HashSet<GR>(grammars);
    this.aliasRules = new HashSet<IProductionRule>();
    this.aliasMap = new HashMap<ISymbol,ISymbol>();
  }

  public GInvocationRule(GInvocationRule instruction) {
    super(instruction);
    this.grammars = new HashSet<GR>(instruction.getGrammars());
    this.aliasRules = new HashSet<IProductionRule>();
    this.aliasMap = new HashMap<ISymbol,ISymbol>();
  }

  public Set<GR> getGrammars() {
    return grammars;
  }

  public Set<IProductionRule> getAliasRules() {
    return aliasRules;
  }

  public Map<ISymbol,ISymbol> getAliasMap() {
    return aliasMap;
  }

  public void addAliasRule(IR ir, SSAInstruction instruction, int paramIndex, IVariable param, ISymbol value) {
    aliasMap.put(param, value);
    GRule grule1 = new GRule(ir, null, param, new ISymbol[]{value});
    aliasRules.add(grule1);
    if (paramIndex >= 0) {
      if (value instanceof IVariable) {
        GRule grule2 = new GRule(ir, instruction, (IVariable) value, new ISymbol[]{param});
        aliasRules.add(grule2);
      }
    }
  }

  public boolean equals(Object obj) {
    if (!super.equals(obj)) return false;
    GInvocationRule grule = (GInvocationRule) obj;
    return aliasRules.equals(grule.aliasRules) && grammars.equals(grule.grammars);
  }

  public void traverse(IRuleVisitor visitor) {
    for (Iterator i = aliasRules.iterator(); i.hasNext(); ) {
      IProductionRule r = (IProductionRule) i.next();
      r.traverse(visitor);
    }
    for (Iterator i = grammars.iterator(); i.hasNext(); ) {
      IGrammar g = (IGrammar) i.next();
      g.traverseRules(visitor);
    }
  }

  public String toString(Set history) {
    if (history.contains(this)) {
      return "...";
    }
    history.add(this);

    StringBuffer callees = new StringBuffer();
    for (Iterator i = grammars.iterator(); i.hasNext(); ) {
      ISimplify g = (ISimplify) i.next();
      String s = null;
      if (g instanceof RegularlyControlledGrammar) {
        s = ((RegularlyControlledGrammar)g).toString(history);
      }
      else {
        s =  g.toString();
      }
      callees.append(s);
      if (i.hasNext()) {
        callees.append(", ");
      }
    }
    return "{rule:" + super.toString() + ", callees:" + callees + "}";
  }

  public String toString() {
    return toString(new HashSet());
  }
}
