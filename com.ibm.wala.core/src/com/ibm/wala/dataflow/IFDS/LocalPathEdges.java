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

import java.util.Iterator;

import com.ibm.wala.util.collections.SparseVector;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IBinaryNaturalRelation;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntPair;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.SparseIntSet;

/**
 * A set of path edges for a particular procedure entry s_p.
 */
public class LocalPathEdges {

  /**
   * Do paranoid error checking? (slow)
   */
  private final static boolean PARANOID = false;

  /**
   * A map from integer (d2) -&gt; (IBinaryNonNegativeIntRelation)
   * 
   * For fact d2, paths[d2] gives a relation R=(n,d1) s.t. (&lt;s_p, d1&gt; -&gt; &lt;n,d2&gt;) is a path edge.
   * 
   * Note that we handle paths of the form &lt;s_p, d1&gt; -&gt; &lt;n,d1&gt; specially, below. We also handle paths of the form &lt;s_p, 0&gt; -&gt; &lt;n,
   * d1> specially below.
   * 
   * We choose this somewhat convoluted representation for the following reasons: 1) of the (n, d1, d2) tuple-space, we expect the
   * set of n to be dense for a given (d1,d2) pair. However the pairs should be sparse. So, we set up so n is the first dimension of
   * the int-relations, which are designed to be dense in the first dimension 2) we need to support getInverse(), so we design
   * lookup to get the d1's for an (n,d2) pair.
   * 
   * Note that this representation is not good for merges. See below.
   * 
   * TODO: more representation optimization. A special representation for triples? sparse representations for CFG? exploit shorts
   * for ints?
   */
  private final SparseVector<IBinaryNaturalRelation> paths = new SparseVector<>(1, 1.1f);

  /**
   * If this is non-null, it holds a redundant representation of the paths information, designed to make getReachable(II) faster.
   * This is designed for algorithms that want to use frequent merges. While it's a shame to waste space, I don't want to compromise
   * space or time of the non-merging IFDS solver, for which the original paths representation works well. Is there a better data
   * structure tradeoff?
   * 
   * A map from integer (d1) -&gt; (IBinaryNonNegativeIntRelation)
   * 
   * For fact d1, paths[d1] gives a relation R=(n,d2) s.t. (&lt;s_p, d1&gt; -&gt; &lt;n,d2&gt;) is a path edge.
   * 
   * 
   * We choose this somewhat convoluted representation for the following reasons: 1) of the (n, d1, d2) tuple-space, we expect the
   * set of n to be dense for a given (d1,d2) pair. However the pairs should be sparse. So, we set up so n is the first dimension of
   * the int-relations, which are designed to be dense in the first dimension 2) we need to support getReachable(), so we design
   * lookup to get the d2's for an (n,d1) pair.
   */
  private final SparseVector<IBinaryNaturalRelation> altPaths;

  /**
   * a map from integer d1 -&gt; int set.
   * 
   * for fact d1, identityPaths[d1] gives the set of block numbers N s.t. for n \in N, &lt;s_p, d1&gt; -&gt; &lt;n, d1&gt; is a path edge.
   */
  private final SparseVector<IntSet> identityPaths = new SparseVector<>(1, 1.1f);

  /**
   * a map from integer d2 -&gt; int set
   * 
   * for fact d2, zeroPaths[d2] gives the set of block numbers N s.t. for n \in N, &lt;s_p, 0&gt; -&gt; &lt;n, d2&gt; is a path edge.
   */
  private final SparseVector<IntSet> zeroPaths = new SparseVector<>(1, 1.1f);

  /**
   * @param fastMerge if true, the representation uses extra space in order to support faster merge operations
   */
  public LocalPathEdges(boolean fastMerge) {
    altPaths = fastMerge ? new SparseVector<>(1, 1.1f) : null;
  }

  /**
   * Record that in this procedure we've discovered a same-level realizable path from (s_p,d_i) to (n,d_j)
   * 
   * @param i
   * @param n local block number of the basic block n
   * 
   * @param j
   */
  @SuppressWarnings("unused")
  public void addPathEdge(int i, int n, int j) {

    if (i == 0) {
      addZeroPathEdge(n, j);
    } else {
      if (i == j) {
        addIdentityPathEdge(i, n);
      } else {
        IBinaryNaturalRelation R = paths.get(j);
        if (R == null) {
          // we expect the first dimension of R to be dense, the second sparse
          R = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY }, BasicNaturalRelation.TWO_LEVEL);
          paths.set(j, R);
        }
        R.add(n, i);

        if (altPaths != null) {
          IBinaryNaturalRelation R2 = altPaths.get(i);
          if (R2 == null) {
            // we expect the first dimension of R to be dense, the second sparse
            R2 = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY }, BasicNaturalRelation.TWO_LEVEL);
            altPaths.set(i, R2);
          }
          R2.add(n, j);
        }

        if (TabulationSolver.DEBUG_LEVEL > 1) {
          // System.err.println("recording path edge, now d2=" + j + " has been reached from " + R);
        }
      }
    }
  }

  /**
   * Record that in this procedure we've discovered a same-level realizable path from (s_p,i) to (n,i)
   * 
   * @param n local block number of the basic block n
   */
  @SuppressWarnings("unused")
  private void addIdentityPathEdge(int i, int n) {
    BitVectorIntSet s = (BitVectorIntSet) identityPaths.get(i);
    if (s == null) {
      s = new BitVectorIntSet();
      identityPaths.set(i, s);
    }
    s.add(n);

    if (altPaths != null) {
      IBinaryNaturalRelation R2 = altPaths.get(i);
      if (R2 == null) {
        // we expect the first dimension of R to be dense, the second sparse
        R2 = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY }, BasicNaturalRelation.TWO_LEVEL);
        altPaths.set(i, R2);
      }
      R2.add(n, i);
    }

    if (TabulationSolver.DEBUG_LEVEL > 1) {
      System.err.println("recording self-path edge, now d1= " + i + " reaches " + s);
    }
  }

  /**
   * Record that in this procedure we've discovered a same-level realizable path from (s_p,0) to (n,d_j)
   * 
   * @param n local block number of the basic block n
   * 
   * @param j
   */
  @SuppressWarnings("unused")
  private void addZeroPathEdge(int n, int j) {

    BitVectorIntSet z = (BitVectorIntSet) zeroPaths.get(j);
    if (z == null) {
      z = new BitVectorIntSet();
      zeroPaths.set(j, z);
    }
    z.add(n);
    if (altPaths != null) {
      IBinaryNaturalRelation R = altPaths.get(0);
      if (R == null) {
        // we expect the first dimension of R to be dense, the second sparse
        R = new BasicNaturalRelation(new byte[] { BasicNaturalRelation.SIMPLE_SPACE_STINGY }, BasicNaturalRelation.TWO_LEVEL);
        altPaths.set(0, R);
      }
      R.add(n, j);
    }
    if (TabulationSolver.DEBUG_LEVEL > 1) {
      System.err.println("recording 0-path edge, now d2= " + j + " reached at " + z);
    }
  }

  /**
   * N.B: If we're using the ZERO_PATH_SHORT_CIRCUIT, then we may have &lt;s_p, d1&gt; -&gt; &lt;n, d2&gt; implicitly represented since we also
   * have &lt;s_p, 0&gt; -&gt; &lt;n,d2&gt;. However, getInverse() &lt;b&gt; will NOT &lt;/b&gt; return these implicit d1 bits in the result. This translates
   * to saying that the caller had better not care about any other d1 other than d1==0 if d1==0 is present. This happens to be true
   * in the single use of getInverse() in the tabulation solver, which uses getInverse() to propagate flow from an exit node back to
   * the caller's return site(s). Since we know that we will see flow from fact 0 to the return sites(s), we don't care about other
   * facts that may induce the same flow to the return site(s).
   * 
   * @param n local block number of a basic block n
   * @param d2
   * @return the sparse int set of d1 s.t. {@literal <s_p, d1> -> <n, d2>} are recorded as path edges. null if none found
   */
  public IntSet getInverse(int n, int d2) {
    IBinaryNaturalRelation R = paths.get(d2);
    BitVectorIntSet s = (BitVectorIntSet) identityPaths.get(d2);
    BitVectorIntSet z = (BitVectorIntSet) zeroPaths.get(d2);
    if (R == null) {
      if (s == null) {
        if (z == null) {
          return null;
        } else {
          return z.contains(n) ? SparseIntSet.singleton(0) : null;
        }
      } else {
        if (s.contains(n)) {
          if (z == null) {
            return SparseIntSet.singleton(d2);
          } else {
            return z.contains(n) ? SparseIntSet.pair(0, d2) : SparseIntSet.singleton(d2);
          }
        } else {
          return null;
        }
      }
    } else {
      if (s == null) {
        if (z == null) {
          return R.getRelated(n);
        } else {
          if (z.contains(n)) {
            IntSet related = R.getRelated(n);
            if (related == null) {
              return SparseIntSet.singleton(0);
            } else {
              MutableSparseIntSet result = MutableSparseIntSet.make(related);
              result.add(0);
              return result;
            }
          } else {
            return R.getRelated(n);
          }
        }
      } else {
        if (s.contains(n)) {
          IntSet related = R.getRelated(n);
          if (related == null) {
            if (z == null || !z.contains(n)) {
              return SparseIntSet.singleton(d2);
            } else {
              return SparseIntSet.pair(0, d2);
            }
          } else {
            MutableSparseIntSet result = MutableSparseIntSet.make(related);
            result.add(d2);
            if (z != null && z.contains(n)) {
              result.add(0);
            }
            return result;
          }
        } else {
          if (z == null || !z.contains(n)) {
            return R.getRelated(n);
          } else {
            IntSet related = R.getRelated(n);
            MutableSparseIntSet result = (related == null) ? MutableSparseIntSet.makeEmpty() : MutableSparseIntSet.make(related);
            result.add(0);
            return result;
          }
        }
      }
    }
  }

  /**
   * @param i
   * @param n local block number of a basic block n
   * @param j
   * @return true iff we have a path edge {@literal <s_p,i> -> <n, j>}
   */
  public boolean contains(int i, int n, int j) {

    if (n < 0) {
      throw new IllegalArgumentException("invalid n: " + n);
    }
    if (i == 0) {
      BitVectorIntSet z = (BitVectorIntSet) zeroPaths.get(j);
      if (z != null && z.contains(n)) {
        return true;
      } else {
        return false;
      }
    } else {
      if (i == j) {
        BitVectorIntSet s = (BitVectorIntSet) identityPaths.get(i);
        if (s != null && s.contains(n)) {
          return true;
        } else {
          return false;
        }
      } else {
        IBinaryNaturalRelation R = paths.get(j);
        if (R == null) {
          return false;
        }
        return R.contains(n, i);
      }
    }
  }

  /**
   * 
   * @param n
   * @return set of d2 s.t. d1 -&gt; d2 is a path edge for node n.
   */
  public IntSet getReachable(int n, int d1) {
    if (PARANOID) {
      assert getReachableSlow(n, d1).sameValue(getReachableFast(n, d1));
    }
    return (altPaths == null) ? getReachableSlow(n, d1) : getReachableFast(n, d1);
  }

  /**
   * Note that this is really slow!!!
   * 
   * @return set of d2 s.t. d1 -&gt; d2 is a path edge for node n
   */
  private IntSet getReachableSlow(int n, int d1) {
    MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
    if (paths.size() > 0) {
      // this is convoluted on purpose for efficiency: to avoid random access to
      // the sparse vector, we do parallel iteration with the vector's indices
      // and contents. TODO: better data structure?
      Iterator<IBinaryNaturalRelation> contents = paths.iterator();
      for (IntIterator it = paths.iterateIndices(); it.hasNext();) {
        int d2 = it.next();
        IBinaryNaturalRelation R = contents.next();
        if (R != null && R.contains(n, d1)) {
          result.add(d2);
        }
      }
    }
    if (identityPaths.size() > 0) {
      BitVectorIntSet s = (BitVectorIntSet) identityPaths.get(d1);
      if (s != null && s.contains(n)) {
        result.add(d1);
      }
    }
    if (d1 == 0 && zeroPaths.size() > 0) {
      // this is convoluted on purpose for efficiency: to avoid random access to
      // the sparse vector, we do parallel iteration with the vector's indices
      // and contents. TODO: better data structure?
      Iterator<IntSet> contents = zeroPaths.iterator();
      for (IntIterator it = zeroPaths.iterateIndices(); it.hasNext();) {
        int d2 = it.next();
        IntSet s = contents.next();
        if (s != null && s.contains(n)) {
          result.add(d2);
        }
      }
    }
    return result;
  }

  /**
   * @return set of d2 s.t. d1 -&gt; d2 is a path edge for node n
   */
  private IntSet getReachableFast(int n, int d1) {

    IBinaryNaturalRelation R = altPaths.get(d1);
    if (R != null) {
      return R.getRelated(n);
    }
    return null;
  }

  /**
   * TODO: optimize this based on altPaths
   * 
   * @param n the local block number of a node
   * @return set of d2 s.t \exists d1 s.t. d1 -&gt; d2 is a path edge for node n
   */
  public IntSet getReachable(int n) {
    MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
    if (paths.size() > 0) {
      // this is convoluted on purpose for efficiency: to avoid random access to
      // the sparse vector, we do parallel iteration with the vector's indices
      // and contents. TODO: better data structure?
      Iterator<IBinaryNaturalRelation> contents = paths.iterator();
      for (IntIterator it = paths.iterateIndices(); it.hasNext();) {
        int d2 = it.next();
        IBinaryNaturalRelation R = contents.next();
        if (R != null && R.anyRelated(n)) {
          result.add(d2);
        }
      }
    }
    if (identityPaths.size() > 0) {
      // this is convoluted on purpose for efficiency: to avoid random access to
      // the sparse vector, we do parallel iteration with the vector's indices
      // and contents. TODO: better data structure?
      Iterator<IntSet> contents = identityPaths.iterator();
      for (IntIterator it = identityPaths.iterateIndices(); it.hasNext();) {
        int d1 = it.next();
        IntSet s = contents.next();
        if (s != null && s.contains(n)) {
          result.add(d1);
        }
      }
    }
    if (zeroPaths.size() > 0) {
      // this is convoluted on purpose for efficiency: to avoid random access to
      // the sparse vector, we do parallel iteration with the vector's indices
      // and contents. TODO: better data structure?
      Iterator<IntSet> contents = zeroPaths.iterator();
      for (IntIterator it = zeroPaths.iterateIndices(); it.hasNext();) {
        int d2 = it.next();
        IntSet s = contents.next();
        if (s != null && s.contains(n)) {
          result.add(d2);
        }
      }
    }
    return result;
  }

  /**
   * TODO: optimize this
   * 
   * @return set of node numbers that are reached by any fact
   */
  public IntSet getReachedNodeNumbers() {
    MutableSparseIntSet result = MutableSparseIntSet.makeEmpty();
    if (paths.size() > 0) {
      for (IBinaryNaturalRelation R : paths) {
        for (IntPair p : R) {
          result.add(p.getX());
        }
      }
    }
    if (identityPaths.size() > 0) {
      for (IntSet s : identityPaths) {
        result.addAll(s);
      }
    }
    if (zeroPaths.size() > 0) {
      for (IntSet s : zeroPaths) {
        result.addAll(s);
      }
    }
    return result;
  }
}
