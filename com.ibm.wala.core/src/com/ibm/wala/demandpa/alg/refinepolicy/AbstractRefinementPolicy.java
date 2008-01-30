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

  protected final CallGraphRefinePolicy cgRefinePolicy;

  protected final FieldRefinePolicy fieldRefinePolicy;

  private static final int DEFAULT_NUM_PASSES = 3;

  private static final int[] DEFAULT_BUDGET_PER_PASS = { 1000, 12000, 12000 };

  public int getBudgetForPass(int passNum) {
    return DEFAULT_BUDGET_PER_PASS[passNum];
  }

  public CallGraphRefinePolicy getCallGraphRefinePolicy() {
    return cgRefinePolicy;
  }

  public FieldRefinePolicy getFieldRefinePolicy() {
    return fieldRefinePolicy;
  }

  public int getNumPasses() {
    return DEFAULT_NUM_PASSES;
  }

  public boolean nextPass() {    
    // don't short-circuit since nextPass() can have side-effects
    boolean fieldNextPass = fieldRefinePolicy.nextPass();
    boolean callNextPass = cgRefinePolicy.nextPass();
    return fieldNextPass || callNextPass;
  }

  public AbstractRefinementPolicy(FieldRefinePolicy fieldRefinePolicy, CallGraphRefinePolicy cgRefinePolicy) {
    this.cgRefinePolicy = cgRefinePolicy;
    this.fieldRefinePolicy = fieldRefinePolicy;
  }

}
