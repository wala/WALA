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
import com.ibm.wala.ssa.SSAGetInstruction;
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
 * 
 * @author sfink
 */
public abstract class AbstractRootMethod extends SyntheticMethod {

  final protected ArrayList<SSAInstruction> statements = new ArrayList<SSAInstruction>();

  private int valueNumberForConstantOne = -1;

  /**
   * The number of the next local value number available for the fake root
   * method. Note that we reserve value number 1 to represent the value "any
   * exception caught by the root method"
   */
  protected int nextLocal = 2;

  protected final IClassHierarchy cha;

  private final AnalysisOptions options;

  protected final AnalysisCache cache;

  public AbstractRootMethod(MethodReference method, IClass declaringClass, final IClassHierarchy cha, AnalysisOptions options,
      AnalysisCache cache) {
    super(method, declaringClass, true, false);
    this.cha = cha;
    this.options = options;
    this.cache = cache;
    assert cache != null;
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
    if (valueNumberForConstantOne > -1) {
      constants = HashMapFactory.make(1);
      constants.put(new Integer(valueNumberForConstantOne), new ConstantValue(new Integer(1)));
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
      s = new SSAInvokeInstruction(params, nextLocal++, newSite);
    } else {
      s = new SSAInvokeInstruction(nextLocal++, params, nextLocal++, newSite);
    }
    statements.add(s);
    cache.invalidate(this, Everywhere.EVERYWHERE);
    return s;
  }

  /**
   * Add a return statement
   */
  public SSAReturnInstruction addReturn(int vn, boolean isPrimitive) {
    SSAReturnInstruction s = new SSAReturnInstruction(vn, isPrimitive);
    statements.add(s);
    cache.invalidate(this, Everywhere.EVERYWHERE);
    return s;
  }

  /**
   * Add a New statement of the given type to the fake root node
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
   * Add a New statement of the given type to the fake root node
   */
  public SSANewInstruction addAllocationWithoutCtor(TypeReference T) {
    return addAllocation(T, false);
  }

  /**
   * Add a New statement of the given type to the fake root node
   * 
   * @param T
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
      NewSiteReference ref = NewSiteReference.make(statements.size(), T);
      if (T.isArrayType()) {
        initValueNumberForConstantOne();
        int[] sizes = new int[T.getDimensionality()];
        Arrays.fill(sizes, valueNumberForConstantOne);
        result = new SSANewInstruction(instance, ref, sizes);
      } else {
        result = new SSANewInstruction(instance, ref);
      }
      statements.add(result);

      IClass klass = cha.lookupClass(T);
      if (klass == null) {
        Warnings.add(AllocationFailure.create(T));
        return null;
      }

      if (klass.isArrayClass()) {
        int arrayRef = result.getDef();
        TypeReference e = klass.getReference().getArrayElementType();
        while (e != null && !e.isPrimitiveType()) {
          // allocate an instance for the array contents
          NewSiteReference n = NewSiteReference.make(statements.size(), e);
          int alloc = nextLocal++;
          SSANewInstruction ni = null;
          if (e.isArrayType()) {
            initValueNumberForConstantOne();
            int[] sizes = new int[T.getDimensionality()];
            Arrays.fill(sizes, valueNumberForConstantOne);
            ni = new SSANewInstruction(alloc, n, sizes);
          } else {
            ni = new SSANewInstruction(alloc, n);
          }
          statements.add(ni);

          // emit an astore
          SSAArrayStoreInstruction store = new SSAArrayStoreInstruction(arrayRef, 0, alloc, e);
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

  private void initValueNumberForConstantOne() {
    if (valueNumberForConstantOne == -1) {
      valueNumberForConstantOne = nextLocal++;
    }
  }

  /**
   * @author sfink
   * 
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
    SSAPhiInstruction phi = new SSAPhiInstruction(result, values);
    statements.add(phi);
    return result;
  }

  public int addGetInstance(FieldReference ref, int object) {
    int result = nextLocal++;
    statements.add(new SSAGetInstruction(result, object, ref));
    return result;
  }

  public int addGetStatic(FieldReference ref) {
    int result = nextLocal++;
    statements.add(new SSAGetInstruction(result, ref));
    return result;
  }

  public int addCheckcast(TypeReference type, int rv) {
    int lv = nextLocal++;
    statements.add(SSAInstructionFactory.CheckCastInstruction(lv, rv, type));
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
