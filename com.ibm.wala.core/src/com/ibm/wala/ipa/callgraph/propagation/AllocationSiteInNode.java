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
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;

/**
 * An {@link InstanceKey} which represents a {@link NewSiteReference} in some {@link CGNode}.
 */
public abstract class AllocationSiteInNode extends AbstractTypeInNode {
  private final NewSiteReference site;

  public AllocationSiteInNode(CGNode node, NewSiteReference allocation, IClass type) {
    super(node, type);
    this.site = allocation;
  }

  @Override
  public abstract boolean equals(Object obj);

  @Override
  public abstract int hashCode();

  @Override
  public String toString() {
    return "SITE_IN_NODE{" + getNode().getMethod() + ":" + site + " in " + getNode().getContext() + "}";
  }

  /**
   * @return Returns the site.
   */
  public NewSiteReference getSite() {
    return site;
  }

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    return new NonNullSingletonIterator<>(Pair.make(getNode(), getSite()));
  }
   
}
