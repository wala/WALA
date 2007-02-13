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

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleGrammarCopier;
import com.ibm.wala.automaton.string.IState;
import com.ibm.wala.automaton.tree.BinaryTree;
import com.ibm.wala.automaton.tree.BinaryTreeVariable;
import com.ibm.wala.automaton.tree.BottomUpTreeAutomaton;
import com.ibm.wala.automaton.tree.IBinaryTree;
import com.ibm.wala.automaton.tree.IBinaryTreeVariable;
import com.ibm.wala.automaton.tree.IParentBinaryTree;
import com.ibm.wala.automaton.tree.StateBinaryTree;

public class RTLBottomUpTranslator extends RTLAbstractTranslator {
  public RTLBottomUpTranslator(BottomUpTreeAutomaton automaton) {
    super(automaton);
  }

  public ITreeGrammar translate(ITreeGrammar g) {
    g = (ITreeGrammar) g.copy(SimpleGrammarCopier.defaultCopier);
    TreeGrammars.normalize(g);

    //System.err.println(AUtil.prettyFormat(g));
    Set rules = new HashSet();
    StateVariable2Name sv2name = new StateVariable2Name(g);

    Set pstates = getPrimitiveStates();
    for (Iterator i = pstates.iterator(); i.hasNext(); ) {
      IState lstate = (IState) i.next();
      for (Iterator j = pstates.iterator(); j.hasNext(); ) {
        IState rstate = (IState) j.next();
        //System.err.println("state: " + lstate + ", " + rstate);
        for (Iterator k = g.getRules().iterator(); k.hasNext(); ) {
          IProductionRule rule = (IProductionRule) k.next();
          IBinaryTree bt = (IBinaryTree) rule.getRight(0);
          if (bt instanceof IParentBinaryTree) {
            IParentBinaryTree pbt = (IParentBinaryTree) bt;
            StateBinaryTree lbt = new StateBinaryTree(lstate, new BinaryTreeVariable(sv2name.get(lstate, (IBinaryTreeVariable)pbt.getLeft())));
            StateBinaryTree rbt = new StateBinaryTree(rstate, new BinaryTreeVariable(sv2name.get(rstate, (IBinaryTreeVariable)pbt.getLeft())));
            bt = new BinaryTree(bt.getLabel(), lbt, rbt);
          }
          //System.err.println(rule + " : " + rule.getLeft() + " -> " + bt);
          Set rs = translate((IBinaryTreeVariable)rule.getLeft(), bt, sv2name);
          Set rs2 = new HashSet();
          //System.err.println("  " + rs);
          for (Iterator l = rs.iterator(); l.hasNext(); ) {
            IProductionRule r = (IProductionRule) l.next();
            StateBinaryTree sv = (StateBinaryTree) r.getRight(0);
            StateBinaryTree sv2 = new StateBinaryTree(sv.getState(), (IBinaryTreeVariable)rule.getLeft());
            String vname = sv2name.get(sv2);
            if (vname == null) throw(new AssertionError("should not be null."));
            IBinaryTreeVariable v = new BinaryTreeVariable(vname);
            rs2.add(new ProductionRule(v, sv.getTree()));
          }
          //System.err.println("  " + rs2);
          rules.addAll(rs2);
        }
      }
    }

    Set finalStates = ((BottomUpTreeAutomaton)getSystem()).getFinalStates();
    for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
      for (Iterator j = finalStates.iterator(); j.hasNext(); ) {
        IState finState = (IState) j.next();
        IProductionRule rule = (IProductionRule) i.next();
        IBinaryTreeVariable v = (IBinaryTreeVariable) rule.getLeft();
        IBinaryTree bt = new BinaryTreeVariable(sv2name.get(finState, v));
        IProductionRule r = new ProductionRule(v, bt);
        rules.add(r);
      }
    }

    ITreeGrammar result = new TreeGrammar((IBinaryTreeVariable)g.getStartSymbol(), rules);
    //TreeGrammars.eliminateDanglingVariables(result, usedVars);
    TreeGrammars.eliminateUselessRules(result);
    return result;
  }
}
