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

import java.util.*;

import com.ibm.wala.automaton.*;
import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;

public class RTLTopDownTranslator extends RTLAbstractTranslator {
  public RTLTopDownTranslator(TopDownTreeAutomaton automaton) {
    super(automaton);
  }

  public ITreeGrammar translate(ITreeGrammar g) {
    g = (ITreeGrammar) g.copy(SimpleGrammarCopier.defaultCopier);
    TreeGrammars.normalize(g);

    Set rules = new HashSet();
    final StateVariable2Name sv2name = new StateVariable2Name(g);

    for (Iterator i = getPrimitiveStates().iterator(); i.hasNext(); ) {
      IState state = (IState) i.next();
      for (Iterator j = g.getRules().iterator(); j.hasNext(); ) {
        IProductionRule rule = (IProductionRule) j.next();
        StateBinaryTree sv = new StateBinaryTree(state, (IBinaryTree) rule.getLeft());
        String vname = sv2name.get(sv);
        if (vname == null) throw(new AssertionError("should not be null."));
        IBinaryTreeVariable v = new BinaryTreeVariable(vname);
        StateBinaryTree bt = new StateBinaryTree(state, (IBinaryTree)rule.getRight(0));
        Set rs = translate(v, bt, sv2name);
        Set rs2 = new HashSet();
        for (Iterator k = rs.iterator(); k.hasNext(); ){
          final IProductionRule r = (IProductionRule) k.next();
          IBinaryTree bt2 = (IBinaryTree) r.getRight(0);
          bt2 = (IBinaryTree) bt2.copy(new DeepSymbolCopier(){
            public ISymbol copy(ISymbol s) {
              if (s instanceof StateBinaryTree) {
                String vname = sv2name.get((StateBinaryTree)s);
                return new BinaryTreeVariable(vname);
              }
              else {
                return super.copy(s);
              }
            }
          });
          rs2.add(new ProductionRule(r.getLeft(), bt2));
        }
        rules.addAll(rs2);
      }
    }

    IState initState = getSystem().getInitialState();
    for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      IBinaryTreeVariable v = (IBinaryTreeVariable) rule.getLeft();
      IBinaryTree bt = new BinaryTreeVariable(sv2name.get(initState, v));
      IProductionRule r = new ProductionRule(v, bt);
      rules.add(r);
    }

    ITreeGrammar result = new TreeGrammar((IBinaryTreeVariable)g.getStartSymbol(), rules);
    //TreeGrammars.eliminateDanglingVariables(result, usedVars);
    TreeGrammars.eliminateUselessRules(result);
    return result;
  }
}
