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
package com.ibm.wala.cast.util;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.graph.traverse.DFSDiscoverTimeIterator;

public class CAstFunctions {

  public static CAstNode findIf(CAstNode tree, Predicate<CAstNode> f) {
    if (f.test(tree)) {
      return tree;
    } else {
      for (int i = 0; i < tree.getChildCount(); i++) {
        CAstNode result = findIf(tree.getChild(i), f);
        if (result != null) {
          return result;
        }
      }
    }

    return null;
  }

  public static Iterator<CAstNode> iterateNodes(final CAstNode tree) {
    return new DFSDiscoverTimeIterator<CAstNode>() {

      private static final long serialVersionUID = -627203481092871529L;
      private final Map<Object, Iterator<? extends CAstNode>> pendingChildren = HashMapFactory.make();

      @Override
      protected Iterator<? extends CAstNode> getPendingChildren(CAstNode n) {
        return pendingChildren.get(n);
      }

      @Override
      protected void setPendingChildren(CAstNode v, Iterator<? extends CAstNode> iterator) {
        pendingChildren.put(v, iterator);
      }

      @Override
      protected Iterator<CAstNode> getConnected(final CAstNode n) {
        return new Iterator<CAstNode>() {
          private int i = 0;

          @Override
          public boolean hasNext() {
            return i < n.getChildCount();
          }

          @Override
          public CAstNode next() {
            return n.getChild(i++);
          }

          @Override
          public void remove() {
            throw new UnsupportedOperationException();
          }
        };
      }

      {
        init(tree);
      }
    };
  }

  public static Iterator<CAstNode> findAll(CAstNode tree, Predicate<? super CAstNode> f) {
    return new FilterIterator<>(iterateNodes(tree), f);
  }

}
