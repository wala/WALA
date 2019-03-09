/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.demandpa.alg.refinepolicy;

import java.util.Arrays;

/**
 * Default {@link RefinementPolicy} implementation, delegating to some provided {@link
 * FieldRefinePolicy} and {@link CallGraphRefinePolicy}
 *
 * @author manu
 */
public abstract class AbstractRefinementPolicy implements RefinementPolicy {

  protected static final int DEFAULT_NUM_PASSES = 4;

  protected static final int LONGER_PASS_BUDGET = 12000;

  private static final int SHORTER_PASS_BUDGET = 1000;

  private static final int[] DEFAULT_BUDGET_PER_PASS;

  static {
    int[] tmp = new int[DEFAULT_NUM_PASSES];
    tmp[0] = SHORTER_PASS_BUDGET;
    Arrays.fill(tmp, 1, DEFAULT_NUM_PASSES, LONGER_PASS_BUDGET);
    DEFAULT_BUDGET_PER_PASS = tmp;
  }

  protected final FieldRefinePolicy fieldRefinePolicy;

  protected final CallGraphRefinePolicy cgRefinePolicy;

  protected final int numPasses;

  protected final int[] budgetPerPass;

  public AbstractRefinementPolicy(
      FieldRefinePolicy fieldRefinePolicy,
      CallGraphRefinePolicy cgRefinePolicy,
      int numPasses,
      int[] budgetPerPass) {
    this.fieldRefinePolicy = fieldRefinePolicy;
    this.cgRefinePolicy = cgRefinePolicy;
    this.numPasses = numPasses;
    this.budgetPerPass = budgetPerPass;
  }

  public AbstractRefinementPolicy(
      FieldRefinePolicy fieldRefinePolicy, CallGraphRefinePolicy cgRefinePolicy) {
    this(fieldRefinePolicy, cgRefinePolicy, DEFAULT_NUM_PASSES, DEFAULT_BUDGET_PER_PASS);
  }

  @Override
  public int getBudgetForPass(int passNum) {
    return budgetPerPass[passNum];
  }

  @Override
  public CallGraphRefinePolicy getCallGraphRefinePolicy() {
    return cgRefinePolicy;
  }

  @Override
  public FieldRefinePolicy getFieldRefinePolicy() {
    return fieldRefinePolicy;
  }

  @Override
  public int getNumPasses() {
    return numPasses;
  }

  @Override
  public boolean nextPass() {
    // don't short-circuit since nextPass() can have side-effects
    boolean fieldNextPass = fieldRefinePolicy.nextPass();
    boolean callNextPass = cgRefinePolicy.nextPass();
    return fieldNextPass || callNextPass;
  }
}
