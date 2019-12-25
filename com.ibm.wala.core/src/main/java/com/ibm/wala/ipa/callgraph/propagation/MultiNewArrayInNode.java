/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;

/** An {@link InstanceKey} which represents a multinewarray allocation site in a {@link CGNode}. */
public final class MultiNewArrayInNode extends AllocationSiteInNode {
  private final int dim;

  /**
   * @return null if the element type is a primitive
   * @throws IllegalArgumentException if T == null
   */
  private static IClass myElementType(ArrayClass T, int d)
      throws IllegalArgumentException, IllegalArgumentException {
    if (T == null) {
      throw new IllegalArgumentException("T == null");
    }
    if (d == 0) {
      return T.getElementClass();
    } else {
      ArrayClass element = (ArrayClass) T.getElementClass();
      if (element == null) {
        return null;
      } else {
        return myElementType(element, d - 1);
      }
    }
  }

  public MultiNewArrayInNode(CGNode node, NewSiteReference allocation, ArrayClass type, int dim) {
    super(node, allocation, myElementType(type, dim));
    this.dim = dim;
  }

  @Override
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final
    if (obj instanceof MultiNewArrayInNode) {
      MultiNewArrayInNode other = (MultiNewArrayInNode) obj;
      return (dim == other.dim
          && getNode().equals(other.getNode())
          && getSite().equals(other.getSite()));
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 9967 * dim + getNode().hashCode() * 8647 * getSite().hashCode();
  }

  @Override
  public String toString() {
    return super.toString() + "<dim:" + dim + '>';
  }

  public int getDim() {
    return dim;
  }
}
