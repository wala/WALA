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

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;

import java.util.*;

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

  private final Map languageSelectors;

  public CrossLanguageClassTargetSelector(Map languageSelectors) {
    this.languageSelectors = languageSelectors;
  }

  private static Atom getLanguage(NewSiteReference target) {
    return target
	.getDeclaredType()
	.getClassLoader()
	.getLanguage();
  }

  private ClassTargetSelector getSelector(NewSiteReference site) {
    return (ClassTargetSelector)languageSelectors.get(getLanguage(site));
  }

  public IClass getAllocatedTarget(CGNode caller, NewSiteReference site) {
    return getSelector(site).getAllocatedTarget(caller, site);
  }

}

