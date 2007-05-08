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
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * For synthetic methods marked as "Factories", we analyze in a context defined by the caller.
 *
 * @author sfink
 */
public class FactoryContextSelector implements ContextSelector {


  private final MethodTargetSelector methodTargetSelector;
  
  private final ClassHierarchy cha;
  
  /**
   * @param methodTargetSelector
   */
  public FactoryContextSelector(ClassHierarchy cha, MethodTargetSelector methodTargetSelector) {
    this.cha = cha;
    this.methodTargetSelector = methodTargetSelector;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (callee.isSynthetic()) {
      SyntheticMethod s = (SyntheticMethod) callee;
      if (s.isFactoryMethod()) {
        return new CallerSiteContext(caller, site);
      }
    }
    return null;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#mayUnderstand(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (targetMethod == null) {
      throw new IllegalArgumentException("targetMethod is null");
    }
    if (targetMethod.isSynthetic()) {
      SyntheticMethod s = (SyntheticMethod) targetMethod;
      if (s.isFactoryMethod()) {
        return true;
      }
    }
    return false;
  }


  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getBoundOnNumberOfTargets(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod callee) {
    if (callee.isSynthetic()) {
      SyntheticMethod s = (SyntheticMethod) callee;
      if (s.isFactoryMethod()) {
        return 1;
      }
    }
    return -1;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    // this object is not bound to a WarningSet
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    boolean result = methodTargetSelector.mightReturnSyntheticMethod(node,site);
    if (result) {
      IMethod callee = methodTargetSelector.getCalleeTarget(node, site, null);
      if (callee != null && callee.isSynthetic()) {
        SyntheticMethod s = (SyntheticMethod) callee;
        if (s.isFactoryMethod()) {
          return false;
        } else {
          return true;
        }
      } else {
        return true;
      }
    } else {
      return true;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    boolean result = methodTargetSelector.mightReturnSyntheticMethod(node,site);
    if (result) {
      IClass recv = cha.lookupClass(site.getDeclaredTarget().getDeclaringClass());
      if (recv == null) {
        return false;
      }
      IMethod callee = methodTargetSelector.getCalleeTarget(node, site, recv);
      if (callee != null && callee.isSynthetic()) {
        SyntheticMethod s = (SyntheticMethod) callee;
        if (s.isFactoryMethod()) {
          return false;
        } else {
          return true;
        }
      } else {
        return true;
      }
    } else {
      return true;
    }
  }
}
