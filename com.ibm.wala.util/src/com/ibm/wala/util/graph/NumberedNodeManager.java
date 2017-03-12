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
package com.ibm.wala.util.graph;

import java.util.Iterator;

import com.ibm.wala.util.intset.IntSet;

/**
 * An object which tracks nodes with numbers.
 */
public interface NumberedNodeManager<T> extends NodeManager<T> {

  public abstract int getNumber(T N);


  public abstract T getNode(int number);


  public abstract int getMaxNumber();

  /**
   * @param s
   * @return iterator of nodes with the numbers in set s
   */
  public abstract Iterator<T> iterateNodes(IntSet s);

}
