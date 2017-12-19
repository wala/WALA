/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
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
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.CodeScanner;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextUtil;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.strings.Atom;

/**
 * A context interpreter for java.lang.Object.clone
 * 
 * TODO: The current implementation does not model CloneNotSupportedExceptions
 */
public class CloneInterpreter implements SSAContextInterpreter {

  /**
   * Comment for <code>cloneAtom</code>
   */
  public final static Atom cloneAtom = Atom.findOrCreateUnicodeAtom("clone");

  private final static Descriptor cloneDesc = Descriptor.findOrCreateUTF8("()Ljava/lang/Object;");

  /**
   * Comment for <code>CLONE</code>
   */
  public final static MethodReference CLONE = MethodReference.findOrCreate(TypeReference.JavaLangObject, cloneAtom, cloneDesc);

  private final static TypeReference SYNTHETIC_SYSTEM = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TypeName
      .string2TypeName("Lcom/ibm/wala/model/java/lang/System"));

  private final static Atom arraycopyAtom = Atom.findOrCreateUnicodeAtom("arraycopy");

  private final static Descriptor arraycopyDesc = Descriptor.findOrCreateUTF8("(Ljava/lang/Object;Ljava/lang/Object;)V");

  private final static MethodReference SYNTHETIC_ARRAYCOPY = MethodReference.findOrCreate(SYNTHETIC_SYSTEM, arraycopyAtom,
      arraycopyDesc);

  /**
   * If the type is an array, the program counter of the synthesized call to arraycopy. Doesn't really matter what it is.
   */
  private final static int ARRAYCOPY_PC = 3;

  private final static CallSiteReference ARRAYCOPY_SITE = CallSiteReference.make(ARRAYCOPY_PC, SYNTHETIC_ARRAYCOPY,
      IInvokeInstruction.Dispatch.STATIC);

  private final static int NEW_PC = 0;

  /**
   * Mapping from TypeReference -&gt; IR TODO: Soft references?
   */
  final private Map<TypeReference, IR> IRCache = HashMapFactory.make();

  private final SSAInstructionFactory insts = Language.JAVA.instructionFactory();

  @Override
  public IR getIR(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    IClass cls = ContextUtil.getConcreteClassFromContext(node.getContext());
    IR result = IRCache.get(cls.getReference());
    if (result == null) {
      result = makeIR(node.getMethod(), node.getContext(), cls);
      IRCache.put(cls.getReference(), result);
    }
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
    return (node.getMethod().getReference().equals(CLONE) && ContextUtil.getConcreteClassFromContext(node.getContext()) != null);
  }

  @Override
  public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
    if (node == null) {
      throw new IllegalArgumentException("node is null");
    }
    assert understands(node);
    IClass cls = ContextUtil.getConcreteClassFromContext(node.getContext());
    return new NonNullSingletonIterator<>(NewSiteReference.make(NEW_PC, cls.getReference()));
  }

  @Override
  public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
    assert understands(node);
    return new NonNullSingletonIterator<>(ARRAYCOPY_SITE);
  }

  /**
   * @return an array of statements that encode the behavior of the clone method for a given type.
   */
  private SSAInstruction[] makeStatements(IClass klass) {
    assert klass != null;

    ArrayList<SSAInstruction> statements = new ArrayList<>();
    // value number 1 is "this".
    int nextLocal = 2;

    int retValue = nextLocal++;
    // value number of the result of the clone()
    NewSiteReference ref = NewSiteReference.make(NEW_PC, klass.getReference());
    SSANewInstruction N = null;
    if (klass.isArrayClass()) {
      int length = nextLocal++;
      statements.add(insts.ArrayLengthInstruction(statements.size(), length, 1));
      int[] sizes = new int[((ArrayClass)klass).getDimensionality()];
      Arrays.fill(sizes, length);
      N = insts.NewInstruction(statements.size(), retValue, ref, sizes);
    } else {
      N = insts.NewInstruction(statements.size(), retValue, ref);
    }
    statements.add(N);

    int exceptionValue = nextLocal++;

    if (klass.getReference().isArrayType()) {
      // generate a synthetic arraycopy from this (v.n. 1) to the clone
      int[] params = new int[2];
      params[0] = 1;
      params[1] = retValue;
      SSAInvokeInstruction S = insts.InvokeInstruction(statements.size(), params, exceptionValue, ARRAYCOPY_SITE, null);
      statements.add(S);
    } else {
      // copy the fields over, one by one.
      // TODO:
      IClass k = klass;
      while (k != null) {
        for (IField f : klass.getDeclaredInstanceFields()) {
          int tempValue = nextLocal++;
          SSAGetInstruction G = insts.GetInstruction(statements.size(), tempValue, 1, f.getReference());
          statements.add(G);
          SSAPutInstruction P = insts.PutInstruction(statements.size(), retValue, tempValue, f.getReference());
          statements.add(P);
        }
        k = k.getSuperclass();
      }

    }

    SSAReturnInstruction R = insts.ReturnInstruction(statements.size(), retValue, false);
    statements.add(R);

    SSAInstruction[] result = new SSAInstruction[statements.size()];
    Iterator<SSAInstruction> it = statements.iterator();
    for (int i = 0; i < result.length; i++) {
      result[i] = it.next();
    }
    return result;
  }

  /**
   * @return an IR that encodes the behavior of the clone method for a given type.
   */
  private IR makeIR(IMethod method, Context context, IClass klass) {
    assert klass != null;
    SSAInstruction instrs[] = makeStatements(klass);
    return new SyntheticIR(method, context, new InducedCFG(instrs, method, context), instrs, SSAOptions.defaultOptions(), null);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.cfa.CFAContextInterpreter#recordFactoryType(com.ibm.wala.ipa.callgraph.CGNode,
   * com.ibm.wala.classLoader.IClass)
   */
  @Override
  public boolean recordFactoryType(CGNode node, IClass klass) {
    return false;
  }

  @Override
  public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
    SSAInstruction[] statements = getIR(node).getInstructions();
    return CodeScanner.getFieldsRead(statements).iterator();
  }

  @Override
  public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
    SSAInstruction[] statements = getIR(node).getInstructions();
    return CodeScanner.getFieldsWritten(statements).iterator();
  }

  public Set getCaughtExceptions(CGNode node) {
    SSAInstruction[] statements = getIR(node).getInstructions();
    return CodeScanner.getCaughtExceptions(statements);
  }

  public boolean hasObjectArrayLoad(CGNode node) {
    SSAInstruction[] statements = getIR(node).getInstructions();
    return CodeScanner.hasObjectArrayLoad(statements);
  }

  public boolean hasObjectArrayStore(CGNode node) {
    SSAInstruction[] statements = getIR(node).getInstructions();
    return CodeScanner.hasObjectArrayStore(statements);
  }

  public Iterator<TypeReference> iterateCastTypes(CGNode node) {
    SSAInstruction[] statements = getIR(node).getInstructions();
    return CodeScanner.iterateCastTypes(statements);
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
