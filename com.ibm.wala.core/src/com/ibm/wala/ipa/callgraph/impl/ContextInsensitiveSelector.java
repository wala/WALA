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

package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.warnings.WarningSet;

/**
 *
 * A basic context selector that ignores context.
 * 
 * @author sfink
 */
public class ContextInsensitiveSelector implements ContextSelector {

  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.detox.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    return Everywhere.EVERYWHERE;
  }

  /* (non-Javadoc)
   * @see com.ibm.detox.ipa.callgraph.ContextSelector#getBoundOnNumberOfTargets(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod targetMethod) {
    return 1;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    // this object is not bound to a WarningSet
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return true;
  }
}
