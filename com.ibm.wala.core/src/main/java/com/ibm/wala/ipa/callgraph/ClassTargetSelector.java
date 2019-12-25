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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;

/**
 * This interface represents policies for selecting a class to allocate at a given new site. The
 * most obvious such policy would be to look at the relevant class hierarchy and lookup the
 * appropriate class based on the type reference at the new site. However, other policies are
 * possible for purposes such as providing an abstraction of unanalyzed libraries or specialized
 * J2EE functionality.
 *
 * <p>Such policies are consulted by the different analysis mechanisms, both the flow-based and
 * non-flow algorithms. The current mechanism is that the policy object are registered with the
 * AnalysisOptions object, and all analyses that need to analyze allocations ask that object for the
 * class selector to use.
 *
 * <p>In general, for specialized selectors, it is good practice to build selectors that handle the
 * special case of interest, and otherwise delegate to a child selector. When registering with the
 * AnalysisOptions object, make the child selector be whatever the options object had before.
 */
public interface ClassTargetSelector {

  /**
   * Given a calling node and a new site, return the type to be allocated.
   *
   * @param caller the GCNode in the call graph containing the new site.
   * @param site the new site reference of the new site.
   * @return the class to be allocated.
   */
  IClass getAllocatedTarget(CGNode caller, NewSiteReference site);
}
