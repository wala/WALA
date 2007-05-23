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

import com.ibm.wala.analysis.reflection.JavaTypeContext;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * This context selector selects a context based on the concrete type of the
 * receiver.
 * 
 * @author sfink
 */
public class ReceiverTypeContextSelector implements ContextSelector {

  public ReceiverTypeContextSelector() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PropagationContextSelector#getCalleeTarget(com.ibm.detox.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (receiver == null) {
      throw new IllegalArgumentException("receiver is null");
    }
    PointType P = new PointType(receiver.getConcreteType());
    return new JavaTypeContext(P);
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
