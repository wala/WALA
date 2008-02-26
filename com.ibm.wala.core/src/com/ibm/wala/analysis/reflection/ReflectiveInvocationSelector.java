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

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.types.TypeReference;

/**
 * A {@link ContextSelector} to intercept calls to reflective method invocations such as
 * Constructor.newInstance and Method.invoke
 * 
 * @author pistoia
 * @author sjfink
 */
class ReflectiveInvocationSelector implements ContextSelector {

  public ReflectiveInvocationSelector() {
  }

  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return false;
  }

  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return false;
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (mayUnderstand(caller, site, callee, receiver)) { 
      return new ReceiverInstanceContext(receiver);
    }
    return null;
  }

  /**
   * This object may understand a dispatch to Constructor.newInstance().
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (targetMethod.getReference().equals(ReflectiveInvocationInterpreter.METHOD_INVOKE) || isConstructorConstant(instance)) {
        return true;
    }
    return false;
  }
  
  private boolean isConstructorConstant(InstanceKey instance) {
    if (instance instanceof ConstantKey) {
      ConstantKey c = (ConstantKey) instance;
      if (c.getConcreteType().getReference().equals(TypeReference.JavaLangReflectConstructor)) {
        return true;
      }
    }
    return false;
  }
}