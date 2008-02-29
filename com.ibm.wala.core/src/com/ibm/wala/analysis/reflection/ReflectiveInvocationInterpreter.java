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

import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.ReceiverInstanceContext;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * An {@link SSAContextInterpreter} specialized to interpret reflective invocations such as Constructor.newInstance and
 * Method.invoke on an {@link IMethod} constant.
 * 
 * @author pistoia
 * @author sjfink
 */
public class ReflectiveInvocationInterpreter extends AbstractReflectionInterpreter {

  public final static MethodReference CTOR_NEW_INSTANCE = MethodReference.findOrCreate(TypeReference.JavaLangReflectConstructor,
      "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;");

  public final static MethodReference METHOD_INVOKE = MethodReference.findOrCreate(TypeReference.JavaLangReflectMethod, "invoke",
      "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getIR(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    ReceiverInstanceContext recv = (ReceiverInstanceContext) node.getContext();
    ConstantKey c = (ConstantKey) recv.getReceiver();
    IMethod m = (IMethod) c.getValue();
    IR result = makeIR(node.getMethod(), m, recv);
    return result;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getNumberOfStatements(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public int getNumberOfStatements(CGNode node) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    return getIR(node).getInstructions().length;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#understands(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (!(node.getContext() instanceof ReceiverInstanceContext)) {
      return false;
    }
    ReceiverInstanceContext r = (ReceiverInstanceContext)node.getContext();
    if (!(r.getReceiver() instanceof ConstantKey)) {
      return false;
    }
    return node.getMethod().getReference().equals(METHOD_INVOKE) || node.getMethod().getReference().equals(CTOR_NEW_INSTANCE);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateNewSites(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    return getIR(node).iterateNewSites();
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateCallSites(com.ibm.wala.ipa.callgraph.CGNode)
   */
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    return getIR(node).iterateCallSites();
  }

  /**
   * TODO: clean this up. Create the IR for the synthetic method (e.g. Method.invoke)
   * 
   * @param method is something like Method.invoke or Construction.newInstance
   * @param target is the method being called reflectively
   */
  private IR makeIR(IMethod method, IMethod target, ReceiverInstanceContext context) {
    SpecializedMethod m = new SpecializedMethod(method, method.getDeclaringClass(), method.isStatic(), false);
    Map<Integer, ConstantValue> constants = HashMapFactory.make();

    int nextLocal = method.getNumberOfParameters() + 1;

    int nargs = target.getNumberOfParameters();
    int args[] = new int[nargs];
    int i = 0;
    int pc = 0;
    int parametersVn = -1;

    if (method.getReference().equals(CTOR_NEW_INSTANCE)) {
      // allocate the new object constructed
      TypeReference allocatedType = target.getDeclaringClass().getReference();
      m.addInstruction(allocatedType, new SSANewInstruction(args[i++] = nextLocal++, NewSiteReference.make(pc++, allocatedType)),
          true);
      parametersVn = 2;
    } else {
      // for Method.invoke, v3 is the parameters to the method being called
      parametersVn = 3;
      if (target.isStatic()) {
        // do nothing
      } else {
        // set up args[0] == v2, the receiver for method.invoke.
        args[i++] = 2;
      }

    }

    // load each of the parameters into a local variable, args[something]
    for (int j = i; j < nargs; j++) {
      int indexConst = nextLocal++;
      m.addInstruction(null, new SSAArrayLoadInstruction(args[i++] = nextLocal++, parametersVn, indexConst,
          TypeReference.JavaLangObject), false);
      constants.put(new Integer(indexConst), new ConstantValue(j - 1));
      pc++;
    }

    int exceptions = nextLocal++;
    int result = -1;

    // emit the dispatch and return instructions
    if (method.getReference().equals(CTOR_NEW_INSTANCE)) {
      m.addInstruction(null, new SSAInvokeInstruction(args, exceptions, CallSiteReference.make(pc++, target.getReference(),
          IInvokeInstruction.Dispatch.SPECIAL)), false);
      m.addInstruction(null, new SSAReturnInstruction(args[0], false), false);
    } else {
      Dispatch d = target.isStatic() ? Dispatch.STATIC : Dispatch.VIRTUAL;
      if (target.getReturnType().equals(TypeReference.Void)) {
        m.addInstruction(null, new SSAInvokeInstruction(args, exceptions, CallSiteReference.make(pc++, target.getReference(), d)),
            false);
      } else {
        result = nextLocal++;
        m.addInstruction(null, new SSAInvokeInstruction(result, args, exceptions, CallSiteReference.make(pc++, target
            .getReference(), d)), false);
        m.addInstruction(null, new SSAReturnInstruction(result, false), false);
      }
    }

    SSAInstruction[] instrs = new SSAInstruction[m.allInstructions.size()];
    m.allInstructions.<SSAInstruction> toArray(instrs);

    return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(), constants);
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

  public ControlFlowGraph<ISSABasicBlock> getCFG(CGNode N) {
    return getIR(N).getControlFlowGraph();
  }

  public DefUse getDU(CGNode node) {
    return new DefUse(getIR(node));
  }
}
