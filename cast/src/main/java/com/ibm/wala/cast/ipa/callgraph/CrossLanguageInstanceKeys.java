/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKeyFactory;
import com.ibm.wala.types.TypeReference;
import java.util.Map;

/**
 * An InstanceKeyFactory implementation that is designed to support multiple languages. This
 * implementation delegates to one of several child instance key factories based on the language
 * associated with the IClass or TypeReference for which an instance key is being chosen.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CrossLanguageInstanceKeys implements InstanceKeyFactory {

  private final Map<Atom, InstanceKeyFactory> languageSelectors;

  public CrossLanguageInstanceKeys(Map<Atom, InstanceKeyFactory> languageSelectors) {
    this.languageSelectors = languageSelectors;
  }

  private static Atom getLanguage(TypeReference type) {
    return type.getClassLoader().getLanguage();
  }

  private static Atom getLanguage(NewSiteReference site) {
    return getLanguage(site.getDeclaredType());
  }

  //  private static Atom getLanguage(CGNode node) {
  //    return getLanguage(node.getMethod().getDeclaringClass().getReference());
  //  }

  private InstanceKeyFactory getSelector(NewSiteReference site) {
    return languageSelectors.get(getLanguage(site));
  }

  private InstanceKeyFactory getSelector(TypeReference type) {
    return languageSelectors.get(getLanguage(type));
  }

  @Override
  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
    return getSelector(allocation).getInstanceKeyForAllocation(node, allocation);
  }

  @Override
  public InstanceKey getInstanceKeyForMultiNewArray(
      CGNode node, NewSiteReference allocation, int dim) {
    return getSelector(allocation).getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  @Override
  public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
    return getSelector(type).getInstanceKeyForConstant(type, S);
  }

  @Override
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) {
    assert getSelector(type) != null : "no instance keys for " + type;
    return getSelector(type).getInstanceKeyForPEI(node, instr, type);
  }

  @Override
  public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
    return getSelector(objType).getInstanceKeyForMetadataObject(obj, objType);
  }
}
