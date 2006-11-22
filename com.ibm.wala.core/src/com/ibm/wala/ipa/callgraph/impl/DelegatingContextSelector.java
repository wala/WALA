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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * A context selector that first checks with A, then defaults to B.
 * 
 * @author sfink
 */
public class DelegatingContextSelector implements ContextSelector {

  private static final boolean DEBUG = false;

  private final ContextSelector A;
  private final ContextSelector B;

  public DelegatingContextSelector(ContextSelector A, ContextSelector B) {
    this.A = A;
    this.B = B;
    if (Assertions.verifyAssertions) {
      Assertions._assert(A != null, "A is null");
      Assertions._assert(B != null, "B is null");
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.detox.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {

    if (DEBUG) {
      Trace.println("getCalleeTarget " + caller + " " + site + " " + callee);
    }
    if (A != null) {
      Context C = A.getCalleeTarget(caller, site, callee, receiver);
      if (C != null) {
        if (DEBUG) {
          Trace.println("Case A " + A.getClass() + " " + C);
        }
        return C;
      }
    }
    Context C = B.getCalleeTarget(caller, site, callee, receiver);
    if (DEBUG) {
      Trace.println("Case B " + B.getClass() + " " + C);
    }
    return C;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getBoundOnNumberOfTargets(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod targetMethod) {
    if (A.mayUnderstand(caller,site,targetMethod,null)) {
      return A.getBoundOnNumberOfTargets(caller,site,targetMethod);
    } else {
      return B.getBoundOnNumberOfTargets(caller,site,targetMethod);
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#mayUnderstand(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (A != null) {
      if (A.mayUnderstand(caller,site,targetMethod, instance)) {
        return true;
      }
    }
    return B.mayUnderstand(caller,site,targetMethod, instance);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    if (A != null) {
      A.setWarnings(newWarnings);
    }
   B.setWarnings(newWarnings);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#contextIsIrrelevant(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return A.contextIsIrrelevant(node,site) && B.contextIsIrrelevant(node,site);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#contextIsIrrelevant(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return A.allSitesDispatchIdentically(node,site) && B.allSitesDispatchIdentically(node,site);
  }
}