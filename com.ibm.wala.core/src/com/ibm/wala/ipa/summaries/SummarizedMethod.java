/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * @author sfink
 * 
 */
public class SummarizedMethod extends SyntheticMethod {
  static final boolean DEBUG = false;

  private MethodSummary summary;

  public SummarizedMethod(MethodReference ref, MethodSummary summary, IClass declaringClass) throws NullPointerException {

    super(ref, declaringClass, summary.isStatic(), summary.isFactory());
    this.summary = summary;
    if (Assertions.verifyAssertions) {
      Assertions._assert(declaringClass != null);
    }
    if (DEBUG) {
      Trace.println("SummarizedMethod ctor: " + ref + " " + summary);
    }
  }

  /**
   * @see com.ibm.wala.classLoader.IMethod#isNative()
   */
  @Override
  public boolean isNative() {
    return summary.isNative();
  }

  /**
   * @see com.ibm.wala.classLoader.IMethod#isAbstract()
   */
  @Override
  public boolean isAbstract() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.SyntheticMethod#getPoison()
   */
  @Override
  public String getPoison() {
    return summary.getPoison();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.SyntheticMethod#getPoisonLevel()
   */
  @Override
  public byte getPoisonLevel() {
    return summary.getPoisonLevel();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.SyntheticMethod#hasPoison()
   */
  @Override
  public boolean hasPoison() {
    return summary.hasPoison();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getStatements(com.ibm.wala.util.WarningSet)
   */
  @Override
  public SSAInstruction[] getStatements(SSAOptions options, WarningSet warnings) {
    if (DEBUG) {
      Trace.println("getStatements: " + this);
    }
    return summary.getStatements();
  }

  // /*
  // * (non-Javadoc)
  // *
  // * @see com.ibm.wala.classLoader.IMethod#getIR(com.ibm.wala.util.WarningSet)
  // */
  // public IR getIR(SSAOptions options, WarningSet warnings) {
  // if (DEBUG) {
  // Trace.println("Get IR: " + this);
  // }
  // return findOrCreateIR(options, warnings);
  // }

  // /**
  // * @return
  // */
  // private IR findOrCreateIR(SSAOptions options, WarningSet warnings) {
  // IR result = (IR) CacheReference.get(ir);
  // if (result == null) {
  // if (DEBUG) {
  // Trace.println("Create IR for " + this);
  // }
  // SSAInstruction instrs[] = getStatements(options, warnings);
  // result = new SyntheticIR(this, Everywhere.EVERYWHERE,
  // makeControlFlowGraph(), instrs, options, summary.getConstants(),
  // warnings);
  // ir = CacheReference.make(result);
  // }
  // return result;
  // }
  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getIR(com.ibm.wala.util.WarningSet)
   */
  @Override
  public IR makeIR(SSAOptions options, WarningSet warnings) {
    SSAInstruction instrs[] = getStatements(options, warnings);
    return new SyntheticIR(this, Everywhere.EVERYWHERE, makeControlFlowGraph(), instrs, options, summary.getConstants(), warnings);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getNumberOfParameters()
   */
  @Override
  public int getNumberOfParameters() {
    return summary.getNumberOfParameters();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#isStatic()
   */
  @Override
  public boolean isStatic() {
    return summary.isStatic();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IMethod#getParameterType(int)
   */
  @Override
  public TypeReference getParameterType(int i) {
    return summary.getParameterType(i);
  }

}
