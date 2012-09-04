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

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;

/**
 * {@link SSAContextInterpreter} specialized to interpret Object.getClass() in a {@link JavaTypeContext}
 */
public class GetClassContextInterpeter implements SSAContextInterpreter {

  private static final boolean DEBUG = false;

  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    if (DEBUG) {
      System.err.println("generating IR for " + node);
    }
    IR result = makeIR(node.getMethod(), (JavaTypeContext) node.getContext());
    return result;
  }

  public int getNumberOfStatements(CGNode node) {
    assert understands(node);
    return getIR(node).getInstructions().length;
  }

  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (!(node.getContext() instanceof JavaTypeContext)) {
      return false;
    }
    return node.getMethod().getReference().equals(GetClassContextSelector.GET_CLASS);
  }

  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    return EmptyIterator.instance();
  }

  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    return EmptyIterator.instance();
  }

  private SSAInstruction[] makeStatements(JavaTypeContext context) {
    ArrayList<SSAInstruction> statements = new ArrayList<SSAInstruction>();
    int nextLocal = 2;
    int retValue = nextLocal++;
    TypeReference tr = context.getType().getTypeReference();
    SSAInstructionFactory insts = context.getType().getType().getClassLoader().getInstructionFactory();
    if (tr != null) {
      SSALoadMetadataInstruction l = insts.LoadMetadataInstruction(retValue, TypeReference.JavaLangClass, tr);
      statements.add(l);
      SSAReturnInstruction R = insts.ReturnInstruction(retValue, false);
      statements.add(R);
    }
    SSAInstruction[] result = new SSAInstruction[statements.size()];
    Iterator<SSAInstruction> it = statements.iterator();
    for (int i = 0; i < result.length; i++) {
      result[i] = it.next();
    }
    return result;
  }

  private IR makeIR(IMethod method, JavaTypeContext context) {
    SSAInstruction instrs[] = makeStatements(context);
    return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(), null);
  }

  public boolean recordFactoryType(CGNode node, IClass klass) {
    return false;
  }

  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    return EmptyIterator.instance();
  }

  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    return EmptyIterator.instance();
  }

  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
    return getIR(N).getControlFlowGraph();
  }

  public DefUse getDU(CGNode node) {
    return new DefUse(getIR(node));
  }
}
