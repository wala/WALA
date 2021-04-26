/*
 * Copyright (c) 2015 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.Constants;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Synthetic class modeling the anonymous class generated at runtime for a lambda expression. The
 * anonymous class implements the relevant functional interface. Our synthetic classes contain
 * instance fields corresponding to the values captured by the lambda. The implementation of the
 * functional interface method is a "trampoline" that invokes the generated lambda body method with
 * the captured values stored in the instance fields.
 *
 * @see LambdaMethodTargetSelector
 */
public class LambdaSummaryClass extends SyntheticClass {

  // Kinds of method handles.
  // see https://docs.oracle.com/javase/specs/jvms/se11/html/jvms-5.html#jvms-5.4.3.5

  private static final int REF_INVOKEVIRTUAL = 5;
  private static final int REF_INVOKESTATIC = 6;
  private static final int REF_INVOKESPECIAL = 7;
  private static final int REF_NEWINVOKESPECIAL = 8;
  private static final int REF_INVOKEINTERFACE = 9;

  /**
   * Create a lambda summary class and add it to the class hierarchy.
   *
   * @param caller method containing the relevant invokedynamic instruction
   * @param inst the invokedynamic instruction
   * @return the summary class
   */
  public static LambdaSummaryClass create(CGNode caller, SSAInvokeDynamicInstruction inst) {
    String bootstrapCls =
        caller.getMethod().getDeclaringClass().getName().toString().replace("/", "$").substring(1);
    int bootstrapIndex = inst.getBootstrap().getIndexInClassFile();
    TypeReference ref =
        TypeReference.findOrCreate(
            ClassLoaderReference.Primordial,
            "Lwala/lambda" + '$' + bootstrapCls + '$' + bootstrapIndex);
    LambdaSummaryClass cls = new LambdaSummaryClass(ref, caller.getClassHierarchy(), inst);
    caller.getClassHierarchy().addClass(cls);
    return cls;
  }

  private final SSAInvokeDynamicInstruction invoke;

  private final Map<Atom, IField> fields;

  private final Map<Selector, IMethod> methods;

  private LambdaSummaryClass(
      TypeReference T, IClassHierarchy cha, SSAInvokeDynamicInstruction invoke) {
    super(T, cha);
    this.invoke = invoke;
    this.fields = makeFields();
    this.methods = Collections.singletonMap(trampoline().getSelector(), makeTrampoline());
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public int getModifiers() throws UnsupportedOperationException {
    return Constants.ACC_FINAL | Constants.ACC_SUPER;
  }

  @Override
  public IClass getSuperclass() {
    return getClassHierarchy().getRootClass();
  }

  /** @return singleton set containing the relevant functional interface */
  @Override
  public Collection<? extends IClass> getDirectInterfaces() {
    return Collections.singleton(getClassHierarchy().lookupClass(invoke.getDeclaredResultType()));
  }

  /** @return relevant functional interface and all of its super-interfaces */
  @Override
  public Collection<IClass> getAllImplementedInterfaces() {
    IClass iface = getClassHierarchy().lookupClass(invoke.getDeclaredResultType());
    Set<IClass> result = HashSetFactory.make(iface.getAllImplementedInterfaces());
    result.add(iface);
    return result;
  }

  @Override
  public IMethod getMethod(Selector selector) {
    return methods.get(selector);
  }

  @Override
  public IField getField(Atom name) {
    return fields.get(name);
  }

  @Override
  public IMethod getClassInitializer() {
    return null;
  }

  @Override
  public Collection<IMethod> getDeclaredMethods() {
    return methods.values();
  }

  @Override
  public Collection<IField> getAllInstanceFields() {
    return fields.values();
  }

  @Override
  public Collection<IField> getAllStaticFields() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IField> getAllFields() {
    return getAllInstanceFields();
  }

  @Override
  public Collection<IMethod> getAllMethods() {
    return methods.values();
  }

  @Override
  public Collection<IField> getDeclaredInstanceFields() {
    return fields.values();
  }

  private Map<Atom, IField> makeFields() {
    Map<Atom, IField> result = HashMapFactory.make();
    for (int i = 0; i < invoke.getNumberOfPositionalParameters(); i++) {
      // needed to reference current value in anonymous class
      final int index = i;
      result.put(
          getCaptureFieldName(index),
          new IField() {
            @Override
            public IClass getDeclaringClass() {
              return LambdaSummaryClass.this;
            }

            @Override
            public Atom getName() {
              return getCaptureFieldName(index);
            }

            @Override
            public Collection<Annotation> getAnnotations() {
              return Collections.emptySet();
            }

            @Override
            public IClassHierarchy getClassHierarchy() {
              return LambdaSummaryClass.this.getClassHierarchy();
            }

            @Override
            public TypeReference getFieldTypeReference() {
              return invoke.getDeclaredTarget().getParameterType(index);
            }

            @Override
            public FieldReference getReference() {
              return FieldReference.findOrCreate(
                  LambdaSummaryClass.this.getReference(), getName(), getFieldTypeReference());
            }

            @Override
            public boolean isFinal() {
              return true;
            }

            @Override
            public boolean isPrivate() {
              return true;
            }

            @Override
            public boolean isProtected() {
              return false;
            }

            @Override
            public boolean isPublic() {
              return false;
            }

            @Override
            public boolean isStatic() {
              return false;
            }

            @Override
            public boolean isVolatile() {
              return false;
            }
          });
    }
    return result;
  }

  private MethodReference trampoline() {
    try {
      return MethodReference.findOrCreate(
          LambdaSummaryClass.this.getReference(),
          invoke.getDeclaredTarget().getName(),
          Descriptor.findOrCreateUTF8(getLambdaDeclaredSignature()));
    } catch (InvalidClassFileException e) {
      throw new RuntimeException(e);
    }
  }

  private String getLambdaCalleeClass() throws InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return 'L' + invoke.getBootstrap().getCP().getCPHandleClass(cpIndex);
  }

  private String getLambdaCalleeName() throws InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return invoke.getBootstrap().getCP().getCPHandleName(cpIndex);
  }

  private String getLambdaCalleeSignature() throws InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return invoke.getBootstrap().getCP().getCPHandleType(cpIndex);
  }

  private String getLambdaDeclaredSignature() throws InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(0);
    return invoke.getBootstrap().getCP().getCPMethodType(cpIndex);
  }

  private int getLambdaCalleeKind() throws InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return invoke.getBootstrap().getCP().getCPHandleKind(cpIndex);
  }

  private IMethod makeTrampoline() {

    // Assume that the functional interface (FI) method takes n arguments (besides the receiver),
    // and the lambda captures k variables.
    // Value numbers v_1 through v_(n+1) are the formal parameters of the trampoline method.
    // v_1 is the lambda summary class instance, and v_2 - v_(n+1) are the args for the FI method.
    // we assign value number v_(n+2) - v_(n+k+2) the captured values, via getfield instructions
    // that read the relevant fields from v_1.
    SSAInstructionFactory insts = getClassLoader().getInstructionFactory();

    MethodReference ref = trampoline();
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

  private static Dispatch getDispatchForMethodHandleKind(int kind) {
    Dispatch code;
    switch (kind) {
      case REF_INVOKEVIRTUAL:
        code = Dispatch.VIRTUAL;
        break;
      case REF_INVOKESTATIC:
        code = Dispatch.STATIC;
        break;
      case REF_INVOKESPECIAL:
      case REF_NEWINVOKESPECIAL:
        code = Dispatch.SPECIAL;
        break;
      case REF_INVOKEINTERFACE:
        code = Dispatch.INTERFACE;
        break;
      default:
        throw new Error("unexpected dynamic invoke type " + kind);
    }
    return code;
  }

  @Override
  public Collection<IField> getDeclaredStaticFields() {
    return Collections.emptySet();
  }

  @Override
  public boolean isReferenceType() {
    return true;
  }

  /**
   * get the synthetic field name for a value captured by the lambda
   *
   * @param i index of the captured value
   * @return the field name
   */
  public static Atom getCaptureFieldName(int i) {
    return Atom.findOrCreateUnicodeAtom("c" + i);
  }

  /**
   * Exception thrown when the method containing the body of the lambda (or the target of a method
   * reference) cannot be resolved.
   */
  static class UnresolvedLambdaBodyException extends RuntimeException {

    private static final long serialVersionUID = -6504849409929928820L;

    public UnresolvedLambdaBodyException(String s) {
      super(s);
    }
  }
}
