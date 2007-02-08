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

import com.ibm.wala.cast.tree.*;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.graph.traverse.*;

import java.util.*;

public class CAstFunctions {

  public static CAstNode findIf(CAstNode tree, Filter f) {
    if (f.accepts(tree)) {
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

  public static Iterator iterateNodes(final CAstNode tree) {
    return new DFSDiscoverTimeIterator() {

      private final Map<Object, Iterator> pendingChildren = new HashMap<Object, Iterator>();

      protected Iterator getPendingChildren(Object n) {
        return pendingChildren.get(n);
      }

      protected void setPendingChildren(Object v, Iterator iterator) {
        pendingChildren.put(v, iterator);
      }

      protected Iterator getConnected(final Object n) {
        return new Iterator() {
          private int i = 0;

          public boolean hasNext() {
            return i < ((CAstNode) n).getChildCount();
          }

          public Object next() {
            return ((CAstNode) n).getChild(i++);
          }

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

  public static Iterator findAll(CAstNode tree, Filter f) {
    return new FilterIterator(iterateNodes(tree), f);
  }

}
