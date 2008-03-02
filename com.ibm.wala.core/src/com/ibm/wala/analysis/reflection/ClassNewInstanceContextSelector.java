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

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

/**
 * A {@link ContextSelector} to intercept calls to Class.newInstance()
 * 
 * @author pistoia
 */
class ClassNewInstanceContextSelector implements ContextSelector {

  public ClassNewInstanceContextSelector() {
  }

  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
    return false;
  }

  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    return false;
  }

  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (mayUnderstand(caller, site, callee, receiver)) {
      IClass c = (IClass) ((ConstantKey) receiver).getValue();
      return new JavaTypeContext(new PointType(c));
    }
    return null;
  }

  /**
   * This object may understand a dispatch to Class.newInstance() when the class is a class constant.
   */
  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (targetMethod.getReference().equals(ClassNewInstanceContextInterpreter.CLASS_NEW_INSTANCE_REF) && isTypeConstant(instance)) {
        return true;
    }
    return false;
  }
  
  private boolean isTypeConstant(InstanceKey instance) {
    if (instance instanceof ConstantKey) {
      ConstantKey c = (ConstantKey) instance;
      if (c.getValue() instanceof IClass) {
        return true;
      }
    }
    return false;
  }
}