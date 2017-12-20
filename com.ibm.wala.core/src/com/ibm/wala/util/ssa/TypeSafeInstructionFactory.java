/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/*
 *  Copyright (c) 2013,
 *      Tobias Blaschke <code@tobiasblaschke.de>
 *  All rights reserved.

 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice,
 *     this list of conditions and the following disclaimer in the documentation
 *     and/or other materials provided with the distribution.
 *
 *  3. The names of the contributors may not be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *  AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *  IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 *  ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 *  LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *  CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 *  SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 *  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 *  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 *  ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 *  POSSIBILITY OF SUCH DAMAGE.
 */
package com.ibm.wala.util.ssa;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 *  Intended for SyntheticMethods, uses JavaInstructionFactory.
 *
 *  As the name states this SSAInstructionFactory does type checks. If they pass the actual instructions are
 *  generated using the JavaInstructionFactory.
 *
 *  Obviously this factory is not complete yet. It's extended on demand.
 *
 *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory
 *
 *  @author Tobias Blaschke &lt;code@tobiasblaschke.de&gt;
 *  @since  2013-10-20
 */
public class TypeSafeInstructionFactory {
    
    private final static boolean DEBUG = false;
    protected final JavaInstructionFactory insts;
    protected final IClassHierarchy cha;

    public TypeSafeInstructionFactory(IClassHierarchy cha) {
        this.cha = cha;
        this.insts = new JavaInstructionFactory();
    }

    /**
     *  result = site(params).
     *
     *  Instruction that calls a method which has a return-value.
     *
     *  All parameters (but exception) are typechecked first. If the check passes they get unpacked and handed over
     *  to the corresponding JavaInstructionFactory.InvokeInstruction.
     *
     *  Calls result.setAssigned()
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#InvokeInstruction(int, int, int[], int, CallSiteReference, BootstrapMethod)
     *
     *  @param  iindex  Zero or a positive number unique to any instruction of the same method
     *  @param  result  Where to place the return-value of the called method. Is SSAValue.setAssigned() automatically.
     *  @param  params  Parameters to the call starting with the implicit this-pointer if necessary 
     *  @param  exception   An SSAValue receiving the exception-object when something in the method throws unhandled
     *  @param  site    The CallSiteReference to this call.
     */
    public SSAInvokeInstruction InvokeInstruction(final int iindex, final SSAValue result, List<? extends SSAValue> params, 
            final SSAValue exception, final CallSiteReference site) {
        info("Now: InvokeInstruction to {} using {}", site, params);
        if (iindex < 0) {
            throw new IllegalArgumentException("The iIndex may not be negative");
        }
        if (result == null) {
            throw new IllegalArgumentException("The result may not be null");
        }
        if (exception == null) {
            throw new IllegalArgumentException("The parameter exception may not be null");
        }
        if (params == null) {
            params = Collections.emptyList();
        }
        if (site == null) {
            throw new IllegalArgumentException("The CallSite may not be null");
        }
        final ParameterAccessor acc = new ParameterAccessor(site.getDeclaredTarget(), this.cha);
        if (acc.hasImplicitThis()) {
            if (params.size() != (acc.getNumberOfParameters() + 1)) {
                throw new IllegalArgumentException("The callee takes " + acc.getNumberOfParameters() + " + 1 (implicit this) " +
                        "parameters. The given parameter-list has length " + params.size() + ". They are: " + params);
            }

            if (site.getInvocationCode() == IInvokeInstruction.Dispatch.STATIC) {   // TODO use Dispatch.hasImplicitThis
                throw new IllegalArgumentException("A function expecting an implicit this can not be invoked static.");
            }
        } else {
            if (params.size() != (acc.getNumberOfParameters())) {
                throw new IllegalArgumentException("The callee takes " + acc.getNumberOfParameters() + " parameters (no implicit this)." +
                        "The given parameter-list has length " + params.size() + ". They are: " + params);
            }

            if (site.getInvocationCode() != IInvokeInstruction.Dispatch.STATIC) {
                throw new IllegalArgumentException("A function without implicit this can only be invoked static.");
            }
        }

        // final MethodReference mRef = site.getDeclaredTarget();

        // ***********
        // Params cosher, start typechecks
        
        { // Return-Value
            final TypeReference retType = acc.getReturnType();
            if (! isAssignableFrom(retType, result.getType())) {
                throw new IllegalArgumentException("The return-value does not stand the TypeCheck! " + retType + 
                        " is not assignable to " + result);
            }
        }

        final int[] aParams = new int[params.size()];
        if (acc.hasImplicitThis()) { // Implicit This
            final SSAValue targetThis = acc.getThis();
            final SSAValue givenThis = params.get(0);

            aParams[0] = givenThis.getNumber();

            if (! isAssignableFrom(givenThis.getType(), targetThis.getType())) {
                throw new IllegalArgumentException("Parameter 'this' is not assignable from\n\t" + givenThis + " to\n\t" + targetThis
                        + "\n----------");
            }

            for (int i = 1; i < params.size(); ++i) {    // "regular" parameters
                final SSAValue givenParam = params.get(i);
                final SSAValue targetParam = acc.getParameter(i);

                aParams[i] = givenParam.getNumber();
                if (! isAssignableFrom(givenParam.getType(), targetParam.getType())) {
                    throw new IllegalArgumentException("Parameter " + i + " is not assignable from " + givenParam + " to " + targetParam);
                }
                if (result.equals(givenParam)) {
                    throw new IllegalArgumentException(result.toString() + " can't be the result and parameter " + i + " at the same time in " + 
                            site);
                }
            }
        } else {
            for (int i = 0; i < params.size(); ++i) {    // "regular" parameters
                final SSAValue givenParam = params.get(i);
                final SSAValue targetParam = acc.getParameter(i + 1);

                aParams[i] = givenParam.getNumber();
                if (! isAssignableFrom(givenParam.getType(), targetParam.getType())) {
                    throw new IllegalArgumentException("Parameter " + (i + 1) + " is not assignable from " + givenParam + " to " + targetParam + " in call " + site);
                }
                if (result.equals(givenParam)) {
                    throw new IllegalArgumentException(result.toString() + " can't be the result and parameter " + i + " at the same time in " + 
                            site);
                }
            }
        }

        // TODO somehow check exception?

        result.setAssigned();
        return insts.InvokeInstruction(iindex, result.getNumber(), aParams, exception.getNumber(), site, null);
    }

    /**
     *  Instruction that calls a void-method.
     *
     *  All parameters (but exception) are typechecked first. If the check passes they get unpacked and handed over
     *  to the corresponding JavaInstructionFactory.InvokeInstruction.
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#InvokeInstruction(int, int[], int, CallSiteReference, BootstrapMethod)
     *
     *  @param  iindex  Zero or a positive number unique to any instruction of the same method
     *  @param  params  Parameters to the call starting with the implicit this-pointer if necessary 
     *  @param  exception   An SSAValue receiving the exception-object when something in the method throws unhandled
     *  @param  site    The CallSiteReference to this call.
     */
    public SSAInvokeInstruction InvokeInstruction(final int iindex, List<? extends SSAValue> params, 
            final SSAValue exception, final CallSiteReference site) {
        info("Now: InvokeInstruction to {} using {}", site, params);
        if (iindex < 0) {
            throw new IllegalArgumentException("The iIndex may not be negative");
        }
        if (exception == null) {
            throw new IllegalArgumentException("The parameter exception may not be null");
        }
        if (params == null) {
            params = Collections.emptyList();
        }
        if (site == null) {
            throw new IllegalArgumentException("The CallSite may not be null");
        }
        final ParameterAccessor acc = new ParameterAccessor(site.getDeclaredTarget(), this.cha);
        if (acc.hasImplicitThis()) {
            if (params.size() != (acc.getNumberOfParameters() + 1)) {
                throw new IllegalArgumentException("The callee takes " + acc.getNumberOfParameters() + " + 1 (implicit this) " +
                        "parameters. The given parameter-list has length " + params.size() + ". They are: " + params);
            }

            if (site.getInvocationCode() == IInvokeInstruction.Dispatch.STATIC) {   // TODO use Dispatch.hasImplicitThis
                throw new IllegalArgumentException("A function expecting an implicit this can not be invoked static.");
            }
        } else {
            if (params.size() != (acc.getNumberOfParameters())) {
                throw new IllegalArgumentException("The callee takes " + acc.getNumberOfParameters() + " parameters (no implicit this)." +
                        "The given parameter-list has length " + params.size() + ". They are: " + params);
            }

            if (site.getInvocationCode() != IInvokeInstruction.Dispatch.STATIC) {
                throw new IllegalArgumentException("A function without implicit this can only be invoked static.");
            }
        }

        final MethodReference mRef = site.getDeclaredTarget();

        // ***********
        // Params cosher, start typechecks
        
        { // Return-Value
            if (acc.hasReturn()) {
                throw new IllegalArgumentException("This InvokeInstruction only works on void-functions but " + mRef + " returns a value.");
            }
        }

        final int[] aParams = new int[params.size()];
        if (acc.hasImplicitThis()) { // Implicit This
            // final SSAValue targetThis = acc.getThis();
            final SSAValue givenThis = params.get(0);

            aParams[0] = givenThis.getNumber();

            /*if (! isAssignableFrom(givenThis.getType(), targetThis.getType())) {
                throw new IllegalArgumentException("Parameter 'this' is not assignable from " + givenThis + " to " + targetThis);
            }*/

            for (int i = 1; i < params.size(); ++i) {    // "regular" parameters
                final SSAValue givenParam = params.get(i);
                final SSAValue targetParam = acc.getParameter(i);

                aParams[i] = givenParam.getNumber();
                if (! isAssignableFrom(givenParam.getType(), targetParam.getType())) {
                    throw new IllegalArgumentException("Parameter " + i + " is not assignable from\n\t" + givenParam + " to\n\t" + targetParam +
                            "\nin call " + site + "\n---------");
                }
            }
        } else {
            for (int i = 0; i < params.size(); ++i) {    // "regular" parameters
                final SSAValue givenParam = params.get(i);
                final SSAValue targetParam = acc.getParameter(i + 1);

                aParams[i] = givenParam.getNumber();
                if (! isAssignableFrom(givenParam.getType(), targetParam.getType())) {
                    throw new IllegalArgumentException("Parameter " + (i + 1) + " is not assignable from " + givenParam + " to " + targetParam);
                }
            }
        }

        // TODO somehow check exception?

        return insts.InvokeInstruction(iindex, aParams, exception.getNumber(), site, null);
    }


    /**
     *  Check if the type of the SSAValue is assignable to the return-value of the method it's valid in.
     *
     *  If type check passes the corresponding ReturnInstruction of the JavaInstructionFactory is called.
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#ReturnInstruction(int, int, boolean)
     *
     *  @param  iindex  Zero or a positive number unique to any instruction of the same method
     *  @param  result  SSAValue to return _with_ validIn _set_!
     *  @throws IllegalArgumentException if result has no validIn set
     */
    public SSAReturnInstruction ReturnInstruction(final int iindex, final SSAValue result) {
        info("Now: ReturnInstruction using {}", result);
        
        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (! isAssignableFrom(result.getType(), result.getValidIn().getReturnType())) {
            throw new IllegalArgumentException("Return type not assignable from " + result.getType() + " to " + result.getValidIn().getReturnType());
        }

        return insts.ReturnInstruction(iindex, result.getNumber(), result.getType().isPrimitiveType());
    }

    /**
     *  targetValue = containingInstance.field.
     *
     *  Reads field from containingInstance into targetValue.
     *  If type check passes the corresponding GetInstruction of the JavaInstructionFactory is called.
     *  Calls targetValue.setAssigned()
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#GetInstruction(int, int, int, FieldReference)
     *
     *  @param  iindex      Zero or a positive number unique to any instruction of the same method
     *  @param  targetValue the result of the GetInstruction is placed there
     *  @param  containingInstance The Object instance to read the field from
     *  @param  field       The description of the field
     */
    public SSAGetInstruction GetInstruction(final int iindex, final SSAValue targetValue, final SSAValue containingInstance, 
            FieldReference field) {
        info("Now: Get {} from {} into {}", field, containingInstance, targetValue);
        
        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (targetValue == null) {
            throw new IllegalArgumentException("targetValue may not be null");
        }
        if (containingInstance == null) {
            throw new IllegalArgumentException("containingInstance may not be null");
        }
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (! isSuperclassOf(field.getDeclaringClass(), containingInstance.getType())) {
            throw new IllegalArgumentException("The targetInstance " + containingInstance + " is not equal or a " +
                " super-class of " + field.getDeclaringClass());
        }
        if (! isAssignableFrom(field.getFieldType(), targetValue.getType())) {
            throw new IllegalArgumentException("The field " + targetValue + " is not assignable from " + field);
        }

        final MethodReference targetValueValidIn = targetValue.getValidIn();
        final MethodReference instValidIn = containingInstance.getValidIn();

        if ((targetValueValidIn != null) && (instValidIn != null) && (! targetValueValidIn.equals(instValidIn))) {
            throw new IllegalArgumentException("containingInstance " + containingInstance + "and targetValue " +
                    targetValue + " are valid in different scopes");
        }

        targetValue.setAssigned();
        return insts.GetInstruction(iindex, targetValue.getNumber(), containingInstance.getNumber(), field);
    }

    /**
     *  Reads static field into targetValue.
     *
     *  If type check passes the corresponding GetInstruction of the JavaInstructionFactory is called.
     *  Calls targetValue.setAssigned()
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#GetInstruction(int, int, int, FieldReference)
     *
     *  @param  iindex      Zero or a positive number unique to any instruction of the same method
     *  @param  targetValue the result of the GetInstruction is placed there
     *  @param  field       The description of the field
     */
    public SSAGetInstruction GetInstruction(final int iindex, final SSAValue targetValue, FieldReference field) {
        info("Now: Get {} into {}", field, targetValue);
        
        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (targetValue == null) {
            throw new IllegalArgumentException("targetValue may not be null");
        }
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (! isAssignableFrom(field.getFieldType(), targetValue.getType())) {
            throw new IllegalArgumentException("The field " + targetValue + " is not assignable from " + field);
        }

        targetValue.setAssigned();
        return insts.GetInstruction(iindex, targetValue.getNumber(), field);
    }



    /**
     *  Writes newValue to field of targetInstance.
     *
     *  If type check passes the corresponding PutInstruction of the JavaInstructionFactory is called.
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#PutInstruction(int, int, int, FieldReference)
     *
     *  @param  iindex      Zero or a psitive number unique to any instruction of the same method
     *  @param  targetInstance the instance of the object to write a field of
     *  @param  newValue    The value to write to the field
     *  @param  field       The description of the target
     */
    public SSAPutInstruction PutInstruction(final int iindex, final SSAValue targetInstance, final SSAValue newValue,
            FieldReference field) {
        info("Now: Put {} to {}", newValue, field);
        
        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (targetInstance == null) {
            throw new IllegalArgumentException("targetInstance may not be null");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("newValue may not be null");
        }
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (! isSuperclassOf(field.getDeclaringClass(), targetInstance.getType())) {
            throw new IllegalArgumentException("The targetInstance " + targetInstance + " is not equal or a " +
                " super-class of " + field.getDeclaringClass());
        }
        if (! isAssignableFrom(newValue.getType(), field.getFieldType())) {
            throw new IllegalArgumentException("The field " + field + " is not assignable from " + newValue);
        }
    
        final MethodReference newValueValidIn = newValue.getValidIn();
        final MethodReference instValidIn = targetInstance.getValidIn();

        if ((newValueValidIn != null) && (instValidIn != null) && (! newValueValidIn.equals(instValidIn))) {
            throw new IllegalArgumentException("targetInstance " + targetInstance + "and newValue " +
                    newValue + " are valid in different scopes");
        }

        return insts.PutInstruction(iindex, targetInstance.getNumber(), newValue.getNumber(), field);
    }

    /**
     *  Writes newValue to static field.
     *
     *  If type check passes the corresponding PutInstruction of the JavaInstructionFactory is called.
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#PutInstruction(int, int, int, FieldReference)
     *
     *  @param  iindex      Zero or a psitive number unique to any instruction of the same method
     *  @param  newValue    The value to write to the field
     *  @param  field       The description of the target
     */
    public SSAPutInstruction PutInstruction(final int iindex, final SSAValue newValue, FieldReference field) {
        info("Now: Put {} to {}", newValue, field);
        
        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (newValue == null) {
            throw new IllegalArgumentException("newValue may not be null");
        }
        if (field == null) {
            throw new IllegalArgumentException("field may not be null");
        }
        if (! isAssignableFrom(newValue.getType(), field.getFieldType())) {
            throw new IllegalArgumentException("The field " + field + " is not assignable from " + newValue);
        }
    
        // final MethodReference newValueValidIn = newValue.getValidIn();

        return insts.PutInstruction(iindex, newValue.getNumber(), field);
    }


    public SSANewInstruction NewInstruction(int iindex, SSAValue result, NewSiteReference site) {
        info("Now: New {}", result);

        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (result == null) {
            throw new IllegalArgumentException("result may not be null");
        }
        if (site == null) {
            throw new IllegalArgumentException("site may not be null");
        }
        if (! isAssignableFrom(site.getDeclaredType(), result.getType())) {
            throw new IllegalArgumentException("type mismatch");
        }
        result.setAssigned();
        return insts.NewInstruction(iindex, result.getNumber(), site);
    }


    public SSANewInstruction NewInstruction(int iindex, SSAValue result, NewSiteReference site, 
            Collection<? extends SSAValue> params) {
        info("Now: New {}", result);

        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (result == null) {
            throw new IllegalArgumentException("result may not be null");
        }
        if (site == null) {
            throw new IllegalArgumentException("site may not be null");
        }
        if (! isAssignableFrom(site.getDeclaredType(), result.getType())) {
            throw new IllegalArgumentException("type mismatch");
        }

        final MethodReference resultValidIn = result.getValidIn();
        final int[] aParams = new int[params.size()];
        int i = 0;
        for (final SSAValue param : params) {
            final MethodReference paramValidIn = param.getValidIn();

            if ((resultValidIn != null) && (paramValidIn != null) && (! paramValidIn.equals(resultValidIn))) {
                throw new IllegalArgumentException("The parameter " + param + " is valid in another scope than" +
                        result);
            }

            aParams[i] = param.getNumber();
            i++;
        }

        result.setAssigned();
        return insts.NewInstruction(iindex, result.getNumber(), site, aParams);
    }


    /**
     *  Combine SSA-Values into a newone.
     *
     *  If type check passes the corresponding PhiInstruction of the JavaInstructionFactory is called.
     *  Calls result.setAssigned().
     *
     *  @see    com.ibm.wala.classLoader.JavaLanguage.JavaInstructionFactory#PhiInstruction(int, int, int[])
     *
     *  @param  iindex  Zero or a positive number unique to any instruction of the same method
     *  @param  result  Where to write result to
     *  @param  params at least one SSAValue to read from
     */
    public SSAPhiInstruction PhiInstruction(int iindex, SSAValue result, Collection<? extends SSAValue> params) {
        info("Now: Phi into {} from {}", result, params);
        
        if (iindex < 0) {
            throw new IllegalArgumentException("iIndex may not be negative");
        }
        if (result == null) {
            throw new IllegalArgumentException("result may not be null");
        }
        if (params == null) {
            throw new IllegalArgumentException("params may not be null");
        }
        if (params.size() < 1) {
            throw new IllegalArgumentException("Phi needs at least one source value. Type is " + result.getType());
        }
        final MethodReference resultValidIn = result.getValidIn();
        final TypeReference resultType = result.getType();

        final int[] aParams = new int[params.size()];
        int i = 0;
        for (final SSAValue param : params) {
            final MethodReference paramValidIn = param.getValidIn();

            if ((resultValidIn != null) && (paramValidIn != null) && (! paramValidIn.equals(resultValidIn))) {
                throw new IllegalArgumentException("The parameter " + param + " is valid in another scope than" +
                        result);
            }

            if (! isAssignableFrom(param.getType(), resultType)) {
                throw new IllegalArgumentException("Param " + param + " is not assignable to " + result);
            }

            if (result.equals(param)) {
                throw new IllegalArgumentException("Cannot phi to myself: " + result);
            }

            aParams[i] = param.getNumber();
            i++;
            // TODO: Assert no parameter appears twice
        }

        result.setAssigned();
        return insts.PhiInstruction(iindex, result.getNumber(), aParams);
    }

    @SuppressWarnings("unused")
    private static boolean isSuperclassOf(final TypeReference superClass, final TypeReference subClass) {
        return true; // TODO
    }

    public boolean isAssignableFrom(final TypeReference from, final TypeReference to) {
        try {
            return ParameterAccessor.isAssignable(from, to, this.cha);
        } catch (ClassLookupException e) {
            return true;
        }
    }

    // ************
    //  Instructions simply handed through follow
   
    /**
     *  Return from a void-function.
     *
     *  Handed through to JavaInstructionFactory directly (unless the iindex is negative).
     *
     *  @param  iindex  Zero or a positive number unique to any instruction of the same method
     */
    public SSAReturnInstruction ReturnInstruction(final int iindex) {
        if (iindex < 0) {
            throw new IllegalArgumentException("The iindex may not be negative");
        }
        return insts.ReturnInstruction(iindex);
    }

    /**
     *  Unconditionally jump to a (non-Phi) Instruction.
     *
     *  @param  target  the iindex of the instruction to jump to
     */
    public SSAGotoInstruction GotoInstruction(final int iindex, final int target) {
        if (iindex < 0) {
            throw new IllegalArgumentException("The iindex may not be negative");
        }
        if (target < 0) {
            throw new IllegalArgumentException("The target-iindex may not be negative");
        }

        return insts.GotoInstruction(iindex, target);
    }

    public SSAConditionalBranchInstruction ConditionalBranchInstruction(final int iindex, final IConditionalBranchInstruction.IOperator operator, final TypeReference type, final int val1, final int val2, final int target) {
      return insts.ConditionalBranchInstruction(iindex, operator, type, val1, val2, target);
    }
    /**
     *  result = array[index].
     *
     *  Load a a reference from an array.
     *
     *  @param  result  The SSAValue to store the loaded stuff in
     *  @param  array   The array to load from
     *  @param  index   Te position in array to load from
     */
    public SSAArrayLoadInstruction ArrayLoadInstruction(final int iindex, final SSAValue result, final SSAValue array, 
            final int index) {
        if (iindex < 0) {
            throw new IllegalArgumentException("The iindex may not be negative. It's " + iindex);
        }
        if (result == null) {
            throw new IllegalArgumentException("Can't use null for the result");
        }
        if (array == null) {
            throw new IllegalArgumentException("Can't load from array null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("The index in the array may not be negative. It's " + index);
        }
        if (! array.getType().isArrayType()) {
            throw new IllegalArgumentException("The array to read from is expected to be ... well ... an array. The given value was " + array );
        }
        final TypeReference innerType = array.getType().getArrayElementType();
        if (! isAssignableFrom(innerType, result.getType())) {
            throw new IllegalArgumentException("Can't assign from an array of " + innerType.getName() + " to " + result.getType().getName());
        }

        result.setAssigned();
        return insts.ArrayLoadInstruction(iindex, result.getNumber(), array.getNumber(), index, innerType); 
    }

    /**
     *  array[index] = value.
     *
     *  Save a value to a specific position in an Array.
     *
     *  @param  array   the array to store to
     *  @param  index   the position in the array to place value at
     *  @param  value   The SSAValue to store in the array
     */
    public SSAArrayStoreInstruction ArrayStoreInstruction(final int iindex, final SSAValue array, final int index, 
            final SSAValue value) {
        if (iindex < 0) {
            throw new IllegalArgumentException("The iindex may not be negative. It's " + iindex);
        }
        if (value == null) {
            throw new IllegalArgumentException("Can't use null for the value to put");
        }
        if (array == null) {
            throw new IllegalArgumentException("Can't write to array null");
        }
        if (index < 0) {
            throw new IllegalArgumentException("The index in the array may not be negative. It's " + index);
        }
        if (! array.getType().isArrayType()) {
            throw new IllegalArgumentException("The array to write to is expected to be ... well ... an array. The given value was " + array );
        }

        final TypeReference innerType = array.getType().getArrayElementType();
        if (! isAssignableFrom(value.getType(), innerType)) {
            throw new IllegalArgumentException("Can't assign to an array of " + innerType.getName() + " from " + value.getType().getName());
        }

        return insts.ArrayStoreInstruction(iindex, array.getNumber(), index, value.getNumber(), innerType); 
    }
    
    private static void info(String s, Object ... args) {    
      if (DEBUG) { System.err.printf(s, args); }
    }

}
