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
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * An {@link SSAContextInterpreter} specialized to interpret Constructor.newInstance in a {@link JavaTypeContext} which
 * represents the point-type of the class object created by the call.
 * 
 * @author pistoia
 */
public class ConstructorNewInstanceContextInterpreter extends AbstractReflectionInterpreter {

  public final static Atom newInstanceAtom = Atom.findOrCreateUnicodeAtom("newInstance");

  private final static Descriptor newInstanceDescriptor = Descriptor.findOrCreateUTF8("([Ljava/lang/Object;)Ljava/lang/Object;");

  public final static MethodReference NEW_INSTANCE_REF = MethodReference.findOrCreate(TypeReference.JavaLangReflectConstructor,
      newInstanceAtom, newInstanceDescriptor);

  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    ReceiverInstanceContext recv = (ReceiverInstanceContext)node.getContext();
    ConstantKey c = (ConstantKey) recv.getReceiver();
    IMethod m = (IMethod) c.getValue();
    IR result = makeIR(node.getMethod(), m, recv);
    return result;
  }

  public int getNumberOfStatements(CGNode node) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    return getIR(node).getInstructions().length;
  }

  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (!(node.getContext() instanceof ReceiverInstanceContext)) {
      return false;
    }
    return node.getMethod().getReference().equals(NEW_INSTANCE_REF);
  }

  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    return getIR(node).iterateNewSites();
  }

  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    return getIR(node).iterateCallSites();
  }

  private IR makeIR(IMethod method, IMethod ctor, ReceiverInstanceContext context) {
    SpecializedMethod m = new SpecializedMethod(method, method.getDeclaringClass(), method.isStatic(), false);

    Map<Integer, ConstantValue> constants = HashMapFactory.make();

    int nextLocal = method.getNumberOfParameters() + 1;

    int nargs = ctor.getNumberOfParameters();
    int args[] = new int[nargs];
    int i = 0;
    int pc = 0;

    TypeReference allocatedType = ctor.getDeclaringClass().getReference();
    m.addInstruction(allocatedType, new SSANewInstruction(args[i++] = nextLocal++, NewSiteReference.make(pc++, allocatedType)),
        true);

    for (int j = 1; j < nargs; j++) {
      int indexConst = nextLocal++;
      m.addInstruction(null, new SSAArrayLoadInstruction(args[i++] = nextLocal++, 2, indexConst, TypeReference.JavaLangObject),
          false);
      constants.put(new Integer(indexConst), new ConstantValue(j - 1));
      pc++;
    }

    int exceptions = nextLocal++;

    m.addInstruction(null, new SSAInvokeInstruction(args, exceptions, CallSiteReference.make(pc++, ctor.getReference(),
        IInvokeInstruction.Dispatch.SPECIAL)), false);

    m.addInstruction(null, new SSAReturnInstruction(args[0], false), false);

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
