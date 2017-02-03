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

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

/**
 * A {@link ContextSelector} to intercept calls to reflective class factories (e.g. Class.forName()) when the parameter is a string
 * constant
 */
class ClassFactoryContextSelector implements ContextSelector {

  public final static Atom forNameAtom = Atom.findOrCreateUnicodeAtom("forName");

  private final static Descriptor forNameDescriptor = Descriptor.findOrCreateUTF8("(Ljava/lang/String;)Ljava/lang/Class;");

  public final static MethodReference FOR_NAME_REF = MethodReference.findOrCreate(TypeReference.JavaLangClass, forNameAtom,
      forNameDescriptor);

  public final static Atom loadClassAtom = Atom.findOrCreateUnicodeAtom("loadClass");

  private final static Descriptor loadClassDescriptor = Descriptor.findOrCreateUTF8("(Ljava/lang/String;)Ljava/lang/Class;");

  private final static TypeReference CLASSLOADER = TypeReference.findOrCreate(ClassLoaderReference.Primordial,
      "Ljava/lang/ClassLoader");

  public final static MethodReference LOAD_CLASS_REF = MethodReference
      .findOrCreate(CLASSLOADER, loadClassAtom, loadClassDescriptor);

  public ClassFactoryContextSelector() {
  }

  public static boolean isClassFactory(MethodReference m) {
    if (m.equals(FOR_NAME_REF)) {
      return true;
    }
    if (m.equals(LOAD_CLASS_REF)) {
      return true;
    }
    return false;
  }

  public int getUseOfStringParameter(SSAAbstractInvokeInstruction call) {
    if (call.isStatic()) {
      return call.getUse(0);
    } else {
      return call.getUse(1);
    }
  }

  /**
   * If the {@link CallSiteReference} invokes Class.forName(s) and s is a string constant, return a {@link JavaTypeContext}
   * representing the type named by s, if we can resolve it in the {@link IClassHierarchy}.
   * 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference, com.ibm.wala.classLoader.IMethod,
   *      InstanceKey[])
   */
  @Override
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (isClassFactory(callee.getReference())) {
      IR ir = caller.getIR();
      SymbolTable symbolTable = ir.getSymbolTable();
      SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
      if (invokeInstructions.length != 1) {
        return null;
      }
      int use = getUseOfStringParameter(invokeInstructions[0]);
      if (symbolTable.isStringConstant(use)) {
        String className = StringStuff.deployment2CanonicalTypeString(symbolTable.getStringValue(use));
        TypeReference t = TypeReference.findOrCreate(caller.getMethod().getDeclaringClass().getClassLoader().getReference(),
            className);
        IClass klass = caller.getClassHierarchy().lookupClass(t);
        if (klass != null) {
          return new JavaTypeContext(new PointType(klass));
        }
      }
    }
    return null;
  }

  private static final IntSet thisParameter = IntSetUtil.make(new int[]{0});

  private static final IntSet firstParameter = IntSetUtil.make(new int[]{1});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (isClassFactory(site.getDeclaredTarget())) {
      SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
      if (invokeInstructions.length != 1) {
        if (invokeInstructions[0].isStatic()) {
          return thisParameter;
        } else {
          return firstParameter;
        }
      } else {
        return EmptyIntSet.instance;
      }
    } else {
      return EmptyIntSet.instance;      
    }
  }
}
