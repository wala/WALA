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

/**
 * A numbered graph is a {@link Graph} where each node has a unique persistent non-negative integer
 * id.
 */
public interface NumberedGraph<T>
    extends Graph<T>, NumberedNodeManager<T>, NumberedEdgeManager<T> {}
