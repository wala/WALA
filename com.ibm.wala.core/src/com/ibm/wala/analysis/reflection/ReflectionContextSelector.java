/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.DelegatingContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * A {@link ContextSelector} to handle default reflection logic.
 * 
 * @author sjfink
 */
public class ReflectionContextSelector extends DelegatingContextSelector {

  public static ReflectionContextSelector createReflectionContextSelector(IClassHierarchy cha,
      MethodTargetSelector methodTargetSelector) {
    return new ReflectionContextSelector(cha, methodTargetSelector);
  }

  /**
   * First check "forName" logic, then factory logic.
   */
  private ReflectionContextSelector(IClassHierarchy cha, MethodTargetSelector methodTargetSelector) {
    super(new ConstructorNewInstanceContextSelector(),
        new DelegatingContextSelector(new GetConstructorContextSelector(),
            new DelegatingContextSelector(new DelegatingContextSelector(new ForNameContextSelector(), new ClassNewInstanceContextSelector()),
        new FactoryContextSelector(cha, methodTargetSelector))));
  }
}
