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
package com.ibm.wala.analysis.reflection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * Understands {@link com.ibm.wala.analysis.reflection.GetMethodContext}.
 * @author Michael Heilmann
 * @see com.ibm.wala.analysis.reflection.GetMethodContext
 * @see com.ibm.wala.analysis.reflection.GetMethodContextSelector
 */
public class GetMethodContextInterpreter implements SSAContextInterpreter {
  /**
   * TODO
   *  MH: Maybe hard-code those in {@link com.ibm.wala.types.MethodReference}?
   */
  public final static MethodReference GET_METHOD = MethodReference.findOrCreate(TypeReference.JavaLangClass, "getMethod",
      "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

  /**
   * TODO
   *  MH: Maybe hard-code those in {@link com.ibm.wala.types.MethodReference}?
   */
  public final static MethodReference GET_DECLARED_METHOD = MethodReference.findOrCreate(TypeReference.JavaLangClass,
      "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

  private static final boolean DEBUG = false;

  /**
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
    IMethod method = node.getMethod();
    GetMethodContext context = (GetMethodContext) node.getContext();
    Map<Integer,ConstantValue> constants = HashMapFactory.make();
    if (method.getReference().equals(GET_METHOD)) {
      Atom name = Atom.findOrCreateAsciiAtom(context.getName());
      SSAInstruction instrs[] = makeGetMethodStatements(context,constants,name);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(), constants);
    }
    if (method.getReference().equals(GET_DECLARED_METHOD)) {
      Atom name = Atom.findOrCreateAsciiAtom(context.getName());
      SSAInstruction instrs[] = makeGetDeclaredMethodStatements(context,constants,name);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(), constants);
    }
    Assertions.UNREACHABLE("Unexpected method " + node);
    return null;
  }

  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

  /**
   * @see com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter#getNumberOfStatements(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public int getNumberOfStatements(CGNode node) {
    assert understands(node);
    return getIR(node).getInstructions().length;
  }

  /**
   * @see com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter#understands(com.ibm.wala.ipa.callgraph.CGNode)
   */
  @Override
  public boolean understands(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (!(node.getContext() instanceof GetMethodContext)) {
      return false;
    }
    MethodReference mRef = node.getMethod().getReference();
    return mRef.equals(GET_METHOD) || mRef.equals(GET_DECLARED_METHOD);
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    GetMethodContext context = (GetMethodContext) node.getContext();
    TypeReference tr = context.getType().getTypeReference();
    if (tr != null) {
      return new NonNullSingletonIterator<>(NewSiteReference.make(0, tr));
    }
    return EmptyIterator.instance();
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    assert understands(node);
    return EmptyIterator.instance();
  }

  /**
   * Get all non-constructor, non-class-initializer methods declared by a class
   * if their name is equal to the specified name.
   * @param cls the class
   * @param name the name
   */
  private static Collection<IMethod> getDeclaredNormalMethods(IClass cls,Atom name) {
    Collection<IMethod> result = HashSetFactory.make();
    for (IMethod m : cls.getDeclaredMethods()) {
      if (!m.isInit() && !m.isClinit() && m.getSelector().getName().equals(name)) {
        result.add(m);
      }
    }
    return result;
  }

  /**
   * Get all non-constructor, non-class-initializer methods declared by a class
   * and all its superclasses if their name is equal to the specified name.
   * @param cls the class
   * @param name the name
   */
  private static Collection<IMethod> getAllNormalPublicMethods(IClass cls,Atom name) {
    Collection<IMethod> result = HashSetFactory.make();
    Collection<IMethod> allMethods = null;
    allMethods = cls.getAllMethods();
    for (IMethod m : allMethods) {
      if (!m.isInit() && !m.isClinit() && m.isPublic() && m.getSelector().getName().equals(name)) {
        result.add(m);
      }
    }
    return result;
  }

  /**
   * Create statements for methods like getMethod() and getDeclaredMethod(),
   * which return a single method. This creates a return statement for each
   * possible return value, each of which is a {@link ConstantValue} for an
   * {@link IMethod}.
   * @param returnValues the possible return values for this method
   * @return the statements
   */
  private static SSAInstruction[] getParticularMethodStatements
      (
        MethodReference ref,
        Collection<IMethod> returnValues,
        GetMethodContext context,
        Map<Integer, ConstantValue> constants
       ) {
    ArrayList<SSAInstruction> statements = new ArrayList<>();
    int nextLocal = ref.getNumberOfParameters() + 2;
    IClass cls = context.getType().getType();
    SSAInstructionFactory insts = context.getType().getType().getClassLoader().getInstructionFactory();
    if (cls != null) {
      for (IMethod m : returnValues) {
        int c = nextLocal++;
        constants.put(c, new ConstantValue(m));
        SSAReturnInstruction R = insts.ReturnInstruction(statements.size(), c, false);
        statements.add(R);
      }
    } else {
      // SJF: This is incorrect. TODO: fix and enable.
      // SSAThrowInstruction t = insts.ThrowInstruction(retValue);
      // statements.add(t);
    }
    SSAInstruction[] result = new SSAInstruction[statements.size()];
    Iterator<SSAInstruction> it = statements.iterator();
    for (int i = 0; i < result.length; i++) {
      result[i] = it.next();
    }
    return result;
  }

  private static SSAInstruction[] makeGetMethodStatements
        (
          GetMethodContext context,
          Map<Integer,ConstantValue> constants,
          Atom name
        ) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getParticularMethodStatements(GET_METHOD, null, context, constants);
    } else {
      return getParticularMethodStatements(GET_METHOD, getAllNormalPublicMethods(cls,name), context, constants);
    }
  }

  /**
   * Create statements for {@link Class#getDeclaredMethod(String, Class...)}.
   */
  private static SSAInstruction[] makeGetDeclaredMethodStatements(GetMethodContext context, Map<Integer, ConstantValue> constants,Atom name) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getParticularMethodStatements(GET_DECLARED_METHOD, null, context, constants);
    } else {
      return getParticularMethodStatements(GET_DECLARED_METHOD, getDeclaredNormalMethods(cls,name), context, constants);
    }
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
  public ControlFlowGraph<SSAInstruction,ISSABasicBlock> getCFG(CGNode N) {
    return getIR(N).getControlFlowGraph();
  }

  @Override
  public DefUse getDU(CGNode node) {
    return new DefUse(getIR(node));
  }
}
