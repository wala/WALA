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
package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.cast.java.test.IRTests.IRAssertion;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Iterator2Iterable;

final class TypeInferenceAssertion implements IRAssertion {
  private String typeName;

  public TypeInferenceAssertion(String packageName) {
    this.typeName = packageName;
  }

  // For now just check things in the main method
  @Override
  public void check(CallGraph cg) {
    IR ir = getIR(cg, typeName, "main", "[Ljava/lang/String;", "V");
    AstJavaTypeInference inference = new AstJavaTypeInference(ir, true);

    for (SSAInstruction instr : Iterator2Iterable.make(ir.iterateAllInstructions())) {
      // Check defs
      for (int def = 0; def < instr.getNumberOfDefs(); def++) {
        int ssaVariable = instr.getDef(def);
        inference.getType(ssaVariable);
      }

      // Check uses
      for (int def = 0; def < instr.getNumberOfUses(); def++) {
        int ssaVariable = instr.getUse(def);
        inference.getType(ssaVariable);
      }
    }

  }

  private static IR getIR(CallGraph cg, String fullyQualifiedTypeName, String methodName, String methodParameter, String methodReturnType) {
    IClassHierarchy classHierarchy = cg.getClassHierarchy();
    MethodReference methodRef = IRTests
        .descriptorToMethodRef(
            String.format("Source#%s#%s#(%s)%s", fullyQualifiedTypeName, methodName, methodParameter, methodReturnType),
            classHierarchy);
    IMethod method = classHierarchy.resolveMethod(methodRef);
    CGNode node = cg.getNode(method, Everywhere.EVERYWHERE);
    return node.getIR();
  }

}
