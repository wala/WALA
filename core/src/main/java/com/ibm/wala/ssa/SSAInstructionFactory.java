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

package com.ibm.wala.ssa;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrike.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

/** An instruction factory for SSA. */
public interface SSAInstructionFactory {

  SSAAddressOfInstruction AddressOfInstruction(
      int iindex, int lval, int local, TypeReference pointeeType);

  SSAAddressOfInstruction AddressOfInstruction(
      int iindex, int lval, int local, int indexVal, TypeReference pointeeType);

  SSAAddressOfInstruction AddressOfInstruction(
      int iindex, int lval, int local, FieldReference field, TypeReference pointeeType);

  SSAArrayLengthInstruction ArrayLengthInstruction(int iindex, int result, int arrayref);

  SSAArrayLoadInstruction ArrayLoadInstruction(
      int iindex, int result, int arrayref, int index, TypeReference declaredType);

  SSAArrayStoreInstruction ArrayStoreInstruction(
      int iindex, int arrayref, int index, int value, TypeReference declaredType);

  SSAAbstractBinaryInstruction BinaryOpInstruction(
      int iindex,
      IBinaryOpInstruction.IOperator operator,
      boolean overflow,
      boolean unsigned,
      int result,
      int val1,
      int val2,
      boolean mayBeInteger);

  SSACheckCastInstruction CheckCastInstruction(
      int iindex, int result, int val, int[] typeValues, boolean isPEI);

  SSACheckCastInstruction CheckCastInstruction(
      int iindex, int result, int val, int typeValue, boolean isPEI);

  SSACheckCastInstruction CheckCastInstruction(
      int iindex, int result, int val, TypeReference[] types, boolean isPEI);

  SSACheckCastInstruction CheckCastInstruction(
      int iindex, int result, int val, TypeReference type, boolean isPEI);

  SSAComparisonInstruction ComparisonInstruction(
      int iindex, IComparisonInstruction.Operator operator, int result, int val1, int val2);

  SSAConditionalBranchInstruction ConditionalBranchInstruction(
      int iindex,
      IConditionalBranchInstruction.IOperator operator,
      TypeReference type,
      int val1,
      int val2,
      int target);

  SSAConversionInstruction ConversionInstruction(
      int iindex,
      int result,
      int val,
      TypeReference fromType,
      TypeReference toType,
      boolean overflow);

  SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(
      int iindex, int bbNumber, int exceptionValueNumber);

  SSAGetInstruction GetInstruction(int iindex, int result, FieldReference field);

  SSAGetInstruction GetInstruction(int iindex, int result, int ref, FieldReference field);

  SSAGotoInstruction GotoInstruction(int iindex, int target);

  SSAInstanceofInstruction InstanceofInstruction(
      int iindex, int result, int ref, TypeReference checkedType);

  SSAAbstractInvokeInstruction InvokeInstruction(
      int iindex,
      int result,
      int[] params,
      int exception,
      CallSiteReference site,
      BootstrapMethod bootstrap);

  SSAAbstractInvokeInstruction InvokeInstruction(
      int iindex, int[] params, int exception, CallSiteReference site, BootstrapMethod bootstrap);

  SSALoadIndirectInstruction LoadIndirectInstruction(
      int iindex, int lval, TypeReference t, int addressVal);

  SSALoadMetadataInstruction LoadMetadataInstruction(
      int iindex, int lval, TypeReference entityType, Object token);

  SSAMonitorInstruction MonitorInstruction(int iindex, int ref, boolean isEnter);

  SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site);

  SSANewInstruction NewInstruction(int iindex, int result, NewSiteReference site, int[] params);

  SSAPhiInstruction PhiInstruction(int iindex, int result, int[] params);

  SSAPiInstruction PiInstruction(
      int iindex, int result, int val, int piBlock, int successorBlock, SSAInstruction cause);

  SSAPutInstruction PutInstruction(int iindex, int ref, int value, FieldReference field);

  SSAPutInstruction PutInstruction(int iindex, int value, FieldReference field);

  SSAReturnInstruction ReturnInstruction(int iindex);

  SSAReturnInstruction ReturnInstruction(int iindex, int result, boolean isPrimitive);

  SSAStoreIndirectInstruction StoreIndirectInstruction(
      int iindex, int addressVal, int rval, TypeReference pointeeType);

  SSASwitchInstruction SwitchInstruction(
      int iindex, int val, int defaultLabel, int[] casesAndLabels);

  SSAThrowInstruction ThrowInstruction(int iindex, int exception);

  SSAUnaryOpInstruction UnaryOpInstruction(
      int iindex, IUnaryOpInstruction.IOperator operator, int result, int val);
}
