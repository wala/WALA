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
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSANewInstruction;
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

/**
 * An {@link SSAContextInterpreter} specialized to interpret methods on java.lang.Class in a {@link JavaTypeContext} which
 * represents the point-type of the class object created by the call.
 * 
 * Currently supported methods:
 * <ul>
 * <li>getConstructor
 * <li>getConstructors
 * <li>getMethod
 * <li>getMethods
 * <li>getDeclaredConstructor
 * <li>getDeclaredConstructors
 * <li>getDeclaredMethod
 * <li>getDeclaredMethods
 * </ul>
 */
public class JavaLangClassContextInterpreter implements SSAContextInterpreter {

  public final static MethodReference GET_CONSTRUCTOR = MethodReference.findOrCreate(TypeReference.JavaLangClass, "getConstructor",
      "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;");

  public final static MethodReference GET_CONSTRUCTORS = MethodReference.findOrCreate(TypeReference.JavaLangClass,
      "getConstructors", "()[Ljava/lang/reflect/Constructor;");

  public final static MethodReference GET_METHOD = MethodReference.findOrCreate(TypeReference.JavaLangClass, "getMethod",
      "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

  public final static MethodReference GET_METHODS = MethodReference.findOrCreate(TypeReference.JavaLangClass, "getMethods",
      "()[Ljava/lang/reflect/Method;");

  public final static MethodReference GET_DECLARED_CONSTRUCTOR = MethodReference.findOrCreate(TypeReference.JavaLangClass,
      "getDeclaredConstructor", "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;");

  public final static MethodReference GET_DECLARED_CONSTRUCTORS = MethodReference.findOrCreate(TypeReference.JavaLangClass,
      "getDeclaredConstructors", "()[Ljava/lang/reflect/Constructor;");

  public final static MethodReference GET_DECLARED_METHOD = MethodReference.findOrCreate(TypeReference.JavaLangClass,
      "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;");

  public final static MethodReference GET_DECLARED_METHODS = MethodReference.findOrCreate(TypeReference.JavaLangClass,
      "getDeclaredMethods", "()[Ljava/lang/reflect/Method;");

  private static final boolean DEBUG = false;

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
/** BEGIN Custom change: caching */
    
    final JavaTypeContext context = (JavaTypeContext) node.getContext();
    final IMethod method = node.getMethod();
    final String hashKey = method.toString() + "@" + context.toString();
    
    IR result = cache.get(hashKey);
    
    if (result == null) {
      result = makeIR(method, context);
      if (result == null) {
        Assertions.UNREACHABLE("Unexpected method " + node);
      }
      cache.put(hashKey, result);
    }
    
    return result;
  }

  @Override
  public IRView getIRView(CGNode node) {
    return getIR(node);
  }

  private static IR makeIR(IMethod method, JavaTypeContext context) {
    Map<Integer, ConstantValue> constants = HashMapFactory.make();
    if (method.getReference().equals(GET_CONSTRUCTOR)) {
      SSAInstruction instrs[] = makeGetCtorStatements(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    if (method.getReference().equals(GET_CONSTRUCTORS)) {
      SSAInstruction instrs[] = makeGetCtorsStatements(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    if (method.getReference().equals(GET_METHOD)) {
      SSAInstruction instrs[] = makeGetMethodStatements(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    if (method.getReference().equals(GET_METHODS)) {
      SSAInstruction instrs[] = makeGetMethodsStatments(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    if (method.getReference().equals(GET_DECLARED_CONSTRUCTOR)) {
      SSAInstruction instrs[] = makeGetDeclCtorStatements(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    if (method.getReference().equals(GET_DECLARED_CONSTRUCTORS)) {
      SSAInstruction instrs[] = makeGetDeclCtorsStatements(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    if (method.getReference().equals(GET_DECLARED_METHOD)) {
      SSAInstruction instrs[] = makeGetDeclaredMethodStatements(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    if (method.getReference().equals(GET_DECLARED_METHODS)) {
      SSAInstruction instrs[] = makeGetDeclaredMethodsStatements(context, constants);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(),
          constants);
    }
    Assertions.UNREACHABLE("Unexpected method " + method);
    return null;
  }
/** END Custom change: caching */
  
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
    if (!(node.getContext() instanceof JavaTypeContext)) {
      return false;
    }
    MethodReference mRef = node.getMethod().getReference();
    return mRef.equals(GET_CONSTRUCTOR) || mRef.equals(GET_CONSTRUCTORS) || mRef.equals(GET_METHOD) || mRef.equals(GET_METHODS)
        || mRef.equals(GET_DECLARED_CONSTRUCTOR) || mRef.equals(GET_DECLARED_CONSTRUCTORS) || mRef.equals(GET_DECLARED_METHOD)
        || mRef.equals(GET_DECLARED_METHODS);
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    JavaTypeContext context = (JavaTypeContext) node.getContext();
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
   */
  private static Collection<IMethod> getDeclaredNormalMethods(IClass cls) {
    Collection<IMethod> result = HashSetFactory.make();
    for (IMethod m : cls.getDeclaredMethods()) {
      if (!m.isInit() && !m.isClinit()) {
        result.add(m);
      }
    }
    return result;
  }

  /**
   * Get all non-constructor, non-class-initializer methods declared by a class and all its superclasses
   */
  private static Collection<IMethod> getAllNormalPublicMethods(IClass cls) {
    Collection<IMethod> result = HashSetFactory.make();
    Collection<IMethod> allMethods = null;
    allMethods = cls.getAllMethods();
    for (IMethod m : allMethods) {
      if (!m.isInit() && !m.isClinit() && m.isPublic()) {
        result.add(m);
      }
    }
    return result;
  }

  /**
   * Get all the constructors of a class
   */
  private static Collection<IMethod> getConstructors(IClass cls) {
    Collection<IMethod> result = HashSetFactory.make();
    for (IMethod m : cls.getDeclaredMethods()) {
      if (m.isInit()) {
        result.add(m);
      }
    }
    return result;
  }

  /**
   * Get all the public constructors of a class
   */
  private static Collection<IMethod> getPublicConstructors(IClass cls) {
    Collection<IMethod> result = HashSetFactory.make();
    for (IMethod m : cls.getDeclaredMethods()) {
      if (m.isInit() && m.isPublic()) {
        result.add(m);
      }
    }
    return result;
  }

  /**
   * create statements for methods like getConstructors() and getMethods(), which return an array of methods.
   * 
   * @param returnValues the possible return values for this method.
   */
  private static SSAInstruction[] getMethodArrayStatements(MethodReference ref, Collection<IMethod> returnValues, JavaTypeContext context,
      Map<Integer, ConstantValue> constants) {
    ArrayList<SSAInstruction> statements = new ArrayList<>();
    int nextLocal = ref.getNumberOfParameters() + 2;
    int retValue = nextLocal++;
    IClass cls = context.getType().getType();
    SSAInstructionFactory insts = context.getType().getType().getClassLoader().getInstructionFactory();
    if (cls != null) {
      TypeReference arrType = ref.getReturnType();
      NewSiteReference site = new NewSiteReference(retValue, arrType);
      int sizeVn = nextLocal++;
      constants.put(sizeVn, new ConstantValue(returnValues.size()));
      SSANewInstruction allocArr = insts.NewInstruction(statements.size(), retValue, site, new int[] { sizeVn });
      statements.add(allocArr);

      int i = 0;
      for (IMethod m : returnValues) {
        int c = nextLocal++;
        constants.put(c, new ConstantValue(m));
        int index = i++;
        int indexVn = nextLocal++;
        constants.put(indexVn, new ConstantValue(index));
        SSAArrayStoreInstruction store = insts
            .ArrayStoreInstruction(statements.size(), retValue, indexVn, c, TypeReference.JavaLangReflectConstructor);
        statements.add(store);
      }
      SSAReturnInstruction R = insts.ReturnInstruction(statements.size(), retValue, false);
      statements.add(R);
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

  /**
   * create statements for methods like getConstructor() and getDeclaredMethod(), which return a single method. This creates a
   * return statement for each possible return value, each of which is a {@link ConstantValue} for an {@link IMethod}.
   * 
   * @param returnValues the possible return values for this method.
   */
  private static SSAInstruction[] getParticularMethodStatements(MethodReference ref, Collection<IMethod> returnValues,
      JavaTypeContext context, Map<Integer, ConstantValue> constants) {
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

  /**
   * create statements for getConstructor()
   */
  private static SSAInstruction[] makeGetCtorStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getParticularMethodStatements(GET_CONSTRUCTOR, null, context, constants);
    } else {
      return getParticularMethodStatements(GET_CONSTRUCTOR, getPublicConstructors(cls), context, constants);
    }
  }

  // TODO
  private static SSAInstruction[] makeGetCtorsStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getMethodArrayStatements(GET_DECLARED_CONSTRUCTORS, null, context, constants);
    } else {
      return getMethodArrayStatements(GET_DECLARED_CONSTRUCTORS, getPublicConstructors(cls), context, constants);
    }
  }

  private static SSAInstruction[] makeGetMethodStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getParticularMethodStatements(GET_METHOD, null, context, constants);
    } else {
      return getParticularMethodStatements(GET_METHOD, getAllNormalPublicMethods(cls), context, constants);
    }
  }

  private static SSAInstruction[] makeGetMethodsStatments(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getMethodArrayStatements(GET_METHODS, null, context, constants);
    } else {
      return getMethodArrayStatements(GET_METHODS, getAllNormalPublicMethods(cls), context, constants);
    }
  }

  /**
   * create statements for getConstructor()
   */
  private static SSAInstruction[] makeGetDeclCtorStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getParticularMethodStatements(GET_DECLARED_CONSTRUCTOR, null, context, constants);
    } else {
      return getParticularMethodStatements(GET_DECLARED_CONSTRUCTOR, getConstructors(cls), context, constants);
    }
  }

  private static SSAInstruction[] makeGetDeclCtorsStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getMethodArrayStatements(GET_DECLARED_CONSTRUCTORS, null, context, constants);
    } else {
      return getMethodArrayStatements(GET_DECLARED_CONSTRUCTORS, getConstructors(cls), context, constants);
    }
  }

  /**
   * create statements for getDeclaredMethod()
   */
  private static SSAInstruction[] makeGetDeclaredMethodStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getParticularMethodStatements(GET_DECLARED_METHOD, null, context, constants);
    } else {
      return getParticularMethodStatements(GET_DECLARED_METHOD, getDeclaredNormalMethods(cls), context, constants);
    }
  }

  /**
   * create statements for getDeclaredMethod()
   */
  private static SSAInstruction[] makeGetDeclaredMethodsStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    IClass cls = context.getType().getType();
    if (cls == null) {
      return getMethodArrayStatements(GET_DECLARED_METHODS, null, context, constants);
    } else {
      return getMethodArrayStatements(GET_DECLARED_METHODS, getDeclaredNormalMethods(cls), context, constants);
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
  public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode N) {
    return getIR(N).getControlFlowGraph();
  }

  @Override
  public DefUse getDU(CGNode node) {
    return new DefUse(getIR(node));
  }
}
