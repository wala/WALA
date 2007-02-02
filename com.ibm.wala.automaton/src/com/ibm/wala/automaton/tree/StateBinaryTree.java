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
package com.ibm.wala.automaton.tree;

import com.ibm.wala.automaton.string.*;

public class StateBinaryTree implements IBinaryTree, IState {
  final String SEPARATOR = ":";

  private IBinaryTree tree;
  private IState state;

  public StateBinaryTree(IBinaryTree tree, IState s) {
    this.tree = tree;
    this.state = s;
  }

  public StateBinaryTree(IState s, IBinaryTree tree) {
    this(tree, s);
  }

  public IBinaryTree getTree() {
    return tree;
  }

  public IState getState() {
    return state;
  }

  public ISymbol getLabel() {
    return tree.getLabel();
  }

  public String getName() {
    return state.getName() + SEPARATOR + tree.getName();
  }

  /*
    public void setName(String name) {
        int sep = name.indexOf(SEPARATOR);
        if (sep >= 0) {
            String prefixStr = name.substring(0, sep);
            String localStr = name.substring(sep+1, name.length());
            state.setName(prefixStr);
            tree.setName(localStr);
        }
        else{
            tree.setName(name);
        }
    }
   */

  public int hashCode() {
    return state.hashCode();
  }

  public boolean equals(Object obj) {
    if (!getClass().equals(obj.getClass())) return false;
    StateBinaryTree t = (StateBinaryTree) obj;
    return ((tree==null) ? t.tree==null : tree.equals(t.tree))
    && state.equals(t.state);
  }

  public boolean matches(ISymbol symbol, IMatchContext context) {
    if (symbol instanceof StateBinaryTree) {
      StateBinaryTree t = (StateBinaryTree) symbol;
      if (state.equals(t.state)) {
        return (tree==null) ? t.tree==null : tree.matches(t.tree, context);
      }
      else {
        return false;
      }
    }
    else {
      return (tree==null) ? symbol==null : tree.matches(symbol, context);
    }
  }

  public boolean possiblyMatches(ISymbol symbol, IMatchContext context) {
    if (symbol instanceof StateBinaryTree) {
      StateBinaryTree t = (StateBinaryTree) symbol;
      if (state.equals(t.state)) {
        return (tree==null) ? t.tree==null : tree.possiblyMatches(t.tree, context);
      }
      else {
        return false;
      }
    }
    else {
      return (tree==null) ? symbol==null : tree.possiblyMatches(symbol, context);
    }
  }

  public void traverse(ISymbolVisitor visitor) {
    visitor.onVisit(this);
    if (tree!=null) {
      tree.traverse(visitor);
    }
    visitor.onLeave(this);
  }

  public ISymbol copy(ISymbolCopier copier) {
    ISymbol s = copier.copy(this);
    if (s instanceof StateBinaryTree) {
      StateBinaryTree t = (StateBinaryTree) s;
      if (tree!=null) {
        t.tree = (IBinaryTree) t.tree.copy(copier);
      }
      if (copier instanceof IStateCopier) {
        IStateCopier scopier = (IStateCopier) copier;
        t.state = t.state.copy(scopier);
      }
    }
    return s;
  }

  public IState copy(IStateCopier copier) {
    return (IState) copy((ISymbolCopier)copier);
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw(new RuntimeException(e));
    }
  }

  public int size() {
    return (tree==null) ? 0 : tree.size();
  }

  public String toString() {
    return state.toString() + "(" + tree + ")";
  }
}
