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
package com.ibm.wala.ipa.callgraph;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import java.util.Arrays;

/** A representation of an entrypoint in the call graph. */
public abstract class Entrypoint implements BytecodeConstants {

  /** The method to be called */
  protected final IMethod method;

  /** @param method the method to be called for this entrypoint */
  protected Entrypoint(IMethod method) {
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    this.method = method;
    assert method.getDeclaringClass() != null : "null declaring class";
  }

  protected Entrypoint(MethodReference method, IClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    IMethod m = cha.resolveMethod(method);
    if (m == null) {
      Assertions.UNREACHABLE("could not resolve " + method);
    }
    this.method = m;
  }

  /**
   * Create a call site reference representing a call to this entrypoint
   *
   * @param programCounter the bytecode index of the synthesize call
   * @return the call site reference, or null if failed to find entrypoint
   */
  public CallSiteReference makeSite(int programCounter) {

    if (method.getSelector().equals(MethodReference.clinitSelector)) {
      assert method.isStatic();
      return CallSiteReference.make(
          programCounter, method.getReference(), IInvokeInstruction.Dispatch.STATIC);
    } else if (method.getSelector().equals(MethodReference.initSelector)) {
      assert !method.isStatic();
      return CallSiteReference.make(
          programCounter, method.getReference(), IInvokeInstruction.Dispatch.SPECIAL);
    } else {
      if (method.getDeclaringClass().isInterface()) {
        return CallSiteReference.make(
            programCounter, method.getReference(), IInvokeInstruction.Dispatch.INTERFACE);
      } else {
        if (method.isStatic()) {
          return CallSiteReference.make(
              programCounter, method.getReference(), IInvokeInstruction.Dispatch.STATIC);
        } else {
          return CallSiteReference.make(
              programCounter, method.getReference(), IInvokeInstruction.Dispatch.VIRTUAL);
        }
      }
    }
  }

  /**
   * Add allocation statements to the fake root method for each possible value of parameter i. If
   * necessary, add a phi to combine the values.
   *
   * @return value number holding the parameter to the call; -1 if there was some error
   */
  protected int makeArgument(AbstractRootMethod m, int i) {
    TypeReference[] p = getParameterTypes(i);
    switch (p.length) {
      case 0:
        return -1;
      case 1:
        if (p[0].isPrimitiveType()) {
          return m.addLocal();
        } else {
          SSANewInstruction n = m.addAllocation(p[0]);
          return (n == null) ? -1 : n.getDef();
        }
      default:
        int[] values = new int[p.length];
        int countErrors = 0;
        for (int j = 0; j < p.length; j++) {
          SSANewInstruction n = m.addAllocation(p[j]);
          int value = (n == null) ? -1 : n.getDef();
          if (value == -1) {
            countErrors++;
          } else {
            values[j - countErrors] = value;
          }
        }
        if (countErrors > 0) {
          int[] oldValues = values;
          values = new int[oldValues.length - countErrors];
          System.arraycopy(oldValues, 0, values, 0, values.length);
        }

        TypeAbstraction a;
        if (p[0].isPrimitiveType()) {
          a = PrimitiveType.getPrimitive(p[0]);
          for (i = 1; i < p.length; i++) {
            a = a.meet(PrimitiveType.getPrimitive(p[i]));
          }
        } else {
          IClassHierarchy cha = m.getClassHierarchy();
          IClass p0 = cha.lookupClass(p[0]);
          a = new ConeType(p0);
          for (i = 1; i < p.length; i++) {
            IClass pi = cha.lookupClass(p[i]);
            a = a.meet(new ConeType(pi));
          }
        }

        return m.addPhi(values);
    }
  }

  @Override
  public boolean equals(Object obj) {
    // assume these are managed canonically
    return this == obj;
  }

  /**
   * Add a call to this entrypoint from the fake root method
   *
   * @param m the Fake Root Method
   * @return the call instruction added, or null if the operation fails
   */
  public SSAAbstractInvokeInstruction addCall(AbstractRootMethod m) {
    int paramValues[];
    CallSiteReference site = makeSite(0);
    if (site == null) {
      return null;
    }
    paramValues = new int[getNumberOfParameters()];
    for (int j = 0; j < paramValues.length; j++) {
      paramValues[j] = makeArgument(m, j);
      if (paramValues[j] == -1) {
        // there was a problem
        return null;
      }
    }

    return m.addInvocation(paramValues, site);
  }

  /** @return the method this call invokes */
  public IMethod getMethod() {
    return method;
  }

  /** @return types to allocate for parameter i; for non-static methods, parameter 0 is "this" */
  public abstract TypeReference[] getParameterTypes(int i);

  /** @return number of parameters to this call, including "this" for non-statics */
  public abstract int getNumberOfParameters();

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(method.toString());
    result.append('(');
    for (int i = 0; i < getNumberOfParameters() - 1; i++) {
      result.append(Arrays.toString(getParameterTypes(i)));
      result.append(',');
    }
    if (getNumberOfParameters() > 0) {
      result.append(Arrays.toString(getParameterTypes(getNumberOfParameters() - 1)));
    }
    result.append(')');
    return result.toString();
  }

  @Override
  public int hashCode() {
    return method.hashCode() * 1009;
  }
}
