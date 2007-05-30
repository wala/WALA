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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.Selector;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * This context selector selects a context based on whether the receiver type
 * dispatches to a given method.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class TargetMethodContextSelector implements ContextSelector {

  private final Selector selector;

  public TargetMethodContextSelector(Selector selector, ClassHierarchy cha) {
    this.selector = selector;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getCalleeTarget(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey R) {
    if (R == null) {
      throw new IllegalArgumentException("R is null");
    }

    final IMethod M = R.getConcreteType().getMethod(selector);

    class MethodDispatchContext implements Context {

      private IMethod getTargetMethod() {
        return M;
      }

      public ContextItem get(ContextKey name) {
        if (name.equals(ContextKey.FILTER)) {
          return new FilteredPointerKey.TargetMethodFilter(M);
        } else {
          return null;
        }
      }

      @Override
      public String toString() {
        return "DispatchContext: " + M;
      }

      @Override
      public int hashCode() {
        return M.hashCode();
      }

      @Override
      public boolean equals(Object o) {
        return (o instanceof MethodDispatchContext) && ((MethodDispatchContext) o).getTargetMethod().equals(M);
      }
    }
    ;

    return new MethodDispatchContext();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getBoundOnNumberOfTargets(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference reference, IMethod targetMethod) {
    return -1;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#mayUnderstand(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    // no-op, not bound to warnings
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#contextIsIrrelevant(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#allSitesDispatchIdentically(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return false;
  }
}
