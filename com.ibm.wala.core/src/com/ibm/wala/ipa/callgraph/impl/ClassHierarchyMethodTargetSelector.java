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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

/**
 * A {@link MethodTargetSelector} that simply looks up the declared type, name and descriptor of a {@link CallSiteReference} in the
 * appropriate class hierarchy.
 */
public class ClassHierarchyMethodTargetSelector implements MethodTargetSelector {

  /**
   * Governing class hierarchy
   */
  private final IClassHierarchy classHierarchy;

  /**
   * Initialization. The class hierarchy is needed for lookups and the warnings are used when the lookups fails (which should never
   * happen).
   * 
   * @param cha The class hierarchy to use.
   */
  public ClassHierarchyMethodTargetSelector(IClassHierarchy cha) {
    classHierarchy = cha;
  }

  /**
   * This target selector searches the class hierarchy for the method matching the signature of the call that is appropriate for the
   * receiver type.
   * 
   * @throws IllegalArgumentException if call is null
   */
  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference call, IClass receiver) {

    if (call == null) {
      throw new IllegalArgumentException("call is null");
    }
    IClass klass;
    TypeReference targetType = call.getDeclaredTarget().getDeclaringClass();

    // java virtual calls
    if (call.isDispatch()) {
      assert receiver != null : "null receiver for " + call;
      klass = receiver;

      // java static calls
    } else if (call.isFixed()) {
      klass = classHierarchy.lookupClass(targetType);
      if (klass == null) {
        return null;
      }
      // anything else
    } else {
      return null;
    }

    return classHierarchy.resolveMethod(klass, call.getDeclaredTarget().getSelector());
  }

  public boolean mightReturnSyntheticMethod() {
    return false;
  }
}
