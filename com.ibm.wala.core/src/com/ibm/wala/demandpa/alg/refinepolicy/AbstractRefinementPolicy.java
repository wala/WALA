/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.demandpa.alg.refinepolicy;

/**
 * Default {@link RefinementPolicy} implementation, delegating to some provided {@link FieldRefinePolicy} and
 * {@link CallGraphRefinePolicy}
 * @author manu
 * 
 */
public abstract class AbstractRefinementPolicy implements RefinementPolicy {

  private static final int DEFAULT_NUM_PASSES = 4;

  private static final int LONGER_PASS_BUDGET = 12000;
  
  private static final int[] DEFAULT_BUDGET_PER_PASS = { 1000, LONGER_PASS_BUDGET, LONGER_PASS_BUDGET, LONGER_PASS_BUDGET };

  protected final FieldRefinePolicy fieldRefinePolicy;

  protected final CallGraphRefinePolicy cgRefinePolicy;

  protected final int numPasses;
  
  protected final int[] budgetPerPass;
  
  public AbstractRefinementPolicy(FieldRefinePolicy fieldRefinePolicy, CallGraphRefinePolicy cgRefinePolicy, int numPasses,
      int[] budgetPerPass) {
    this.fieldRefinePolicy = fieldRefinePolicy;
    this.cgRefinePolicy = cgRefinePolicy;
    this.numPasses = numPasses;
    this.budgetPerPass = budgetPerPass;
  }

  public AbstractRefinementPolicy(FieldRefinePolicy fieldRefinePolicy, CallGraphRefinePolicy cgRefinePolicy) {
    this(fieldRefinePolicy, cgRefinePolicy, DEFAULT_NUM_PASSES, DEFAULT_BUDGET_PER_PASS);
  }

  public int getBudgetForPass(int passNum) {
    return budgetPerPass[passNum];
  }

  public CallGraphRefinePolicy getCallGraphRefinePolicy() {
    return cgRefinePolicy;
  }

  public FieldRefinePolicy getFieldRefinePolicy() {
    return fieldRefinePolicy;
  }

  public int getNumPasses() {
    return numPasses;
  }

  public boolean nextPass() {    
    // don't short-circuit since nextPass() can have side-effects
    boolean fieldNextPass = fieldRefinePolicy.nextPass();
    boolean callNextPass = cgRefinePolicy.nextPass();
    return fieldNextPass || callNextPass;
  }


}
