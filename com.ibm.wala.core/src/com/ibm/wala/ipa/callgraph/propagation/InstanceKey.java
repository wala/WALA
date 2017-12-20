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

import java.util.Iterator;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.util.collections.Pair;

/**
 * An InstanceKey serves as the representative for an equivalence class of
 * objects in the heap, that can be pointed to.
 * 
 * For example, for 0-CFA, an InstanceKey would embody an &lt;IClass&gt;... we model
 * all instances of a particular class
 * 
 * For 0-1-CFA, an InstanceKey could be &lt;IMethod,statement #&gt;, representing a
 * particular allocation statement in a particular method.
 */
public interface InstanceKey extends ContextItem {

  /**
   * For now, we assert that each InstanceKey represents a set of classes which
   * are all of the same concrete type (modulo the fact that all arrays of
   * references are considered concrete type []Object;)
   */
  IClass getConcreteType();

  /**
   * Get the creation sites of <code>this</code>, i.e., the statements that may
   * allocate objects represented by <code>this</code>. A creation site is a
   * pair (n,s), where n is the containing {@link CGNode} in the given
   * {@link CallGraph} <code>CG</code> and s is the allocating
   * {@link NewSiteReference}.
   * 
   */
  Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG);

}
