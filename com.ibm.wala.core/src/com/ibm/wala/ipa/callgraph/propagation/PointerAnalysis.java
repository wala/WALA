/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Collection;

import com.ibm.wala.analysis.pointers.HeapGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;

/**
 * Abstract definition of pointer analysis
 */
public interface PointerAnalysis<T extends InstanceKey> {
  
  /**
   * @param key representative of an equivalence class of pointers
   * @return Set of InstanceKey, representing the instance abstractions that define
   * the points-to set computed for the pointer key
   */
  OrdinalSet<T> getPointsToSet(PointerKey key);

  /**
   * @return an Object that determines how to model abstract locations in the heap.
   */
  HeapModel getHeapModel();

  /**
   * @return a graph view of the pointer analysis solution
   */
  HeapGraph<T> getHeapGraph();
  
  /**
   * @return the bijection between InstanceKey &lt;=&gt; Integer that defines the
   * interpretation of points-to-sets.
   */
  OrdinalSetMapping<T> getInstanceKeyMapping();

  /**
   * @return all pointer keys known
   */
  Iterable<PointerKey> getPointerKeys();
  
  
  /**
   * @return all instance keys known
   */
  Collection<T> getInstanceKeys();

  /**
   * did the pointer analysis use a type filter for a given points-to set?
   * (this is ugly).
   */
  boolean isFiltered(PointerKey pk);
  
  public IClassHierarchy getClassHierarchy();

}
