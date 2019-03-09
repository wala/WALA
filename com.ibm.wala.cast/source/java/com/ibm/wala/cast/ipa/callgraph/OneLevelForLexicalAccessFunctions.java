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
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.ScopeMappingInstanceKeys.ScopeMappingInstanceKey;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * Adds one-level of {@link ArgumentInstanceContext} on the function argument for functions that
 * perform lexical accesses (i.e., those functions represented by a {@link
 * ScopeMappingInstanceKey}). In essence, this guarantees that when a function is cloned according
 * to some {@link ContextSelector}, its nested functions that may do lexical accesses if its
 * variables have corresponding clones.
 */
public class OneLevelForLexicalAccessFunctions implements ContextSelector {

  private final ContextSelector baseSelector;

  public OneLevelForLexicalAccessFunctions(ContextSelector baseSelector) {
    this.baseSelector = baseSelector;
  }

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    final Context base = baseSelector.getCalleeTarget(caller, site, callee, receiver);
    if (receiver != null && receiver[0] != null && receiver[0] instanceof ScopeMappingInstanceKey) {
      final ScopeMappingInstanceKey smik = (ScopeMappingInstanceKey) receiver[0];
      return new ArgumentInstanceContext(base, 0, smik);
    } else {
      return base;
    }
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return IntSetUtil.make(new int[] {0}).union(baseSelector.getRelevantParameters(caller, site));
  }
}
