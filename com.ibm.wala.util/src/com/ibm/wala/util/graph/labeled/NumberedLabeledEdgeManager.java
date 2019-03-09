/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.graph.labeled;

import com.ibm.wala.util.graph.NumberedEdgeManager;
import com.ibm.wala.util.intset.IntSet;

public interface NumberedLabeledEdgeManager<T, U>
    extends LabeledEdgeManager<T, U>, NumberedEdgeManager<T> {

  public IntSet getPredNodeNumbers(T node, U label) throws IllegalArgumentException;

  public IntSet getSuccNodeNumbers(T node, U label) throws IllegalArgumentException;
}
