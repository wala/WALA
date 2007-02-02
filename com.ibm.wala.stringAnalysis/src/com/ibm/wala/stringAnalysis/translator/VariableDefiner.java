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
import com.ibm.wala.automaton.regex.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.stringAnalysis.util.SAUtil;

public class VariableDefiner extends RuleAdder {
  static private StringPatternCompiler compiler = new StringPatternCompiler();
  
  public VariableDefiner(ISimplify cfg, IVariable v, IPattern pat) {
    this(cfg, new IVariable[]{v}, new IPattern[]{pat});
  }
  
  public VariableDefiner(ISimplify cfg, Map<IVariable,IPattern> defs) {
    this(cfg.toSimple(), defs);
  }

  public VariableDefiner(ISimplify cfg, IVariable v[], IPattern pat[]) {
    this(cfg.toSimple(), SAUtil.map(v, pat));
  }
  
  private VariableDefiner(SimpleGrammar g, Map<IVariable,IPattern> defs) {
    super(g, createRules(g, defs));
  }
  
  static private Set<IProductionRule> createRules(IGrammar g, Map<IVariable, IPattern> defs) {
    Set<IProductionRule> rules = new HashSet<IProductionRule>();
    for (IVariable v : defs.keySet()) {
      IPattern pat = defs.get(v);
      IAutomaton fst = compiler.compile(pat);
      Automatons.eliminateEpsilonTransitions(fst);
      IContextFreeGrammar cfg = Grammars.toCFG(fst);
      Map m = new HashMap();
      Grammars.useUniqueVariables(cfg, g, m);
      IMatchContext ctx = new MatchContext();
      ctx.put(cfg.getStartSymbol(), v);
      cfg = (IContextFreeGrammar) cfg.copy(new DeepGrammarCopier(new DeepRuleCopier(new VariableReplacer(ctx))));
      rules.addAll(cfg.getRules());
    }
    return rules;
  }
  
}
