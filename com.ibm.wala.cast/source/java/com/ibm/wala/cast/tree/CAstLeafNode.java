package com.ibm.wala.cast.tree;

import java.util.Collections;
import java.util.List;

/** Convenience interface for implementing an AST node with no children */
public interface CAstLeafNode extends CAstNode {

  @Override
  default List<CAstNode> getChildren() {
    return Collections.emptyList();
  }
}
