/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.intset;

/** interface for array of integer */
public interface IntVector {

  int get(int x);

  void set(int x, int value);

  /**
   * @return max i s.t set(i) was called.
   */
  int getMaxIndex();
}
