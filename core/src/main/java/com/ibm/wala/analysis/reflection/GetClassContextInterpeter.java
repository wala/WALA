/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.analysis.reflection;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

/**
 * {@link SSAContextInterpreter} specialized to interpret Object.getClass() in a {@link
 * JavaTypeContext}
 */
public class GetClassContextInterpeter implements SSAContextInterpreter {

  /* BEGIN Custom change: caching */
  private final Map<String, IR> cache = HashMapFactory.make();

  /* END Custom change: caching */
  private static final boolean DEBUG = false;

  @Override
  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    if (DEBUG) {
      System.err.println("generating IR for " + node);
    }
    /* BEGIN Custom change: caching */
    final Context context = node.getContext();
    final IMethod method = node.getMethod();
    final String hashKey = method.toString() + '@' + context.toString();

    IR result = cache.get(hashKey);

    if (result == null) {
      result = makeIR(method, context);
      cache.put(hashKey, result);
    }

    /* END Custom change: caching */
    return result;
  }

  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

  @Override
  public int getNumberOfStatements(CGNode node) {
    assert understands(node);
    return getIR(node).getInstructions().length;
  }

  @Override
  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (!node.getContext().isA(JavaTypeContext.class)) {
      return false;
    }
    return node.getMethod().getReference().equals(GetClassContextSelector.GET_CLASS);
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    return EmptyIterator.instance();
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    return EmptyIterator.instance();
  }

  private static SSAInstruction[] makeStatements(Context context) {
    ArrayList<SSAInstruction> statements = new ArrayList<>();
    int nextLocal = 2;
    int retValue = nextLocal++;
    TypeReference tr = ((TypeAbstraction) context.get(ContextKey.RECEIVER)).getTypeReference();
    SSAInstructionFactory insts =
        ((TypeAbstraction) context.get(ContextKey.RECEIVER))
            .getType()
            .getClassLoader()
            .getInstructionFactory();
    if (tr != null) {
      SSALoadMetadataInstruction l =
          insts.LoadMetadataInstruction(
              statements.size(), retValue, TypeReference.JavaLangClass, tr);
      statements.add(l);
      SSAReturnInstruction R = insts.ReturnInstruction(statements.size(), retValue, false);
      statements.add(R);
    }
    return statements.toArray(new SSAInstruction[0]);
  }

  private static IR makeIR(IMethod method, Context context) {
    SSAInstruction instrs[] = makeStatements(context);
    return new SyntheticIR(
        method,
        context,
        new InducedCFG(instrs, method, context),
        instrs,
        SSAOptions.defaultOptions(),
        null);
  }

  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    return false;
  }

  @Override
  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    return EmptyIterator.instance();
  }

  @Override
  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    return EmptyIterator.instance();
  }

  @Override
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
    return getIR(N).getControlFlowGraph();
  }

  @Override
  public DefUse getDU(CGNode node) {
    return new DefUse(getIR(node));
  }
}
