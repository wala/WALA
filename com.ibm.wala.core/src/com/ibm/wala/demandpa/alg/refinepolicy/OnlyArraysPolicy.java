/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.demandpa.alg.refinepolicy;

import com.ibm.wala.classLoader.IField;
import com.ibm.wala.demandpa.alg.statemachine.StateMachine.State;
import com.ibm.wala.demandpa.flowgraph.IFlowLabel;
import com.ibm.wala.demandpa.util.ArrayContents;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;

/**
 * Only refines for the array contents pseudo-field.
 * 
 * @author manu
 *
 */
public class OnlyArraysPolicy implements FieldRefinePolicy {

  /* 
   * @see com.ibm.wala.demandpa.alg.refinepolicy.FieldRefinePolicy#shouldRefine(com.ibm.wala.classLoader.IField, com.ibm.wala.ipa.callgraph.propagation.PointerKey, com.ibm.wala.ipa.callgraph.propagation.PointerKey, com.ibm.wala.demandpa.flowgraph.IFlowLabel, com.ibm.wala.demandpa.alg.statemachine.StateMachine.State)
   */
  @Override
  public boolean shouldRefine(IField field, PointerKey basePtr, PointerKey val, IFlowLabel label, State state) {
    return field == ArrayContents.v();
  }

  /* 
   * @see com.ibm.wala.demandpa.alg.refinepolicy.FieldRefinePolicy#nextPass()
   */
  @Override
  public boolean nextPass() {
    return false;
  }

}
