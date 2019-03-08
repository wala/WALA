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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;

/** This is a context selector that adds one level of calling context to a base context selector. */
public class OneLevelSiteContextSelector implements ContextSelector {

  private final ContextSelector baseSelector;

  /**
   * @param baseSelector a context selector which provides the context to analyze a method in, but
   *     without one level of calling context.
   */
  public OneLevelSiteContextSelector(ContextSelector baseSelector) {
    if (baseSelector == null) {
      throw new IllegalArgumentException("null baseSelector");
    }
    this.baseSelector = baseSelector;
  }

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    Context baseContext = baseSelector.getCalleeTarget(caller, site, callee, receiver);
    if (baseContext.equals(Everywhere.EVERYWHERE)) {
      return new CallerSiteContext(caller, site);
    } else {
      return new CallerSiteContextPair(caller, site, baseContext);
    }
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return baseSelector.getRelevantParameters(caller, site);
  }
}
