/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.Pair;

/**
 * Represents the JavaScript global object.  
 * 
 * @see com.ibm.wala.cast.js.ipa.callgraph.JSSSAPropagationCallGraphBuilder
 */
@SuppressWarnings("javadoc")
public class GlobalObjectKey implements InstanceKey {

  private final IClass concreteType;
  
  public GlobalObjectKey(IClass concreteType) {
    this.concreteType = concreteType;
  }
  
  @Override
  public IClass getConcreteType() {
    return concreteType;
  }

  @Override
  public String toString() {
    return "Global Object";
  }

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    return EmptyIterator.instance();
  }
}
