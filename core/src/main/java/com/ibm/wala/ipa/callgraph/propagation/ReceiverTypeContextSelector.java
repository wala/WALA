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

import com.ibm.wala.analysis.reflection.JavaTypeContext;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/** This context selector selects a context based on the concrete type of the receiver. */
public class ReceiverTypeContextSelector implements ContextSelector {

  public ReceiverTypeContextSelector() {}

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (site.isStatic()) {
      return Everywhere.EVERYWHERE;
    } else {
      if (receiver == null) {
        throw new IllegalArgumentException("receiver is null");
      }
      PointType P = new PointType(receiver[0].getConcreteType());
      return new JavaTypeContext(P);
    }
  }

  private static final IntSet receiver = IntSetUtil.make(new int[] {0});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (site.isStatic()) {
      return EmptyIntSet.instance;
    } else {
      return receiver;
    }
  }
}
