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

import com.ibm.wala.analysis.reflection.CloneInterpreter;
import com.ibm.wala.analysis.reflection.Malleable;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.warnings.WarningSet;

/**
 *
 * This context selector selects a context based on the concrete type of
 * the receiver to a call of java.lang.Object.clone
 * 
 * @author sfink
 */
public class CloneContextSelector implements ContextSelector {

  private final ReceiverTypeContextSelector selector;
  
  public CloneContextSelector(ClassHierarchy cha) {
    this.selector = new ReceiverTypeContextSelector(cha);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getCalleeTarget(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod, com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (receiver == null) {
      return null;
    }
    if (Malleable.isMalleable(receiver.getConcreteType().getReference())) {
      // don't try to clone Malleable
      return null;
    }
    if (callee.getReference().equals(CloneInterpreter.CLONE)) {
      return selector.getCalleeTarget(caller,site,callee,receiver);
    } else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getBoundOnNumberOfTargets(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference reference, IMethod targetMethod) {
    return -1;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#mayUnderstand(com.ibm.detox.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod)
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (targetMethod == null) {
      throw new IllegalArgumentException("targetMethod is null");
    }
    return targetMethod.getReference().equals(CloneInterpreter.CLONE);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    // no-op, this object not bound to warnings
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#contextIsIrrelevant(com.ibm.wala.ipa.callgraph.CGNode, com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    if (!site.getDeclaredTarget().equals(CloneInterpreter.CLONE)) {
      return true;
    } else {
      return false;
    }
  }

  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    if (!site.getDeclaredTarget().equals(CloneInterpreter.CLONE)) {
      return true;
    } else {
      return false;
    }
  }

}
