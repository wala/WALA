package com.ibm.wala.dataflow.graph;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.fixpoint.FixedPointConstants;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;

/**
 * Operator U(n) = U(n) n U(j)
 */
public final class BitVectorIntersection extends AbstractMeetOperator<BitVectorVariable> implements FixedPointConstants {

  private static final BitVectorIntersection INSTANCE = new BitVectorIntersection();

  public static BitVectorIntersection instance() {
    return INSTANCE;
  }
  
  private BitVectorIntersection() {
  }

  @Override
  public byte evaluate(final BitVectorVariable lhs, final BitVectorVariable[] rhs) {
    IntSet intersect = lhs.getValue();
    if (intersect == null || intersect.isEmpty()) {
      intersect = rhs[0].getValue();
    }

    for (final BitVectorVariable bv : rhs) {
      final IntSet vlhs = bv.getValue();
      intersect = intersect.intersection(vlhs);
    }

    if (lhs.getValue() != null && intersect.sameValue(lhs.getValue())) {
      return NOT_CHANGED;
    } else {
      final BitVectorVariable bvv = new BitVectorVariable();
      intersect.foreach(new IntSetAction() {
        @Override
        public void act(final int x) {
          bvv.set(x);
        }
      });
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