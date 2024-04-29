/*
 * Copyright (c) 2002 - 2020 IBM Corporation.
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
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.DelegatingContext;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSiteInNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * k-limited object sensitive context selector
 *
 * <ul>
 *   <li>for static method : For a few well-known static factory methods from the standard
 *       libraries, use {@link CallerSiteContext}.Otherwise, directly copy the context of the last
 *       non-static method
 *   <li>for virtual method : The {@link Context} consists of n allocation sites
 *   <li>for an object(fixed at allocation) : The heap context consists of n allocation sites
 *       inherited from {@link CGNode}
 * </ul>
 */
public class nObjContextSelector implements ContextSelector {

  public static final ContextKey ALLOCATION_STRING_KEY =
      new ContextKey() {
        @Override
        public String toString() {
          return "ALLOCATION_STRING_KEY";
        }
      };

  private final int n;

  private final ContextSelector base;

  public nObjContextSelector(int n, ContextSelector base) {
    if (n <= 0) {
      throw new IllegalArgumentException("n must be a positive number");
    }
    this.n = n;
    this.base = base;
  }

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {

    Context calleeContext = Everywhere.EVERYWHERE;

    InstanceKey receiver =
        (actualParameters != null && actualParameters.length > 0 && actualParameters[0] != null)
            ? actualParameters[0]
            : null;

    if (site.isStatic()) {
      calleeContext = getCalleeTargetForStaticCall(caller, site, callee);
    } else if (receiver instanceof AllocationSiteInNode) {
      AllocationString allocationString =
          assemblyReceiverAllocString((AllocationSiteInNode) receiver);
      calleeContext = new AllocationStringContext(allocationString);
    }

    Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
    return appendBaseContext(calleeContext, baseContext);
  }

  private AllocationString assemblyReceiverAllocString(AllocationSiteInNode receiver) {
    Context receiverHeapContext = receiver.getNode().getContext();
    AllocationSite receiverAllocSite =
        new AllocationSite(
            receiver.getNode().getMethod(), receiver.getSite(), receiver.getConcreteType());

    if (receiverHeapContext.get(ALLOCATION_STRING_KEY) != null) {
      AllocationString receiverAllocString =
          (AllocationString) receiverHeapContext.get(ALLOCATION_STRING_KEY);

      int siteLength = Math.min(n, receiverAllocString.getLength() + 1);
      AllocationSite[] sites = new AllocationSite[siteLength];
      sites[0] = receiverAllocSite;
      System.arraycopy(receiverAllocString.getAllocationSites(), 0, sites, 1, siteLength - 1);
      return new AllocationString(sites);
    } else {
      return new AllocationString(receiverAllocSite);
    }
  }

  protected Context getCalleeTargetForStaticCall(
      CGNode caller, CallSiteReference site, IMethod callee) {
    return ContainerContextSelector.isWellKnownStaticFactory(callee.getReference())
        ? new CallerSiteContext(caller, site)
        : caller.getContext();
  }

  private Context appendBaseContext(Context curr, Context base) {
    if (curr == Everywhere.EVERYWHERE) {
      return base;
    } else {
      return base == Everywhere.EVERYWHERE ? curr : new DelegatingContext(curr, base);
    }
  }

  private static final IntSet thisParameter = IntSetUtil.make(new int[] {0});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (site.isDispatch()) {
      return thisParameter;
    } else {
      return EmptyIntSet.instance;
    }
  }
}
