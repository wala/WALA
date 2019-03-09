/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.analysis.reflection.InstanceKeyWithNode;
import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions.JavaScriptConstructor;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey.SingleInstanceFilter;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.util.intset.IntSet;

/**
 * A context selector that attempts to detect recursion beyond some depth in a base selector. If
 * such recursion is detected, the base selector's context is replaced with {@link
 * Everywhere#EVERYWHERE}.
 */
public class RecursionBoundContextSelector implements ContextSelector {

  private final ContextSelector base;

  private final int recursionBound;

  /**
   * the highest parameter index that we'll check . this is a HACK. ideally, given a context, we'd
   * have some way to know all the {@link ContextKey}s that it knows about.
   *
   * @see ContextKey#PARAMETERS
   */
  private static final int MAX_INTERESTING_PARAM = 5;

  /**
   * @param recursionBound bound on recursion depth, with the top level of the context returned by
   *     the base selector being depth 0. The {@link Everywhere#EVERYWHERE} context is returned if
   *     the base context <em>exceeds</em> this bound.
   */
  public RecursionBoundContextSelector(ContextSelector base, int recursionBound) {
    this.base = base;
    this.recursionBound = recursionBound;
  }

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
    final boolean exceedsRecursionBound = exceedsRecursionBound(baseContext, 0);
    if (!exceedsRecursionBound) {
      return baseContext;
    } else if (callee instanceof JavaScriptConstructor) {
      // for constructors, we want to keep some basic context sensitivity to
      // avoid horrible imprecision
      return new CallStringContext(new CallString(site, caller.getMethod()));
    } else {
      // TODO somehow k-limit more smartly?
      return Everywhere.EVERYWHERE;
    }
  }

  private boolean exceedsRecursionBound(Context baseContext, int curLevel) {
    if (curLevel > recursionBound) {
      return true;
    }
    // we just do a case analysis here. we might have to add cases later to
    // account for new types of context / recursion.
    CGNode callerNode = (CGNode) baseContext.get(ContextKey.CALLER);
    if (callerNode != null && exceedsRecursionBound(callerNode.getContext(), curLevel + 1)) {
      return true;
    }
    for (int i = 0; i < MAX_INTERESTING_PARAM; i++) {
      FilteredPointerKey.SingleInstanceFilter filter =
          (SingleInstanceFilter) baseContext.get(ContextKey.PARAMETERS[i]);
      if (filter != null) {
        InstanceKey ik = filter.getInstance();
        if (ik instanceof ScopeMappingInstanceKey) {
          ik = ((ScopeMappingInstanceKey) ik).getBase();
        }
        if (ik instanceof InstanceKeyWithNode) {
          if (exceedsRecursionBound(
              ((InstanceKeyWithNode) ik).getNode().getContext(), curLevel + 1)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }
}
