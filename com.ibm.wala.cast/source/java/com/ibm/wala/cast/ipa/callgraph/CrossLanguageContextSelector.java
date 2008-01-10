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
import com.ibm.wala.util.Atom;

/**
 * A ContextSelector implementation adapted to work for analysi across
 * multiple languages.  This context selector delegates to one of several
 * child selectors based on the language of the code body for which a 
 * context is being selected.  
 *
 *  This provides a convenient way to integrate multiple, lanuage-specific
 * specialized context policies---such as the ones used for clone() in
 * Java and runtime primitives in JavaScript.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageContextSelector implements ContextSelector {

  private final Map languageSelectors;

  public CrossLanguageContextSelector(Map languageSelectors) {
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
    return (ContextSelector)languageSelectors.get(getLanguage(site));
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    return getSelector(site).getCalleeTarget(caller, site, callee, receiver);
  }

  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return getSelector(site).contextIsIrrelevant(node, site);
  }

  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return getSelector(site).mayUnderstand(caller, site, targetMethod, instance);
  }


  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return getSelector(site).allSitesDispatchIdentically(node, site);
  }
}
