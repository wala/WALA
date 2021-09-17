/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/** A {@link SyntheticMethod} representing the semantics encoded in a {@link MethodSummary} */
public class SummarizedMethod extends SyntheticMethod {
  static final boolean DEBUG = false;

  private final MethodSummary summary;

  public SummarizedMethod(MethodReference ref, MethodSummary summary, IClass declaringClass)
      throws NullPointerException {
    super(ref, declaringClass, summary.isStatic(), summary.isFactory());
    this.summary = summary;
    assert declaringClass != null;
    if (DEBUG) {
      System.err.println(("SummarizedMethod ctor: " + ref + ' ' + summary));
    }
  }

  /** @see com.ibm.wala.classLoader.IMethod#isNative() */
  @Override
  public boolean isNative() {
    return summary.isNative();
  }

  /** @see com.ibm.wala.classLoader.IMethod#isAbstract() */
  @Override
  public boolean isAbstract() {
    return false;
  }

  @Override
  public String getPoison() {
    return summary.getPoison();
  }

  @Override
  public byte getPoisonLevel() {
    return summary.getPoisonLevel();
  }

  @Override
  public boolean hasPoison() {
    return summary.hasPoison();
  }

  @SuppressWarnings("deprecation")
  @Override
  public SSAInstruction[] getStatements(SSAOptions options) {
    if (DEBUG) {
      System.err.println(("getStatements: " + this));
    }
    return summary.getStatements();
  }

  @Override
  public IR makeIR(Context context, SSAOptions options) {
    SSAInstruction instrs[] = getStatements(options);
    return new SyntheticIR(
        this,
        Everywhere.EVERYWHERE,
        makeControlFlowGraph(instrs),
        instrs,
        options,
        summary.getConstants());
  }

  /** @see com.ibm.wala.classLoader.IMethod#getNumberOfParameters() */
  @Override
  public int getNumberOfParameters() {
    return summary.getNumberOfParameters();
  }

  /** @see com.ibm.wala.classLoader.IMethod#isStatic() */
  @Override
  public boolean isStatic() {
    return summary.isStatic();
  }

  /** @see com.ibm.wala.classLoader.IMethod#getParameterType(int) */
  @Override
  public TypeReference getParameterType(int i) {
    return summary.getParameterType(i);
  }

  @Override
  public String getLocalVariableName(int bcIndex, int localNumber) {
    return summary.getValue(localNumber).toString();
  }
}
