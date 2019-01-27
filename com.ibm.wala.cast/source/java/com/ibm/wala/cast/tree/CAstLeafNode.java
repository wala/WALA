package com.ibm.wala.cast.tree;

import java.util.NoSuchElementException;

/**
 * Convenience interface for implementing an AST node with no children
 */
public interface CAstLeafNode extends CAstNode {

    @Override
    default CAstNode getChild(int n) {
        throw new NoSuchElementException("leaf AST node has no children");
    }

    @Override
    default int getChildCount() {
        return 0;
    }

}
