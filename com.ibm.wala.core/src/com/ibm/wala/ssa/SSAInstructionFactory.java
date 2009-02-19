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

package com.ibm.wala.ssa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

/**
 *
 * An instruction factory for SSA.
 * Note: this class wouldn't be needed if all the package-access constructors
 * in instruction classes were made public.
 * 
 * @author igor
 */
public final class SSAInstructionFactory {

  private SSAInstructionFactory() { }

  public static SSAArrayLengthInstruction ArrayLengthInstruction(int result, int arrayref) {
    return new SSAArrayLengthInstruction(result, arrayref);
  }
  public static SSAArrayLoadInstruction ArrayLoadInstruction(int result, int arrayref, int index, TypeReference declaredType) {
    return new SSAArrayLoadInstruction(result, arrayref, index, declaredType);
  }
  public static SSAArrayStoreInstruction ArrayStoreInstruction(int arrayref, int index, int value, TypeReference declaredType) {
    return new SSAArrayStoreInstruction(arrayref, index, value, declaredType);
  }
  public static SSABinaryOpInstruction BinaryOpInstruction(IBinaryOpInstruction.IOperator operator, int result, int val1, int val2, boolean mayBeInteger) {
    return new SSABinaryOpInstruction(operator, result, val1, val2, mayBeInteger);
  }
  public static SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference type) {
    return new SSACheckCastInstruction(result, val, type);
  }
  public static SSAComparisonInstruction ComparisonInstruction(short opcode, int result, int val1, int val2) {
    return new SSAComparisonInstruction(opcode, result, val1, val2);
  }
  public static SSAConditionalBranchInstruction ConditionalBranchInstruction(IConditionalBranchInstruction.IOperator operator, TypeReference type, int val1, int val2) {
    return new SSAConditionalBranchInstruction(operator,type,val1, val2);
  }
  public static SSAConversionInstruction ConversionInstruction(int result, int val, TypeReference fromType, TypeReference toType) {
    return new SSAConversionInstruction(result, val, fromType, toType );
  }
  public static SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(int bbNumber, int exceptionValueNumber) {
    return new SSAGetCaughtExceptionInstruction(bbNumber, exceptionValueNumber);
  }
  public static SSAGetInstruction GetInstruction(int result, FieldReference field) {
    return new SSAGetInstruction(result, field);
  }
  public static SSAGetInstruction GetInstruction(int result, int ref, FieldReference field) {
    return new SSAGetInstruction(result, ref, field);
  }
  public static SSAGotoInstruction GotoInstruction() {
    return new SSAGotoInstruction();
  }
  public static SSAInstanceofInstruction InstanceofInstruction(int result, int ref, TypeReference checkedType) {
    return new SSAInstanceofInstruction(result, ref, checkedType);
  }
  public static SSAInvokeInstruction InvokeInstruction(int result, int[] params, int exception, CallSiteReference site) {
    return new SSAInvokeInstruction(result, params, exception, site);
  }
  public static SSAInvokeInstruction InvokeInstruction(int[] params, int exception, CallSiteReference site) {
    return new SSAInvokeInstruction(params, exception, site);
  }
  public static SSAMonitorInstruction MonitorInstruction(int ref, boolean isEnter) {
    return new SSAMonitorInstruction(ref, isEnter);
  }
  public static SSANewInstruction NewInstruction(int result, NewSiteReference site) {
    return new SSANewInstruction(result, site);
  }
  public static SSAPhiInstruction PhiInstruction(int result, int[] params) throws IllegalArgumentException {
    return new SSAPhiInstruction(result, params);
  }
  public static SSAPutInstruction PutInstruction(int ref, int value, FieldReference field) {
    return new SSAPutInstruction(ref, value, field);
  }
  public static SSAPutInstruction PutInstruction(int value, FieldReference field) {
    return new SSAPutInstruction(value, field);
  }
  public static SSAReturnInstruction ReturnInstruction() {
    return new SSAReturnInstruction();
  }
  public static SSAReturnInstruction ReturnInstruction(int result, boolean isPrimitive) {
    return new SSAReturnInstruction(result, isPrimitive);
  }
  public static SSASwitchInstruction SwitchInstruction(int val, int defaultLabel, int[] casesAndLabels) {
    return new SSASwitchInstruction(val, defaultLabel, casesAndLabels);
  }
  public static SSAThrowInstruction ThrowInstruction(int exception) {
    return new SSAThrowInstruction(exception);
  }
  public static SSAUnaryOpInstruction UnaryOpInstruction(IUnaryOpInstruction.IOperator operator,int result, int val) {
    return new SSAUnaryOpInstruction(operator, result, val);
  }
}

