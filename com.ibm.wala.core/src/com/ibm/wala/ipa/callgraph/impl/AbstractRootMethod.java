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
package com.ibm.wala.ipa.callgraph.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.cfg.InducedCFG;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.propagation.rta.RTAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.SyntheticIR;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * A synthetic method from the {@link FakeRootClass}
 */
public abstract class AbstractRootMethod extends SyntheticMethod {

  final protected ArrayList<SSAInstruction> statements = new ArrayList<SSAInstruction>();

  private Map<ConstantValue, Integer> constant2ValueNumber = HashMapFactory.make();

  /**
   * The number of the next local value number available for the fake root method. Note that we reserve value number 1 to represent
   * the value "any exception caught by the root method"
   */
  protected int nextLocal = 2;

  protected final IClassHierarchy cha;

  private final AnalysisOptions options;

  protected final AnalysisCache cache;

  protected final SSAInstructionFactory insts;

  public AbstractRootMethod(MethodReference method, IClass declaringClass, final IClassHierarchy cha, AnalysisOptions options,
      AnalysisCache cache) {
    super(method, declaringClass, true, false);
    this.cha = cha;
    this.options = options;
    this.cache = cache;
    this.insts = declaringClass.getClassLoader().getInstructionFactory();
    if (cache == null) {
      throw new IllegalArgumentException("null cache");
    }
    // I'd like to enforce that declaringClass is a FakeRootClass ... but CASt would currently break.
    // so checking dynamically instead.
    if (declaringClass instanceof FakeRootClass) {
      ((FakeRootClass) declaringClass).addMethod(this);
    }
  }

  public AbstractRootMethod(MethodReference method, final IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
    this(method, new FakeRootClass(cha), cha, options, cache);
  }

  /*
   * @see com.ibm.wala.classLoader.IMethod#getStatements(com.ibm.wala.util.warnings.WarningSet)
   */
  @Override
  public SSAInstruction[] getStatements(SSAOptions options) {
    SSAInstruction[] result = new SSAInstruction[statements.size()];
    int i = 0;
    for (Iterator<SSAInstruction> it = statements.iterator(); it.hasNext();) {
      result[i++] = it.next();
    }

    return result;
  }

  @Override
  public IR makeIR(Context context, SSAOptions options) {
    SSAInstruction instrs[] = getStatements(options);
    Map<Integer, ConstantValue> constants = null;
    if (!constant2ValueNumber.isEmpty()) {
      constants = HashMapFactory.make(constant2ValueNumber.size());
      for (ConstantValue c : constant2ValueNumber.keySet()) {
        int vn = constant2ValueNumber.get(c);
        constants.put(vn, c);
      }
    }
    InducedCFG cfg = makeControlFlowGraph(instrs);
    return new SyntheticIR(this, Everywhere.EVERYWHERE, cfg, instrs, options, constants);
  }

  public int addLocal() {
    return nextLocal++;
  }

  /**
   * @return the invoke instructions added by this operation
   * @throws IllegalArgumentException if site is null
   */
  public SSAInvokeInstruction addInvocation(int[] params, CallSiteReference site) {
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    CallSiteReference newSite = CallSiteReference.make(statements.size(), site.getDeclaredTarget(), site.getInvocationCode());
    SSAInvokeInstruction s = null;
    if (newSite.getDeclaredTarget().getReturnType().equals(TypeReference.Void)) {
      s = insts.InvokeInstruction(params, nextLocal++, newSite);
    } else {
      s = insts.InvokeInstruction(nextLocal++, params, nextLocal++, newSite);
    }
    statements.add(s);
    cache.invalidate(this, Everywhere.EVERYWHERE);
    return s;
  }

  /**
   * Add a return statement
   */
  public SSAReturnInstruction addReturn(int vn, boolean isPrimitive) {
    SSAReturnInstruction s = insts.ReturnInstruction(vn, isPrimitive);
    statements.add(s);
    cache.invalidate(this, Everywhere.EVERYWHERE);
    return s;
  }

  /**
   * Add a New statement of the given type
   * 
   * Side effect: adds call to default constructor of given type if one exists.
   * 
   * @return instruction added, or null
   * @throws IllegalArgumentException if T is null
   */
  public SSANewInstruction addAllocation(TypeReference T) {
    return addAllocation(T, true);
  }

  /**
   * Add a New statement of the given array type and length
   */
  public SSANewInstruction add1DArrayAllocation(TypeReference T, int length) {
    int instance = nextLocal++;
    NewSiteReference ref = NewSiteReference.make(statements.size(), T);
    assert T.isArrayType();
    assert ((ArrayClass)cha.lookupClass(T)).getDimensionality() == 1;
    int[] sizes = new int[1];
    Arrays.fill(sizes, getValueNumberForIntConstant(length));
    SSANewInstruction result = insts.NewInstruction(instance, ref, sizes);
    statements.add(result);
    cache.invalidate(this, Everywhere.EVERYWHERE);
    return result;
  }

  /**
   * Add a New statement of the given type
   */
  public SSANewInstruction addAllocationWithoutCtor(TypeReference T) {
    return addAllocation(T, false);
  }

  /**
   * Add a New statement of the given type
   * 
   * @return instruction added, or null
   * @throws IllegalArgumentException if T is null
   */
  private SSANewInstruction addAllocation(TypeReference T, boolean invokeCtor) {
    if (T == null) {
      throw new IllegalArgumentException("T is null");
    }
    int instance = nextLocal++;
    SSANewInstruction result = null;

    if (T.isReferenceType()) {
      IClass klass = cha.lookupClass(T);
      if (klass == null) {
        Warnings.add(AllocationFailure.create(T));
        return null;
      }

      NewSiteReference ref = NewSiteReference.make(statements.size(), T);
      if (T.isArrayType()) {
        int[] sizes = new int[((ArrayClass)cha.lookupClass(T)).getDimensionality()];
        Arrays.fill(sizes, getValueNumberForIntConstant(1));
        result = insts.NewInstruction(instance, ref, sizes);
      } else {
        result = insts.NewInstruction(instance, ref);
      }
      statements.add(result);

      if (klass.isArrayClass()) {
        int arrayRef = result.getDef();
        TypeReference e = klass.getReference().getArrayElementType();
        while (e != null && !e.isPrimitiveType()) {
          // allocate an instance for the array contents
          NewSiteReference n = NewSiteReference.make(statements.size(), e);
          int alloc = nextLocal++;
          SSANewInstruction ni = null;
          if (e.isArrayType()) {
            int[] sizes = new int[((ArrayClass)cha.lookupClass(T)).getDimensionality()];
            Arrays.fill(sizes, getValueNumberForIntConstant(1));
            ni = insts.NewInstruction(alloc, n, sizes);
          } else {
            ni = insts.NewInstruction(alloc, n);
          }
          statements.add(ni);

          // emit an astore
          SSAArrayStoreInstruction store = insts.ArrayStoreInstruction(arrayRef, getValueNumberForIntConstant(0), alloc, e);
          statements.add(store);

          e = e.isArrayType() ? e.getArrayElementType() : null;
          arrayRef = alloc;
        }
      }
      if (invokeCtor) {
        IMethod ctor = cha.resolveMethod(klass, MethodReference.initSelector);
        if (ctor != null) {
          addInvocation(new int[] { instance }, CallSiteReference.make(statements.size(), ctor.getReference(),
              IInvokeInstruction.Dispatch.SPECIAL));
        }
      }
    }
    cache.invalidate(this, Everywhere.EVERYWHERE);
    return result;
  }

  protected int getValueNumberForIntConstant(int c) {
    ConstantValue v = new ConstantValue(c);
    Integer result = constant2ValueNumber.get(v);
    if (result == null) {
      result = nextLocal++;
      constant2ValueNumber.put(v, result);
    }
    return result;
  }

  protected int getValueNumberForByteConstant(byte c) {
    // treat it like an int constant for now.
    ConstantValue v = new ConstantValue(c);
    Integer result = constant2ValueNumber.get(v);
    if (result == null) {
      result = nextLocal++;
      constant2ValueNumber.put(v, result);
    }
    return result;
  }

  protected int getValueNumberForCharConstant(char c) {
    // treat it like an int constant for now.
    ConstantValue v = new ConstantValue(c);
    Integer result = constant2ValueNumber.get(v);
    if (result == null) {
      result = nextLocal++;
      constant2ValueNumber.put(v, result);
    }
    return result;
  }

  /**
   * A warning for when we fail to allocate a type in the fake root method
   */
  private static class AllocationFailure extends Warning {

    final TypeReference t;

    AllocationFailure(TypeReference t) {
      super(Warning.SEVERE);
      this.t = t;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + t;
    }

    public static AllocationFailure create(TypeReference t) {
      return new AllocationFailure(t);
    }
  }

  public int addPhi(int[] values) {
    int result = nextLocal++;
    SSAPhiInstruction phi = insts.PhiInstruction(result, values);
    statements.add(phi);
    return result;
  }

  public int addGetInstance(FieldReference ref, int object) {
    int result = nextLocal++;
    statements.add(insts.GetInstruction(result, object, ref));
    return result;
  }

  public int addGetStatic(FieldReference ref) {
    int result = nextLocal++;
    statements.add(insts.GetInstruction(result, ref));
    return result;
  }

  public int addCheckcast(TypeReference[] types, int rv, boolean isPEI) {
    int lv = nextLocal++;

    statements.add(insts.CheckCastInstruction(lv, rv, types, isPEI));
    return lv;
  }

  public RTAContextInterpreter getInterpreter() {
    return new RTAContextInterpreter() {

      public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
        ArrayList<NewSiteReference> result = new ArrayList<NewSiteReference>();
        SSAInstruction[] statements = getStatements(options.getSSAOptions());
        for (int i = 0; i < statements.length; i++) {
          if (statements[i] instanceof SSANewInstruction) {
            SSANewInstruction s = (SSANewInstruction) statements[i];
            result.add(s.getNewSite());
          }
        }
        return result.iterator();
      }

      public Iterator<SSAInstruction> getInvokeStatements() {
        ArrayList<SSAInstruction> result = new ArrayList<SSAInstruction>();
        SSAInstruction[] statements = getStatements(options.getSSAOptions());
        for (int i = 0; i < statements.length; i++) {
          if (statements[i] instanceof SSAInvokeInstruction) {
            result.add(statements[i]);
          }
        }
        return result.iterator();
      }

      public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
        final Iterator<SSAInstruction> I = getInvokeStatements();
        return new Iterator<CallSiteReference>() {
          public boolean hasNext() {
            return I.hasNext();
          }

          public CallSiteReference next() {
            SSAInvokeInstruction s = (SSAInvokeInstruction) I.next();
            return s.getCallSite();
          }

          public void remove() {
            Assertions.UNREACHABLE();
          }
        };
      }

      public boolean understands(CGNode node) {
        return node.getMethod().getDeclaringClass().getReference().equals(FakeRootClass.FAKE_ROOT_CLASS);
      }

      public boolean recordFactoryType(CGNode node, IClass klass) {
        // not a factory type
        return false;
      }

      public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
        return EmptyIterator.instance();
      }

      public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
        return EmptyIterator.instance();
      }
    };
  }
}
