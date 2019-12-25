/*
 * Copyright (c) 2008 IBM Corporation.
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
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;

/** A {@link ContextSelector} to handle default reflection logic. */
public class ReflectionContextSelector {

  public static ContextSelector createReflectionContextSelector(AnalysisOptions options) {

    if (options == null) {
      throw new IllegalArgumentException("null options");
    }

    // start with a dummy
    ContextSelector result =
        new ContextSelector() {
          @Override
          public Context getCalleeTarget(
              CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
            return null;
          }

          @Override
          public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
            return EmptyIntSet.instance;
          }
        };
    if (options.getReflectionOptions().getNumFlowToCastIterations() > 0) {
      result = new DelegatingContextSelector(new FactoryContextSelector(), result);
    }
    if (!options.getReflectionOptions().isIgnoreStringConstants()) {
      result =
          new DelegatingContextSelector(
              new DelegatingContextSelector(
                  new DelegatingContextSelector(
                      new ClassFactoryContextSelector(), new GetClassContextSelector()),
                  new ClassNewInstanceContextSelector()),
              result);
    }
    if (!options.getReflectionOptions().isIgnoreMethodInvoke()) {
      result =
          new DelegatingContextSelector(
              new ReflectiveInvocationSelector(),
              new DelegatingContextSelector(new JavaLangClassContextSelector(), result));
    }
    // if NEITHER string constants NOR method invocations are ignored
    if (!options.getReflectionOptions().isIgnoreStringConstants()
        && !options.getReflectionOptions().isIgnoreMethodInvoke()) {
      result =
          new DelegatingContextSelector(
              new GetMethodContextSelector(
                  options.getReflectionOptions().isApplicationClassesOnly()),
              result);
    }
    return result;
  }
}
