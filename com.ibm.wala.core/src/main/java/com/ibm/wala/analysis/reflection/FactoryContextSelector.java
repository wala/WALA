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
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallString;
import com.ibm.wala.ipa.callgraph.propagation.cfa.CallStringContext;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

/** For synthetic methods marked as "Factories", we analyze in a context defined by the caller. */
class FactoryContextSelector implements ContextSelector {

  public FactoryContextSelector() {}

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee == null) {
      throw new IllegalArgumentException("callee is null");
    }
    if (callee.isWalaSynthetic()) {
      SyntheticMethod s = (SyntheticMethod) callee;
      if (s.isFactoryMethod()) {
        return new CallStringContext(new CallString(site, caller.getMethod()));
      }
    }
    return null;
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return EmptyIntSet.instance;
  }
}
