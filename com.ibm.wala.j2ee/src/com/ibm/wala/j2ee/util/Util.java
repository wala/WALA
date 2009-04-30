/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.j2ee.util;

import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.j2ee.BeanMetaData;
import com.ibm.wala.j2ee.DeploymentMetaData;
import com.ibm.wala.j2ee.J2EEClassTargetSelector;
import com.ibm.wala.j2ee.J2EEMethodTargetSelector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * Miscellaneous J2EE Utilities
 */
public class Util {

  public static void addJ2EEBypassLogic(AnalysisOptions options, AnalysisScope scope, DeploymentMetaData dmd, IClassHierarchy cha,
      ReceiverTypeInferenceCache typeInference) {

    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    MethodTargetSelector ms = new J2EEMethodTargetSelector(scope, options.getMethodTargetSelector(), dmd, cha, typeInference);
    options.setSelector(ms);

    ClassTargetSelector cs = new J2EEClassTargetSelector(options.getClassTargetSelector(), dmd, cha, cha.getLoader(scope
        .getLoader(Atom.findOrCreateUnicodeAtom("Synthetic"))));
    options.setSelector(cs);
  }

  /**
   * @param cha governing class hierarchy
   * @return the Set of CMR fields for this bean, including inherited CMRs
   */
  public static Set<Object> getCMRFields(BeanMetaData bean, DeploymentMetaData dmd, ClassHierarchy cha) {
    Set<Object> result = HashSetFactory.make(5);
    TypeReference T = bean.getEJBClass();
    while (T != null) {
      BeanMetaData B = dmd.getBeanMetaData(T);
      if (B != null) {
        result.addAll(B.getCMRFields());
      }
      IClass klass = cha.lookupClass(T);
      if (Assertions.verifyAssertions) {
        assert klass != null;
      }
      IClass superKlass = klass.getSuperclass();
      T = (superKlass == null) ? null : superKlass.getReference();
    }
    return result;
  }

  private static final String benignExtSpec = "benignext.xml";

  public static void addDefaultJ2EEBypassLogic(AnalysisOptions options, AnalysisScope scope, ClassLoader cl, IClassHierarchy cha) {
    com.ibm.wala.ipa.callgraph.impl.Util.addDefaultBypassLogic(options, scope, cl, cha);
    com.ibm.wala.ipa.callgraph.impl.Util.addBypassLogic(options, scope, cl, benignExtSpec, cha);
  }

}
