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

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.string.*;

public class Grammars {
  static public String variablePrefix = "v";

  static public Set collectTerminals(IGrammar grammar) {
    final Set terminals = new HashSet();
    grammar.traverseRules(new IRuleVisitor(){
      public void onVisit(IProductionRule rule) {
        rule.traverseSymbols(new ISymbolVisitor(){
          public void onVisit(ISymbol symbol) {
            if (!(symbol instanceof IVariable)) {
              terminals.add(symbol);
            }
          }
          public void onLeave(ISymbol symbol) {
          }
        });
      }
    });
    return terminals;
  }

  static public void collectReachableRules(IGrammar grammar, IVariable v, Set rules) {
    final Set ss = new HashSet();
    ISymbolVisitor collector = new ISymbolVisitor(){
      public void onVisit(ISymbol symbol) {
        if (symbol instanceof IVariable) {
          ss.add(symbol);
        }
      }
      public void onLeave(ISymbol symbol) {
      }
    };

    Set rs = grammar.getRules(v);
    for (Iterator i = rs.iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      if (rules.contains(rule)) {
        continue;
      }
      rules.add(rule);
      for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
        ISymbol sym = (ISymbol) j.next();
        sym.traverse(collector);
      }
    }
    for (Iterator i = ss.iterator(); i.hasNext(); ) {
      IVariable vv = (IVariable) i.next();
      collectReachableRules(grammar, vv, rules);
    }
  }
  
  static public Set collectVariables(IGrammar grammar) {
    final Set<IVariable> vars = new HashSet<IVariable>();
    grammar.traverseSymbols(new ISymbolVisitor(){
      public void onLeave(ISymbol symbol) {
        if (symbol instanceof IVariable) {
          vars.add((IVariable)symbol);
        }
      }
      public void onVisit(ISymbol symbol) {
      }
    });
    return vars;
  }

  static public Set collectUsedVariables(IGrammar grammar) {
    return collectUsedVariables(grammar, grammar.getStartSymbol());
  }

  static public Set collectUsedVariables(IGrammar grammar, IVariable v) {
    Set usedVars = new HashSet();
    collectUsedVariables(grammar, v, usedVars);
    return usedVars;
  }

  static public void collectUsedVariables(final IGrammar grammar, IVariable v, final Set s) {
    Set rules = new HashSet();
    collectReachableRules(grammar, v, rules);
    for (Iterator i = rules.iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      rule.traverseSymbols(new ISymbolVisitor(){
        public void onVisit(ISymbol symbol) {
          if (symbol instanceof IVariable) {
            s.add(symbol);
          }
        }
        public void onLeave(ISymbol symbol) {
        }
      });
    }
  }

//  static private void collectUsedVariables(final IGrammar grammar, IVariable v, final Set s, final Set rules) {
//  }

  /*
  static public IVariable createUniqueVariable(Set baseNames) {
    String name = AUtil.createUniqueName(prefixVariable, baseNames);
    return new Variable(name);
  }

  static public IVariable createUniqueVariable(Set baseNames, String prefix) {
    String name = AUtil.createUniqueName(prefix, baseNames);
    return new Variable(name);
  }
  */

  static public Set collectVariableNames(Set variables) {
    return new HashSet(AUtil.collect(variables, new AUtil.IElementMapper(){
      public Object map(Object obj) {
        return ((IVariable) obj).getName();
      }
    }));
  }

  static public Set collectLeftVariables(IGrammar g) {
    final Set vars = new HashSet();
    g.traverseRules(new IRuleVisitor(){
      public void onVisit(IProductionRule rule) {
        vars.add(rule.getLeft());
      }
    });
    return vars;
  }
  
  static public IContextFreeGrammar useUniqueVariables(IContextFreeGrammar cfg1, IGrammar g2, Map m) {
    IVariableFactory varFactory = new FreshVariableFactory(SimpleVariableFactory.defaultFactory, g2);
    return (IContextFreeGrammar) useUniqueVariables(cfg1, varFactory, m);
  }

  static public IGrammar useUniqueVariables(IGrammar g, IVariableFactory varFactory, final Map m) {
    Set variables = g.getNonterminals();
    for (Iterator i = variables.iterator(); i.hasNext(); ) {
      IVariable v = (IVariable) i.next();
      IVariable newVar = varFactory.createVariable(variablePrefix);
      m.put(v, newVar);
    }
    final IMatchContext mctx = new MatchContext(m);

    g = g.copy(new DeepGrammarCopier(new DeepRuleCopier(new VariableReplacer(mctx))));
    return g;
  }

  static public Set getRules(IGrammar grammar, IVariable v) {
    HashSet s = new HashSet();
    for (Iterator i = grammar.getRules().iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      if (rule.getLeft().equals(v)) {
        s.add(rule);
      }
    }
    return s;
  }

  /**
   * translate the context-free grammar into the chomsky normal form.
   * @return normalized context-free grammar
   */
   static public void normalize(IContextFreeGrammar cfg, IVariableFactory varFactory){
    Set vars = Grammars.collectUsedVariables(cfg);
    refreshProductionRules(cfg);
    eliminateEpsilonRules(cfg);
    eliminateUnitRules(cfg);
    simplifyRules(cfg, varFactory);
    moveTerminalsToUnitRules(cfg, varFactory);
    eliminateDanglingVariables(cfg, vars);
    eliminateUselessRules(cfg);
  }

  /**
   * convert production rules into instances of ProductionRule.
   */
   static public void refreshProductionRules(final IGrammar g, final ISymbolCopier symbolCopier) {
     Set newRules = new HashSet();
     for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
       IProductionRule rule = (IProductionRule) i.next();
       ProductionRule newRule = new ProductionRule(
         (IVariable) symbolCopier.copy(rule.getLeft()),
         (List) symbolCopier.copySymbols(rule.getRight()));
       newRules.add(newRule);
     }
     g.getRules().clear();
     g.getRules().addAll(newRules);
   }

   static public void refreshProductionRules(IGrammar g) {
     refreshProductionRules(g, SimpleSymbolCopier.defaultCopier);
   }

   /**
    * eliminate epsilon rules.
    *
    */
   static public void eliminateEpsilonRules(IContextFreeGrammar cfg){
     boolean isAcceptEpsilon = false;
     if (acceptEpsilon(cfg, cfg.getStartSymbol())) {
       isAcceptEpsilon = true;
     }

     Set allNullVars = new HashSet();
     Set nullVars = new HashSet();
     Set newRules = new HashSet();
     do{
       allNullVars.addAll(nullVars);
       nullVars.clear();
       newRules.clear();
       for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
         IProductionRule rule = (IProductionRule) i.next();
         if (rule.isEpsilonRule()) {
           i.remove();
           nullVars.add(rule.getLeft());
         }
       }
       for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
         IProductionRule rule = (IProductionRule) i.next();
         List right = new ArrayList();
         for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
           ISymbol v = (ISymbol) j.next();
           if (!nullVars.contains(v)) {
             right.add(v);
           }
         }
         if (right.size()==0 && allNullVars.contains(rule.getLeft())) {
           // do nothing
         }
         else {
           IProductionRule newRule = new ProductionRule(rule.getLeft(), right);
           newRules.add(newRule);
         }
       }
       cfg.addRules(newRules);
     }while(!nullVars.isEmpty());

     if (isAcceptEpsilon) {
       IProductionRule newRule = new ProductionRule(cfg.getStartSymbol(), new ISymbol[]{});
       cfg.addRule(newRule);
     }
   }

   static public Set eliminateUselessRules(IGrammar g) {
     Set usedVars = collectUsedVariables(g, g.getStartSymbol());
     Set rs = new HashSet();
     for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
       IProductionRule rule = (IProductionRule) i.next();
       if (!usedVars.contains(rule.getLeft())) {
         rs.add(rule);
       }
     }
     // TODO: use rules.removeAll(rs).
     /*
        Set s = new HashSet(g.getRules());
        g.getRules().clear();
        for (Iterator i = s.iterator(); i.hasNext(); ) {
            IProductionRule r = (IProductionRule) i.next();
            if (!rs.contains(r)) {
                g.getRules().add(r);
            }
        }
      */
     g.getRules().removeAll(rs);
     return rs;
   }

   /**
    * eliminate dangling variables.
    * @param g        regular tree grammar
    */
   static public Set eliminateDanglingVariables(IGrammar g, Set<IVariable> usedVars) {
     final Set drules = new HashSet();
     final Set rs = new HashSet();
     do {
       drules.clear();
       final Set<IVariable> vars = collectLeftVariables(g);
       vars.addAll(usedVars);
       g.traverseRules(new IRuleVisitor(){
         public void onVisit(final IProductionRule rule) {
           for (final Iterator i = rule.getRight().iterator(); i.hasNext(); ) {
             final ISymbol right = (ISymbol) i.next();
             right.traverse(new ISymbolVisitor(){
               public void onVisit(ISymbol symbol) {
                 if (symbol instanceof IVariable && !vars.contains(symbol)) {
                   drules.add(rule);
                   rs.add(right);
                 }
               }
               public void onLeave(ISymbol symbol) {
               }
             });
           }
         }
       });
       for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
         IProductionRule r = (IProductionRule) i.next();
         if (drules.contains(r)) {
           i.remove();
         }
       }
     } while(!drules.isEmpty());
     return rs;
   }

   static public Set eliminateDanglingVariables(IGrammar g) {
     return eliminateDanglingVariables(g, new HashSet<IVariable>());
   }
     
   static private boolean acceptEpsilon(IContextFreeGrammar cfg, IVariable v) {
     return acceptEpsilon(cfg, v, new HashSet());
   }

   static private boolean acceptEpsilon(IContextFreeGrammar cfg, IVariable v, Set history) {
     if (history.contains(v)) return false;
     history.add(v);

     for (Iterator i = cfg.getRules(v).iterator(); i.hasNext(); ){
       IProductionRule rule = (IProductionRule) i.next();
       if (rule.isEpsilonRule()) {
         return true;
       }
       else {
         boolean allEpsilon = true;
         for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
           ISymbol sym = (ISymbol) j.next();
           if (sym instanceof IVariable) {
             if (!acceptEpsilon(cfg, (IVariable)sym, history)) {
               allEpsilon = false;
               break;
             }
           }
           else {
             allEpsilon = false;
             break;
           }
         }
         if (allEpsilon) {
           return true;
         }
       }
     }
     return false;
   }

   /**
    * get rid of unit rules that consists of only non-terminals.
    */
    static public void eliminateUnitRules(IGrammar g){
     ArrayList unitRules = new ArrayList();
     ArrayList newRules = new ArrayList();
     for (Iterator i = g.getRules().iterator(); i.hasNext(); ){
       IProductionRule rule = (IProductionRule) i.next();
       if (rule.getRight().size() == 1
           && rule.getRight().get(0) instanceof IVariable) {
         unitRules.add(rule);
       }
     }
     for (Iterator i = unitRules.iterator(); i.hasNext(); ){
       IProductionRule rule = (IProductionRule) i.next();
       IVariable lv = (IVariable) rule.getLeft();
       IVariable rv = (IVariable) rule.getRight().get(0);
       for (Iterator j = getNonUnitRules(g, rv).iterator(); j.hasNext(); ) {
         IProductionRule r = (IProductionRule) j.next();
         IProductionRule newRule = new ProductionRule(lv, r.getRight());
         newRules.add(newRule);
       }
     }
     g.getRules().removeAll(unitRules);
     g.getRules().addAll(newRules);
    }

    static private Set getNonUnitRules(IGrammar g, IVariable v) {
      return getNonUnitRules(g, v, new HashSet());
    }

    static private Set getNonUnitRules(IGrammar g, IVariable v, Set history) {
      if (history.contains(v)) {
        return new HashSet();
      }
      history.add(v);

      Set urules = g.getRules(v);
      Set nrules = new HashSet(); 
      for (Iterator i = urules.iterator(); i.hasNext(); ) {
        IProductionRule rule = (IProductionRule) i.next();
        if (rule.getRight().size() == 1 && rule.getRight().get(0) instanceof IVariable) {
          i.remove();
          Set rs = getNonUnitRules(g, (IVariable)rule.getRight().get(0), history);
          nrules.addAll(rs);
        }
        else if (rule.getRight().size() <= 1) {
          nrules.add(rule);
        }
      }
      urules.addAll(nrules);
      return urules;
    }

    /**
     * replace every rules by shorter rules that have two non-terminals/terminals at most.
     */
    static public void simplifyRules(IContextFreeGrammar cfg, IVariableFactory varFactory){
      Set longRules = new HashSet();
      if (varFactory == null) {
        varFactory = new FreshVariableFactory(SimpleVariableFactory.defaultFactory, cfg);
      }
      for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
        IProductionRule rule = (IProductionRule) i.next();
        if (rule.getRight().size() > 2) {
          i.remove();
          longRules.add(rule);
        }
      }
      for (Iterator i = longRules.iterator(); i.hasNext(); ) {
        IProductionRule rule = (IProductionRule) i.next();
        Set newRules = simplifyRules(rule, varFactory);
        cfg.addRules(newRules);
      }
    }

    static private Set simplifyRules(IProductionRule rule, IVariableFactory varFactory) {
      List l = rule.getRight();
      if (l.size() <= 2) {
        HashSet s = new HashSet();
        s.add(rule);
        return s;
      }
      IVariable v = varFactory.createVariable("N");

      List l1 = new ArrayList();
      l1.add(l.get(0));
      l1.add(v);
      IProductionRule rule1 = new ProductionRule(rule.getLeft(), l1);

      List l2 = new ArrayList(l);
      l2.remove(0);
      IProductionRule rule2 = new ProductionRule(v, l2);

      Set s = simplifyRules(rule2, varFactory);
      s.add(rule1);

      return s;
    }

    /**
     * move all the terminals to unit rules.
     */
    static public void moveTerminalsToUnitRules(IContextFreeGrammar cfg, IVariableFactory varFactory){
      Map m = new HashMap();
      if (varFactory == null) {
        varFactory = new FreshVariableFactory(SimpleVariableFactory.defaultFactory, cfg);
      }
      for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
        IProductionRule rule = (IProductionRule) i.next();
        if (rule.getRight().size() < 2) { // unit rule or epsilon rule
          continue;
        }
        List right = new ArrayList();
        for (Iterator r = rule.getRight().iterator(); r.hasNext(); ) {
          ISymbol symbol = (ISymbol) r.next();
          if (!(symbol instanceof IVariable)) {
            IVariable v = (IVariable) m.get(symbol);
            if (v==null) {
              v = varFactory.createVariable("N");
              m.put(symbol, v); 
            }
            right.add(v);
          }
          else {
            right.add(symbol);
          }
        }
        rule.getRight().clear();
        rule.getRight().addAll(right);
      }
      for (Iterator i = m.keySet().iterator(); i.hasNext(); ) {
        ISymbol s = (ISymbol) i.next();
        IVariable v = (IVariable) m.get(s);
        cfg.addRule(new ProductionRule(v, new ISymbol[]{s}));
      }
    }

    static public String createUniqueVariableName(IGrammar grammar) {
      final Set variables = new HashSet();
      grammar.traverseRules(new IRuleVisitor(){
        public void onVisit(IProductionRule rule) {
          IVariable v = rule.getLeft();
          variables.add(v.getName());
        }
      });
      return AUtil.createUniqueName("v", variables);
    }

    /**
     * create an union of two grammars.
     * @param cfg1
     * @param cfg2
     * @param m1
     * @param m2
     * @return
     */
    static public IContextFreeGrammar createUnion(IContextFreeGrammar cfg1, IContextFreeGrammar cfg2, Map m2) {
      Set rules = new HashSet();
      IVariableFactory varFactory = new FreshVariableFactory(SimpleVariableFactory.defaultFactory, cfg1);
      IVariable startSymbol = varFactory.createVariable(variablePrefix);
      cfg1 = (IContextFreeGrammar) cfg1.copy(SimpleGrammarCopier.defaultCopier);
      cfg2 = (IContextFreeGrammar) cfg2.copy(SimpleGrammarCopier.defaultCopier);
      cfg2 = (IContextFreeGrammar) useUniqueVariables(cfg2, varFactory, m2);
      IProductionRule r1 = new ProductionRule(startSymbol, new ISymbol[]{cfg1.getStartSymbol()});
      IProductionRule r2 = new ProductionRule(startSymbol, new ISymbol[]{cfg2.getStartSymbol()});
      rules.add(r1);
      rules.add(r2);
      rules.addAll(cfg1.getRules());
      rules.addAll(cfg2.getRules());
      IContextFreeGrammar cfg = new ContextFreeGrammar(startSymbol, rules);
      return cfg;
    }

    static public IContextFreeGrammar createUnion(IContextFreeGrammar cfg1, IContextFreeGrammar cfg2) {
      return createUnion(cfg1, cfg2, new HashMap());
    }

    /**
     * create a concatenation of two grammars.
     * @param cfg1
     * @param cfg2
     * @param m1
     * @param m2
     * @return
     */
    static public IContextFreeGrammar createConcatenation(IContextFreeGrammar cfg1, IContextFreeGrammar cfg2, Map m2) {
      Set rules = new HashSet();
      IVariableFactory varFactory = new FreshVariableFactory(SimpleVariableFactory.defaultFactory, cfg1);
      IVariable startSymbol = varFactory.createVariable(variablePrefix);
      cfg1 = (IContextFreeGrammar) cfg1.copy(SimpleGrammarCopier.defaultCopier);
      cfg2 = (IContextFreeGrammar) cfg2.copy(SimpleGrammarCopier.defaultCopier);
      cfg2 = (IContextFreeGrammar) useUniqueVariables(cfg2, varFactory, m2);
      IProductionRule r1 = new ProductionRule(startSymbol, new ISymbol[]{cfg1.getStartSymbol(), cfg2.getStartSymbol()});
      rules.add(r1);
      rules.addAll(cfg1.getRules());
      rules.addAll(cfg2.getRules());
      IContextFreeGrammar cfg = new ContextFreeGrammar(startSymbol, rules);
      return cfg;
    }

    static public IContextFreeGrammar createConcatenation(IContextFreeGrammar cfg1, IContextFreeGrammar cfg2) {
      return createConcatenation(cfg1, cfg2, new HashMap());
    }

    static public IContextFreeGrammar createConcatenation(ISymbol sym1, IContextFreeGrammar cfg2) {
      IVariable start = new Variable("N0");
      IProductionRule rule = new ProductionRule(start, new ISymbol[]{sym1});
      IContextFreeGrammar cfg1 = new ContextFreeGrammar(start, new IProductionRule[]{rule});
      return createConcatenation(cfg1, cfg2);
    }

    static public String toRuleChain(IGrammar g) {
      return toRuleChain(g, g.getStartSymbol());
    }

    static public String toRuleChain(IGrammar g, IVariable v) {
      return toRuleChain(g, v, "", new HashSet());
    }

    static private String toRuleChain(final IGrammar g, final IProductionRule r, final String prefix, final Set h) {
      if (h.contains(r)) {
        return AUtil.lineSeparator;
      }
      h.add(r);

      final StringBuffer buff = new StringBuffer();
      buff.append(prefix + r.toString());
      buff.append(AUtil.lineSeparator);
      for (Iterator i = r.getRight().iterator(); i.hasNext(); ) {
        ISymbol s = (ISymbol) i.next();
        s.traverse(new ISymbolVisitor(){
          public void onVisit(ISymbol symbol) {
            if (symbol instanceof IVariable) {
              String s = toRuleChain(g, (IVariable)symbol, prefix, h);
              buff.append(s);
            }
          }
          public void onLeave(ISymbol symbol) {
          }
        });
      }
      return buff.toString();
    }

    static private String toRuleChain(final IGrammar g, IVariable v, final String prefix, final Set h) {
      StringBuffer buff = new StringBuffer();
      for (Iterator j = g.getRules(v).iterator(); j.hasNext(); ) {
        IProductionRule nr = (IProductionRule) j.next();
        String s = toRuleChain(g, nr, prefix + "  ", h);
        buff.append(s);
      }
      return buff.toString();
    }


    static public interface ITransitionSymbol {
      List getSymbols(ITransition transition);
    }

    static public class TransitionInput implements ITransitionSymbol {
      public List getSymbols(ITransition transition) {
        List l = new ArrayList();
        ISymbol is = transition.getInputSymbol();
        if (is != null) {
          l.add(is);
        }
        return l;
      }

      static public TransitionInput defaultInstance = new TransitionInput();
    }
    
    static public class TransitionOutput implements ITransitionSymbol {
      public List getSymbols(ITransition transition) {
        List l = new ArrayList();
        for (Iterator i = transition.getOutputSymbols(); i.hasNext(); ) {
          l.add(i.next());
        }
        return l;
      }

      static public TransitionOutput defaultInstance = new TransitionOutput();
    }

    /**
     * transform the finite-state automaton into a regular grammar.
     * @param automaton   a finite-state automaton 
     * @param allSymbols  used by the Automatons.expand method.
     * @return a regular grammar
     */
    static public IContextFreeGrammar toCFG(IAutomaton automaton, Set allSymbols, ITransitionSymbol transSym) {
      automaton = (IAutomaton) automaton.copy(new DeepSTSCopier(SimpleTransitionCopier.defaultCopier));
      automaton = Automatons.expand(automaton, allSymbols);
      return toCFG(automaton, transSym);
    }

    /**
     * transform the finite-state automaton into a regular grammar.
     * @param automaton   a finite-state automaton in which all the transitions have concrete input symbols.
     * @return a regular grammar
     */
    static public IContextFreeGrammar toCFG(IAutomaton automaton, ITransitionSymbol transSym) {
      Set rules = new HashSet();
      for (Iterator i = automaton.getTransitions().iterator(); i.hasNext(); ) {
        Transition t = (Transition) i.next();
        IState preState = t.getPreState();
        IState postState = t.getPostState();
        List rs = transSym.getSymbols(t);
        IVariable preVar = new Variable(preState.getName());
        IVariable postVar = new Variable(postState.getName());
        rs.add(postVar);
        ProductionRule rule = new ProductionRule(preVar, rs);
        rules.add(rule);
      }
      for (Iterator i = automaton.getFinalStates().iterator(); i.hasNext(); ) {
        IState finState = (IState) i.next();
        IVariable finVar = new Variable(finState.getName());
        ProductionRule rule = new ProductionRule(finVar, new ArrayList());
        rules.add(rule);
      }
      IVariable initVar = new Variable(automaton.getInitialState().getName());
      ContextFreeGrammar cfg = new ContextFreeGrammar(initVar, rules);
      return cfg;
    }

    static public IContextFreeGrammar toCFG(IAutomaton automaton) {
      return toCFG(automaton, TransitionInput.defaultInstance);
    }

    public static Set collectLinearRules(IContextFreeGrammar cfg) {
      Set s = new HashSet();
      collectLinearRules(cfg, s, s);
      return s;
    }

    public static Set collectLeftLinearRules(IContextFreeGrammar cfg) {
      Set l = new HashSet();
      Set r = new HashSet();
      collectLinearRules(cfg, l, r);
      return l;
    }

    public static Set collectRightLinearRules(IContextFreeGrammar cfg) {
      Set l = new HashSet();
      Set r = new HashSet();
      collectLinearRules(cfg, l, r);
      return r;
    }

    public static void collectLinearRules(IContextFreeGrammar cfg, Set lset, Set rset) {
      nextRule:
        for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
          IProductionRule rule = (IProductionRule) i.next();
          List right = rule.getRight();
          if (right.size() > 1) {
            Iterator r = right.iterator();
            ISymbol s = (ISymbol) r.next();
            if (s instanceof IVariable) {
              while (r.hasNext()) {
                s = (ISymbol) r.next();
                if (s instanceof IVariable) {
                  continue nextRule;
                }
              }
              lset.add(rule);
            }
            else {
              while (r.hasNext()) {
                s = (ISymbol) r.next();
                if (s instanceof IVariable && r.hasNext()) {
                  continue nextRule;
                }
              }
              rset.add(rule);
            }
          }
        }
    }

    public static Set collectMutuallyRecursiveVariables(final IGrammar g) {
      Set mvars = new HashSet();
      for (Iterator i = g.getNonterminals().iterator(); i.hasNext(); ) {
        IVariable v = (IVariable) i.next();
        boolean isRec = isMutuallyRecursiveVariables(g, v, v, new HashSet());
        if (isRec) {
          mvars.add(v);
        }
      }
      return mvars;
    }

    private static boolean isMutuallyRecursiveVariables(final IGrammar g, final IVariable sv, final IVariable cv, final Set history) {
      if (history.contains(cv)) {
        return false;
      }
      history.add(cv);

      final RuntimeException isMutualException = new RuntimeException();
      try {
        for (Iterator i = g.getRules(cv).iterator(); i.hasNext(); ) {
          IProductionRule rule = (IProductionRule) i.next();
          for (Iterator j = rule.getRight().iterator(); j.hasNext(); ) {
            ISymbol s = (ISymbol) j.next();
            s.traverse(new ISymbolVisitor(){
              public void onVisit(ISymbol symbol) {
                if (sv.equals(symbol)) {
                  throw(isMutualException);
                }
                if (symbol instanceof IVariable) {
                  if (isMutuallyRecursiveVariables(g, sv, (IVariable)symbol, history)) {
                    throw(isMutualException);
                  }
                }
              }
              public void onLeave(ISymbol symbol) {
              }
            });
          }
        }
        return false;
      }
      catch(RuntimeException e) {
        if (e == isMutualException) {
          return true;
        }
        throw(e);
      }
    }

    static public class RegularApproximation {
      static private class VarMap {
//        private Map supMap;
//        private Map supUpMap;
        private IVariableFactory varFactory;

        public VarMap(IContextFreeGrammar cfg) {
          varFactory = new FreshVariableFactory(SimpleVariableFactory.defaultFactory, cfg);
//          supMap = new HashMap();
//          supUpMap = new HashMap();
        }

        private IVariable getVar(Map m, IVariable v, IVariable w) {
          PrefixedSymbol vw = new PrefixedSymbol(v,w);
          if (m.containsKey(vw)) {
            IVariable x = (IVariable) m.get(vw);
            return x;
          }
          else {
            IVariable x = varFactory.createVariable(vw.getName()+":");
            m.put(vw, x);
            return x;
          }
        }

        public IVariable getSupVar(IVariable v, IVariable w) {
          //return new Variable(v.getName());
          return new Variable(v.getName() + "^" + v.getName());
          //return getVar(supMap, v, w);
        }

        public IVariable getSupUpVar(IVariable v, IVariable w) {
          //return new Variable(v.getName() + "'");
          return new Variable(v.getName() + "^" + v.getName() + "'");
          //return getVar(supUpMap, v, w);
        }

        public List getSupVar(List seq, IVariable w) {
          List l = new ArrayList(seq);
          IVariable v = (IVariable) l.get(l.size()-1);
          IVariable x = getSupVar(v, w);
          l.remove(l.size()-1);
          l.add(x);
          return l;
        }

        public List getSupUpVar(List seq, IVariable w) {
          List l = new ArrayList(seq);
          IVariable v = (IVariable) l.get(l.size()-1);
          IVariable x = getSupUpVar(v, w);
          l.remove(l.size()-1);
          l.add(x);
          return l;
        }
      }

      /**
       * performs the regular approximation described in the following literature.
       *   M. Mohri: "Regular Approximation of Context-Free Grammars Through Transformation",
       *   http://citeseer.ist.psu.edu/mohri00regular.html
       * @param cfg
       */
      public static void approximateToRegular(IContextFreeGrammar cfg) {
        Set mvars = collectMutuallyRecursiveVariables(cfg);
        Set mvarsEx = collectMutuallyRecursiveVariablesEx(cfg, mvars);
        Set rules = new HashSet();
        VarMap vmap = new VarMap(cfg);

        for (Iterator i = mvarsEx.iterator(); i.hasNext(); ) {
          IVariable v = (IVariable) i.next();
          IProductionRule rule = new ProductionRule(vmap.getSupUpVar(v,v), new ISymbol[]{});
          rules.add(rule);
        }

        for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
          IProductionRule rule = (IProductionRule) i.next();
          if (mvars.contains(rule.getLeft())) {
            Set rs = splitRule(rule, mvars, mvarsEx, vmap);
            //System.err.println(rule);
            //System.err.println("  " + rs);
            rules.addAll(rs);
          }
          else {
            rules.add(replaceRule(rule, mvarsEx, vmap));
          }
        }

        if (mvarsEx.contains(cfg.getStartSymbol())) {
          cfg.setStartSymbol(vmap.getSupVar(cfg.getStartSymbol(), cfg.getStartSymbol()));
        }

        cfg.getRules().clear();
        cfg.getRules().addAll(rules);
      }

      static private IProductionRule replaceRule(IProductionRule rule, Set mvarsEx, VarMap vmap) {
        IVariable left = rule.getLeft();
        List right = new ArrayList();
        if (mvarsEx.contains(left)) {
          left = vmap.getSupVar(left, left);
        }
        for (Iterator i = rule.getRight().iterator(); i.hasNext(); ) {
          ISymbol s = (ISymbol) i.next();
          if (s instanceof IVariable) {
            if (mvarsEx.contains(s)) {
              s = vmap.getSupVar((IVariable)s, (IVariable)s);
            }
          }
          right.add(s);
        }
        return new ProductionRule(left, right);
      }

      static private Set splitRule(IProductionRule rule, Set mvars, Set mvarsEx, VarMap vmap) {
        Set result = new HashSet();
        List rhss = splitRight(rule.getRight(), mvars);
        //System.err.println("splitRight:" + rhss);
        if (rhss.size()>1) {
          for (Iterator i = mvarsEx.iterator(); i.hasNext(); ) {
            IVariable c = (IVariable) i.next();
            IVariable left = vmap.getSupVar(rule.getLeft(), c);
            List right = vmap.getSupVar((List)rhss.get(0), c);
            IProductionRule r = new ProductionRule(left, right);
            result.add(r);
          }
          for (int idx = 0; idx < rhss.size()-2; idx++) {
            for (Iterator i = mvarsEx.iterator(); i.hasNext(); ) {
              IVariable c = (IVariable) i.next();
              List rhs0 = (List) rhss.get(idx);
              IVariable left = vmap.getSupUpVar((IVariable)rhs0.get(rhs0.size()-1), c);
              List rhs1 = (List) rhss.get(idx+1);
              List right = vmap.getSupVar(rhs1, c);
              IProductionRule r = new ProductionRule(left, right);
              result.add(r);
            }
          }
        }
        for (Iterator i = mvarsEx.iterator(); i.hasNext(); ) {
          IVariable c = (IVariable) i.next();
          IVariable left = null;
          if (rhss.size()>1) {
            List rhs0 = (List) rhss.get(rhss.size()-2);
            left = vmap.getSupUpVar((IVariable)rhs0.get(rhs0.size()-1), c);
          }
          else {
            left = vmap.getSupVar(rule.getLeft(), c);
          }
          List right = new ArrayList((List) rhss.get(rhss.size()-1));
          IVariable rightN = vmap.getSupUpVar(rule.getLeft(), c);
          right.add(rightN);
          IProductionRule r = new ProductionRule(left, right);
          result.add(r);
        }
        return result;
      }

      static private List splitRight(List right, Set mvars) {
        List l = new ArrayList();
        List result = new ArrayList();
        for (Iterator i = right.iterator(); i.hasNext(); ) {
          ISymbol s = (ISymbol) i.next();
          if (mvars.contains(s)) {
            l.add(s);
            result.add(l);
            l = new ArrayList();
          }
          else {
            l.add(s);
          }
        }
        result.add(l);
        return result;
      }

      /**
       * obtain the set of M'={A|(A < M) and ((A = S) or (\exists (B -> aAb) < P . B < M)},
       * where 'M' is a set of mutually recursive variables, 'a' and 'b' is a sequence
       * of terminal symbols and variables that is not contained by 'M'.
       * @param cfg
       * @return M'
       */
      public static Set collectMutuallyRecursiveVariablesEx(IContextFreeGrammar cfg) {
        Set mvars = collectMutuallyRecursiveVariables(cfg);
        return collectMutuallyRecursiveVariablesEx(cfg, mvars);
      }

      private static Set collectMutuallyRecursiveVariablesEx(IContextFreeGrammar cfg, Set mvars) {
        Set excludes = new HashSet();
        nextRule:
          for (Iterator i = cfg.getRules().iterator(); i.hasNext(); ) {
            IProductionRule rule = (IProductionRule) i.next();
            if (mvars.contains(rule.getLeft())) {
              continue nextRule;
            }
            List right = rule.getRight();
            if (right.size() > 2) {
              Iterator r = right.iterator();
              ISymbol s = (ISymbol) r.next();
              if (mvars.contains(s)) {
                continue nextRule;
              }
              IVariable exclude = null;
              while (r.hasNext()) {
                s = (ISymbol) r.next();
                if (mvars.contains(s)) {
                  if (r.hasNext()) {
                    exclude = (IVariable) s;
                    break;
                  }
                  else {
                    continue nextRule;
                  }
                }
              }
              while (r.hasNext()) {
                s = (ISymbol) r.next();
                if (mvars.contains(s)) {
                  continue nextRule;
                }
              }
              excludes.add(exclude);
            }
          }
        IVariable sv = cfg.getStartSymbol();
        for (Iterator i = mvars.iterator(); i.hasNext(); ) {
          IVariable v = (IVariable) i.next();
          if (!v.equals(sv) && excludes.contains(v)) {
            i.remove();
          }
        }
        return mvars;
      }
    }

    /**
     * @param cfg should be a regular grammar.
     * @return
     */
    public static IAutomaton toAutomaton(IContextFreeGrammar cfg, final IVariableFactory varFactory) {
      IContextFreeGrammar ncfg = (IContextFreeGrammar) cfg.copy(SimpleGrammarCopier.defaultCopier);
      RegularApproximation.approximateToRegular(ncfg);
      normalize(ncfg, varFactory);
      Set<ITransition> transitions = new HashSet<ITransition>();
      final Set<String> stateNames = new HashSet<String>();
      Map<IVariable, IState> varMap = new DMap<IVariable, IState>(new DMap.Factory<IVariable,IState>(){
        public IState create(IVariable key) {
          return new State(AUtil.createUniqueName("s", stateNames));
        }
      });
      IState startState = varMap.get(ncfg.getStartSymbol()); 
      IState finalState = new State(AUtil.createUniqueName("s", stateNames));
      
      for (Iterator<IProductionRule> i = ncfg.getRules().iterator(); i.hasNext(); ) {
        IProductionRule r = i.next();
        int rsize = r.getRight().size();
        switch(rsize) {
        case 0: {
          IVariable lv = r.getLeft();
          if (!ncfg.getStartSymbol().equals(lv)) {
            throw(new RuntimeException("epsilon rules should be eliminated."));
          }
          ITransition t = new Transition(startState, finalState);
          transitions.add(t);
          break;
        }
        case 1: {
          IVariable lv = r.getLeft();
          ISymbol rv = r.getRight(0);
          if (rv instanceof IVariable) {
            throw(new RuntimeException("unit rules should be eliminated."));
          }
          ITransition t = new Transition(varMap.get(lv), finalState, rv);
          transitions.add(t);
          break;
        }
        case 2: {
          IVariable lv = r.getLeft();
          ISymbol input = r.getRight(0);
          if (!(r.getRight(1) instanceof IVariable)) {
            throw(new RuntimeException("should be normalized."));
          }
          IVariable rv = (IVariable) r.getRight(1);
          ITransition t = new Transition(varMap.get(lv), varMap.get(rv), input);
          transitions.add(t);
          break;
        }
        default:
          throw(new RuntimeException("should be normalized."));
        }
      }
      
      Set<IState> finalStates = new HashSet<IState>();
      finalStates.add(finalState);
      return new Automaton(startState, finalStates, transitions);
    }
    
    static public IAutomaton toAutomaton(IContextFreeGrammar cfg) {
      return toAutomaton(cfg, new FreshVariableFactory(SimpleVariableFactory.defaultFactory, cfg));
    }

    /**
     * returns the strings generated by the given context-free grammar.
     * @param cfg
     * @return a set of strings.
     *         If a cyclic rule or an invocation symbol is detected, this function returns null.
     */
    public static Set<String> stringValues(IContextFreeGrammar cfg, IVariable v) {
      return stringValues(cfg, v, new HashSet<IVariable>());
    }

    public static Set<String> stringValues(IContextFreeGrammar cfg, IVariable v, Set<IVariable> h) {
      if (h.contains(v)) {
        return null;
      }
      h.add(v);
      Set<String> vals = new HashSet<String>();
      Set<IProductionRule> rules = Grammars.getRules(cfg, v);
      for (IProductionRule r : rules) {
        Set<String> vvals = new HashSet<String>();
        vvals.add("");
        for (Iterator j = r.getRight().iterator(); j.hasNext(); ) {
          ISymbol s = (ISymbol) j.next();
          if (s instanceof CharSymbol) {
            CharSymbol c = (CharSymbol) s;
            String str = c.getName();
            vvals = appendString(vvals, str);
          }
          else if (s instanceof StringSymbol) {
            StringSymbol strSym = (StringSymbol) s;
            String str = strSym.getName();
            vvals = appendString(vvals, str);
          }
          else if (s instanceof IVariable) {
            Set<String> ss = stringValues(cfg, (IVariable)s, new HashSet<IVariable>(h));
            if (ss == null) {
              return null;
            }
            vvals = appendString(vvals, ss);
          }
          else {
            return null;
          }
        }
        vals.addAll(vvals);
      }
      return vals;
    }

    private static Set appendString(Set<String> s1, Set<String> s2) {
      Set<String> s = new HashSet<String>();
      for (String str1 : s1) {
        for (String str2 : s2) {
          s.add(str1 + str2);
        }
      }
      return s;
    }

    private static Set<String> appendString(Set<String> s1, String str2) {
      return appendString(s1, AUtil.set(new String[]{str2}));
    }
//
//    private static Set<String> appendString(String str1, Set<String> s2) {
//      return appendString(AUtil.set(new String[]{str1}), s2);
//    }
    
    public static IContextFreeGrammar createCFG(List<ISymbol> symbols) {
      IVariable startVar = new Variable(variablePrefix + "1");
      IProductionRule rule = new ProductionRule(startVar, symbols);
      IContextFreeGrammar cfg = new ContextFreeGrammar(startVar, new IProductionRule[]{rule});
      return cfg;
    }
    
    public static IContextFreeGrammar createCFG(ISymbol symbols[]) {
      IVariable startVar = new Variable(variablePrefix + "1");
      IProductionRule rule = new ProductionRule(startVar, symbols);
      IContextFreeGrammar cfg = new ContextFreeGrammar(startVar, new IProductionRule[]{rule});
      return cfg;
    }
}
