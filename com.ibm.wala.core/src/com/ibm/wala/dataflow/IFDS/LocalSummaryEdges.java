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
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.SparseLongIntVector;
import com.ibm.wala.util.math.LongUtil;

/**
 * A set of summary edges for a particular procedure.
 */
public class LocalSummaryEdges {

  /**
   * A map from integer n -&gt; (IBinaryNonNegativeIntRelation)
   * 
   * Let s_p be an entry to this procedure, and x be an exit. n is a integer which uniquely identifies an (s_p,x) relation. For any
   * such n, summaries[n] gives a relation R=(d1,d2) s.t. (&lt;s_p, d1&gt; -&gt; &lt;x,d2&gt;) is a summary edge.
   * 
   * Note that this representation is a little different from the representation described in the PoPL 95 paper. We cache summary
   * edges at the CALLEE, not at the CALLER!!! This allows us to avoid eagerly installing summary edges at all call sites to a
   * procedure, which may be a win.
   * 
   * we don't technically need this class, since this information is redundantly stored in LocalPathEdges. However, we're keeping it
   * cached for now for more efficient access when looking up summary edges.
   * 
   * TODO: more representation optimization.
   */
  private final SparseVector<IBinaryNaturalRelation> summaries = new SparseVector<>(1, 1.1f);

  /**
   * Let (s_p,x) be an entry-exit pair, and let l := the long whose high word is s_p and low word is x.
   * 
   * Then entryExitMap(l) is an int which uniquely identifies (s_p,x)
   * 
   * we populate this map on demand!
   */
  private final static int UNASSIGNED = -1;

  private final SparseLongIntVector entryExitMap = new SparseLongIntVector(UNASSIGNED);

  private int nextEntryExitIndex = 0;

  /**
   * 
   */
  public LocalSummaryEdges() {
  }

  /**
   * Record a summary edge for the flow d1 -&gt; d2 from an entry s_p to an exit x.
   * 
   * @param s_p local block number an entry
   * @param x local block number of an exit block
   * @param d1 source dataflow fact
   * @param d2 target dataflow fact
   */
  public void insertSummaryEdge(int s_p, int x, int d1, int d2) {
    int n = getIndexForEntryExitPair(s_p, x);
    IBinaryNaturalRelation R = summaries.get(n);
    if (R == null) {
      // we expect R to usually be sparse
      R = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY }, BasicNaturalRelation.SIMPLE);
      summaries.set(n, R);
    }
    R.add(d1, d2);
//    if (TabulationSolver.DEBUG_LEVEL > 1) {
//      // System.err.println("recording summary edge, now n=" + n + " summarized by " + R);
//    }
  }

  /**
   * Does a particular summary edge exist?
   * 
   * @param s_p local block number an entry
   * @param x local block number of an exit block
   * @param d1 source dataflow fact
   * @param d2 target dataflow fact
   */
  public boolean contains(int s_p, int x, int d1, int d2) {
    int n = getIndexForEntryExitPair(s_p, x);
    IBinaryNaturalRelation R = summaries.get(n);
    if (R == null) {
      return false;
    } else {
      return R.contains(d1, d2);
    }
  }

  /**
   * @param s_p local block number an entry
   * @param x local block number of an exit block
   * @param d1 source dataflow fact
   * @return set of d2 s.t. d1 -&gt; d2 recorded as a summary edge for (s_p,x), or null if none
   */
  public IntSet getSummaryEdges(int s_p, int x, int d1) {
    int n = getIndexForEntryExitPair(s_p, x);
    IBinaryNaturalRelation R = summaries.get(n);
    if (R == null) {
      return null;
    } else {
      return R.getRelated(d1);
    }
  }

  /**
   * Note: This is inefficient. Use with care.
   * 
   * @param s_p local block number an entry
   * @param x local block number of an exit block
   * @param d2 target dataflow fact
   * @return set of d1 s.t. d1 -&gt; d2 recorded as a summary edge for (s_p,x), or null if none
   */
  public IntSet getInvertedSummaryEdgesForTarget(int s_p, int x, int d2) {
    int n = getIndexForEntryExitPair(s_p, x);
    IBinaryNaturalRelation R = summaries.get(n);
    if (R == null) {
      return null;
    } else {
      MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
      for (IntPair p : R) {
        if (p.getY() == d2) {
          result.add(p.getX());
        }
      }
      return result;
    }
  }

  /**
   * @return unique id n that represents the pair (s_p,x)
   */
  private int getIndexForEntryExitPair(int c, int r) {
    long id = LongUtil.pack(c, r);
    int result = entryExitMap.get(id);
    if (result == UNASSIGNED) {
      result = nextEntryExitIndex++;
      entryExitMap.set(id, result);
    }
    return result;
  }

}
