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
 * translator from Callgraph to GR. 
 */
package com.ibm.wala.stringAnalysis.translator;

import java.util.*;

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.cfg.*;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.*;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.stringAnalysis.grammar.*;

public class BB2GR implements IBB2Grammar {
  ISSA2Rule ssa2rule;

  public BB2GR(ISSA2Rule ssa2rule) {
    this.ssa2rule = ssa2rule;
  }

  public IGrammar translate(IBasicBlock bb, TranslationContext ctx) {
    List rules = new ArrayList();
    List phis = new ArrayList();
    int first = bb.getFirstInstructionIndex();
    int last = bb.getLastInstructionIndex();
    SSAInstruction instructions[] = ctx.getIR().getInstructions();

    if (bb instanceof BasicBlock) {
      for (Iterator i = ((BasicBlock)bb).iteratePhis(); i.hasNext(); ) {
        SSAPhiInstruction phi = (SSAPhiInstruction) i.next();
        Collection rs = ssa2rule.translate(phi, ctx);
        phis.add(rs);
      }
    }

    for (int i = first; i <= last; i++) {
      Collection rs = ssa2rule.translate(instructions[i], ctx);
      rules.addAll(rs);
    }
    if (bb instanceof BasicBlock) {
      for (Iterator i = ((BasicBlock)bb).iteratePis(); i.hasNext(); ) {
        SSAPiInstruction pi = (SSAPiInstruction) i.next();
        Collection rs = ssa2rule.translate(pi, ctx);
        rules.addAll(rs);
      }
    }

    Map ruleMap = new HashMap();
    Set fails = new HashSet();
    IState initState = new State("s" + System.identityHashCode(bb) + "[0]");
    IState preState = initState;
    Set finalStates = new HashSet();
    Set states = new HashSet();
    Set transitions = new HashSet();
    int idx = 1;
    for (Iterator i = phis.iterator(); i.hasNext(); ) {
      Collection rs = (Collection) i.next();
      IState nextState = new State("s" + System.identityHashCode(bb) + "[" + (idx) + "]");
      idx ++;
      for (Iterator j = rs.iterator(); j.hasNext(); ) {
        IProductionRule rule = (IProductionRule) j.next();
        IState state = new State("s" + System.identityHashCode(bb) + "[" + Integer.toString(idx) + "]");
        states.add(state);
        ISymbol input = new Symbol("p" + System.identityHashCode(bb) + "[" + Integer.toString(idx) + "]");
        ITransition transition = new Transition(preState, state, input);
        ruleMap.put(input, rule);
        transitions.add(transition);
        ITransition nextTransition = new Transition(state, nextState);
        transitions.add(nextTransition);
        idx ++;
      }
      preState = nextState;
    }
    for (Iterator i = rules.iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      IState state = new State("s" + System.identityHashCode(bb) + "[" + Integer.toString(idx) + "]");
      states.add(state);
      ISymbol input = new Symbol("p" + System.identityHashCode(bb) + "[" + Integer.toString(idx) + "]");
      ITransition transition = new Transition(preState, state, input);
      ruleMap.put(input, rule);
      transitions.add(transition);
      preState = state;
      idx ++;
    }
    finalStates.add(preState);
    IAutomaton automaton = new Automaton(initState, finalStates, transitions);
    IGrammar gr = new GR(ctx.getIR(), null, null, automaton, fails, ruleMap);
    return gr;
  }

  public ISSA2Rule getSSA2Rule() {
    return ssa2rule;
  }
}
