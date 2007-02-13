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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import com.ibm.wala.automaton.grammar.string.DeepRuleCopier;
import com.ibm.wala.automaton.grammar.string.IGrammar;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ISimplify;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammar;
import com.ibm.wala.automaton.grammar.string.SimpleRuleCopier;
import com.ibm.wala.automaton.string.Automatons;
import com.ibm.wala.automaton.string.IAutomaton;
import com.ibm.wala.automaton.string.IMatchContext;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.ITransition;
import com.ibm.wala.automaton.string.IVariable;
import com.ibm.wala.automaton.string.MatchContext;
import com.ibm.wala.automaton.string.StringSymbol;
import com.ibm.wala.automaton.string.VariableReplacer;
import com.ibm.wala.stringAnalysis.grammar.GInvocationRule;
import com.ibm.wala.stringAnalysis.grammar.GR;
import com.ibm.wala.stringAnalysis.grammar.IRegularlyControlledGrammar;
import com.ibm.wala.stringAnalysis.grammar.InvocationSymbol;
import com.ibm.wala.stringAnalysis.grammar.UpdatedObject;
import com.ibm.wala.stringAnalysis.util.SAUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

public class SideEffectSolver implements ISimplify {
  private IRegularlyControlledGrammar gr;
  private Set<String> setOperations;
  private Set<String> getOperations;

  static private class LastUpdatedObject extends UpdatedObject {
    public LastUpdatedObject(IVariable v) {
      super(v, null, -1);
    }
  }

  static private class Env extends HashMap<IVariable, UpdatedObject> {
    public Env(Env env) {
      super(env);
    }

    public Env() {
      super();
    }
  }

  static private class SolverContext {
    Map<Integer, Map<IState, Set<Env>>> history;
    Map<IGrammar, Set<IProductionRule>> arules;
    Map<IGrammar, Set<IProductionRule>> drules;
    Map<IVariable, Set<UpdatedObject>> lastVars;

    public SolverContext() {
      history = new HashMap<Integer,Map<IState,Set<Env>>>();
      arules = new HashMap<IGrammar,Set<IProductionRule>>();
      drules = new HashMap<IGrammar,Set<IProductionRule>>();
      lastVars = new HashMap<IVariable, Set<UpdatedObject>>();
    }

    private Integer getGRKey(IRegularlyControlledGrammar gr) {
      return new Integer(System.identityHashCode(gr));
    }

    public void put(IRegularlyControlledGrammar gr) {
      history.put(getGRKey(gr), new HashMap<IState,Set<Env>>());
    }

    public void put(IRegularlyControlledGrammar gr, IState state, Env env) {
      Integer grk = getGRKey(gr);
      Map<IState,Set<Env>> m = history.get(grk);
      if (m == null) {
        m = new HashMap<IState,Set<Env>>();
        history.put(grk, m);
      }
      Set<Env> envs = m.get(state);
      if (envs == null) {
        envs = new HashSet<Env>();
        m.put(state, envs);
      }
      envs.add(env);
    }

    public boolean contains(IRegularlyControlledGrammar gr) {
      return history.containsKey(gr);
    }

    public boolean contains(IRegularlyControlledGrammar gr, IState state, Env env) {
      Map<IState,Set<Env>> m = history.get(getGRKey(gr));
      if (m == null) return false;
      Set<Env> envs = m.get(state);
      if (envs == null) return false;
      return envs.contains(env);
    }

    public void addRule(IGrammar g, IProductionRule rule) {
      Set<IProductionRule> s = arules.get(g);
      if (s == null) {
        s = new HashSet<IProductionRule>();
        arules.put(g, s);
      }
      Trace.println("add: " + rule);
      s.add(rule);
    }

    public void removeRule(IGrammar g, IProductionRule rule) {
      Set<IProductionRule> s = drules.get(g);
      if (s == null) {
        s = new HashSet<IProductionRule>();
        drules.put(g, s);
      }
      Trace.println("remove: " + rule);
      s.add(rule);
    }

    public void addLastVar(IVariable v, UpdatedObject uo) {
      Set<UpdatedObject> s = lastVars.get(v);
      if (s == null) {
        s = new HashSet<UpdatedObject>();
        lastVars.put(v, s);
      }
      s.add(uo);
    }

    public void addLastVarMap(Env m) {
      for (IVariable v : m.keySet()) {
        UpdatedObject uo = m.get(v);
        addLastVar(v, uo);
      }
    }

    public void translateGrammars(SimpleGrammar cfg) {
      for (Iterator i = drules.keySet().iterator(); i.hasNext(); ) {
        IGrammar g = (IGrammar) i.next();
        Set rs = (Set) drules.get(g);
        if (rs != null) {
          cfg.getRules().removeAll(rs);
        }
      }
      for (Iterator i = arules.keySet().iterator(); i.hasNext(); ) {
        IGrammar g = (IGrammar) i.next();
        Set rs = (Set) arules.get(g);
        if (rs != null) {
          cfg.getRules().addAll(rs);
        }
      }
      Set<IProductionRule> newRules = new HashSet<IProductionRule>();
      for (Iterator<IProductionRule> i = cfg.getRules().iterator(); i.hasNext(); ) {
        IProductionRule rule = i.next();
        if (rule.getRight().size() != 1) continue;
        if (!(rule.getRight(0) instanceof InvocationSymbol)) continue;
        InvocationSymbol invoke = (InvocationSymbol) rule.getRight(0);
        if (invoke.getParameters().size() < 2) continue;
        ISymbol s = invoke.getParameter(1);
        if (s instanceof LastUpdatedObject) {
          LastUpdatedObject luo = (LastUpdatedObject) s;
          Set<UpdatedObject> vs = lastVars.get(luo.getVariable());
          i.remove();
          if (vs == null) {
            // TODO: if a target node is not updated, there is no corresponding rule.
          }
          else {
            for (IVariable v : vs) {
              IMatchContext m = new MatchContext();
              m.put(luo, v);
              InvocationSymbol invoke2 = (InvocationSymbol) VariableReplacer.replace(invoke, m);
              IProductionRule r = new ProductionRule(rule.getLeft(), invoke2);
              newRules.add(r);
            }
          }
        }
      }
      cfg.getRules().addAll(newRules);
    }
  }

  public SideEffectSolver(IRegularlyControlledGrammar gr, Set<String> setOperations, Set<String> getOperations) {
    this.gr = gr;
    this.setOperations = new HashSet<String>(setOperations);
    this.getOperations = new HashSet<String>(getOperations);
  }

  public SideEffectSolver(IRegularlyControlledGrammar gr, String setOperations[], String getOperations[]) {
    this(gr, SAUtil.set(setOperations), SAUtil.set(getOperations));
  }

  public SimpleGrammar toSimple() {
    SolverContext ctx = new SolverContext();
    translateGR((IRegularlyControlledGrammar)gr, ctx);
    SimpleGrammar<IProductionRule> cfg = gr.toSimple();
    ctx.translateGrammars(cfg);
    Trace.println("-- after solving side effects");
    for (IProductionRule r : cfg.getRules()) {
      Trace.println(r.getLeft() + " -> " + r.getRight() + " : " + r);
    }
    return cfg;
  }

  protected Set<Env> translateGR(IRegularlyControlledGrammar gr, SolverContext ctx) {
    if (ctx.contains(gr)) {
      return new HashSet<Env>();
    }
    Trace.println("SideEffectSolver#translateGR --");
    Trace.println("flow: ");
    Trace.println(Automatons.toGraphviz(gr.getAutomaton()));
    Trace.println("ruleMap: " + gr.getRuleMap());
    Set<Env> envs = translateGR(gr, gr.getAutomaton().getInitialState(), new Env(), ctx);
    for (Env m : envs) {
      ctx.addLastVarMap(m);
    }
    Trace.println("exit:");
    return envs;
  }

  private Set<Env> translateGR(IRegularlyControlledGrammar gr, IState state, Env env, SolverContext ctx) {
    IAutomaton flow = gr.getAutomaton();
    
    Stack<IState> stackState = new Stack<IState>();
    Stack<Env> stackEnv = new Stack<Env>();
    stackState.push(state);
    stackEnv.push(env);

    Set<Env> envs = new HashSet<Env>();

    while (!stackState.isEmpty()) {
      IState s = stackState.pop();
      Env e = new Env(stackEnv.pop());

      if (ctx.contains(gr, s, e)) {
        Trace.println("skip:");
        continue;
      }
      ctx.put(gr, s, e);

      Set<ITransition> nextTransitions = flow.getTransitions(s);
      if (nextTransitions.isEmpty()) {
        envs.add(e);
      }
      else {
        for (ITransition t : nextTransitions) {
          Trace.println("transition: " + t.getInputSymbol());
          IProductionRule rule = gr.getRule(t.getInputSymbol());
          Set<Env> es = new HashSet<Env>();
          if (rule == null) {
            es.add(e);
          }
          else {
            Set<Env> ess = translateRule(gr, rule, e, ctx);
            es.addAll(ess);
          }
          for (Env nextEnv : es) {
            stackState.push(t.getPostState());
            stackEnv.push(nextEnv);
          }
        }
      }
    }
    return envs;
  }

  private Set translateRule(IGrammar g, Env env, SolverContext ctx) {
    Set envs = new HashSet();
    for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      Set es = translateRule(g, rule, env, ctx);
      envs.addAll(es);
    }
    return envs;
  }

  private Set<Env> translateRule(IGrammar g, IProductionRule rule, Env env, SolverContext ctx) {
    if (rule instanceof GInvocationRule) {
      return translateInvokeRule(g, rule, env, ctx);            
    }
    else {
      Set<Env> envs = new HashSet<Env>();
      List right = rule.getRight();
      if (right.size()>0 && right.get(0) instanceof InvocationSymbol) {
        InvocationSymbol invoke = (InvocationSymbol) right.get(0);
        if (invoke.getReceiver() instanceof IVariable) {
          solveSideEffect(g, rule, invoke, env, ctx);
        }
        else {
          replaceRhs(g, rule, env, ctx);
        }
      }
      else {
        replaceRhs(g, rule, env, ctx);
      }
      envs.add(env);
      return envs;
    }
  }
  
  private Set<Env> translateSpecialRule(IGrammar g, IProductionRule rule, Env env, SolverContext ctx) {
    Set<Env> envs = new HashSet<Env>();
    return envs;
  }

  private Set<Env> translateInvokeRule(IGrammar g, IProductionRule rule, Env env, SolverContext ctx) {
    Set<Env> envs = new HashSet<Env>();
    
    replaceRhs(g, rule, env, ctx);
    GInvocationRule crule = (GInvocationRule) rule;

    Set<IProductionRule> aliasRules = new HashSet<IProductionRule>(crule.getAliasRules());
    Set<IProductionRule> newAliasRules = new HashSet<IProductionRule>();
    for (Iterator i = crule.getAliasRules().iterator(); i.hasNext(); ) {
      IProductionRule alias = (IProductionRule) i.next();
      Trace.println("alias: " + alias);
      alias = alias.copy(new DeepRuleCopier(new VariableReplacer(env)));
      newAliasRules.add(alias);
      Trace.println("-> " + alias);
    }
    crule.getAliasRules().clear();
    crule.getAliasRules().addAll(newAliasRules);

    for (Iterator j = crule.getGrammars().iterator(); j.hasNext(); ) {
      IGrammar gg = (IGrammar) j.next();
      if (gg instanceof IRegularlyControlledGrammar) {
        IRegularlyControlledGrammar gr = (GR) gg;
        Set es = translateGR(gr, ctx);
        for (Iterator k = aliasRules.iterator(); k.hasNext(); ) {
          IProductionRule alias = (IProductionRule) k.next();
          Assertions._assert(alias.getRight().size()==1);
          IVariable left = (IVariable) alias.getLeft();
          ISymbol right = (ISymbol) alias.getRight(0);
          if (!(right instanceof IVariable)) {
            continue;
          }
          for (Iterator l = es.iterator(); l.hasNext(); ) {
            Map e = (Map) l.next();
            IVariable lv = (IVariable) e.get(left);
            if (lv != null) {
              UpdatedObject nv = new UpdatedObject((IVariable)right, crule.getIR(), crule.getSSAInstruction());
              IProductionRule r = new ProductionRule(nv, lv);
              crule.getAliasRules().add(r);
              Trace.println("add(alias): " + r);
              env.put((IVariable)right, nv);
            }
          }
        }
        envs.add(env);
      }
      else {
        Set es = translateRule(gg, env, ctx);
        envs.addAll(es);
      }
    }
    
    return envs;
  }

  private void solveSideEffect(IGrammar g, IProductionRule rule, InvocationSymbol invoke, Map env, SolverContext ctx) {
    IVariable recv = (IVariable) invoke.getReceiver();
    if (recv instanceof UpdatedObject) {
      return ;
    }

    String funcName = invoke.getFunction().getName();
    if (getOperations.contains(funcName)) {
      Trace.println(rule);
      // b = recv.getXxxx(...) ->  b = getXxxx(recv, ...)
      replaceRhs(g, rule, env, ctx);
      // b = recv.getXxxx(...) ->  recv' = getXxxx^r(recv,last(b),...)
      IProductionRule rr2 = replaceRhs(rule, env);
      IVariable b = rr2.getLeft();
      UpdatedObject nextRecv = new UpdatedObject(recv, invoke.getIR(), invoke.getInstruction());
      InvocationSymbol invoke2 = (InvocationSymbol) rr2.getRight(0);
      List params = invoke2.getParameters();
      params.add(1, new LastUpdatedObject(b));
      ISymbol func = new StringSymbol(invoke2.getFunction().getName() + "^r");
      invoke2 = new InvocationSymbol(
        invoke2.getIR(), invoke2.getInstruction(),
        func, invoke2.getReceiver(),
        params);
      rr2.setLeft(nextRecv);
      rr2.getRight().clear();
      rr2.getRight().add(invoke2);
      env.put(recv, nextRecv);
      ctx.addRule(g, rr2);
      Trace.println("-> " + rr2);
    }
    else if (setOperations.contains(funcName)) {
      // recv.setXxxx(...) ->  recv' = setXxxx(recv,...)
      Trace.println(rule);
      IProductionRule rr = replaceRhs(rule, env);
      UpdatedObject nextRecv = new UpdatedObject(recv, invoke.getIR(), invoke.getInstruction());
      rr.setLeft(nextRecv);
      env.put(recv, nextRecv);
      ctx.addRule(g, rr);
      ctx.removeRule(g, rule);
      Trace.println("-> " + rr);
    }
    else {
      replaceRhs(g, rule, env, ctx);
    }
  }

  private void replaceRhs(IGrammar g, IProductionRule rule, Map env, SolverContext ctx) {
    IProductionRule rr = replaceRhs(rule, env);
    if (!rr.equals(rule)) {
      ctx.removeRule(g, rule);
      ctx.addRule(g, rr);
    }
  }

  private IProductionRule replaceRhs(IProductionRule rule, Map env) {
    List newRights = new ArrayList();
    IMatchContext ctx = new MatchContext(env);

    rule = rule.copy(SimpleRuleCopier.defaultCopier);
    for (Iterator i = rule.getRight().iterator(); i.hasNext(); ) {
      ISymbol right = (ISymbol) i.next();
      right = VariableReplacer.replace(right, ctx);
      newRights.add(right);
    }
    rule.getRight().clear();
    rule.getRight().addAll(newRights);

    return rule;
  }
}
