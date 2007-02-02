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

import com.ibm.wala.automaton.grammar.string.*;
import com.ibm.wala.automaton.string.*;
import com.ibm.wala.automaton.tree.*;

public class TreeGrammar extends SimpleGrammar implements ITreeGrammar {
  public TreeGrammar(IBinaryTreeVariable startSymbol, IProductionRule rules[]) {
    super(startSymbol, rules);
  }
  
  public TreeGrammar(IBinaryTreeVariable startSymbol, Set rules) {
    super(startSymbol, rules);
  }
  
  public TreeGrammar(SimpleGrammar g) {
    this((IBinaryTreeVariable)translateVariable(g.getStartSymbol()), collectRules(g.getRules()));
  }
  
  static public class VariableReplacer extends DeepSymbolCopier {
    public ISymbol copy(ISymbol symbol) {
      if ((symbol instanceof IVariable) && !(symbol instanceof IBinaryTreeVariable)) {
        return new BinaryTreeVariable((IVariable)symbol);
      }
      else {
        return super.copy(symbol);
      }
    }
  }
  
  static public VariableReplacer variableReplacer = new VariableReplacer();
  
  static private ISymbol translateVariable(ISymbol s) {
    if (s instanceof IBinaryTree) {
      return s;
    }
    else {
      return s.copy(variableReplacer);
    }
  }
  
  static private Set collectRules(Set rules) {
    final Set rtgRules = new HashSet();
    final Set cfgRules = new HashSet();
    for (Iterator i = rules.iterator(); i.hasNext(); ) {
      IProductionRule rule = (IProductionRule) i.next();
      if (rule.getRight().size() == 1) {
        if (rule.getRight(0) instanceof IBinaryTree) {
          if (rule.getLeft() instanceof IBinaryTreeVariable) {
            rtgRules.add(rule);
          }
          else {
            IProductionRule r = new ProductionRule((IVariable)translateVariable(rule.getLeft()), rule.getRight());
            rtgRules.add(r);
          }
        }
        else if (rule.getRight(0) instanceof IVariable) {
          IVariable v = (IVariable)rule.getRight(0);
          IProductionRule r = new ProductionRule((IVariable)translateVariable(rule.getLeft()), new BinaryTreeVariable(v));
          rtgRules.add(r);
        }
        else if (rule.getRight(0) instanceof IValueSymbol) {
          cfgRules.add(rule);
        }
        else if (rule.getRight(0) instanceof Symbol){
          cfgRules.add(rule);
        }
        else {
          throw(new RuntimeException("unsupported symbol: " + rule.getRight(0)));
          /*
          ISymbol s = rule.getRight(0).copy(new DeepSymbolCopier(){
            public ISymbol copy(ISymbol s) {
              if (s instanceof IVariable) {
                return translateVariable((IVariable)s);
              }
              else {
                return super.copy(s);
              }
            }
          });
          IProductionRule r = new ProductionRule((IVariable)translateVariable(rule.getLeft()), s);
          rtgRules.add(r);
          */
        }
      }
      else {
        cfgRules.add(rule);
      }
    }
    for (Iterator i = cfgRules.iterator(); i.hasNext(); ) {
      IProductionRule r = (IProductionRule) i.next();
      IContextFreeGrammar cfg = new ContextFreeGrammar(r.getLeft(), rules);
      Grammars.eliminateUselessRules(cfg);
      //Grammars.eliminateDanglingVariables(cfg);
      IProductionRule t = new ProductionRule((IVariable)translateVariable(r.getLeft()), new BinaryTree(new CFGSymbol(cfg)));
      rtgRules.add(t);
    }
    return rtgRules;
  }
  
  public SimpleGrammar toSimple() {
    SimpleGrammar g = super.toSimple();
    final Set newRules = new HashSet();
    g.traverseRules(new IRuleVisitor(){
      public void onVisit(IProductionRule rule) {
        if (rule.getLeft() instanceof IBinaryTreeVariable) {
          IBinaryTreeVariable btv = (IBinaryTreeVariable) rule.getLeft();
          newRules.add(new ProductionRule(btv, btv.getLabel()));
          newRules.add(new ProductionRule((IVariable)btv.getLabel(), rule.getRight()));
        }
        else {
          newRules.add(rule);
        }
      }
    });
    IVariable startVar = g.getStartSymbol();
    if (startVar instanceof IBinaryTreeVariable) {
      startVar = (IVariable) ((IBinaryTreeVariable)startVar).getLabel();
    }
    g = new SimpleGrammar(startVar, newRules);
    return g;
  }
}
