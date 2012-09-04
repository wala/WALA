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
import com.ibm.wala.shrikeBT.IComparisonInstruction;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

/**
 * An instruction factory for SSA. 
 */
public interface SSAInstructionFactory {

  SSAAddressOfInstruction AddressOfInstruction(int lval, int local, TypeReference pointeeType);

  SSAAddressOfInstruction AddressOfInstruction(int lval, int local, int indexVal, TypeReference pointeeType);

  SSAAddressOfInstruction AddressOfInstruction(int lval, int local, FieldReference field, TypeReference pointeeType);
  
  SSAArrayLengthInstruction ArrayLengthInstruction(int result, int arrayref);

  SSAArrayLoadInstruction ArrayLoadInstruction(int result, int arrayref, int index, TypeReference declaredType);

  SSAArrayStoreInstruction ArrayStoreInstruction(int arrayref, int index, int value, TypeReference declaredType);

  SSABinaryOpInstruction BinaryOpInstruction(IBinaryOpInstruction.IOperator operator, boolean overflow, boolean unsigned,
      int result, int val1, int val2, boolean mayBeInteger);

  SSACheckCastInstruction CheckCastInstruction(int result, int val, int[] typeValues, boolean isPEI);

  SSACheckCastInstruction CheckCastInstruction(int result, int val, int typeValue, boolean isPEI);

  SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference[] types, boolean isPEI);

  SSACheckCastInstruction CheckCastInstruction(int result, int val, TypeReference type, boolean isPEI);

  SSAComparisonInstruction ComparisonInstruction(IComparisonInstruction.Operator operator, int result, int val1, int val2);

  SSAConditionalBranchInstruction ConditionalBranchInstruction(IConditionalBranchInstruction.IOperator operator,
      TypeReference type, int val1, int val2);

  SSAConversionInstruction ConversionInstruction(int result, int val, TypeReference fromType, TypeReference toType, boolean overflow);

  SSAGetCaughtExceptionInstruction GetCaughtExceptionInstruction(int bbNumber, int exceptionValueNumber);

  SSAGetInstruction GetInstruction(int result, FieldReference field);

  SSAGetInstruction GetInstruction(int result, int ref, FieldReference field);

  SSAGotoInstruction GotoInstruction();

  SSAInstanceofInstruction InstanceofInstruction(int result, int ref, TypeReference checkedType);

  SSAInvokeInstruction InvokeInstruction(int result, int[] params, int exception, CallSiteReference site);

  SSAInvokeInstruction InvokeInstruction(int[] params, int exception, CallSiteReference site);

  SSALoadIndirectInstruction LoadIndirectInstruction(int lval, TypeReference t, int addressVal);
  
  SSALoadMetadataInstruction LoadMetadataInstruction(int lval, TypeReference entityType, Object token);

  SSAMonitorInstruction MonitorInstruction(int ref, boolean isEnter);

  SSANewInstruction NewInstruction(int result, NewSiteReference site);

  SSANewInstruction NewInstruction(int result, NewSiteReference site, int[] params);

  SSAPhiInstruction PhiInstruction(int result, int[] params);

  SSAPiInstruction PiInstruction(int result, int val, int piBlock, int successorBlock, SSAInstruction cause);

  SSAPutInstruction PutInstruction(int ref, int value, FieldReference field);

  SSAPutInstruction PutInstruction(int value, FieldReference field);

  SSAReturnInstruction ReturnInstruction();

  SSAReturnInstruction ReturnInstruction(int result, boolean isPrimitive);

  SSAStoreIndirectInstruction StoreIndirectInstruction(int addressVal, int rval, TypeReference pointeeType);
  
  SSASwitchInstruction SwitchInstruction(int val, int defaultLabel, int[] casesAndLabels);

  SSAThrowInstruction ThrowInstruction(int exception);

  SSAUnaryOpInstruction UnaryOpInstruction(IUnaryOpInstruction.IOperator operator, int result, int val);

}
