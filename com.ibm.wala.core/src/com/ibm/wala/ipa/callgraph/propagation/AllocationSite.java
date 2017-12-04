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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.collections.Pair;

/**
 * An {@link InstanceKey} which represents a {@link NewSiteReference} in some {@link IMethod}. Note that this differs from
 * {@link AllocationSiteInNode}, which represents an allocation in a {@link CGNode} that may carry some {@link Context}. This type
 * is useful for a context-<em>insensitive</em> heap abstraction.
 */
public class AllocationSite implements InstanceKey {
  private final NewSiteReference site;

  private final IMethod method;

  private final IClass concreteType;

  public AllocationSite(IMethod method, NewSiteReference allocation, IClass type) {
    this.site = allocation;
    this.method = method;
    this.concreteType = type;
  }

  @Override
  public String toString() {
    return "SITE{" + getMethod() + ":" + site + "}";
  }

  public NewSiteReference getSite() {
    return site;
  }

  public IMethod getMethod() {
    return method;
  }

  @Override
  public IClass getConcreteType() {
    return concreteType;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((method == null) ? 0 : method.hashCode());
    result = prime * result + ((site == null) ? 0 : site.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final AllocationSite other = (AllocationSite) obj;
    if (method == null) {
      if (other.method != null)
        return false;
    } else if (!method.equals(other.method))
      return false;
    if (site == null) {
      if (other.site != null)
        return false;
    } else if (!site.equals(other.site))
      return false;
    return true;
  }

  @Override
  public Iterator<Pair<CGNode, NewSiteReference>> getCreationSites(CallGraph CG) {
    return new MapIterator<>(
        new FilterIterator<>(
          CG.getNodes(method.getReference()).iterator(),
          o -> o.getMethod().equals(method)
        ), 
        object -> Pair.make(object, site));
  }
}
