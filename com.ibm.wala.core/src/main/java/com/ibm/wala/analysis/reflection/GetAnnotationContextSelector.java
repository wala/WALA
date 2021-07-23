/*
 * Copyright (c) 2021 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.IParameter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/** A {@link ContextSelector} to intercept calls to Object.getClass() */
public class GetAnnotationContextSelector implements ContextSelector {

  public static final MethodReference GET_ANNOTATION_CLASS =
      MethodReference.findOrCreate(TypeReference.JavaLangClass, "getAnnotation", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;");

  public static final MethodReference GET_ANNOTATION_APPLICATION =
      MethodReference.findOrCreate(TypeReference.JavaLangClassApplication, "getAnnotation", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;");

  public static final MethodReference GET_ANNOTATION_CONSTRUCTOR =
      MethodReference.findOrCreate(TypeReference.JavaLangReflectConstructor, "getAnnotation", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;");

  public static final MethodReference GET_ANNOTATION_FIELD =
      MethodReference.findOrCreate(TypeReference.JavaLangReflectField, "getAnnotation", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;");

  public static final MethodReference GET_ANNOTATION_METHOD =
      MethodReference.findOrCreate(TypeReference.JavaLangReflectMethod, "getAnnotation", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;");

  public static final MethodReference GET_ANNOTATION_PARAMETER =
      MethodReference.findOrCreate(TypeReference.JavaLangReflectParameter, "getAnnotation", "(Ljava/lang/Class;)Ljava/lang/annotation/Annotation;");


  private final IClassHierarchy cha;

  public GetAnnotationContextSelector(IClassHierarchy cha) {
    this.cha = cha;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod,
   *      com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
   */
  @Override
  public Context getCalleeTarget(
      CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    IMethod meth = cha.resolveMethod(site.getDeclaredTarget());
    IClass rec1 = null;

    if (meth != null && (meth.getReference().equals(GET_ANNOTATION_CLASS))) {
      if (receiver[0] instanceof ConstantKey<?> && receiver[1] instanceof ConstantKey<?>) {
        IClass rec0 = (IClass) ((ConstantKey<?>) receiver[0]).getValue();
        rec1 = (IClass) ((ConstantKey<?>) receiver[1]).getValue();

        return new GetAnnotationContext(new PointType(rec0), new PointType(rec1));
      }
    } else if (meth != null && (meth.getReference().equals(GET_ANNOTATION_CONSTRUCTOR))) {
      if (receiver[0] instanceof ConstantKey && receiver[1] instanceof ConstantKey) {
        IMethod ctor =  (IMethod) ((ConstantKey<?>) receiver[0]).getValue();
        rec1 = (IClass) ((ConstantKey<?>) receiver[1]).getValue();

        return new GetAnnotationContext(ctor, new PointType(rec1));
      }
    } else if (meth != null && ( meth.getReference().equals(GET_ANNOTATION_METHOD))) {
      if (receiver[0] instanceof ConstantKey && receiver[1] instanceof ConstantKey) {
        IMethod m =  (IMethod) ((ConstantKey<?>) receiver[0]).getValue();
        rec1 = (IClass) ((ConstantKey<?>) receiver[1]).getValue();

        return new GetAnnotationContext(m, new PointType(rec1));
      }
    } else if (meth != null && (meth.getReference().equals(GET_ANNOTATION_FIELD))) {
      if (receiver[0] instanceof ConstantKey && receiver[1] instanceof ConstantKey) {
        IField fld =  (IField) ((ConstantKey<?>) receiver[0]).getValue();
        rec1 = (IClass) ((ConstantKey<?>) receiver[1]).getValue();

        return new GetAnnotationContext(fld, new PointType(rec1));
      }
    } else if (meth != null && (meth.getReference().equals(GET_ANNOTATION_PARAMETER))) {
      if (receiver[0] instanceof ConstantKey && receiver[1] instanceof ConstantKey) {
        IParameter prm =  (IParameter) ((ConstantKey<?>) receiver[0]).getValue();
        rec1 = (IClass) ((ConstantKey<?>) receiver[1]).getValue();

        return new GetAnnotationContext(prm, new PointType(rec1));
      }
    }
    return null;
  }

  private static final IntSet thisParameter = IntSetUtil.make(new int[] {0, 1});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    IMethod meth = cha.resolveMethod(site.getDeclaredTarget());
    if (meth != null && (meth.getReference().equals(GET_ANNOTATION_CLASS)
        || meth.getReference().equals(GET_ANNOTATION_CONSTRUCTOR)
        || meth.getReference().equals(GET_ANNOTATION_METHOD)
        || meth.getReference().equals(GET_ANNOTATION_FIELD)
        || meth.getReference().equals(GET_ANNOTATION_PARAMETER)
    )) {
      return thisParameter;
    } else {
      return EmptyIntSet.instance;
    }
  }
}

