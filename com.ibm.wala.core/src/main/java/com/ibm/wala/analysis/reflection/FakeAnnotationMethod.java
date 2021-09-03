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

import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.Map;

/** A {@link SyntheticMethod} representing the semantics encoded in a {@link MethodSummary} */
public class FakeAnnotationMethod extends SyntheticMethod {
  static final boolean DEBUG = false;

  public final IClassHierarchy cha;

  protected final SSAInstructionFactory insts;

  private final IClass iinterface;

  public FakeAnnotationMethod(
      MethodReference method, IClass declaringClass, final IClassHierarchy cha, IClass iinterface)
      throws NullPointerException {
    super(method, declaringClass, false, false);
    this.cha = cha;
    this.insts = declaringClass.getClassLoader().getInstructionFactory();
    this.iinterface = iinterface;
  }

  public FakeAnnotationMethod(
      MethodReference method, final IClassHierarchy cha, IClass iinterface) {
    this(
        method,
        new FakeAnnotationClass(method.getDeclaringClass().getClassLoader(), cha, iinterface),
        cha,
        iinterface);
  }

  public SSAInstruction[] makeStatements() {
    MethodSummary ms = new MethodSummary(this.getReference());
    // SSAInstructionFactory insts = Language.JAVA.instructionFactory();
    ms.addStatement(
        this.insts.PutInstruction(
            ms.getNumberOfStatements(),
            2,
            1,
            FieldReference.findOrCreate(
                this.getDeclaringClass().getReference(),
                this.getReference().getName(),
                this.getReference().getReturnType())));

    SSAReturnInstruction R = insts.ReturnInstruction(ms.getNumberOfStatements(), 2, false);
    ms.addStatement(R);
    return ms.getStatements();
  }

  @Override
  public IR makeIR(Context context, SSAOptions options) {
    Map<Integer, ConstantValue> constants = HashMapFactory.make();
    SSAInstruction[] instrs = makeStatements();
    return new SyntheticIR(
        this,
        context,
        new InducedCFG(instrs, this, context),
        instrs,
        SSAOptions.defaultOptions(),
        constants);
  }
}
