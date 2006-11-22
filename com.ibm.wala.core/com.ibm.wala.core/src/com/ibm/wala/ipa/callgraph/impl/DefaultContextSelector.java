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

import com.ibm.wala.analysis.reflection.FactoryContextSelector;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.CloneContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * Default object to control context-insensitive context selection
 * 
 * @author sfink
 */
public class DefaultContextSelector implements ContextSelector {

  private final ContextSelector delegate;

  /**
   * @param cha
   * @param methodTargetSelector
   */
  public DefaultContextSelector(ClassHierarchy cha, MethodTargetSelector methodTargetSelector) {
    FactoryContextSelector R = new FactoryContextSelector(cha, methodTargetSelector);
    ContextInsensitiveSelector S = new ContextInsensitiveSelector();
    ContextSelector s = new DelegatingContextSelector(R, S);
    delegate = new DelegatingContextSelector(new CloneContextSelector(cha),s);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    return delegate.getCalleeTarget(caller, site, callee, receiver);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.callgraph.ContextSelector#getBoundOnNumberOfTargets(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod targetMethod) {
    return delegate.getBoundOnNumberOfTargets(caller, site, targetMethod);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode caller, CallSiteReference site) {
    return delegate.contextIsIrrelevant(caller, site);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode caller, CallSiteReference site) {
    return delegate.allSitesDispatchIdentically(caller,site);
  }

  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return delegate.mayUnderstand(caller, site, targetMethod, instance);
  }
}