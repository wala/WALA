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
package com.ibm.wala.util.collections;

/**
 * simple interface for a vector.
 * 
 * TODO: get rid of this and use java.util.collection.RandomAccess
 */
public interface IVector<T> extends Iterable<T> {
  /*
   * @see com.ibm.wala.util.intset.IntVector#get(int)
   */
  public abstract T get(int x);

  /**
   * TODO: this can be optimized
   * 
   * @see com.ibm.wala.util.intset.IntVector#set(int, int)
   */
  public abstract void set(int x, T value);

  /*
   * @see com.ibm.wala.util.debug.VerboseAction#performVerboseAction()
   */
  public abstract void performVerboseAction();

  /**
   * @return max i s.t get(i) != null
   */
  public abstract int getMaxIndex();
}
