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

import java.util.*;

import com.ibm.wala.automaton.string.*;

public class Tree implements IParentTree {
  // private List<ITree> children;
  private List children;
  private ISymbol label;

  public Tree(ISymbol label, List children) {
    this.label = label;
    if (children == null) {
      this.children = new ArrayList();
    }
    else{
      this.children = new ArrayList(children);
    }
  }

  public Tree(String label, List children) {
    this(new StringSymbol(label), children);
  }

  public Tree(ISymbol label, ITree children[]) {
    this(label);
    for (int i = 0; i < children.length; i++ ){
      addChild(children[i]);
    }
  }

  public Tree(String label, ITree children[]) {
    this(new StringSymbol(label), children);
  }

  public Tree(ISymbol label){
    this(label, (List) null);
  }

  public Tree(String label) {
    this(new StringSymbol(label));
  }

  public List getChildren() {
    return children;
  }

  public ISymbol getLabel() {
    return label;
  }

  public int hashCode() {
    return label.hashCode();
    /* return label.hashCode() + children.hashCode(); */
  }

  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (!getClass().equals(obj.getClass())) return false;
    Tree tree = (Tree) obj;
    if (!label.equals(tree.getLabel())) {
      return false;
    }
    for (int i = 0; i < children.size(); i++ ) {
      ITree child1 = getChild(i);
      ITree child2 = tree.getChild(i);
      if (!child1.equals(child2)) {
        return false;
      }
    }
    return true;
  }

  public boolean matches(ISymbol symbol, IMatchContext ctx) {
    if (!(symbol instanceof IParentTree)) {
      return false;
    }
    IParentTree tree = (IParentTree) symbol;
    if (!label.matches(tree.getLabel(), ctx)) {
      return false;
    }
    for (int i = 0; i < size(); i++ ) {
      ITree child1 = getChild(i);
      if (i < tree.size()) {
        ITree child2 = tree.getChild(i);
        if (!child1.matches(child2, ctx)) {
          return false;
        }
      }
      else{
        if (!child1.matches(null, ctx)) {
          return false;
        }
      }
    }
    ctx.put(this, symbol);
    return true;
  }

  public boolean possiblyMatches(ISymbol symbol, IMatchContext ctx) {
    if (!(symbol instanceof IParentTree)) {
      return false;
    }
    IParentTree tree = (IParentTree) symbol;
    if (!label.possiblyMatches(tree.getLabel(), ctx)) {
      return false;
    }
    for (int i = 0; i < size(); i++ ) {
      ITree child1 = getChild(i);
      if (i < tree.size()) {
        ITree child2 = tree.getChild(i);
        if (!child1.possiblyMatches(child2, ctx)) {
          return false;
        }
      }
      else{
        if (!child1.possiblyMatches(null, ctx)) {
          return false;
        }
      }
    }
    ctx.put(this, symbol);
    return true;
  }

  public void traverse(ISymbolVisitor visitor) {
    visitor.onVisit(this);
    label.traverse(visitor);
    for (Iterator i = children.iterator(); i.hasNext(); ) {
      ITree child = (ITree) i.next();
      child.traverse(visitor);
    }
    visitor.onLeave(this);
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw(new RuntimeException(e));
    }
  }

  public ISymbol copy(ISymbolCopier copier) {
    ISymbol s = copier.copy(this);
    if (s instanceof Tree) {
      Tree t = (Tree) s;
      t.label = copier.copySymbolReference(t, t.label);
      t.children = new ArrayList((List) copier.copySymbolReferences(t, t.children));
    }
    return s;
  }

  public int size() {
    return children.size();
  }

  public ITree getChild(int index){
    return (ITree) children.get(index);
  }

  public void addChild(ITree symbol) {
    children.add(symbol);
  }

  public void addChildren(Collection children) {
    for (Iterator i = children.iterator(); i.hasNext(); ){
      addChild((ITree)i.next());
    }
  }

  public void clearChildren() {
    children.clear();
  }

  public String getName() {
    return label.getName();
  }

  public String toString() {
    StringBuffer buff = new StringBuffer();
    for (Iterator i = children.iterator(); i.hasNext(); ) {
      ITree t = (ITree) i.next();
      buff.append((t==null) ? "#null" : t.toString());
      if (i.hasNext()) {
        buff.append(", ");
      }
    }
    return label.toString() + "[" + buff.toString() + "]";
  }
}
