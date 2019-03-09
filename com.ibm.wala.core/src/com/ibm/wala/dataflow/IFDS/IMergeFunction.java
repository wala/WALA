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
package com.ibm.wala.dataflow.IFDS;

import com.ibm.wala.util.intset.IntSet;

/**
 * Special case: if supportsMerge(), then the problem is not really IFDS anymore. (TODO: rename
 * it?). Instead, we perform a merge operation before propagating at every program point. This way,
 * we can implement standard interprocedural dataflow and ESP-style property simulation, and various
 * other things.
 */
public interface IMergeFunction {

  /**
   * @param x set of factoid numbers that previously have been established to hold at a program
   *     point
   * @param j a new factoid number which has been discovered to hold at a program point
   * @return the factoid number z which should actually be propagated, based on a merge of the new
   *     fact j into the old state represented by x. return -1 if no fact should be propagated.
   */
  int merge(IntSet x, int j);
}
