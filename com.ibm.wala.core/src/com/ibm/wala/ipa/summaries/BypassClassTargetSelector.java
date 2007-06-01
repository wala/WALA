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
package com.ibm.wala.ipa.summaries;

import java.util.Set;

import com.ibm.wala.analysis.reflection.Malleable;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

/**
 * 
 * A ClassTargetSelector that looks up the declared type of a NewSiteReference
 * based on bypass rules.
 * 
 * @author dolby
 * @author sfink
 */
public class BypassClassTargetSelector implements ClassTargetSelector {
  private static final boolean DEBUG = false;

  /**
   * Set of TypeReference that should be considered allocatable
   */
  private final Set<TypeReference> allocatableTypes;

  /**
   * Governing class hierarchy
   */
  private final IClassHierarchy cha;

  /**
   * Delegate
   */
  private final ClassTargetSelector parent;

  /**
   * class loader used for synthetic classes
   */
  private final BypassSyntheticClassLoader bypassLoader;

  public BypassClassTargetSelector(ClassTargetSelector parent, Set<TypeReference> allocatableTypes, IClassHierarchy cha, IClassLoader bypassLoader) {
    if (Assertions.verifyAssertions) {
      if (!(bypassLoader instanceof BypassSyntheticClassLoader)) {
        Assertions._assert(false, "unexpected bypass loader: " + bypassLoader.getClass());
      }
    }

    this.allocatableTypes = allocatableTypes;
    this.bypassLoader = (BypassSyntheticClassLoader) bypassLoader;
    this.parent = parent;
    this.cha = cha;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.ClassTargetSelector#getAllocatedTarget(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.NewSiteReference)
   */
  public IClass getAllocatedTarget(CGNode caller, NewSiteReference site) {

    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    TypeReference nominalRef = site.getDeclaredType();
    if (DEBUG) {
      Trace.println("BypassClassTargetSelector getAllocatedTarget: " + nominalRef);
    }

    if (Malleable.isMalleable(nominalRef)) {
      return null;
    }
    IClass realType = cha.lookupClass(nominalRef);
    if (realType == null) {
      if (DEBUG) {
        Trace.println("cha lookup failed.  Delegating to " + parent.getClass());
      }
      return parent.getAllocatedTarget(caller, site);
    }
    TypeReference realRef = realType.getReference();

    if (allocatableTypes.contains(realRef)) {
      if (DEBUG) {
        Trace.println("allocatableType! ");
      }
      if (realType.isAbstract() || realType.isInterface()) {
        TypeName syntheticName = BypassSyntheticClass.getName(realRef);
        IClass result = bypassLoader.lookupClass(syntheticName, realType.getClassHierarchy());
        if (result != null) {
          return result;
        } else {
          IClass x = new BypassSyntheticClass(realType, bypassLoader, cha);
          bypassLoader.registerClass(syntheticName, x);
          return x;
        }
      } else {
        return realType;
      }
    } else {
      if (DEBUG) {
        Trace.println("not allocatable.  Delegating to " + parent.getClass());
      }
      return parent.getAllocatedTarget(caller, site);
    }
  }
}