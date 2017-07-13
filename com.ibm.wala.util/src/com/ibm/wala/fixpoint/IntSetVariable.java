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
package com.ibm.wala.fixpoint;

import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

/**
 * A variable for dataflow analysis, representing a set of integers.
 * 
 * TODO: optimize the representation more; e.g. BitVectors with non-zero lower bound.
 */
@SuppressWarnings("rawtypes")
public abstract class IntSetVariable<T extends IntSetVariable<T>> extends AbstractVariable<T> {

  MutableIntSet V;

  @Override
  public void copyState(T other) {
    if (V == null) {
      if (other.V == null) {
        return;
      } else {
        V = IntSetUtil.getDefaultIntSetFactory().makeCopy(other.V);
        return;
      }
    } else {
      if (other.V != null) {
        V.copySet(other.V);
      }
    }
  }

  /**
   * Add all integers from the set B
   * 
   * @return true iff the value of this changes
   */
  public boolean addAll(IntSet B) {
    if (V == null) {
      V = IntSetUtil.getDefaultIntSetFactory().makeCopy(B);
      return (B.size() > 0);
    } else {
      boolean result = V.addAll(B);
      return result;
    }
  }

  /**
   * Add all integers from the other int set variable.
   * 
   * @return true iff the contents of this variable changes.
   */
  public boolean addAll(T other) {
    if (V == null) {
      copyState(other);
      return (V != null);
    } else {
      if (other.V != null) {
        boolean result = addAll(other.V);
        return result;
      } else {
        return false;
      }
    }
  }

  public boolean sameValue(IntSetVariable other) {
    if (V == null) {
      return (other.V == null);
    } else {
      if (other.V == null) {
        return false;
      } else {
        return V.sameValue(other.V);
      }
    }
  }

  @Override
  public String toString() {
    if (V == null) {
      return "[Empty]";
    }
    return V.toString();
  }

  /**
   * Set a particular bit
   * 
   * @param b the bit to set
   */
  public void add(int b) {
    if (V == null) {
      V = IntSetUtil.getDefaultIntSetFactory().make();
    }
    V.add(b);
  }

  /**
   * Is a particular bit set?
   * 
   * @param b the bit to check
   */
  public boolean contains(int b) {
    if (V == null) {
      return false;
    } else {
      return V.contains(b);
    }
  }

  /**
   * @return the value of this variable as a MutableSparseIntSet ... null if the set is empty.
   */
  public MutableIntSet getValue() {
    return V;
  }

  /**
   * @param i
   */
  public void remove(int i) {
    if (V != null) {
      V.remove(i);
    }
  }

  public int size() {
    return (V == null) ? 0 : V.size();
  }

  public boolean containsAny(IntSet instances) {
    return V.containsAny(instances);
  }

  public boolean addAllInIntersection(T other, IntSet filter) {
    if (V == null) {
      copyState(other);
      if (V != null) {
        V.intersectWith(filter);
        if (V.isEmpty()) {
          V = null;
        }
      }
      return (V != null);
    } else {
      if (other.V != null) {
        boolean result = addAllInIntersection(other.V, filter);
        return result;
      } else {
        return false;
      }
    }
  }

  public boolean addAllInIntersection(IntSet other, IntSet filter) {
    if (V == null) {
      V = IntSetUtil.getDefaultIntSetFactory().makeCopy(other);
      V.intersectWith(filter);
      if (V.isEmpty()) {
        V = null;
      }
      return (V != null);
    } else {
      boolean result = V.addAllInIntersection(other, filter);
      return result;
    }
  }

  public void removeAll() {
    V = null;
  }
}
