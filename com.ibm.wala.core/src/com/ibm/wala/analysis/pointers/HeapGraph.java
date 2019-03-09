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
package com.ibm.wala.analysis.pointers;

import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.NumberedGraph;
import java.util.Collection;
import java.util.Set;

/**
 * A {@link Graph} view of a pointer analysis solution.
 *
 * <p>Nodes in the Graph are {@link PointerKey}s and {@link InstanceKey}s.
 *
 * <p>There is an edge from a PointerKey P to an InstanceKey I iff the PointerAnalysis indicates
 * that P may point to I.
 *
 * <p>There is an edge from an InstanceKey I to a PointerKey P iff - P represents a field of an
 * object instance modeled by I, or - P represents the array contents of array instance I.
 */
public interface HeapGraph<T extends InstanceKey> extends NumberedGraph<Object> {

  public Collection<Object> getReachableInstances(Set<Object> roots);

  public HeapModel getHeapModel();

  public PointerAnalysis<T> getPointerAnalysis();
}
