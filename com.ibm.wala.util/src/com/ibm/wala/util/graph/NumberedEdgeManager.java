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
package com.ibm.wala.util.graph;

import com.ibm.wala.util.intset.IntSet;

/** Additional functionality for edges in numbered graphs */
public interface NumberedEdgeManager<T> extends EdgeManager<T> {

  /** @return the numbers identifying the immediate successors of node */
  public IntSet getSuccNodeNumbers(T node);

  /** @return the numbers identifying the immediate predecessors of node */
  public IntSet getPredNodeNumbers(T node);
}
