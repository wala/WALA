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
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

/**
 * 
 * An interface to an object which helps control context-sensitivity
 * 
 * @author sfink
 */
public interface ContextSelector {
  /**
   * Given a call site, returns the Context in which the callee should be
   * evaluated.
   * 
   * @param site
   *          description of the call site
   * @return the Context in which the callee should be evaluated, or null if no
   *         information is available.
   */
  Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver);

  /**
   * @param instance
   *          the instance dispatched on. null means "any possible instance"
   * @return true iff this object may understand how to select a context for the
   *         given target
   */
  boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance);


  /**
   * @param site
   * @return true iff this context selector will always return the same context
   *         for a given call site, regardless of the receiver object
   */
  boolean contextIsIrrelevant(CGNode node, CallSiteReference site);

  /**
   * @return true iff \forAll n \in nodes, \forAll s \in sites with this
   *         declaredTarget, the set of targets is the same
   */
  boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site);

}
