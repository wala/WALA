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
/**
 * 
 */
package com.ibm.wala.stringAnalysis.translator.repository;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.string.CharSymbol;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.automaton.string.NumberSymbol;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.Variable;
import com.ibm.wala.stringAnalysis.translator.IConstraintSolver;

public class Strpad extends StringTranslator {
  private int target; // TODO: should reuse super.target.
  
  private int lenNumIndex;

  private int strNumIndex;

  private int typeNumIndex;

  public Strpad(int target, int len, int str, int type) {
    super(TARGET_ALL);
    this.target = target;
    this.lenNumIndex = len;
    this.strNumIndex = str;
    this.typeNumIndex = type;
  }

  public Strpad(int len, int str, int type) {
    super(TARGET_ALL);
    this.target = -1;
    this.lenNumIndex = len;
    this.strNumIndex = str;
    this.typeNumIndex = type;
  }

  public SimpleGrammar prepare(IConstraintSolver translator, String funcName,
                               ISymbol recv, List params,
                               IProductionRule rule, SimpleGrammar g,
                               IVariableFactory varFactory) {
    NumberSymbol lenNumSym = (NumberSymbol) params.get(lenNumIndex);
    Variable targetVar = (Variable) params.get(target);
    Variable strVar = new Variable("");
    Variable typeVar = new Variable("");
    if (params.size() > strNumIndex) {
      strVar = (Variable) params.get(strNumIndex);
    }
    if (params.size() > typeNumIndex) {
      typeVar = (Variable) params.get(typeNumIndex);
    }
    int length = lenNumSym.intValue();

    SimpleGrammar g2 = super.prepare(translator, funcName, recv, params,
      rule, g, varFactory);

    List strVarList = new ArrayList();
    List targetVars = new ArrayList();
    List typeVars = new ArrayList();
    Set rules = (Set) g2.getRules();
    Iterator rulesIte = rules.iterator();
    ProductionRule strRule = null;
    while (rulesIte.hasNext()) {
      ProductionRule varRule = (ProductionRule) rulesIte.next();
      IVariable left = varRule.getLeft();
      List right = varRule.getRight();
      if (left.equals(strVar)) {
        strVarList = right;
        strRule = varRule;
      }
      else if (left.equals(targetVar)) {
        targetVars = right;
      }
      else if (left.equals(typeVar)) {
        typeVars = right;
      }
    }
    if (typeVars.isEmpty()) {
      typeVars = new StringSymbol("STR_PAD_RIGHT").toCharSymbols();
    }
    if (strVarList.isEmpty()) {
      strVarList = new StringSymbol(" ").toCharSymbols();
    }
    CharSymbol[] typeChars = (CharSymbol[]) typeVars
        .toArray(new CharSymbol[0]);
    String type = "";
    for (int i = 0; i < typeChars.length; i++) {
      type += typeChars[i].value();
    }
    List newStrVars = new ArrayList();
    for (int i = 0; i < (length - targetVars.size()); i++) {
      int index = i;
      if (i >= strVarList.size()) {
        index = i % strVarList.size();
      }
      newStrVars.add(strVarList.get(index));
    }

    Variable postStrVar = null;
    g2.getRules().remove(strRule);
    if (type.equals("STR_PAD_BOTH")) {
      g2.getRules().add(
        new ProductionRule(strVar, newStrVars.subList(0,
          newStrVars.size() / 2)));
      postStrVar = new Variable("post" + strVar.getName());
      g2.getRules().add(
        new ProductionRule(postStrVar, newStrVars.subList(
          newStrVars.size() / 2, newStrVars.size())));
    }
    else {
      g2.getRules().add(new ProductionRule(strVar, newStrVars));
    }
    List l = new ArrayList();
    if (type.equals("STR_PAD_RIGHT")) {
      l.add(targetVar);
      l.add(strVar);
    }
    else if (type.equals("STR_PAD_LEFT")) {
      l.add(strVar);
      l.add(targetVar);
    }
    else if (type.equals("STR_PAD_BOTH")) {
      l.add(strVar);
      l.add(targetVar);
      if (postStrVar != null) {
        l.add(postStrVar);
      }
    }

    IProductionRule newRule = new ProductionRule(rule.getLeft(), l);
    g2.getRules().remove(rule);
    g2.getRules().add(newRule);
    return g2;
  }

  public boolean acceptCyclic() {
    return true;
  }

  public Set possibleOutputs(Set terminals) {
    return terminals;
  }

  public SimpleGrammar translate(SimpleGrammar cfg) {
    return cfg;
  }

  public SimpleGrammar translateCyclic(SimpleGrammar cfg, Set terminals) {
    return translate(cfg);
  }

}