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
package com.ibm.wala.demandpa.alg.refinepolicy;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.demandpa.alg.statemachine.StateMachine;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

/** A field refine policy that first checks with A, then delegates to B */
public class DelegatingFieldRefinePolicy implements FieldRefinePolicy {

  private final FieldRefinePolicy A;

  private final FieldRefinePolicy B;

  public DelegatingFieldRefinePolicy(FieldRefinePolicy a, FieldRefinePolicy b) {
    if (a == null) {
      throw new IllegalArgumentException("null A");
    }
    if (b == null) {
      throw new IllegalArgumentException("null B");
    }
    A = a;
    B = b;
  }

  @Override
  public boolean nextPass() {
    // careful not to short-circuit here, since nextPass() can have side-effects
    boolean AnextPass = A.nextPass();
    boolean BnextPass = B.nextPass();
    return AnextPass || BnextPass;
  }

  /**
   * returns {@code true} if {@code A.shouldRefine(field) || B.shouldRefine(field)}. Note that if
   * {@code A.shouldRefine(field)} is {@code true}, {@code B.shouldRefine(field)} is <em>not</em>
   * called.
   */
  @Override
  public boolean shouldRefine(
      IField field,
      PointerKey basePtr,
      PointerKey val,
      IFlowLabel label,
      StateMachine.State state) {
    // make code explicit to avoid subtle reliance on short-circuiting
    boolean AshouldRefine = A.shouldRefine(field, basePtr, val, label, state);
    if (AshouldRefine) {
      return true;
    } else {
      return B.shouldRefine(field, basePtr, val, label, state);
    }
  }
}
