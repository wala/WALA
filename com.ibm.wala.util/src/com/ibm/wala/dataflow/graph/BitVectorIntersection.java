/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.util.intset.IntSet;

/**
 * Operator U(n) = U(n) n U(j)
 */
public final class BitVectorIntersection extends AbstractMeetOperator<BitVectorVariable> {

  private static final BitVectorIntersection INSTANCE = new BitVectorIntersection();

  public static BitVectorIntersection instance() {
    return INSTANCE;
  }
  
  private BitVectorIntersection() {
  }

  @Override
  public byte evaluate(final BitVectorVariable lhs, final BitVectorVariable[] rhs) {
    // as null is the initial value, we treat null as the neutral element to intersection
    // which is a set of all possible elements.
    IntSet intersect = lhs.getValue();
    if (intersect == null) {
      for (BitVectorVariable r : rhs) {
        intersect = r.getValue();
        if (intersect != null) { break; }
      }
      
      if (intersect == null) {
        // still null - so all rhs is null -> no change
        return NOT_CHANGED;
      }
    } else if (intersect.isEmpty()) {
      return NOT_CHANGED_AND_FIXED;
    }
    
    for (final BitVectorVariable bv : rhs) {
      final IntSet vlhs = bv.getValue();
      if (vlhs != null) {
        intersect = intersect.intersection(vlhs);
      }
    }

    if (lhs.getValue() != null && intersect.sameValue(lhs.getValue())) {
      return NOT_CHANGED;
    } else {
      final BitVectorVariable bvv = new BitVectorVariable();
      intersect.foreach(bvv::set);
      lhs.copyState(bvv);

      return CHANGED;
    }
  }

  @Override
  public int hashCode() {
    return 9903;
  }

  @Override
  public boolean equals(final Object o) {
    return o instanceof BitVectorIntersection;
  }

  @Override
  public String toString() {
    return "INTERSECTION";
  }

}
