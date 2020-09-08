/*
 * Copyright (c) 2002 - 2012 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.callgraph.fieldbased.flowgraph.vertices;

import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.util.debug.Assertions;
import java.util.Arrays;

/**
 * A variable vertex represents an SSA variable inside a given function.
 *
 * @author mschaefer
 */
public final class VarVertex extends Vertex implements PointerKey {
  private final FuncVertex func;
  private final int valueNumber;

  VarVertex(FuncVertex func, int valueNumber) {
    Assertions.productionAssertion(valueNumber >= 0, "Invalid value number for VarVertex");
    this.func = func;
    this.valueNumber = valueNumber;
  }

  public FuncVertex getFunction() {
    return func;
  }

  public int getValueNumber() {
    return valueNumber;
  }

  @Override
  public <T> T accept(VertexVisitor<T> visitor) {
    return visitor.visitVarVertex(this);
  }

  @Override
  public String toString() {
    return "Var(" + func + ", " + valueNumber + ')';
  }

  @Override
  public String toSourceLevelString(IAnalysisCacheView cache) {
    // we want to get a variable name rather than a value number
    IClass concreteType = func.getConcreteType();
    AstMethod method = (AstMethod) concreteType.getMethod(AstMethodReference.fnSelector);
    IR ir = cache.getIR(method);
    String methodPos = method.getSourcePosition().prettyPrint();
    // we rely on the fact that the CAst IR ignores the index position!
    String[] localNames = ir.getLocalNames(0, valueNumber);
    StringBuilder result = new StringBuilder("Var(").append(methodPos).append(", ");
    if (localNames != null && localNames.length > 0) {
      result.append(Arrays.toString(localNames));
    } else {
      result.append("%ssa_val ").append(valueNumber);
    }
    result.append(")");
    return result.toString();
  }
}
