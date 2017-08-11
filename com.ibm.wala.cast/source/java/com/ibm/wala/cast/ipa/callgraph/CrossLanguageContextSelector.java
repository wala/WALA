/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.ipa.callgraph;

import java.util.Map;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;

/**
 * A ContextSelector implementation adapted to work for analysis across
 * multiple languages.  This context selector delegates to one of several
 * child selectors based on the language of the code body for which a 
 * context is being selected.  
 *
 *  This provides a convenient way to integrate multiple, language-specific
 * specialized context policies---such as the ones used for clone() in
 * Java and runtime primitives in JavaScript.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageContextSelector implements ContextSelector {

  private final Map<Atom, ContextSelector> languageSelectors;

  public CrossLanguageContextSelector(Map<Atom, ContextSelector> languageSelectors) {
    this.languageSelectors = languageSelectors;
  }

  private static Atom getLanguage(CallSiteReference site) {
    return site
	.getDeclaredTarget()
	.getDeclaringClass()
	.getClassLoader()
	.getLanguage();
  }

  private ContextSelector getSelector(CallSiteReference site) {
    return languageSelectors.get(getLanguage(site));
  }

  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    return getSelector(site).getCalleeTarget(caller, site, callee, receiver);
  }

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return getSelector(site).getRelevantParameters(caller, site);
  }
}
