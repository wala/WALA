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

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.cha.ClassHierarchy;

/**
 * A ClassTargetSelector that simply looks up the declared type of a
 * NewSiteReference in the appropriate class hierarchy.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class ClassHierarchyClassTargetSelector implements ClassTargetSelector {

  private final ClassHierarchy cha;

  /**
   * @param cha governing class hierarchy
   */
  public ClassHierarchyClassTargetSelector(ClassHierarchy cha) {
    this.cha = cha;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.callgraph.ClassTargetSelector#getAllocatedTarget(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.NewSiteReference)
   */
  public IClass getAllocatedTarget(CGNode caller, NewSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    IClass klass = cha.lookupClass(site.getDeclaredType());
    if (klass == null) {
      return null;
    } else if (klass.isAbstract()) {
      return null;
    } else {
      return klass;
    }
  }
}