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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.DelegatingContext;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;

/**
 * A context selector that first checks with A, then defaults to B.
 *
 * <p>If A generates a Context B is ignored completely regardless of the ContextKeys in A.
 *
 * @see com.ibm.wala.ipa.callgraph.impl.UnionContextSelector
 */
public class DelegatingContextSelector implements ContextSelector {

  private static final boolean DEBUG = false;

  private final ContextSelector A;
  private final ContextSelector B;

  public DelegatingContextSelector(ContextSelector A, ContextSelector B) {
    this.A = A;
    this.B = B;
    if (A == null) {
      throw new IllegalArgumentException("null A");
    }
    if (B == null) {
      throw new IllegalArgumentException("null B");
    }
  }

  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (DEBUG) {
      System.err.println(("getCalleeTarget " + caller + ' ' + site + ' ' + callee));
      System.err.println("Trying with A: " + A.getClass());
      System.err.println("Trying with B: " + B.getClass());
    }
    if (A != null) {
      Context C = A.getCalleeTarget(caller, site, callee, receiver);
      if (C != null) {
        if (DEBUG) {
          System.err.println(("Case A " + A.getClass() + ' ' + C));
        }
        Context CB = B.getCalleeTarget(caller, site, callee, receiver);
        if (CB != null) {
          return new DelegatingContext(C, CB);
        } else {
          return C;
        }
      }
    }
    Context C = B.getCalleeTarget(caller, site, callee, receiver);
    if (DEBUG) {
      System.err.println(("Case B " + B.getClass() + ' ' + C));
    }
    return C;
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return A.getRelevantParameters(caller, site).union(B.getRelevantParameters(caller, site));
  }

  @Override
  public String toString() {
    return "<DelegatingContextSelector A=" + A + " B=" + B + " />";
  }
}
