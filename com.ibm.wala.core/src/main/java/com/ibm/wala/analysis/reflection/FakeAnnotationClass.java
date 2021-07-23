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

import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;

/** A synthetic class for the fake root method. */
public class FakeAnnotationClass extends SyntheticClass {
  public static final TypeReference fakeAnnotationClass(ClassLoaderReference clr) {
    return TypeReference.findOrCreate(clr, TypeName.string2TypeName("Lcom/ibm/wala/FakeAnnotationClass"));
  }

  private Map<Atom, IField> fakeAnnotationFields = null;

  private final Map<Selector, IMethod> fakeAnnotationMethods = HashMapFactory.make();

  private IClass iinterface;

  public FakeAnnotationClass(ClassLoaderReference clr, IClassHierarchy cha, IClass iinterface) {
    this(fakeAnnotationClass(clr), cha);
    this.iinterface = iinterface;
  }

  public FakeAnnotationClass(TypeReference typeRef, IClassHierarchy cha) {
    super(typeRef, cha);
  }

  @Override
  public IClassLoader getClassLoader() {
    return getClassHierarchy().getLoader(getReference().getClassLoader());
  }

  public void addMethod(IMethod m) {
    assert (this.fakeAnnotationMethods != null);
    if (!fakeAnnotationMethods.containsKey(m.getSelector())) {
      this.fakeAnnotationMethods.put(m.getSelector(), m);
    }
  }

  public void addField(final Atom name, final TypeReference fieldType) {
    if (fakeAnnotationFields == null) {
      fakeAnnotationFields = HashMapFactory.make(2);
    }

    fakeAnnotationFields.put(
        name,
        new IField() {
          @Override
          public IClassHierarchy getClassHierarchy() {
            return com.ibm.wala.analysis.reflection.FakeAnnotationClass.this.getClassHierarchy();
          }

          @Override
          public TypeReference getFieldTypeReference() {
            return fieldType;
          }

          @Override
          public IClass getDeclaringClass() {
            return com.ibm.wala.analysis.reflection.FakeAnnotationClass.this;
          }

          @Override
          public Atom getName() {
            return name;
          }

          @Override
          public boolean isStatic() {
            return false;
          }

          @Override
          public boolean isVolatile() {
            return false;
          }

          @Override
          public FieldReference getReference() {
            return FieldReference.findOrCreate(com.ibm.wala.analysis.reflection.FakeAnnotationClass.this.getReference(), name, fieldType);
          }

          @Override
          public boolean isFinal() {
            return false;
          }

          @Override
          public boolean isPrivate() {
            return false;
          }

          @Override
          public boolean isProtected() {
            return false;
          }

          @Override
          public boolean isPublic() {
            return true;
          }

          @Override
          public Collection<Annotation> getAnnotations() {
            return Collections.emptySet();
          }
        });
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getModifiers()
   */
  @Override
  public int getModifiers() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getSuperclass()
   */
  @Override
  public IClass getSuperclass() throws UnsupportedOperationException {
    return this.getClassHierarchy().lookupClass(TypeReference.JavaLangObject);
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces()
   */
  @Override
  public Collection<IClass> getAllImplementedInterfaces() throws UnsupportedOperationException {
    return getDirectInterfaces();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllAncestorInterfaces()
   */
  public Collection<IClass> getAllAncestorInterfaces() throws UnsupportedOperationException {
    return getDirectInterfaces();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.classLoader.Selector)
   */
  @Override
  public IMethod getMethod(Selector selector) {
    if (fakeAnnotationMethods.containsKey(selector)) {
      return fakeAnnotationMethods.get(selector);
    }
    return null;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.classLoader.Selector)
   */
  @Override
  public IField getField(Atom name) {
    if (fakeAnnotationFields != null) {
      return fakeAnnotationFields.get(name);
    } else {
      return null;
    }
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
   */
  @Override
  public IMethod getClassInitializer() {
    return null;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredMethods()
   */
  @Override
  public Collection<IMethod> getDeclaredMethods() throws UnsupportedOperationException {
    if (this.fakeAnnotationMethods != null) {
      return fakeAnnotationMethods.values();
    }
    return Collections.emptySet();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredInstanceFields()
   */
  @Override
  public Collection<IField> getDeclaredInstanceFields() throws UnsupportedOperationException {
    if ( fakeAnnotationFields != null) {
      return fakeAnnotationFields.values();
    } else {
      return Collections.emptySet();
    }
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredStaticFields()
   */
  @Override
  public Collection<IField> getDeclaredStaticFields() {
    return Collections.emptySet();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#isReferenceType()
   */
  @Override
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDirectInterfaces()
   */
  @Override
  public Collection<IClass> getDirectInterfaces() throws UnsupportedOperationException {
    return Collections.singleton(iinterface);
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllInstanceFields()
   */
  @Override
  public Collection<IField> getAllInstanceFields() {
    return getDeclaredInstanceFields();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllStaticFields()
   */
  @Override
  public Collection<IField> getAllStaticFields() {
    return getDeclaredStaticFields();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllMethods()
   */
  @Override
  public Collection<IMethod> getAllMethods() {
    return getDeclaredMethods();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllFields()
   */
  @Override
  public Collection<IField> getAllFields() {
    return getDeclaredInstanceFields();
  }

  @Override
  public boolean isPublic() {
    return true;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public Reader getSource() {
    return null;
  }

/*
  private IMethod makeMethod(String fieldName) {
    SSAInstructionFactory insts = getClassLoader().getInstructionFactory();

    MethodReference ref = MethodReference.findOrCreate(com.ibm.wala.analysis.reflection.FakeAnnotationClass.this.getReference(),
        fieldName, "()Ljava/lang/String;" );   // TODO: Fix when we know the return types.
    int numFIMethodArgs = ref.getNumberOfParameters();
    int lastFIArgValNum = numFIMethodArgs + 1;
    MethodSummary summary = new MethodSummary(ref);

    int inst = 0;
    int numCapturedValues = invoke.getNumberOfPositionalParameters();
    int firstCapturedValNum = lastFIArgValNum + 1;
    int curValNum = firstCapturedValNum;
    // arguments are the captured values, which were stored in the instance fields of the summary
    // class
    for (int i = 0; i < numCapturedValues; i++) {
      summary.addStatement(
          insts.GetInstruction(
              inst++, curValNum++, 1, getField(getCaptureFieldName(i)).getReference()));
    }

    try {
      MethodReference lambdaBodyCallee =
          MethodReference.findOrCreate(
              ClassLoaderReference.Application,
              getLambdaCalleeClass(),
              getLambdaCalleeName(),
              getLambdaCalleeSignature());

      int kind = getLambdaCalleeKind();
      boolean isNew = kind == REF_NEWINVOKESPECIAL;
      Dispatch code = getDispatchForMethodHandleKind(kind);

      IMethod resolved = getClassHierarchy().resolveMethod(lambdaBodyCallee);
      if (resolved == null) {
        throw new UnresolvedLambdaBodyException("could not resolve " + lambdaBodyCallee);
      }
      int numLambdaCalleeParams = resolved.getNumberOfParameters();
      // new calls (i.e., <init>) take one extra argument at position 0, the newly allocated object
      if (numLambdaCalleeParams != numFIMethodArgs + numCapturedValues + (isNew ? 1 : 0)) {
        throw new RuntimeException(
            "unexpected # of args "
                + numLambdaCalleeParams
                + " lastFIArgValNum "
                + lastFIArgValNum
                + " numCaptured "
                + numCapturedValues
                + " "
                + lambdaBodyCallee);
      }
      int params[] = new int[numLambdaCalleeParams];

      // if it's a new invocation, holds the value number for the new object
      int newValNum = -1;
      int curParamInd = 0;
      if (isNew) {
        // first pass the newly allocated object
        summary.addStatement(
            insts.NewInstruction(
                inst++,
                newValNum = curValNum++,
                NewSiteReference.make(inst, lambdaBodyCallee.getDeclaringClass())));
        params[curParamInd] = newValNum;
        curParamInd++;
      }

      // pass the captured values
      for (int i = 0; i < numCapturedValues; i++, curParamInd++) {
        params[curParamInd] = firstCapturedValNum + i;
      }

      // pass the FI method args
      for (int i = 0; i < numFIMethodArgs; i++, curParamInd++) {
        // args start at v_2
        params[curParamInd] = 2 + i;
      }

      if (lambdaBodyCallee.getReturnType().equals(TypeReference.Void)) {
        summary.addStatement(
            insts.InvokeInstruction(
                inst++,
                params,
                curValNum++,
                CallSiteReference.make(inst, lambdaBodyCallee, code),
                null));
        if (isNew) {
          // trampoline needs to return the new object
          summary.addStatement(insts.ReturnInstruction(inst++, newValNum, false));
        }
      } else {
        int ret = curValNum++;
        summary.addStatement(
            insts.InvokeInstruction(
                inst++,
                ret,
                params,
                curValNum++,
                CallSiteReference.make(inst, lambdaBodyCallee, code),
                null));
        summary.addStatement(
            insts.ReturnInstruction(
                inst++, ret, lambdaBodyCallee.getReturnType().isPrimitiveType()));
      }
    } catch (InvalidClassFileException e) {
      throw new RuntimeException(e);
    }

    SummarizedMethod method = new SummarizedMethod(ref, summary, LambdaSummaryClass.this);
    return method;
  }
  */

}
