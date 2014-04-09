package com.ibm.wala.cast.tree.pattern;

import com.ibm.wala.cast.tree.CAstNode;

/**
 * Pattern to match one of two alternatives.
 * 
 * @author mschaefer
 *
 */
public class Alt implements NodePattern {
  private final NodePattern left, right;
  
  public Alt(NodePattern left, NodePattern right) {
    this.left = left;
    this.right = right;
  }

  public boolean matches(CAstNode node) {
    return left.matches(node) || right.matches(node);
  }

}
