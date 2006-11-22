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
package com.ibm.wala.ipa.callgraph.propagation.rta;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.debug.Assertions;

/**
 * A utility class consisting of a pair CallSiteReference x CGNode
 */
public final class CallSite {
  private final CallSiteReference site;

  private final CGNode node;

  public CallSite(CallSiteReference site, CGNode node) {
    this.site = site;
    this.node = node;
  }

  public int hashCode() {
    return 3229 * site.hashCode() + node.hashCode();
  }

  public boolean equals(Object o) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(getClass().equals(o.getClass()));
    }
    CallSite other = (CallSite) o;
    return node.equals(other.node) && site.equals(other.site);
  }

  public String toString() {
    return site.toString() + " " + node.toString();
  }

  public CGNode getNode() {
    return node;
  }

  public CallSiteReference getSite() {
    return site;
  }

  /**
   * @return the Selector that identifies this site
   */
  public Selector getSelector() {
    return site.getDeclaredTarget().getSelector();
  }

}