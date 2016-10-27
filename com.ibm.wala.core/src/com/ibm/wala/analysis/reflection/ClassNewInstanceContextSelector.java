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
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * A {@link ContextSelector} to intercept calls to Class.newInstance()
 */
class ClassNewInstanceContextSelector implements ContextSelector {

  public ClassNewInstanceContextSelector() {
  }

  /**
   * If receiver is a {@link ConstantKey} whose value is an {@link IClass}, return a {@link JavaTypeContext}
   * representing the type of the IClass. (This corresponds to the case where we know the exact type that will be
   * allocated by the <code>Class.newInstance()</code> call.)  Otherwise, return <code>null</code>.    
   */
  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee.getReference().equals(ClassNewInstanceContextInterpreter.CLASS_NEW_INSTANCE_REF) && isTypeConstant(receiver[0])) {
      IClass c = (IClass) ((ConstantKey) receiver[0]).getValue();
      if (!c.isAbstract() && !c.isInterface()) {
        return new JavaTypeContext(new PointType(c));
      }
    }
    return null;
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

  private static final IntSet thisParameter = IntSetUtil.make(new int[]{0});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (ClassNewInstanceContextInterpreter.CLASS_NEW_INSTANCE_REF.equals(site.getDeclaredTarget())) {
      return thisParameter;
    } else {
      return EmptyIntSet.instance;
    }
  }
}