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

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.stringAnalysis.translator.*;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.Trace;

public abstract class BasicTranslator implements ITranslator, Cloneable {
  static protected int TARGET_RECV = -1;
  static protected int TARGET_ALL  = -2;
  
  protected int target;

  protected String funcName;

  protected ISymbol recv;

  protected List params;

  protected IProductionRule rule;

  protected IVariableFactory varFactory;

  protected SimpleGrammar grammar;

  protected IConstraintSolver translator;
  
  public BasicTranslator() {
    this.target = -1;
  }
  
  public BasicTranslator(int target) {
    this.target = target;
  }
  
  public SimpleGrammar solve(final IConstraintSolver solver,
                             final String funcName,
                             final ISymbol recv,
                             final List params,
                             IProductionRule invokeRule,
                             SimpleGrammar targetGrammar,
                             SimpleGrammar otherRules,
                             Stack<CallEnv> callStack,
                             IVariableFactory varFactory) {
    SimpleGrammar pre = prepare(solver, funcName, recv, params, invokeRule, targetGrammar, varFactory);
    Trace.println("-- prepared grammar for " + funcName + ":");
    Trace.println(SAUtil.prettyFormat(pre));
    if (!pre.getStartSymbol().equals(targetGrammar.getStartSymbol())) {
      IProductionRule r = new ProductionRule(targetGrammar.getStartSymbol(), pre.getStartSymbol());
      otherRules.addRule(r);
    }
    
    SimpleGrammar target = solver.solve(pre, varFactory, callStack);
    Trace.println("-- target grammar without function applications:");
    Trace.println(SAUtil.prettyFormat(target));
    assert(target.getStartSymbol().equals(pre.getStartSymbol()));

    SimpleGrammar translatedGrammar = translate(target);
    if (!translatedGrammar.getStartSymbol().equals(target.getStartSymbol())) {
      IProductionRule r = new ProductionRule(target.getStartSymbol(), translatedGrammar.getStartSymbol());
      otherRules.addRule(r);
    }
    // TODO: should fix.
    //translatedGrammar = substituteGrammars(translatedGrammar);

    Trace.println("-- grammar translated by " + funcName + ":");
    Trace.println(SAUtil.prettyFormat(translatedGrammar));

    otherRules.addRules(translatedGrammar.getRules());
    SimpleGrammar g3 = solver.solve(otherRules, varFactory, callStack);
    Grammars.eliminateUselessRules(g3);
    Trace.println("-- solved (" + funcName + "):");
    Trace.println(SAUtil.prettyFormat(g3));
    
    return g3;
  }

  public SimpleGrammar prepare(final IConstraintSolver translator,
                               final String funcName, final ISymbol recv,
                               final List params, IProductionRule rule,
                               SimpleGrammar grammar,
                               IVariableFactory varFactory) {
    this.funcName = funcName;
    this.recv = recv;
    this.params = params;
    this.rule = rule;
    this.varFactory = varFactory;
    this.translator = translator;
    this.grammar = grammar;
    SimpleGrammar g2 = (SimpleGrammar) grammar.copy(SimpleGrammarCopier.defaultCopier);
    if (target >= 0) {
      setStartSymbol(g2, (ISymbol) params.get(target), varFactory);
    }
    else if (target == TARGET_RECV){
      setStartSymbol(g2, recv, varFactory);
    }
    else if (target == TARGET_ALL) {
      setStartSymbol(g2, rule.getLeft(), varFactory);
    }
    else {
      throw(new RuntimeException("unsupported target"));
    }
    return g2;
  }
  
  abstract public SimpleGrammar translate(SimpleGrammar g);


  public ITranslator copy() {
    return (ITranslator) clone();
  }

  public Object clone() {
    try {
      return super.clone();
    }
    catch (CloneNotSupportedException e) {
      throw (new RuntimeException(e));
    }
  }

  protected void setStartSymbol(SimpleGrammar cfg, ISymbol symbol, IVariableFactory varFactory) {
    if (symbol instanceof IVariable) {
      cfg.setStartSymbol((IVariable) symbol);
    }
    else {
      IVariable v = varFactory.createVariable(Grammars.variablePrefix);
      if (symbol instanceof StringSymbol) {
        StringSymbol ss = (StringSymbol) symbol;
        cfg.addRule(new ProductionRule(v, ss.toCharSymbols()));
      }
      else {
        cfg.addRule(new ProductionRule(v, symbol));
      }
      cfg.setStartSymbol(v);
    }
  }

  protected IVariable createVariable() {
    return varFactory.createVariable(Grammars.variablePrefix);
  }

  static public interface IParamConv {
    ISymbol convert(SimpleGrammar g);
    ISymbol convert(IValueSymbol s);
  }
  
  static public class StringParamConv implements IParamConv {
    private Set<String> vals = new HashSet<String>();
    public ISymbol convert(SimpleGrammar sg) {
      IContextFreeGrammar cfg = new ContextFreeGrammar(sg);
      Set strs = Grammars.stringValues(cfg, sg.getStartSymbol());
      if (strs == null) {
        CFGSymbol cfgSym = new CFGSymbol(cfg);
        return cfgSym;
      }
      else {
        vals.addAll(strs);
        if (strs.size() == 1) {
          String str = (String) strs.iterator().next();
          StringSymbol ss = new StringSymbol(str);
          return ss;
        }
        else {
          CFGSymbol cfgSym = new CFGSymbol(cfg);
          return cfgSym;
        }
      }
    }
    
    public ISymbol convert(IValueSymbol s) {
      return s;
    }
    
    public Set<String> getValues() {
      return vals;
    }
  }

  protected ISymbol solveParameter(int n, IParamConv conv) {
    ISymbol s = (ISymbol) params.get(n);
    Trace.println("solveParameter( " + n + " -> " + s);
    if (s instanceof IVariable) {
      SimpleGrammar g = new SimpleGrammar(grammar);
      SimpleGrammar sg = translator.solve(g, (IVariable) s);
      Grammars.eliminateUselessRules(sg);
      return conv.convert(sg);
    }
    else if (s instanceof IValueSymbol) {
      return conv.convert((IValueSymbol)s);
    }
    else {
      throw (new RuntimeException("unexpected parameter symbol: " + s));
    }
  }

  protected ISymbol solveParameter(int n) {
    return solveParameter(n, new StringParamConv());
  }
}