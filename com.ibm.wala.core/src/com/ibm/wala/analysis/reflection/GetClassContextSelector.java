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
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * A {@link ContextSelector} to intercept calls to Object.getClass()
 */
class GetClassContextSelector implements ContextSelector {

  public final static MethodReference GET_CLASS = MethodReference.findOrCreate(TypeReference.JavaLangObject, "getClass",
      "()Ljava/lang/Class;");

  public GetClassContextSelector() {
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (callee.getReference().equals(GET_CLASS)) {
      return new JavaTypeContext(new PointType(receiver.getConcreteType()));
    }
    return null;
  }
}