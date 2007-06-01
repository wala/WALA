package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;

import java.util.*;

public class CrossLanguageMethodTargetSelector
    implements MethodTargetSelector 
{

  private final Map languageSelectors;

  public CrossLanguageMethodTargetSelector(Map languageSelectors) {
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

  private MethodTargetSelector getSelector(MethodReference site) {
    return (MethodTargetSelector)languageSelectors.get(getLanguage(site));
  }

  private MethodTargetSelector getSelector(CallSiteReference site) {
    return (MethodTargetSelector)languageSelectors.get(getLanguage(site));
  }

  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    assert getSelector(site) != null: "no selector for " + getLanguage(site) + " method " + site;
    return getSelector(site).getCalleeTarget(caller, site, receiver);
  }

  public boolean mightReturnSyntheticMethod(CGNode caller, CallSiteReference site) {
    return getSelector(site).mightReturnSyntheticMethod(caller, site);
  }

  public boolean mightReturnSyntheticMethod(MethodReference declaredTarget) {
    return getSelector(declaredTarget).mightReturnSyntheticMethod(declaredTarget);
  }

}
