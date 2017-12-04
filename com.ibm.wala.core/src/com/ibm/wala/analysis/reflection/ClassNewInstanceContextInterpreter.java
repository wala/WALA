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
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.strings.Atom;

/**
 * An {@link SSAContextInterpreter} specialized to interpret Class.newInstance in a {@link JavaTypeContext} which represents the
 * point-type of the class object created by the call.
 */
public class ClassNewInstanceContextInterpreter extends AbstractReflectionInterpreter {

  public final static Atom newInstanceAtom = Atom.findOrCreateUnicodeAtom("newInstance");

  private final static Descriptor classNewInstanceDescriptor = Descriptor.findOrCreateUTF8("()Ljava/lang/Object;");

  public final static MethodReference CLASS_NEW_INSTANCE_REF = MethodReference.findOrCreate(TypeReference.JavaLangClass,
      newInstanceAtom, classNewInstanceDescriptor);

  private final static Atom defCtorAtom = Atom.findOrCreateUnicodeAtom("<init>");

  private final static Descriptor defCtorDescriptor = Descriptor.findOrCreateUTF8("()V");

  private final static Selector defCtorSelector = new Selector(defCtorAtom, defCtorDescriptor);

  private final IClassHierarchy cha;

/** BEGIN Custom change: caching */
  private final Map<String, IR> cache = HashMapFactory.make();
  
/** END Custom change: caching */
  public ClassNewInstanceContextInterpreter(IClassHierarchy cha) {
    this.cha = cha;
  }

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
      cache.put(hashKey, result);
    }
    
/** END Custom change: caching */
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
    if (!(node.getContext() instanceof JavaTypeContext)) {
      return false;
    }
    return node.getMethod().getReference().equals(CLASS_NEW_INSTANCE_REF);
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

  private IR makeIR(IMethod method, JavaTypeContext context) {
    SSAInstructionFactory insts = context.getType().getType().getClassLoader().getInstructionFactory();
    TypeReference tr = context.getType().getTypeReference();
    if (tr != null) {
      SpecializedMethod m = new SpecializedMethod(method, method.getDeclaringClass(), method.isStatic(), false);
      IClass klass = cha.lookupClass(tr);
      IMethod publicDefaultCtor = getPublicDefaultCtor(klass);
      if (publicDefaultCtor != null) {
        m.addStatementsForConcreteSimpleType(tr);
      } else if (klass.getMethod(defCtorSelector) == null) {
        TypeReference instantiationExceptionRef = TypeReference.findOrCreateClass(ClassLoaderReference.Primordial, "java/lang",
            "InstantiationException");
        int xobj = method.getNumberOfParameters() + 1;
        SSAInstruction newStatement = insts.NewInstruction(m.allInstructions.size(), xobj, NewSiteReference.make(2, instantiationExceptionRef));
        m.addInstruction(tr, newStatement, true);
        SSAInstruction throwStatement = insts.ThrowInstruction(m.allInstructions.size(), xobj);
        m.addInstruction(tr, throwStatement, false);
      } else {
        TypeReference illegalAccessExceptionRef = TypeReference.findOrCreateClass(ClassLoaderReference.Primordial, "java/lang",
            "IllegalAccessException");
        int xobj = method.getNumberOfParameters() + 1;
        SSAInstruction newStatement = insts.NewInstruction(m.allInstructions.size(), xobj, NewSiteReference.make(2, illegalAccessExceptionRef));
        m.addInstruction(tr, newStatement, true);
        SSAInstruction throwStatement = insts.ThrowInstruction(m.allInstructions.size(), xobj);
        m.addInstruction(tr, throwStatement, false);
      }

      SSAInstruction[] instrs = new SSAInstruction[m.allInstructions.size()];
      m.allInstructions.<SSAInstruction> toArray(instrs);
      return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(), null);
    }

    return null;
  }

  private static IMethod getPublicDefaultCtor(IClass klass) {
    IMethod ctorMethod = klass.getMethod(defCtorSelector);
    if (ctorMethod != null && ctorMethod.isPublic() && ctorMethod.getDeclaringClass() == klass) {
      return ctorMethod;
    }
    return null;
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
