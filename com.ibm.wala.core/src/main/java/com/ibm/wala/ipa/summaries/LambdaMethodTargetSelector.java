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
import com.ibm.wala.ipa.summaries.LambdaSummaryClass.UnresolvedLambdaBodyException;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.strings.Atom;
import java.util.Map;

/**
 * Generates synthetic summaries to model the behavior of Java 8 lambdas. See
 * https://cr.openjdk.java.net/~briangoetz/lambda/lambda-translation.html for a good discussion of
 * how lambdas look in bytecode.
 *
 * <p>We generate two types of summaries.
 *
 * <ol>
 *   <li>A summary class corresponding to an anonymous class generated for the lambda; see {@link
 *       LambdaSummaryClass}.
 *   <li>A summary method corresponding to the lambda factory generated by the bootstrap method;
 *       this is the method actually called by {@code invokedynamic}.
 * </ol>
 */
public class LambdaMethodTargetSelector implements MethodTargetSelector {

  /** cache of summary methods for lambda factories */
  private final Map<BootstrapMethod, SummarizedMethod> methodSummaries = HashMapFactory.make();

  /** cache of summaries for lambda anonymous classes */
  private final Map<BootstrapMethod, LambdaSummaryClass> classSummaries = HashMapFactory.make();

  private final MethodTargetSelector base;

  public LambdaMethodTargetSelector(MethodTargetSelector base) {
    this.base = base;
  }

  /**
   * Return a synthetic method target for invokedynamic calls corresponding to Java lambdas
   *
   * @param caller the GCNode in the call graph containing the call
   * @param site the call site reference of the call site
   * @param receiver the type of the target object or null
   * @return a synthetic method if the call is an invokedynamic for Java lambdas; the callee target
   *     from the base selector otherwise
   */
  @Override
  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    IR ir = caller.getIR();
    if (ir.getCallInstructionIndices(site) != null) {
      SSAAbstractInvokeInstruction call = ir.getCalls(site)[0];
      if (call instanceof SSAInvokeDynamicInstruction) {
        SSAInvokeDynamicInstruction invoke = (SSAInvokeDynamicInstruction) call;
        BootstrapMethod bootstrap = invoke.getBootstrap();
        if (bootstrap.isBootstrapForJavaLambdas()) {
          IClassHierarchy cha = caller.getClassHierarchy();
          // our summary generation relies on being able to resolve the Lambdametafactory class
          if (cha.lookupClass(TypeReference.LambdaMetaFactory) != null) {
            MethodReference target = site.getDeclaredTarget();
            try {
              return methodSummaries.computeIfAbsent(
                  invoke.getBootstrap(),
                  (b) -> {
                    MethodSummary summary = getLambdaFactorySummary(caller, site, target, invoke);
                    return new SummarizedMethod(
                        summary.getMethod(), summary, cha.lookupClass(target.getDeclaringClass()));
                  });
            } catch (UnresolvedLambdaBodyException e) {
              // give up on modeling the lambda
              return null;
            }
          }
        }
      }
    }
    return base.getCalleeTarget(caller, site, receiver);
  }

  /**
   * Create a summary for a lambda factory, as it would be generated by the lambda metafactory. The
   * lambda factory summary returns an instance of the summary anonymous class for the lambda (see
   * {@link LambdaSummaryClass}). If the lambda captures values from the enclosing scope, the lambda
   * factory summary stores these values in fields of the summary class.
   */
  private MethodSummary getLambdaFactorySummary(
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
    IClass lambda =
        classSummaries.computeIfAbsent(
            invoke.getBootstrap(), (b) -> LambdaSummaryClass.create(caller, invoke));
    SSAInstructionFactory insts = Language.JAVA.instructionFactory();
    // allocate an anonymous class object.
    // v is a value number beyond the value numbers used for the invokedynamic arguments
    int v = target.getNumberOfParameters() + 2;
    summary.addStatement(
        insts.NewInstruction(index, v, NewSiteReference.make(index, lambda.getReference())));
    index++;
    // store captured values in anonymous class fields
    for (int i = 0; i < target.getNumberOfParameters(); i++) {
      summary.addStatement(
          insts.PutInstruction(
              index++,
              v,
              i + 1,
              lambda.getField(LambdaSummaryClass.getCaptureFieldName(i)).getReference()));
    }
    // return the anonymous class instance
    summary.addStatement(insts.ReturnInstruction(index++, v, false));
    return summary;
  }
}
