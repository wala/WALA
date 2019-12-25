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

/** */
public interface OrderedMultiGraph<T> extends Graph<T> {

  /** get the ith successor of a node */
  public T getSuccessor(T node, int i);

  /** add an edge and record it so dst is the ith successor of src */
  public void addEdge(int i, T src, T dst);
}
