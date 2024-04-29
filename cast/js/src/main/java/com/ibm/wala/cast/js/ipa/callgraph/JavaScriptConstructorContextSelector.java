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

import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptConstructorFunctions.JavaScriptConstructor;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneLevelSiteContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.cfa.nCFAContextSelector;
import com.ibm.wala.util.intset.IntSet;

public class JavaScriptConstructorContextSelector implements ContextSelector {
  private final ContextSelector base;

  /**
   * for generating contexts with one-level of call strings, to match standard Andersen's heap
   * abstraction
   */
  private final nCFAContextSelector oneLevelCallStrings;

  private final OneLevelSiteContextSelector oneLevelCallerSite;

  public JavaScriptConstructorContextSelector(ContextSelector base) {
    this.base = base;
    this.oneLevelCallStrings = new nCFAContextSelector(1, base);
    this.oneLevelCallerSite = new OneLevelSiteContextSelector(base);
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return base.getRelevantParameters(caller, site);
  }

  @Override
  public Context getCalleeTarget(
      final CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee instanceof JavaScriptConstructor) {
      final Context oneLevelCallStringContext =
          oneLevelCallStrings.getCalleeTarget(caller, site, callee, receiver);
      if (AstLexicalInformation.hasExposedUses(caller, site)) {
        // use a caller-site context, to enable lexical scoping lookups (via caller CGNode)
        return oneLevelCallerSite.getCalleeTarget(caller, site, callee, receiver);
      } else {
        // use at least one-level of call-string sensitivity for constructors
        // always
        return oneLevelCallStringContext;
      }
    } else {
      return base.getCalleeTarget(caller, site, callee, receiver);
    }
  }
}
