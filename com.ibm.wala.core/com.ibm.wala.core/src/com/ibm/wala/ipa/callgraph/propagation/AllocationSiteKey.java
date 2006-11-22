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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * An instance key which represents a unique set for each allocation site in
 * each CGNode
 */
public abstract class AllocationSiteKey extends AbstractAllocationSiteKey {
  private final NewSiteReference site;

  public AllocationSiteKey(CGNode node, NewSiteReference allocation, IClass type) {
    super(node,type);
    this.site = allocation;
  }

  public abstract boolean equals(Object obj);

  public abstract int hashCode();

  public String toString() {
    return "SITE{" + getNode().getMethod() + ":" + site + " in " + getNode().getContext() + "}";
  }

  /**
   * @return Returns the site.
   */
  public NewSiteReference getSite() {
    return site;
  }
}

