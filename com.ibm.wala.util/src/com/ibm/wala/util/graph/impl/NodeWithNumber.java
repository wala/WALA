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
package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.graph.INodeWithNumber;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * A node which carries it's own number; which identifies it in a {@link NumberedGraph}
 * implementation.
 *
 * <p>Note that a {@link NodeWithNumber} can live it at most one {@link NumberedGraph} at a time.
 * The {@link NumberedGraph} will mutate the number here. So this is a bit fragile. Use this only if
 * you know what you're doing.
 */
public class NodeWithNumber implements INodeWithNumber {

  private int number = -1;

  /** @return the number which identifies this node in the numbered graph */
  @Override
  public int getGraphNodeId() {
    return number;
  }

  @Override
  public void setGraphNodeId(int i) {
    number = i;
  }
}
