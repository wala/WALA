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

public class BinaryTree implements IParentBinaryTree {
  static protected class Leaf extends Symbol implements IBinaryTree {
    public Leaf(String name) {
      super(name);
    }
    public ISymbol getLabel() {
      return this;
    }
  }
  
  static public IBinaryTree LEAF = new Leaf("#");
  
  private ISymbol label;
  private IBinaryTree left;
  private IBinaryTree right;
  
  public BinaryTree(ISymbol label) {
    this(label, LEAF, LEAF);
  }
  
  public BinaryTree(String label) {
    this(new StringSymbol(label));
  }
  
  public BinaryTree(ISymbol label, IBinaryTree left, IBinaryTree right) {
    if (label == null) throw(new RuntimeException("should not be null."));
    this.label = label;
    setLeft(left);
    setRight(right);
  }
  
  public BinaryTree(String label, IBinaryTree left, IBinaryTree right) {
    this(new StringSymbol(label), left, right);
  }
  
  public ISymbol getLabel() {
    return label;
  }
  
  public IBinaryTree getLeft() {
    return left;
  }
  
  public IBinaryTree getRight() {
    return right;
  }
  
  public void setLeft(IBinaryTree tree) {
    if (tree == null){
      left = LEAF;
    }
    else {
      left = tree;
    }
  }
  
  public void setRight(IBinaryTree tree) {
    if (tree == null) {
      right = LEAF;
    }
    else {
      right = tree;
    }
  }
  
  public String getName() {
    return label.getName();
  }
  
  public int hashCode() {
    return label.hashCode();
    /*
     return label.hashCode()
     + left.hashCode()
     + right.hashCode();
     */
  }
  
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (!getClass().equals(obj.getClass())) return false;
    BinaryTree btree = (BinaryTree) obj;
    return label.equals(btree.getLabel())
    && left.equals(btree.getLeft())
    && right.equals(btree.getRight());
  }
  
  public boolean matches(ISymbol symbol, IMatchContext ctx) {
    if (!(symbol instanceof IParentBinaryTree)) {
      return false;
    }
    IParentBinaryTree tree = (IParentBinaryTree) symbol;
    if (label.matches(tree.getLabel(), ctx)
        && left.matches(tree.getLeft(), ctx)
        && right.matches(tree.getRight(), ctx) ) {
      ctx.put(this, symbol);
      return true;
    }
    else {
      return false;
    }
  }
  
  public boolean possiblyMatches(ISymbol symbol, IMatchContext ctx) {
    if (!(symbol instanceof IParentBinaryTree)) {
      return false;
    }
    IParentBinaryTree tree = (IParentBinaryTree) symbol;
    if (label.possiblyMatches(tree.getLabel(), ctx)
        && left.possiblyMatches(tree.getLeft(), ctx)
        && right.possiblyMatches(tree.getRight(), ctx) ) {
      ctx.put(this, symbol);
      return true;
    }
    else {
      return false;
    }
  }
  
  public void traverse(ISymbolVisitor visitor) {
    visitor.onVisit(this);
    label.traverse(visitor);
    left.traverse(visitor);
    right.traverse(visitor);
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
    IBinaryTreeCopier btCopier = null;
    if (copier instanceof IBinaryTreeCopier) {
      btCopier = (IBinaryTreeCopier) copier;
    }
    else {
      btCopier = new DeepBinaryTreeCopier(copier);
    }
    
    ISymbol s = btCopier.copy(this);
    if (s instanceof BinaryTree) {
      BinaryTree bt = (BinaryTree) s;
      bt.label = btCopier.copyLabel(bt, bt.label);
      bt.left = (IBinaryTree) btCopier.copySymbolReference(bt, bt.left);
      bt.right = (IBinaryTree) btCopier.copySymbolReference(bt, bt.right);
    }
    return s;
  }
  
  public int size() {
    return 2;
  }
  
  public String toString() {
    return label.toString() + "["
    + left.toString() + ", "
    + right.toString() + "]";
  }
  
}
