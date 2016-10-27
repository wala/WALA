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

import java.util.Collection;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * A {@link ContextSelector} to intercept calls to certain methods on java.lang.Class when the receiver is a type constant
 * 
 * Currently supported methods:
 * <ul>
 * <li>getConstructor
 * <li>getConstructors
 * <li>getDeclaredMethod
 * <li>getMethods
 * </ul>
 */
class JavaLangClassContextSelector implements ContextSelector {

  public JavaLangClassContextSelector() {
  }

  /**
   * If the {@link CallSiteReference} invokes a method we understand and c is a type constant, return a {@link JavaTypeContext}
   * representing the type named by s, if we can resolve it in the {@link IClassHierarchy}.
   */
  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (receiver != null && receiver.length > 0 && mayUnderstand(caller, site, callee, receiver[0])) {
      return new JavaTypeContext(new PointType(getTypeConstant(receiver[0])));
    }
    return null;
  }

  private IClass getTypeConstant(InstanceKey instance) {
    if (instance instanceof ConstantKey) {
      ConstantKey c = (ConstantKey) instance;
      if (c.getValue() instanceof IClass) {
        return (IClass) c.getValue();
      }
    }
    return null;
  }

  private static final Collection<MethodReference> UNDERSTOOD_METHOD_REFS = HashSetFactory.make();

  static {
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_CONSTRUCTOR);
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_CONSTRUCTORS);
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_METHOD);
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_METHODS);
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_DECLARED_CONSTRUCTOR);
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_DECLARED_CONSTRUCTORS);
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_DECLARED_METHOD);
    UNDERSTOOD_METHOD_REFS.add(JavaLangClassContextInterpreter.GET_DECLARED_METHODS);
  }

  /**
   * This object may understand a dispatch to Class.getContructor when the receiver is a type constant.
   */
  private boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    return UNDERSTOOD_METHOD_REFS.contains(targetMethod.getReference()) && getTypeConstant(instance) != null;
  }

  private static final IntSet thisParameter = IntSetUtil.make(new int[]{0});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (UNDERSTOOD_METHOD_REFS.contains(site.getDeclaredTarget())) {
      return thisParameter;
    } else {
      return EmptyIntSet.instance;
    }
  }
}