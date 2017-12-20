/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.collections.SparseVector;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.BimodalMutableIntSet;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * A set of call flow edges which lead to a particular procedure entry s_p.
 */
public class CallFlowEdges {

  /**
   * A map from integer -&gt; (IBinaryNonNegativeIntRelation)
   * 
   * For a fact d2, edges[d2] gives a relation R=(c,d1) s.t. (&lt;c, d1&gt; -&gt; &lt;s_p,d2&gt;) was recorded as a call flow edge.
   * 
   * Note that we handle paths of the form &lt;c, d1&gt; -&gt; &lt;s_p,d1&gt; specially, below.
   * 
   * TODO: more representation optimization. A special representation for triples? sparse representations for CFG? exploit shorts
   * for ints?
   */
  private final SparseVector<IBinaryNaturalRelation> edges = new SparseVector<>(1, 1.1f);

  /**
   * a map from integer d1 -&gt; int set.
   * 
   * for fact d1, identityPaths[d1] gives the set of block numbers C s.t. for c \in C, &lt;c, d1&gt; -&gt; &lt;s_p, d1&gt; is an edge.
   */
  private final SparseVector<IntSet> identityEdges = new SparseVector<>(1, 1.1f);

  public CallFlowEdges() {
  }

  /**
   * Record that we've discovered a call edge &lt;c,d1&gt; -&gt; &lt;s_p, d2&gt;
   * 
   * @param c global number identifying the call site node
   * @param d1 source fact at the call edge
   * @param d2 result fact (result of the call flow function)
   */
  @SuppressWarnings("unused")
  public void addCallEdge(int c, int d1, int d2) {
    if (TabulationSolver.DEBUG_LEVEL > 0) {
      System.err.println("addCallEdge " + c + " " + d1 + " " + d2);
    }
    if (d1 == d2) {
      BimodalMutableIntSet s = (BimodalMutableIntSet) identityEdges.get(d1);
      if (s == null) {
        s = new BimodalMutableIntSet();
        identityEdges.set(d1, s);
      }
      s.add(c);
    } else {
      IBinaryNaturalRelation R = edges.get(d2);
      if (R == null) {
        // we expect the first dimension of R to be dense, the second sparse
        R = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.TWO_LEVEL }, BasicNaturalRelation.TWO_LEVEL);
        edges.set(d2, R);
      }
      R.add(c, d1);
    }
  }

  /**
   * @param c
   * @param d2
   * @return set of d1 s.t. {@literal <c, d1> -> <s_p, d2>} was recorded as call flow, or null if none found.
   */
  @SuppressWarnings("unused")
  public IntSet getCallFlowSources(int c, int d2) {
    if (c < 0) {
      throw new IllegalArgumentException("invalid c : " + c);
    }
    if (d2 < 0) {
      throw new IllegalArgumentException("invalid d2: " + d2);
    }
    IntSet s = identityEdges.get(d2);
    IBinaryNaturalRelation R = edges.get(d2);
    IntSet result = null;
    if (R == null) {
      if (s != null) {
        result = s.contains(c) ? SparseIntSet.singleton(d2) : null;
      }
    } else {
      if (s == null) {
        result = R.getRelated(c);
      } else {
        if (s.contains(c)) {
          if (R.getRelated(c) == null) {
            result = SparseIntSet.singleton(d2);
          } else {
            result = MutableSparseIntSet.make(R.getRelated(c));
            ((MutableSparseIntSet) result).add(d2);
          }
        } else {
          result = R.getRelated(c);
        }
      }
    }
    if (TabulationSolver.DEBUG_LEVEL > 0) {
      System.err.println("getCallFlowSources " + c + " " + d2 + " " + result);
    }
    return result;
  }

  /**
   * 
   * @param d2
   * @return set of c s.t. {@literal <c, d1> -> <s_p, d2>} was recorded as call flow (for some d1), or null if none found.
   */
  @SuppressWarnings("unused")
  public IntSet getCallFlowSourceNodes(int d2) {
    IntSet s = identityEdges.get(d2);
    IBinaryNaturalRelation R = edges.get(d2);
    IntSet result = null;
    if (R == null) {
      if (s != null) {
        result = s;
      }
    } else {
      if (s == null) {
        result = getDomain(R);
      } else {
        result = MutableSparseIntSet.make(s);
        ((MutableSparseIntSet) result).addAll(getDomain(R));
      }
    }
    if (TabulationSolver.DEBUG_LEVEL > 0) {
      System.err.println("getCallFlowSources " + d2 + " " + result);
    }
    return result;

  }

  // TODO optimize
  private static IntSet getDomain(IBinaryNaturalRelation r) {
    MutableIntSet result = MutableSparseIntSet.makeEmpty();
    int maxKeyValue = r.maxKeyValue();
    for (int i = 0; i <= maxKeyValue; i++) {
      if (r.getRelated(i) != null) {
        result.add(i);
      }
    }
    return result;
  }

}
