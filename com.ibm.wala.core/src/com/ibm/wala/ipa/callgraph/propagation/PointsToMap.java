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
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Iterator;

import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.IVector;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntegerUnionFind;
import com.ibm.wala.util.intset.MutableMapping;

/**
 * An object that tracks the mapping between pointer keys and points-to set variables
 */
public class PointsToMap {

  /**
   * An object that manages the numbering of pointer keys
   */
  private final MutableMapping<PointerKey> pointerKeys = MutableMapping.make();

  /**
   * pointsToSets[i] says something about the representation of the points-to set for the ith {@link PointerKey}, as determined by
   * the pointerKeys mapping. pointsToSets[i] can be one of the following:
   * <ul>
   * <li>a PointsToSetVariable
   * <li>IMPLICIT
   * <li>UNIFIED
   * </ul>
   */
  private final IVector<Object> pointsToSets = new SimpleVector<>();

  private final IntegerUnionFind uf = new IntegerUnionFind();

  /**
   * A hack: used to represent points-to-sets that are represented implicitly
   */
  final static Object IMPLICIT = new Object() {
    @Override
    public String toString() {
      return "IMPLICIT points-to set";
    }
  };

  /**
   * A hack: used to represent points-to-sets that are unified with another
   */
  final static Object UNIFIED = new Object() {
    @Override
    public String toString() {
      return "UNIFIED points-to set";
    }
  };

  /**
   * Numbers of pointer keys (non locals) that are roots of transitive closure. A "root" is a points-to-set whose contents do not
   * result from flow from other points-to-sets; these points-to-sets are the primordial assignments from which the transitive
   * closure flows.
   */
  private final BitVector transitiveRoots = new BitVector();

  /**
   * @return iterator of all PointerKeys tracked
   */
  public Iterator<PointerKey> iterateKeys() {
    return pointerKeys.iterator();
  }

  /**
   * If p is unified, returns the representative for p.
   */
  public PointsToSetVariable getPointsToSet(PointerKey p) {
    if (p == null) {
      throw new IllegalArgumentException("null p");
    }
    if (isImplicit(p)) {
      throw new IllegalArgumentException("unexpected: shouldn't ask a PointsToMap for an implicit points-to-set: " + p);
    }
    int i = pointerKeys.getMappedIndex(p);
    if (i == -1) {
      return null;
    }
    int repI = uf.find(i);
    PointsToSetVariable result = (PointsToSetVariable) pointsToSets.get(repI);
    if (result != null && p instanceof FilteredPointerKey && (!(result.getPointerKey() instanceof FilteredPointerKey))) {
      upgradeToFilter(result, ((FilteredPointerKey) p).getTypeFilter());
    }
    return result;
  }

  /**
   * @return the {@link PointsToSetVariable} recorded for a particular id
   */
  public PointsToSetVariable getPointsToSet(int id) {
    int repI = uf.find(id);
    return (PointsToSetVariable) pointsToSets.get(repI);
  }

  /**
   * record that a particular points-to-set is represented implicitly
   */
  public void recordImplicit(PointerKey key) {
    if (key == null) {
      throw new IllegalArgumentException("null key");
    }
    int i = findOrCreateIndex(key);
    pointsToSets.set(i, IMPLICIT);
  }

  public void put(PointerKey key, PointsToSetVariable v) {
    int i = findOrCreateIndex(key);
    pointsToSets.set(i, v);
  }

  private int findOrCreateIndex(PointerKey key) {
    int result = pointerKeys.getMappedIndex(key);
    if (result == -1) {
      result = pointerKeys.add(key);
    }
    return result;
  }

  /**
   * record that a particular points-to-set has been unioned with another
   */
  public void recordUnified(PointerKey key) {
    if (key == null) {
      throw new IllegalArgumentException("null key");
    }
    int i = findOrCreateIndex(key);
    pointsToSets.set(i, UNIFIED);
  }

  /**
   * record points-to-sets that are "roots" of the transitive closure. These points-to-sets can't be thrown away for a
   * pre-transitive solver. A "root" is a points-to-set whose contents do not result from flow from other points-to-sets; there
   * points-to-sets are the primordial assignments from which the transitive closure flows.
   */
  public void recordTransitiveRoot(PointerKey key) {
    if (key == null) {
      throw new IllegalArgumentException("null key");
    }
    int i = findOrCreateIndex(key);
    transitiveRoots.set(i);
  }

  /**
   * A "root" is a points-to-set whose contents do not result from flow from other points-to-sets; there points-to-sets are the
   * primordial assignments from which the transitive closure flows.
   */
  boolean isTransitiveRoot(PointerKey key) {
    int i = findOrCreateIndex(key);
    return transitiveRoots.get(i);
  }

  public boolean isUnified(PointerKey p) {
    if (p == null) {
      throw new IllegalArgumentException("null p");
    }
    int i = findOrCreateIndex(p);
    return pointsToSets.get(i) == UNIFIED;
  }

  public boolean isImplicit(PointerKey p) {
    int i = getIndex(p);
    return i != -1 && pointsToSets.get(i) == IMPLICIT;
  }

  protected int getNumberOfPointerKeys() {
    return pointerKeys.getSize();
  }

  /**
   * Wipe out the cached transitive closure information
   */
  public void revertToPreTransitive() {
    for (PointerKey key : Iterator2Iterable.make(iterateKeys())) {
      if (!isTransitiveRoot(key) && !isImplicit(key) && !isUnified(key)) {
        PointsToSetVariable v = getPointsToSet(key);
        v.removeAll();
      }
    }
  }

  /**
   * @return {@link Iterator}&lt;{@link PointerKey}&gt;
   */
  public Iterator<PointerKey> getTransitiveRoots() {
    return new FilterIterator<>(iterateKeys(), this::isTransitiveRoot);
  }

  /**
   * Unify the points-to-sets for the variables identified by the set s
   * 
   * @param s numbers of points-to-set variables
   * @throws IllegalArgumentException if s is null
   */
  public void unify(IntSet s) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    if (s.size() <= 1) {
      throw new IllegalArgumentException("Can't unify set of size " + s.size());
    }
    IntIterator it = s.intIterator();
    int i = it.next();
    while (it.hasNext()) {
      unify(i, it.next());
    }
  }

  /**
   * Unify the points-to-sets for the variables with numbers i and j
   */
  public void unify(int i, int j) {
    int repI = uf.find(i);
    int repJ = uf.find(j);
    if (repI != repJ) {
      PointsToSetVariable pi = (PointsToSetVariable) pointsToSets.get(repI);
      PointsToSetVariable pj = (PointsToSetVariable) pointsToSets.get(repJ);
      if (pi == null) {
        throw new IllegalArgumentException("No PointsToSetVariable for i: " + i);
      }
      if (pj == null) {
        throw new IllegalArgumentException("No PointsToSetVariable for j: " + j);
      }
      uf.union(repI, repJ);
      int rep = uf.find(repI);
      PointsToSetVariable p = (PointsToSetVariable) pointsToSets.get(rep);
      if (pi.getValue() != null) {
        p.addAll(pi.getValue());
      }
      if (pj.getValue() != null) {
        p.addAll(pj.getValue());
      }
      if (p != pi) {
        recordUnified(pi.getPointerKey());
        upgradeTypeFilter(pi, p);
      }
      if (p != pj) {
        recordUnified(pj.getPointerKey());
        upgradeTypeFilter(pj, p);
      }
      if (isTransitiveRoot(pi.getPointerKey()) || isTransitiveRoot(pj.getPointerKey())) {
        recordTransitiveRoot(p.getPointerKey());
      }
    }
  }

  private void upgradeTypeFilter(PointsToSetVariable src, PointsToSetVariable dest) {
    if (src.getPointerKey() instanceof FilteredPointerKey) {
      FilteredPointerKey fpk = (FilteredPointerKey) src.getPointerKey();
      if (dest.getPointerKey() instanceof FilteredPointerKey) {
        FilteredPointerKey fp = (FilteredPointerKey) dest.getPointerKey();
        if (!fp.getTypeFilter().equals(fpk.getTypeFilter())) {
          Assertions.UNREACHABLE("src " + fpk.getTypeFilter() + " dest " + fp.getTypeFilter());
        }
      } else {
        upgradeToFilter(dest, fpk.getTypeFilter());
      }
    }
  }

  private void upgradeToFilter(PointsToSetVariable p, FilteredPointerKey.TypeFilter typeFilter) {
    if (p.getPointerKey() instanceof LocalPointerKey) {
      LocalPointerKey lpk = (LocalPointerKey) p.getPointerKey();
      LocalPointerKeyWithFilter f = new LocalPointerKeyWithFilter(lpk.getNode(), lpk.getValueNumber(), typeFilter);
      p.setPointerKey(f);
      pointerKeys.replace(lpk, f);
    } else if (p.getPointerKey() instanceof ReturnValueKey) {
      ReturnValueKey r = (ReturnValueKey) p.getPointerKey();
      ReturnValueKeyWithFilter f = new ReturnValueKeyWithFilter(r.getNode(), typeFilter);
      p.setPointerKey(f);
      pointerKeys.replace(r, f);
    } else {
      Assertions.UNREACHABLE(p.getPointerKey().getClass().toString());
    }
  }

  /**
   * @return the unique integer that identifies this pointer key
   */
  public int getIndex(PointerKey p) {
    return pointerKeys.getMappedIndex(p);
  }

  public int getRepresentative(int i) {
    return uf.find(i);
  }

}
