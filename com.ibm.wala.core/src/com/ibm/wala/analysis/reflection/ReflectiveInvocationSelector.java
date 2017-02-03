/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * A {@link ContextSelector} to intercept calls to reflective method invocations such as Constructor.newInstance and Method.invoke
 */
class ReflectiveInvocationSelector implements ContextSelector {

  public ReflectiveInvocationSelector() {
  }

  /**
   * Creates a callee target based on the following criteria:
   * <ol>
   * <li>If the method being invoked through reflection is definitely static, then do not create a callee target for any
   * <code>callee</code> method that is not static. In this case, return <code>null</code>.
   * <li>If the method being invoked through reflection takes a constant number of parameters, <code>n</code>, then do not create a
   * callee target for any <code>callee</code> method that takes a number of parameters different from <code>n</code>. In this case,
   * return <code>null</code>.
   * <li>Otherwise, return a new {@link ReceiverInstanceContext} for <code>receiver</code>.
   * </ol>
   */
  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (receiver == null || receiver.length == 0 || !mayUnderstand(caller, site, callee, receiver[0])) {
      return null;
    }
    IR ir = caller.getIR();
    SSAAbstractInvokeInstruction[] invokeInstructions = ir.getCalls(site);
    if (invokeInstructions.length != 1) {
      return new ReceiverInstanceContext(receiver[0]);
    }
    SymbolTable st = ir.getSymbolTable();
    ConstantKey receiverConstantKey = (ConstantKey) receiver[0];
    IMethod m = (IMethod) receiverConstantKey.getValue();
    boolean isStatic = m.isStatic();
    boolean isConstructor = isConstructorConstant(receiver[0]);

    // If the method being invoked through reflection is not a constructor and is definitely static, then
    // we should not create a callee target for any method that is not static
    if (!isConstructor) {
      int recvUse = invokeInstructions[0].getUse(1);
      if (st.isNullConstant(recvUse) && !isStatic) {
        return null;
      }
    }

    // If the method being invoked through reflection is being passed n parameters,
    // then we should not create a callee target for any method that takes a number
    // of parameters different from n
    int numberOfParams = isStatic ? m.getNumberOfParameters() : m.getNumberOfParameters() - 1;
    // instruction[0] is a call to Method.invoke(), where the receiver is a specific method,
    // the first parameter is the receiver of the method invocation, and the second parameter
    // is an array of objects corresponding to the parameters passed to the method.
    int paramIndex = isConstructor ? 1 : 2;
    int paramUse = invokeInstructions[0].getUse(paramIndex);
    SSAInstruction instr = caller.getDU().getDef(paramUse);
    if (!(instr instanceof SSANewInstruction)) {
      return new ReceiverInstanceContext(receiver[0]);
    }
    SSANewInstruction newInstr = (SSANewInstruction) instr;
    if (!newInstr.getConcreteType().isArrayType()) {
      return null;
    }
    int vn = newInstr.getUse(0);
    try {
      int arrayLength = st.getIntValue(vn);
      if (arrayLength == numberOfParams) {
        return new ReceiverInstanceContext(receiver[0]);
      } else {
        return new IllegalArgumentExceptionContext();
      }
    } catch (IllegalArgumentException e) {
      return new ReceiverInstanceContext(receiver[0]);
    }
  }

  /**
   * This object may understand a dispatch to Constructor.newInstance().
   */
  private boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (instance instanceof ConstantKey) {
      if (targetMethod.getReference().equals(ReflectiveInvocationInterpreter.METHOD_INVOKE) || 
          isConstructorConstant(instance)
          && targetMethod.getReference().equals(ReflectiveInvocationInterpreter.CTOR_NEW_INSTANCE)) {
        return true;
      }
    }
    return false;
  }

  private boolean isConstructorConstant(InstanceKey instance) {
    if (instance instanceof ConstantKey) {
      ConstantKey c = (ConstantKey) instance;
      if (c.getConcreteType().getReference().equals(TypeReference.JavaLangReflectConstructor)) {
        return true;
      }
    }
    return false;
  }

  private static final IntSet thisParameter = IntSetUtil.make(new int[]{0});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (site.getDeclaredTarget().equals(ReflectiveInvocationInterpreter.METHOD_INVOKE) || 
        site.getDeclaredTarget().equals(ReflectiveInvocationInterpreter.CTOR_NEW_INSTANCE)) {
      return thisParameter;
    } else {
      return EmptyIntSet.instance;
    }
  }
}
