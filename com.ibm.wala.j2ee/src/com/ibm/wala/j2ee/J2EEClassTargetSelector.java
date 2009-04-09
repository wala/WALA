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
package com.ibm.wala.j2ee;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassSyntheticClass;
import com.ibm.wala.ipa.summaries.BypassSyntheticClassLoader;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

/**
 * A class that selects concrete types for new statements.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 * @author Stephen Fink
 */
public class J2EEClassTargetSelector implements ClassTargetSelector {

  private static final boolean DEBUG = false;

  /**
   * A delegate if the j2ee rules fail
   */
  private final ClassTargetSelector parent;

  /**
   * Governing deployment information
   */
  private final DeploymentMetaData metaData;

  /**
   * Governing class hierarchy
   */
  private final IClassHierarchy cha;

  private final BypassSyntheticClassLoader bypassLoader;

  /**
   * @param parent
   *          a target selector to delegate to if logic here fails
   * @param metaData
   *          information about the deployment descriptor
   * @param cha
   *          governing class hierarchy
   * @param bypassLoader
   *          class loader to deal with J2EE bypass logic
   */
  public J2EEClassTargetSelector(ClassTargetSelector parent, DeploymentMetaData metaData, IClassHierarchy cha,
      IClassLoader bypassLoader) {
    this.cha = cha;
    this.parent = parent;
    this.metaData = metaData;
    this.bypassLoader = (BypassSyntheticClassLoader) bypassLoader;

    assert metaData != null;
    IClass x = new J2EEContainerModel(metaData, cha);
    this.bypassLoader.registerClass(J2EEContainerModel.containerModelName, x);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.detox.ipa.callgraph.ClassTargetSelector#getAllocatedTarget(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.NewSiteReference)
   */

  public IClass getAllocatedTarget(CGNode caller, NewSiteReference site) {

    TypeReference nominalRef = site.getDeclaredType();
    if (DEBUG) {
      System.err.println(("J2EEClassTargetSelector getAllocatedTarget: " + nominalRef));
    }
    if (Assertions.verifyAssertions) {
      if (nominalRef == null) {
        Assertions._assert(nominalRef != null, "null declared type in site " + site);
      }
    }
    IClass realType = cha.lookupClass(nominalRef);
    if (realType == null) {
      if (DEBUG) {
        System.err.println(("cha lookup failed.  Delegating to " + parent.getClass()));
      }
      return parent.getAllocatedTarget(caller, site);
    }
    TypeReference realRef = realType.getReference();

    if (metaData.isContainerManaged(realRef) || metaData.isEJBInterface(realRef)) {
      if (realType.isAbstract() || realType.isInterface()) {
        TypeName syntheticName = BypassSyntheticClass.getName(realRef);
        IClass result = bypassLoader.lookupClass(syntheticName);
        if (result != null) {
          return result;
        } else {
          IClass x = new BypassSyntheticClass(realType, bypassLoader, cha);
          bypassLoader.registerClass(syntheticName, x);
          return x;
        }
      } else {
        if (Assertions.verifyAssertions) {
          if (realType.isInterface()) {
            Assertions.UNREACHABLE("did not hijack allocation of " + realType);
          }
        }
        return realType;
      }
    } else {
      if (DEBUG) {
        System.err.println(("Not bypassed.  Delegating to " + parent.getClass()));
      }
      return parent.getAllocatedTarget(caller, site);
    }
  }

}
