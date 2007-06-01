package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.warnings.*;

import java.util.*;

public class CrossLanguageInstanceKeys implements InstanceKeyFactory {

  private final Map languageSelectors;

  public CrossLanguageInstanceKeys(Map languageSelectors) {
    this.languageSelectors = languageSelectors;
  }

  private static Atom getLanguage(TypeReference type) {
    return type.getClassLoader().getLanguage();
  }

  private static Atom getLanguage(NewSiteReference site) {
    return getLanguage(site.getDeclaredType());
  }

  private static Atom getLanguage(CGNode node) {
    return getLanguage(node.getMethod().getDeclaringClass().getReference());
  }

  private InstanceKeyFactory getSelector(NewSiteReference site) {
    return (InstanceKeyFactory)languageSelectors.get(getLanguage(site));
  }

  private InstanceKeyFactory getSelector(TypeReference type) {
    return (InstanceKeyFactory)languageSelectors.get(getLanguage(type));
  }

  private InstanceKeyFactory getSelector(CGNode node) {
      return (InstanceKeyFactory)languageSelectors.get(getLanguage(node));
  }

  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    return getSelector(allocation).getInstanceKeyForAllocation(node, allocation);
  }

  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
    return getSelector(allocation).getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  public InstanceKey getInstanceKeyForConstant(CGNode node, Object S) {
    return getSelector(node).getInstanceKeyForConstant(node, S);
  }

  public String getStringConstantForInstanceKey(CGNode node, InstanceKey I) {
    return getSelector(node).getStringConstantForInstanceKey(node, I);
  }

  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
    assert getSelector(type) != null : "no instance keys for " + type;
    return getSelector(type).getInstanceKeyForPEI(node, instr, type);
  }

  public InstanceKey getInstanceKeyForClassObject(TypeReference type) {
    return getSelector(type).getInstanceKeyForClassObject(type);
  }

}
