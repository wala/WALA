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

import com.ibm.wala.analysis.reflection.ReflectionContextSelector;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.CloneContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * Default object to control context-insensitive context selection,
 * This includes reflection logic.
 * 
 * @author sfink
 */
public class DefaultContextSelector implements ContextSelector {

  private final ContextSelector delegate;

  public DefaultContextSelector(IClassHierarchy cha, MethodTargetSelector methodTargetSelector) {
    ReflectionContextSelector r = ReflectionContextSelector.createReflectionContextSelector(cha, methodTargetSelector);
    ContextInsensitiveSelector ci = new ContextInsensitiveSelector();
    ContextSelector s = new DelegatingContextSelector(r, ci);
    delegate = new DelegatingContextSelector(new CloneContextSelector(),s);
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    return delegate.getCalleeTarget(caller, site, callee, receiver);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode caller, CallSiteReference site) {
    return delegate.contextIsIrrelevant(caller, site);
  }

  /* 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode caller, CallSiteReference site) {
    return delegate.allSitesDispatchIdentically(caller,site);
  }

  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return delegate.mayUnderstand(caller, site, targetMethod, instance);
  }
}