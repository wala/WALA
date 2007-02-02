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
package com.ibm.wala.stringAnalysis.translator;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.stringAnalysis.grammar.*;
import com.ibm.wala.stringAnalysis.translator.repository.ITranslator;
import com.ibm.wala.stringAnalysis.translator.repository.ITranslatorRepository;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.*;

abstract public class SimpleConstraintSolver implements IConstraintSolver {
  ITranslatorRepository translators;
  
  static protected int MAX_CYCLE = 3;

  static public class TranslationError extends UnimplementedError {
    public TranslationError(String msg) {
      super(msg);
    }
  }
  
  static protected interface ISubstitution extends ISymbolCopier {
    Set<IProductionRule> getRules();
  }
  
  static protected class Substitution extends DeepSymbolCopier {
    protected Set<IProductionRule> rules;
    
    public Substitution() {
      rules  = new HashSet<IProductionRule>();
    }

    public Set<IProductionRule> getRules() {
      return rules;
    }
  }
  
  static protected class SimpleSubstitution extends Substitution implements ISubstitution {
    public SimpleSubstitution() {
      super();
    }
    
    public ISymbol copy(ISymbol s) {
      if (s instanceof IGrammarSymbol) {
        IGrammarSymbol gsym = (IGrammarSymbol) s;
        SimpleGrammar sg = (SimpleGrammar) gsym.getGrammar();
        rules.addAll(sg.toSimple().getRules());
        return gsym.getGrammar().getStartSymbol();
      }
      else {
        return super.copy(s);
      }
    }
    
    public Collection copySymbols(Collection c) {
      ArrayList<ISymbol> l = new ArrayList<ISymbol>();
      Collection<ISymbol> ss = super.copySymbols(c);
      for (ISymbol s : ss) {
        if (s instanceof StringSymbol) {
          l.addAll(((StringSymbol)s).toCharSymbols());
        }
        else {
          l.add(s);
        }
      }
      return l;
    }
  }
  
  protected SimpleGrammar substituteGrammars(SimpleGrammar g) {
    return substituteGrammars(g, new SimpleSubstitution());
  }
  
  protected SimpleGrammar substituteGrammars(SimpleGrammar g, Substitution subst) {
    SimpleGrammar g2 = (SimpleGrammar) g.copy(new DeepGrammarCopier(new DeepRuleCopier(subst)));
    g2.addRules(subst.getRules());
    return g2;
  }

  
  public SimpleConstraintSolver(ITranslatorRepository translators) {
    this.translators = translators;
  }


  private List splitStringSymbols(List symbols, final List newRules, final IVariableFactory varFactory) {
    List newSymbols = new ArrayList();
    for (Iterator i = symbols.iterator(); i.hasNext(); ) {
      ISymbol s = (ISymbol) i.next();
      if (s instanceof StringSymbol) {
        List chars = ((StringSymbol)s).toCharSymbols();
        IVariable v = varFactory.createVariable(Grammars.variablePrefix);
        IProductionRule r = new ProductionRule(v, chars);
        newRules.add(r);
        s = v;
      }
      newSymbols.add(s);
    }
    return newSymbols;
  }

  private SimpleGrammar splitStringSymbols(SimpleGrammar cfg, final IVariableFactory varFactory) {
    final List newRules = new ArrayList();
    cfg.traverseRules(new IRuleVisitor(){
      public void onVisit(IProductionRule rule) {
        IVariable var = rule.getLeft();
        List right = rule.getRight();
        if (!right.isEmpty() && (right.get(0) instanceof InvocationSymbol)) {
          InvocationSymbol invoke = (InvocationSymbol) right.get(0);
          List params = invoke.getParameters();
          List newParams = splitStringSymbols(params, newRules, varFactory);
          params.clear();
          params.addAll(newParams);
        }
        else {
          List newSymbols = splitStringSymbols(right, newRules, varFactory);
          rule.getRight().clear();
          rule.getRight().addAll(newSymbols);
        }
      }
    });
    cfg = (SimpleGrammar) cfg.copy(SimpleGrammarCopier.defaultCopier);
    cfg.getRules().addAll(newRules);
    return cfg;
  }

  public SimpleGrammar solve(ISimplify grammar, IVariable startSymbol) {
    SimpleGrammar cfg = grammar.toSimple();
    Grammars.refreshProductionRules(cfg);

    // TODO: refactoring
    for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      if (rule.getRight().size() == 1) {
        if (rule.getLeft().equals(rule.getRight(0))) {
          i.remove();
        }
      }
    }
    
    Trace.println("-- GR2CFG#translate: cfg1 =");
    Trace.println(Grammars.toRuleChain(cfg, startSymbol));
    if (startSymbol != null) {
      cfg.setStartSymbol(startSymbol);
    }
    else {
      startSymbol = cfg.getStartSymbol();
    }
    Grammars.eliminateDanglingVariables(cfg);
    Grammars.eliminateUselessRules(cfg);

    Trace.println("-- GR2CFG#translate: cfg2 =");
    Trace.println(SAUtil.prettyFormat(cfg));

    IVariableFactory<IVariable> varFactory = new FreshVariableFactory(SimpleVariableFactory.defaultFactory, cfg);
    cfg = splitStringSymbols(cfg, varFactory);
    return solve(cfg, varFactory, new Stack());
  }


  static protected class CyclicException extends Exception {
    final public List<CallEnv> cyclicCalls;

    public CyclicException(List<CallEnv> cyclicCalls) {
      super();
      this.cyclicCalls = cyclicCalls;
    }
  }
  
  protected Set<IProductionRule> cyclicRules(SimpleGrammar g, IVariable v) {
    Set<IProductionRule> rules = cyclicRules(g, v, v, new HashSet<IVariable>());
    return rules;
  }
  
  protected Set<IProductionRule> cyclicRules(SimpleGrammar g, IVariable v, IVariable cur, Set<IVariable> h) {
    Set<IProductionRule> rules = new HashSet<IProductionRule>();
    if (h.contains(cur)) {
      return rules;
    }
    h.add(cur);
    for (Iterator<IProductionRule> i = g.getRules(cur).iterator(); i.hasNext(); ) {
      IProductionRule r = i.next();
      for (Iterator<ISymbol> j = r.getRight().iterator(); j.hasNext(); ) {
        ISymbol s = j.next();
        final Set<IVariable> nx = new HashSet<IVariable>();
        s.traverse(new ISymbolVisitor(){
          public void onLeave(ISymbol symbol) {
            if (symbol instanceof IVariable) {
              nx.add((IVariable)symbol);
            }
          }
          public void onVisit(ISymbol symbol) {
          }
        });
        for (Iterator<IVariable> k = nx.iterator(); k.hasNext(); ) {
          IVariable nv = k.next();
          if (nv.equals(v)) {
            rules.add(r);
          }
          else {
            rules.addAll(cyclicRules(g, v, nv, h));
          }
        }
      }
    }
    return rules;
  }
  
  protected SimpleGrammar[] splitGrammar(SimpleGrammar cfg, IVariable start) {
    Set<IProductionRule> rules = new HashSet<IProductionRule>();
    Grammars.collectReachableRules(cfg, start, rules);
    Set<IProductionRule> others = new HashSet<IProductionRule>(cfg.getRules());
    for (Iterator<IProductionRule> i = others.iterator(); i.hasNext(); ) {
      IProductionRule prod = i.next();
      if (prod.getLeft().equals(start)) {
        i.remove();
      }
    }
    SimpleGrammar g1 = new SimpleGrammar(start, rules);
    SimpleGrammar g2 = new SimpleGrammar(cfg.getStartSymbol(), others);
    Grammars.eliminateUselessRules(g2);
    return new SimpleGrammar[]{g1, g2};
  }
  
  protected SimpleGrammar[] splitCyclicRules(SimpleGrammar g) {
    Set<IProductionRule> cyclicRules = cyclicRules(g, g.getStartSymbol());
    SimpleGrammar g1 = new SimpleGrammar(g.getStartSymbol(), g.getRules());
    g1.getRules().removeAll(cyclicRules);
    SimpleGrammar g2 = new SimpleGrammar(g.getStartSymbol(), cyclicRules);
    return new SimpleGrammar[]{g1, g2};
  }
    
  public SimpleGrammar solve(SimpleGrammar g, IVariableFactory<IVariable> varFactory, Stack<CallEnv> callStack) {
    Trace.println("-- solving: g =");
    Trace.println(SAUtil.prettyFormat(g));

    g = (SimpleGrammar) g.copy(SimpleGrammarCopier.defaultCopier);
    Grammars.eliminateUselessRules(g);
    Trace.println("-- eliminate useless rules: g =");
    Trace.println(SAUtil.prettyFormat(g));

    IProductionRule invokeRule = null;
    InvocationSymbol invoke = null;
    for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      if (rule.getRight().size() > 0
          && (rule.getRight(0) instanceof InvocationSymbol)) {
        invoke = (InvocationSymbol) rule.getRight(0);
        invokeRule = rule;
        break;
      }
    }

    if (invoke == null) {
      return g;
    }
    
    ISymbol func = invoke.getFunction();
    if (func instanceof IVariable) {
      Trace.println("the function name should be resolved by CG2Simple: " + func);
      throw(new TranslationError("the function name should be resolved by CG2Simple: " + func));
    }
    String funcName = func.getName();
    List params = invoke.getParameters();
    ISymbol recv = invoke.getReceiver();
    ITranslator translator = translators.getTranslator(funcName);
    if (translator == null) {
      Trace.println("can't find a translator for " + funcName);
      throw(new RuntimeException("can't find a translator for " + funcName));
    }
    
    SimpleGrammar g1[] = splitGrammar(g, invokeRule.getLeft());
    SimpleGrammar targetGrammar = g1[0]; // target grammar
    SimpleGrammar otherRules = g1[1]; // other rules

    Trace.println("-- applying the translator " + funcName + " to the grammar:");
    Trace.println(SAUtil.prettyFormat(targetGrammar));
    Trace.println("-- other rules:");
    Trace.println(SAUtil.prettyFormat(otherRules));
    
    SimpleGrammar g2[] = splitCyclicRules(targetGrammar);
    SimpleGrammar nonCyclicTarget = g2[0]; // grammar without cyclic rules
    SimpleGrammar cyclicRules = g2[1]; // cyclic rules

    if (translator.acceptCyclic() || cyclicRules.getRules().isEmpty()) {
      return substituteGrammars(
        translator.solve(
          this, funcName, recv, params,
          invokeRule, targetGrammar, otherRules,
          callStack, varFactory));
    }
    else {
      Trace.println("-- extracted non cyclic grammar:");
      Trace.println(SAUtil.prettyFormat(nonCyclicTarget));
      Trace.println("-- cyclic rules:");
      Trace.println(SAUtil.prettyFormat(cyclicRules));

      SimpleGrammar translated = solve(nonCyclicTarget, varFactory, callStack);
      Trace.println("-- translated:");
      Trace.println(SAUtil.prettyFormat(translated));

      for (int i = 0; i < MAX_CYCLE; i++) {
        SimpleGrammar base = translator.toComparable(translated);
        Trace.println("-- base:");
        Trace.println(SAUtil.prettyFormat(base));
      
        IVariable v = varFactory.createVariable(Grammars.variablePrefix);
        IMatchContext ctx = new MatchContext();
        ctx.put(targetGrammar.getStartSymbol(), v);
        SimpleGrammar cyclicGrammar = (SimpleGrammar) cyclicRules.copy(new DeepGrammarCopier(new DeepRuleCopier(new VariableReplacer(ctx))));
        SimpleGrammar base2 = (SimpleGrammar) base.copy(new DeepGrammarCopier(new DeepRuleCopier(new VariableReplacer(ctx))));
        cyclicGrammar.addRules(nonCyclicTarget.getRules());
        cyclicGrammar.addRules(base2.getRules());
        cyclicGrammar.setStartSymbol(targetGrammar.getStartSymbol());
        Trace.println("-- cyclic grammar:");
        Trace.println(SAUtil.prettyFormat(cyclicGrammar));
        
        translated = solve(cyclicGrammar, varFactory, callStack);
        if (translator.isFixpoint(base, translated)) {
          Trace.println("-- found a fixpoint:");
          Trace.println(SAUtil.prettyFormat(base));

          otherRules.addRules(base.getRules());
          SimpleGrammar g3 = solve(otherRules, varFactory, callStack);
          Trace.println("-- solved (" + funcName + "):");
          Trace.println(SAUtil.prettyFormat(g3));
          return g3;
        }
      }
      throw(new UnimplementedError());
    }
  }
}
