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
import com.ibm.wala.util.collections.Pair;

/**
 * A utility class consisting of a pair CallSiteReference x CGNode
 */
public final class CallSite extends Pair<CallSiteReference, CGNode> {

  public CallSite(CallSiteReference site, CGNode node) {
    super(site, node);
    if (site == null) {
      throw new IllegalArgumentException("null site");
    }
    if (node == null) {
      throw new IllegalArgumentException("null node");
    }
  }

  public CGNode getNode() {
    return snd;
  }

  public CallSiteReference getSite() {
    return fst;
  }

  /**
   * @return the Selector that identifies this site
   */
  public Selector getSelector() {
    return getSite().getDeclaredTarget().getSelector();
  }

}
