/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.slicer;

import java.util.Iterator;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchyDweller;
import com.ibm.wala.ipa.slicer.Slicer.ControlDependenceOptions;
import com.ibm.wala.util.graph.NumberedGraph;

/**
 * Interface for an SDG (loosely defined here as a graph of {@link Statement}s. 
 * This interface implies that the underlying graph is computed lazily on demand.
 */
public interface ISDG extends NumberedGraph<Statement>, IClassHierarchyDweller {

  /**
   * {@link ControlDependenceOptions} used to construct this graph.
   */
  ControlDependenceOptions getCOptions();

  /**
   * Get the program dependence graph constructed for a particular node.
   */
  PDG<? extends InstanceKey> getPDG(CGNode node);

  /**
   * Iterate over the nodes which have been discovered so far, but do <em>NOT</em> eagerly construct the entire graph.
   */
  Iterator<? extends Statement> iterateLazyNodes();

}
