/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.demandpa.alg.refinepolicy;

import java.util.Collection;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.demandpa.alg.statemachine.StateMachine;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;

public class TunedFieldRefinementPolicy implements FieldRefinePolicy {

  private static final boolean DEBUG = false;

  private final IClassHierarchy cha;

  private final Collection<IClass> typesToRefine = HashSetFactory.make();

  private IClass firstSkippedClass = null;

  @Override
  public boolean nextPass() {
    if (firstSkippedClass != null) {
      typesToRefine.add(firstSkippedClass);
      if (DEBUG) {
        System.err.println("now refining " + firstSkippedClass);
      }
      firstSkippedClass = null;
      return true;
    } else {
      return false;
    }
  }

  @Override
  public boolean shouldRefine(IField field, PointerKey basePtr, PointerKey val, IFlowLabel label, StateMachine.State state) {
    if (field == null) {
      throw new IllegalArgumentException("null field");
    }
    if (field == ArrayContents.v()) {
      return true;
    }
    IClass classToCheck = removeInner(field.getDeclaringClass());
    if (superOfAnyEncountered(classToCheck)) {
      return true;
    } else {
      if (firstSkippedClass == null) {
        firstSkippedClass = classToCheck;
      }
      return false;
    }
  }

  private boolean superOfAnyEncountered(IClass klass) {
    for (IClass toRefine : typesToRefine) {
      if (cha.isAssignableFrom(klass, toRefine) || cha.isAssignableFrom(toRefine, klass)) {
        return true;
      }
    }
    return false;
  }

  /**
   * 
   * @param klass
   * @return the top-level {@link IClass} where klass is declared, or klass itself if klass is top-level or if top-level
   *         class not loaded
   */
  private IClass removeInner(IClass klass) {
    ClassLoaderReference cl = klass.getClassLoader().getReference();
    String klassStr = klass.getName().toString();
    int dollarIndex = klassStr.indexOf('$');
    if (dollarIndex == -1) {
      return klass;
    } else {
      String topMostName = klassStr.substring(0, dollarIndex);
      IClass topMostClass = cha.lookupClass(TypeReference.findOrCreate(cl, topMostName));
      return (topMostClass != null) ? topMostClass : klass;
    }
  }

  public TunedFieldRefinementPolicy(IClassHierarchy cha) {
    this.cha = cha;
  }

}
