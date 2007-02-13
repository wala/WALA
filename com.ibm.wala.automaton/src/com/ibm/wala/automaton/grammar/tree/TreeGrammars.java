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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.automaton.grammar.string.DeepRuleCopier;
import com.ibm.wala.automaton.grammar.string.Grammars;
import com.ibm.wala.automaton.grammar.string.IProductionRule;
import com.ibm.wala.automaton.grammar.string.ProductionRule;
import com.ibm.wala.automaton.grammar.string.SimpleRuleCopier;
import com.ibm.wala.automaton.string.DeepSymbolCopier;
import com.ibm.wala.automaton.string.FreshVariableFactory;
import com.ibm.wala.automaton.string.ISymbol;
import com.ibm.wala.automaton.string.IVariableFactory;
import com.ibm.wala.automaton.string.SimpleVariableFactory;
import com.ibm.wala.automaton.tree.BinaryTree;
import com.ibm.wala.automaton.tree.BinaryTreeVariableFactory;
import com.ibm.wala.automaton.tree.IBinaryTree;
import com.ibm.wala.automaton.tree.IBinaryTreeVariable;
import com.ibm.wala.automaton.tree.IParentBinaryTree;

public class TreeGrammars extends Grammars {
  /**
   * normalize a regular tree grammar.
   * @param g        regular tree grammar (ITreeGrammar object)
   */
  static public void normalize(ITreeGrammar g, IVariableFactory<IBinaryTreeVariable> varFactory) {
    refreshProductionRules(g, new DeepRuleCopier(DeepSymbolCopier.defaultCopier));
    eliminateUnitRules(g);

    if (varFactory == null) {
      varFactory = 
        new BinaryTreeVariableFactory(
          new FreshVariableFactory(
              SimpleVariableFactory.defaultFactory, g));
    }
    IBinaryTreeVariable leafVar = null;
    Set newRules = new HashSet();

    // obtain/create a variable for the leaf.
    /*
    for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      IBinaryTree bt = (IBinaryTree) rule.getRight(0);
      if (bt.equals(BinaryTree.LEAF)) {
        if (g.getRules(rule.getLeft()).size()==1) {
          leafVar = (IBinaryTreeVariable) rule.getLeft();
        }
      }
    }
    */
    if (leafVar == null) {
      leafVar = varFactory.createVariable("N");
      IProductionRule r = new ProductionRule(leafVar, BinaryTree.LEAF);
      g.getRules().add(r);
    }

    do {
      newRules.clear();
      for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
        IProductionRule rule = (IProductionRule) i.next();
        IBinaryTree bt = (IBinaryTree) rule.getRight(0);
        if (bt.equals(BinaryTree.LEAF)){
          // do nothing
        }
        else if (bt instanceof IParentBinaryTree) {
          IParentBinaryTree pbt = (IParentBinaryTree) bt;
          if (!(pbt.getLeft() instanceof IBinaryTreeVariable)) {
            if (pbt.getLeft().equals(BinaryTree.LEAF)) {
              pbt.setLeft(leafVar);
            }
            else {
              IBinaryTreeVariable v = varFactory.createVariable("N");
              IProductionRule r = new ProductionRule(v, pbt.getLeft());
              pbt.setLeft(v);
              newRules.add(r);
            }
          }
          if (!(pbt.getRight() instanceof IBinaryTreeVariable)) {
            if (pbt.getRight().equals(BinaryTree.LEAF)) {
              pbt.setRight(leafVar);
            }
            else {
              IBinaryTreeVariable v = varFactory.createVariable("N");
              IProductionRule r = new ProductionRule(v, pbt.getRight());
              pbt.setRight(v);
              newRules.add(r);
            }
          }
        }
        else {
          // TODO: 
          throw(new RuntimeException("unimplemented yet."));
        }
      }
      g.getRules().addAll(newRules);
    } while(!newRules.isEmpty());
  }

  static public void normalize(ITreeGrammar g) {
    normalize(g, null);
  }

  static public void reduce(final ITreeGrammar g) {
    refreshProductionRules(g);

    for (Iterator i = g.getRules().iterator(); i.hasNext(); ) {
      final IProductionRule rule = (IProductionRule) i.next();
      IBinaryTree bt = (IBinaryTree) rule.getRight(0);
      bt = (IBinaryTree) bt.copy(new DeepSymbolCopier(){
        private Set history = new HashSet();
        {
          history.add(rule.getLeft());
        }
        public ISymbol copy(ISymbol s) {
          if (s instanceof IBinaryTreeVariable) {
            Set drules = g.getRules((IBinaryTreeVariable)s);
            if (drules.size()==1) {
              IProductionRule drule = (IProductionRule) drules.iterator().next();
              if (!history.contains(drule.getLeft())) {
                return drule.getRight(0).copy(this);
              }
            }
          }
          return super.copy(s);
        }
      });
      rule.getRight().clear();
      rule.getRight().add(bt);
    }

    eliminateUselessRules(g);
  }

  public static void append(ITreeGrammar g, IBinaryTree s) {
    append(g, s, new BinaryTreeVariableFactory(new FreshVariableFactory(SimpleVariableFactory.defaultFactory, g)));
  }

  public static void append(ITreeGrammar g, final IBinaryTree s, final IVariableFactory<IBinaryTreeVariable> varFactory) {
    normalize(g, varFactory);
    appendInternal(g, s, varFactory);
    eliminateUselessRules(g);
  }

  protected static void appendInternal(ITreeGrammar g, final IBinaryTree s, IVariableFactory<IBinaryTreeVariable> varFactory) {
    final Set<IProductionRule> newRules = new HashSet<IProductionRule>();
    final Map<IBinaryTreeVariable, IBinaryTreeVariable> varMap = new HashMap<IBinaryTreeVariable, IBinaryTreeVariable>();
    for (Iterator<IProductionRule> i = g.getRules().iterator(); i.hasNext(); ) {
      IProductionRule r = i.next();
      IBinaryTreeVariable v = (IBinaryTreeVariable) r.getLeft();
      IBinaryTreeVariable newVar = null;
      if (varMap.containsKey(v)) {
        newVar = varMap.get(v);
      }
      else {
        newVar = varFactory.createVariable(variablePrefix);
        varMap.put(v, newVar);
      }
      IProductionRule newRule = r.copy(new DeepRuleCopier(DeepSymbolCopier.defaultCopier));
      newRule.setLeft(newVar);
      newRules.add(newRule);
    }
    final Set<IProductionRule> rules = new HashSet<IProductionRule>();
    for (Iterator<IProductionRule> i = newRules.iterator(); i.hasNext(); ) {
      IProductionRule r = i.next();
      r = (IProductionRule) r.copy(SimpleRuleCopier.defaultCopier);
      if (r.getRight(0) instanceof IParentBinaryTree) {
        IParentBinaryTree pbt = (IParentBinaryTree) r.getRight(0);
        IBinaryTreeVariable v = (IBinaryTreeVariable) pbt.getRight();
        if (varMap.containsKey(v)) {
          pbt.setRight(varMap.get(v));
        }
        rules.add(r);
      }
      else if (r.getRight(0).equals(BinaryTree.LEAF)) {
        r.getRight().clear();
        r.getRight().add(s);
        rules.add(r);
      }
      else if (r.getRight(0) instanceof IBinaryTreeVariable) {
        // do nothing
      }
      else {
        throw(new RuntimeException("unexpected symbol: " + r.getRight(0)));
      }
    }
    g.getRules().addAll(rules);
    g.setStartSymbol(varMap.get(g.getStartSymbol()));
  }

  public static void append(IBinaryTree bt, IBinaryTree s) {
    if (bt instanceof IParentBinaryTree) {
      IParentBinaryTree pbt = (IParentBinaryTree) bt;
      if (pbt.getRight().equals(BinaryTree.LEAF)) {
        pbt.setRight(s);
      }
      else {
        append(pbt.getRight(), s);
      }
    }
    else {
      throw(new RuntimeException("should not append the tree to the leaf"));
    }
  }

  public static void appendChild(ITreeGrammar g, IBinaryTree s) {
    appendChild(g, s, new BinaryTreeVariableFactory(new FreshVariableFactory(SimpleVariableFactory.defaultFactory, g)));
  }
  
  public static void appendChild(ITreeGrammar g, IBinaryTree s, IVariableFactory<IBinaryTreeVariable> varFactory) {
    normalize(g, varFactory);
    appendChildInternal(g, s, varFactory);
    eliminateUselessRules(g);
  }

  protected static void appendChildInternal(ITreeGrammar g, IBinaryTree s, IVariableFactory<IBinaryTreeVariable> varFactory) {
    Set<IProductionRule> rules = new HashSet<IProductionRule>();
    IBinaryTreeVariable origStart = (IBinaryTreeVariable) g.getStartSymbol();
    IBinaryTreeVariable newStart = varFactory.createVariable(variablePrefix);
    for (Iterator<IProductionRule> i = g.getRules(origStart).iterator(); i.hasNext(); ) {
      IProductionRule r = i.next();
      IProductionRule newRule = r.copy(new DeepRuleCopier(DeepSymbolCopier.defaultCopier));
      newRule.setLeft(newStart);
      rules.add(newRule);
    }
    for (Iterator<IProductionRule> i = rules.iterator(); i.hasNext(); ) {
      IProductionRule r = i.next();
      if (r.getRight(0) instanceof IParentBinaryTree) {
        IParentBinaryTree pbt = (IParentBinaryTree) r.getRight(0);
        IBinaryTreeVariable v = (IBinaryTreeVariable) pbt.getLeft();
        g.setStartSymbol(v);
        appendInternal(g, s, varFactory);
        pbt.setLeft((IBinaryTreeVariable)g.getStartSymbol());
        g.setStartSymbol(origStart);
      }
    }
    g.getRules().addAll(rules);
    g.setStartSymbol(newStart);
  }
}
