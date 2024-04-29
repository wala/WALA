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
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;

/** An interface to an object which helps control context-sensitivity. */
public interface ContextSelector {
  /**
   * Given a calling node and a call site, returns the Context in which the callee should be
   * evaluated.
   *
   * @param caller the node containing the call site
   * @param site description of the call site
   * @param actualParameters the abstract objects (InstanceKeys) of parameters of interest to the
   *     selector
   * @return the Context in which the callee should be evaluated, or null if no information is
   *     available.
   */
  Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters);

  /**
   * Given a calling node and a call site, return the set of parameters based on which this selector
   * may choose to specialize contexts.
   *
   * @param caller the calling node
   * @param site the specific call site
   * @return the set of parameters of interest
   */
  IntSet getRelevantParameters(CGNode caller, CallSiteReference site);
}
