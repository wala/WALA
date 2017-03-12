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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.analysis.reflection.ReflectionContextSelector;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.CloneContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.intset.IntSet;

/**
 * Default object to control context-insensitive context selection, This includes reflection logic.
 */
public class DefaultContextSelector implements ContextSelector {

  private final ContextSelector delegate;

  public DefaultContextSelector(AnalysisOptions options, IClassHierarchy cha) {
    if (options == null) {
      throw new IllegalArgumentException("null options");
    }
    ContextInsensitiveSelector ci = new ContextInsensitiveSelector();
    ContextSelector r = ReflectionContextSelector.createReflectionContextSelector(options);
    ContextSelector s = new DelegatingContextSelector(r, ci);
    delegate = new DelegatingContextSelector(new CloneContextSelector(cha), s);
  }

  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (caller == null) {
      throw new IllegalArgumentException("null caller");
    }
    return delegate.getCalleeTarget(caller, site, callee, receiver);
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return delegate.getRelevantParameters(caller, site);
  }

}
