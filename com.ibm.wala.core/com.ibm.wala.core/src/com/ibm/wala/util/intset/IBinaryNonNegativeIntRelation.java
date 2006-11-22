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
package com.ibm.wala.util.intset;

import com.ibm.wala.util.debug.VerboseAction;

/**
 * a relation R(x,y) where x >= 0
 * 
 * @author sfink
 */
public interface IBinaryNonNegativeIntRelation extends VerboseAction, Iterable<IntPair> {
  /**
   * Add (x,y) to the relation
   * 
   * @param x
   * @param y
   * @return true iff the relation changes as a result of this call.
   */
  public abstract boolean add(int x, int y);

  /**
   * @param x
   * @return IntSet of y s.t. R(x,y) or null if none.
   */
  public abstract IntSet getRelated(int x);

  /**
   * @param x
   * @return number of y s.t. R(x,y)
   */
  public abstract int getRelatedCount(int x);

  /**
   * @param x
   * @param y
   */
  public abstract void remove(int x, int y);

  /**
   */
  public abstract void removeAll(int x);

  /**
   * @param x
   * @param y
   * @return true iff (x,y) \in R
   */
  public abstract boolean contains(int x, int y);

  public abstract int maxKeyValue();

}
