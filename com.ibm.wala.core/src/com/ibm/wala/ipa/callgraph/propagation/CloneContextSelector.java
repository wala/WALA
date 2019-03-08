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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.analysis.reflection.CloneInterpreter;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

/**
 * This context selector selects a context based on the concrete type of the receiver to a call of
 * java.lang.Object.clone
 */
public class CloneContextSelector implements ContextSelector {

  private final ReceiverTypeContextSelector selector;

  private final IClassHierarchy cha;

  public CloneContextSelector(IClassHierarchy cha) {
    this.selector = new ReceiverTypeContextSelector();
    this.cha = cha;
  }

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (receiver == null) {
      return null;
    }
    if (callee.getReference().equals(CloneInterpreter.CLONE)) {
      return selector.getCalleeTarget(caller, site, callee, receiver);
    } else {
      return null;
    }
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    IMethod declaredTarget = cha.resolveMethod(site.getDeclaredTarget());
    if (declaredTarget != null && declaredTarget.getReference().equals(CloneInterpreter.CLONE)) {
      return selector.getRelevantParameters(caller, site);
    } else {
      return EmptyIntSet.instance;
    }
  }
}
