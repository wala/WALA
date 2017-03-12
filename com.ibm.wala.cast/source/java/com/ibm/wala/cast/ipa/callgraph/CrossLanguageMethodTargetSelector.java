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
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.strings.Atom;

/**
 *  A MethodTargetSelector implementation that supports multiple languages.
 * It works by delegating to a language-specific child selector based on
 * the language associated with the MethodReference for which a target is
 * being chosen.  
 *
 *  This provides a simple way to combine language-specific target
 * selection policies---such as those used for constructor calls in
 * JavaScript and for bean methods in J2EE.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageMethodTargetSelector
    implements MethodTargetSelector 
{

  private final Map<Atom,MethodTargetSelector> languageSelectors;

  public CrossLanguageMethodTargetSelector(Map<Atom, MethodTargetSelector> languageSelectors) {
    this.languageSelectors = languageSelectors;
  }

  private static Atom getLanguage(MethodReference target) {
    return target
	.getDeclaringClass()
	.getClassLoader()
	.getLanguage();
  }

  private static Atom getLanguage(CallSiteReference site) {
    return getLanguage(site.getDeclaredTarget());
  }

  private MethodTargetSelector getSelector(CallSiteReference site) {
    return languageSelectors.get(getLanguage(site));
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    assert getSelector(site) != null: "no selector for " + getLanguage(site) + " method " + site;
    return getSelector(site).getCalleeTarget(caller, site, receiver);
  }

}
