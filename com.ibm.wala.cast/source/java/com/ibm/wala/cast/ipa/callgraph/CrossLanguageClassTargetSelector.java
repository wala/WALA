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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.util.strings.Atom;

/**
 *  A ClassTargetSelector implementation that delegates to one of several
 * child selectors based on the language of the type being allocated.  This
 * selector uses the language associated with the TypeReference of the
 * allocated type to delagate t =o the appropriate language-specific 
 * selector.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageClassTargetSelector implements ClassTargetSelector {

  private final Map<Atom, ClassTargetSelector> languageSelectors;

  public CrossLanguageClassTargetSelector(Map<Atom, ClassTargetSelector> languageSelectors) {
    this.languageSelectors = languageSelectors;
  }

  private static Atom getLanguage(NewSiteReference target) {
    return target
	.getDeclaredType()
	.getClassLoader()
	.getLanguage();
  }

  private ClassTargetSelector getSelector(NewSiteReference site) {
    return languageSelectors.get(getLanguage(site));
  }

  @Override
  public IClass getAllocatedTarget(CGNode caller, NewSiteReference site) {
    return getSelector(site).getAllocatedTarget(caller, site);
  }

}

