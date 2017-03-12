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
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;

/**
 * An {@link SSAContextInterpreter} specialized to interpret reflective invocations such as Constructor.newInstance and
 * Method.invoke on an {@link IMethod} constant.
 */
public class ReflectiveInvocationInterpreter extends AbstractReflectionInterpreter {

  public final static MethodReference CTOR_NEW_INSTANCE = MethodReference.findOrCreate(TypeReference.JavaLangReflectConstructor,
      "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;");

  public final static MethodReference METHOD_INVOKE = MethodReference.findOrCreate(TypeReference.JavaLangReflectMethod, "invoke",
      "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;");

/** BEGIN Custom change: caching */
  private final Map<String, IR> cache = HashMapFactory.make();
  
/** END Custom change: caching */
  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getIR(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    if (DEBUG) {
      System.err.println("generating IR for " + node);
    }
    ReceiverInstanceContext recv = (ReceiverInstanceContext) node.getContext();
    ConstantKey c = (ConstantKey) recv.getReceiver();
    IMethod m = (IMethod) c.getValue();
/** BEGIN Custom change: caching */
    final IMethod method = node.getMethod();
    final String hashKey = method.toString() + "@" + recv.toString();
    
    IR result = cache.get(hashKey);
    
    if (result == null) {
      result = makeIR(method, m, recv);
      cache.put(hashKey, result);
    }
    
/** END Custom change: caching */
    return result;
  }

  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getNumberOfStatements(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public int getNumberOfStatements(CGNode node) {
    assert understands(node);
    return getIR(node).getInstructions().length;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#understands(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (!(node.getContext() instanceof ReceiverInstanceContext)) {
      return false;
    }
    ReceiverInstanceContext r = (ReceiverInstanceContext) node.getContext();
    if (!(r.getReceiver() instanceof ConstantKey)) {
      return false;
    }
    return node.getMethod().getReference().equals(METHOD_INVOKE) || node.getMethod().getReference().equals(CTOR_NEW_INSTANCE);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateNewSites(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    return getIR(node).iterateNewSites();
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#iterateCallSites(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    assert understands(node);
    return getIR(node).iterateCallSites();
  }

  /**
   * TODO: clean this up. Create the IR for the synthetic method (e.g. Method.invoke)
   * 
   * @param method is something like Method.invoke or Construction.newInstance
   * @param target is the method being called reflectively
   */
  private IR makeIR(IMethod method, IMethod target, ReceiverInstanceContext context) {
    SSAInstructionFactory insts = method.getDeclaringClass().getClassLoader().getInstructionFactory();

    SpecializedMethod m = new SpecializedMethod(method, method.getDeclaringClass(), method.isStatic(), false);
    Map<Integer, ConstantValue> constants = HashMapFactory.make();

    int nextLocal = method.getNumberOfParameters() + 1; // nextLocal = first free value number

    int nargs = target.getNumberOfParameters(); // nargs := number of parameters to target, including "this" pointer
    int args[] = new int[nargs];
    int pc = 0;
    int parametersVn = -1; // parametersVn will hold the value number of parameters array

    if (method.getReference().equals(CTOR_NEW_INSTANCE)) {
      // allocate the new object constructed
      TypeReference allocatedType = target.getDeclaringClass().getReference();
      m
          .addInstruction(allocatedType, insts.NewInstruction(m.allInstructions.size(), args[0] = nextLocal++, NewSiteReference.make(pc++, allocatedType)),
              true);
      parametersVn = 2;
    } else {
      // for Method.invoke, v3 is the parameter to the method being called
      parametersVn = 3;
      if (target.isStatic()) {
        // do nothing
      } else {
        // set up args[0] == the receiver for method.invoke, held in v2.
        // insert a cast for v2 to filter out bogus types
        args[0] = nextLocal++;
        TypeReference type = target.getParameterType(0);
        SSACheckCastInstruction cast = insts.CheckCastInstruction(m.allInstructions.size(), args[0], 2, type, true);
        m.addInstruction(null, cast, false);
      }
    }
    int nextArg = target.isStatic() ? 0 : 1; // nextArg := next index in args[] array that needs to be initialized
    int nextParameter = 0; // nextParameter := next index in the parameters[] array that needs to be copied into the args[] array.

    // load each of the parameters into a local variable, args[something]
    for (int j = nextArg; j < nargs; j++) {
      // load the next parameter into v_temp.
      int indexConst = nextLocal++;
      constants.put(new Integer(indexConst), new ConstantValue(nextParameter++));
      int temp = nextLocal++;
      m.addInstruction(null, insts.ArrayLoadInstruction(m.allInstructions.size(), temp, parametersVn, indexConst, TypeReference.JavaLangObject), false);
      pc++;

      // cast v_temp to the appropriate type and store it in args[j]
      args[j] = nextLocal++;
      TypeReference type = target.getParameterType(j);
      // we insert a cast to filter out bogus types
      SSACheckCastInstruction cast = insts.CheckCastInstruction(m.allInstructions.size(), args[j], temp, type, true);
      m.addInstruction(null, cast, false);
      pc++;
    }

    int exceptions = nextLocal++;
    int result = -1;

    // emit the dispatch and return instructions
    if (method.getReference().equals(CTOR_NEW_INSTANCE)) {
      m.addInstruction(null, insts.InvokeInstruction(m.allInstructions.size(), args, exceptions, CallSiteReference.make(pc++, target.getReference(),
          IInvokeInstruction.Dispatch.SPECIAL), null), false);
      m.addInstruction(null, insts.ReturnInstruction(m.allInstructions.size(), args[0], false), false);
    } else {
      Dispatch d = target.isStatic() ? Dispatch.STATIC : Dispatch.VIRTUAL;
      if (target.getReturnType().equals(TypeReference.Void)) {
        m.addInstruction(null, insts.InvokeInstruction(m.allInstructions.size(), args, exceptions, CallSiteReference.make(pc++, target.getReference(), d), null),
            false);
      } else {
        result = nextLocal++;
        m.addInstruction(null, insts.InvokeInstruction(m.allInstructions.size(), result, args, exceptions, CallSiteReference.make(pc++,
            target.getReference(), d), null), false);
        m.addInstruction(null, insts.ReturnInstruction(m.allInstructions.size(), result, false), false);
      }
    }

    SSAInstruction[] instrs = new SSAInstruction[m.allInstructions.size()];
    m.allInstructions.<SSAInstruction> toArray(instrs);

    return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(), constants);
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
