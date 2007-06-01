package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.warnings.*;

import java.util.*;

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

  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference reference, IMethod targetMethod) {
    return getSelector(reference).getBoundOnNumberOfTargets(caller, reference, targetMethod);
  }

  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return getSelector(site).mayUnderstand(caller, site, targetMethod, instance);
  }

  public void setWarnings(WarningSet newWarnings) {
    for(Iterator ss = languageSelectors.values().iterator(); ss.hasNext(); ) {  
      ((ContextSelector)ss.next()).setWarnings(newWarnings);
    }
  }

  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return getSelector(site).allSitesDispatchIdentically(node, site);
  }
}
