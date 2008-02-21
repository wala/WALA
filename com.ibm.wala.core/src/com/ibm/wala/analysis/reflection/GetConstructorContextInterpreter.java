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
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * An {@link SSAContextInterpreter} specialized to interpret Class.forName in a {@link JavaTypeContext} which
 * represents the point-type of the class object created by the call.
 * 
 * @author pistoia
 */
public class GetConstructorContextInterpreter implements SSAContextInterpreter {

  public final static Atom getConstructorAtom = Atom.findOrCreateUnicodeAtom("getConstructor");

  private final static Descriptor getConstructorDescriptor = Descriptor.findOrCreateUTF8("([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;");

  public final static MethodReference GET_CONSTRUCTOR_REF = MethodReference.findOrCreate(TypeReference.JavaLangClass, getConstructorAtom,
      getConstructorDescriptor);

  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    IR result = makeIR(node.getMethod(), (JavaTypeContext) node.getContext());
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
    if (!(node.getContext() instanceof JavaTypeContext))
      return false;
    return node.getMethod().getReference().equals(GET_CONSTRUCTOR_REF);
  }

  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    JavaTypeContext context = (JavaTypeContext) node.getContext();
    TypeReference tr = context.getType().getTypeReference();
    if (tr != null) {
      return new NonNullSingletonIterator<NewSiteReference>(NewSiteReference.make(0, tr));
    }
    return EmptyIterator.instance();
  }

  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(understands(node));
    }
    return EmptyIterator.instance();
  }

  private SSAInstruction[] makeStatements(JavaTypeContext context, Map<Integer, ConstantValue> constants) {
    ArrayList<SSAInstruction> statements = new ArrayList<SSAInstruction>();
    int nextLocal = 2;
    int retValue = nextLocal++;
    IClass cls = context.getType().getType();
    if (cls != null) {
      for(Iterator methods = cls.getDeclaredMethods().iterator(); methods.hasNext(); ) {
        IMethod m = (IMethod) methods.next();
        if (m.isInit()) {
          int c = nextLocal++;
          constants.put(c, new ConstantValue(m));
          SSAReturnInstruction R = new SSAReturnInstruction(c, false);
          statements.add(R);
          retValue++;
        }
      }
    } else {
      SSAThrowInstruction t = new SSAThrowInstruction(retValue);
      statements.add(t);
    }
    SSAInstruction[] result = new SSAInstruction[statements.size()];
    Iterator<SSAInstruction> it = statements.iterator();
    for (int i = 0; i < result.length; i++) {
      result[i] = it.next();
    }
    return result;
  }

  private IR makeIR(IMethod method, JavaTypeContext context) {
    Map<Integer,ConstantValue> constants = HashMapFactory.make();
    
    SSAInstruction instrs[] = makeStatements(context, constants);
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
