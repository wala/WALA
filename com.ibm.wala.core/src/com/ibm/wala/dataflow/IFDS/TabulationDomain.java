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

import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Domain of facts for tabulation.
 *
 * @param <F> factoid type
 * @param <T> type of nodes in the supergraph
 */
public interface TabulationDomain<F, T> extends OrdinalSetMapping<F> {

  /**
   * returns {@code true} if p1 should be processed before p2 by the {@link TabulationSolver}
   *
   * <p>For example, if this domain supports a partial order on facts, return true if p1.d2 is
   * weaker than p2.d2 (intuitively p1.d2 meet p2.d2 = p1.d2)
   *
   * <p>return false otherwise
   */
  boolean hasPriorityOver(PathEdge<T> p1, PathEdge<T> p2);
}
