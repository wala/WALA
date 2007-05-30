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
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * This is a context selector that adds one level of calling context to a base
 * context selector.
 * 
 * @author sfink
 */
public class OneLevelContextSelector implements ContextSelector {

  private final ContextSelector baseSelector;

  /**
   * @param baseSelector
   *          a context selector which provides the context to analyze a method
   *          in, but without one level of calling context.
   */
  public OneLevelContextSelector(ContextSelector baseSelector) {
    this.baseSelector = baseSelector;
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    Context baseContext = baseSelector.getCalleeTarget(caller, site, callee, receiver);
    if (baseContext.equals(Everywhere.EVERYWHERE)) {
      return new CallerContext(caller);
    } else {
      return new CallerContextPair(caller, baseContext);
    }
  }

  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return baseSelector.mayUnderstand(caller, site, targetMethod, instance);
  }

  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod targetMethod) {
    return baseSelector.getBoundOnNumberOfTargets(caller, site, targetMethod);
  }

  public void setWarnings(WarningSet newWarnings) {
    baseSelector.setWarnings(newWarnings);
  }

  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return baseSelector.contextIsIrrelevant(node, site);
  }

  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return false;
  }
}
