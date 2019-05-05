/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;
import java.util.Map;
import java.util.Objects;

public class LambdaMethodTargetSelector implements MethodTargetSelector {

  private final Map<BootstrapMethod, SummarizedMethod> methodSummaries = HashMapFactory.make();

  private final Map<BootstrapMethod, LambdaSummaryClass> classSummaries = HashMapFactory.make();

  private final MethodTargetSelector base;

  public LambdaMethodTargetSelector(MethodTargetSelector base) {
    this.base = base;
  }

  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    IClassHierarchy cha = caller.getClassHierarchy();
    MethodReference target = site.getDeclaredTarget();
    if (isNonClinitLambdaMetafactoryMethod(cha, target)) {
      SSAInvokeDynamicInstruction invoke =
          (SSAInvokeDynamicInstruction) caller.getIR().getCalls(site)[0];

      if (!methodSummaries.containsKey(invoke.getBootstrap())) {
        MethodSummary summary = getSummaryForBootstrapMethod(caller, site, target, invoke);

        methodSummaries.put(
            invoke.getBootstrap(),
            new SummarizedMethod(
                summary.getMethod(), summary, cha.lookupClass(target.getDeclaringClass())));
      }
      return methodSummaries.get(invoke.getBootstrap());

    } else {
      return base.getCalleeTarget(caller, site, receiver);
    }
  }

  /**
   * create a summary for a bootstrap method. The generated IR in the summary allocates a
   * corresponding {@link LambdaSummaryClass} and sets its fields to correspond to the target
   * parameters.
   */
  private MethodSummary getSummaryForBootstrapMethod(
      CGNode caller,
      CallSiteReference site,
      MethodReference target,
      SSAInvokeDynamicInstruction invoke) {
    String cls =
        caller.getMethod().getDeclaringClass().getName().toString().replace("/", "$").substring(1);
    int bootstrapIndex = invoke.getBootstrap().getIndexInClassFile();
    MethodReference ref =
        MethodReference.findOrCreate(
            target.getDeclaringClass(),
            Atom.findOrCreateUnicodeAtom(
                target.getName().toString() + '$' + cls + '$' + bootstrapIndex),
            target.getDescriptor());

    MethodSummary summary = new MethodSummary(ref);

    if (site.isStatic()) {
      summary.setStatic(true);
    }

    int index = 0;
    int v = target.getNumberOfParameters() + 2;
    IClass lambda = getLambdaSummaryClass(caller, invoke);
    SSAInstructionFactory insts = Language.JAVA.instructionFactory();
    summary.addStatement(
        insts.NewInstruction(index, v, NewSiteReference.make(index, lambda.getReference())));
    index++;
    for (int i = 0; i < target.getNumberOfParameters(); i++) {
      summary.addStatement(
          insts.PutInstruction(
              index++,
              v,
              i + 1,
              lambda.getField(Atom.findOrCreateUnicodeAtom("c" + i)).getReference()));
    }
    summary.addStatement(insts.ReturnInstruction(index++, v, false));
    return summary;
  }

  private boolean isNonClinitLambdaMetafactoryMethod(IClassHierarchy cha, MethodReference target) {
    return !target.getName().equals(MethodReference.clinitName)
        && Objects.equals(
            cha.lookupClass(TypeReference.LambdaMetaFactory),
            cha.lookupClass(target.getDeclaringClass()));
  }

  private LambdaSummaryClass getLambdaSummaryClass(
      CGNode caller, SSAInvokeDynamicInstruction invoke) {
    BootstrapMethod bootstrap = invoke.getBootstrap();
    LambdaSummaryClass result = classSummaries.get(bootstrap);
    if (result == null) {
      result = LambdaSummaryClass.create(caller, invoke);
      classSummaries.put(bootstrap, result);
    }
    return result;
  }
}
