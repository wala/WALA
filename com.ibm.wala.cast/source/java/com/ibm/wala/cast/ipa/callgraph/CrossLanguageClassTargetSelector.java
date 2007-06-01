package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;

import java.util.*;

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

