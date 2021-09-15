/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Manu Sridharan - initial API and implementation
 */
package com.ibm.wala.util.graph.impl;

import com.ibm.wala.util.graph.AbstractGraph;
import com.ibm.wala.util.graph.EdgeManager;
import com.ibm.wala.util.graph.NodeManager;

/**
 * Basic implementation of a {@link com.ibm.wala.util.graph.Graph}. Does not support node or edge
 * deletion.
 */
public class BasicGraph<T> extends AbstractGraph<T> {

  private final NodeManager<T> nodeManager = new BasicNodeManager<>();

  private final EdgeManager<T> edgeManager = new BasicEdgeManager<>();

  @Override
  protected NodeManager<T> getNodeManager() {
    return nodeManager;
  }

  @Override
  protected EdgeManager<T> getEdgeManager() {
    return edgeManager;
  }
}
