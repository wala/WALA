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

import static com.ibm.wala.types.TypeName.ArrayMask;
import static com.ibm.wala.types.TypeName.ElementMask;
import static com.ibm.wala.types.TypeName.PrimitiveMask;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.Warning;

/**
 * An abstract superclass of various {@link SSAContextInterpreter}s that deal with reflection methods.
 */
public abstract class AbstractReflectionInterpreter implements SSAContextInterpreter {

  protected static final boolean DEBUG = false;

  protected final static int CONE_BOUND = 10;

  protected int indexLocal = 100;

  protected final Map<TypeReference, Integer> typeIndexMap = HashMapFactory.make();

  /**
   * Governing analysis options
   */
  protected AnalysisOptions options;

  /**
   * cache of analysis information
   */
  protected IAnalysisCacheView cache;


  protected int getLocalForType(TypeReference T) {
    Integer I = typeIndexMap.get(T);
    if (I == null) {
      typeIndexMap.put(T, I = new Integer(indexLocal += 2));
    }
    return I.intValue();
  }

  protected int getExceptionsForType(TypeReference T) {
    return getLocalForType(T) + 1;
  }

  protected int getCallSiteForType(TypeReference T) {
    return getLocalForType(T);
  }

  protected int getNewSiteForType(TypeReference T) {
    return getLocalForType(T) + 1;
  }

  /**
   * @param type
   * @return a TypeAbstraction object representing this type. We just use ConeTypes by default, since we don't propagate
   *         information allowing us to distinguish between points and cones yet.
   */
  protected TypeAbstraction typeRef2TypeAbstraction(IClassHierarchy cha, TypeReference type) {
    IClass klass = cha.lookupClass(type);
    if (klass != null) {
      return new ConeType(klass);
    }
    Assertions.UNREACHABLE(type.toString());
    return null;
  }

  /**
   * A warning when we expect excessive pollution from a factory method
   */
  protected static class ManySubtypesWarning extends Warning {

    final int nImplementors;

    final TypeAbstraction T;

    ManySubtypesWarning(TypeAbstraction T, int nImplementors) {
      super(Warning.MODERATE);
      this.T = T;
      this.nImplementors = nImplementors;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + T + " " + nImplementors;
    }

    public static ManySubtypesWarning create(TypeAbstraction T, int n) {
      return new ManySubtypesWarning(T, n);
    }
  }

  /**
   * A warning when we fail to find subtypes for a factory method
   */
  protected static class NoSubtypesWarning extends Warning {

    final TypeAbstraction T;

    NoSubtypesWarning(TypeAbstraction T) {
      super(Warning.SEVERE);
      this.T = T;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + T;
    }

    public static NoSubtypesWarning create(TypeAbstraction T) {
      return new NoSubtypesWarning(T);
    }
  }

  /**
   * A warning when we find flow of a factory allocation to a cast to {@link Serializable}
   */
  protected static class IgnoreSerializableWarning extends Warning {

    final private static IgnoreSerializableWarning instance = new IgnoreSerializableWarning();

    @Override
    public String getMsg() {
      return getClass().toString();
    }

    public static IgnoreSerializableWarning create() {
      return instance;
    }
  }

  protected class SpecializedMethod extends SyntheticMethod {

    /**
     * Set of types that we have already inserted an allocation for.
     */
    protected final HashSet<TypeReference> typesAllocated = HashSetFactory.make(5);

    /**
     * List of synthetic allocation statements we model for this specialized instance
     */
    final protected ArrayList<SSAInstruction> allocations = new ArrayList<>();

    /**
     * List of synthetic invoke instructions we model for this specialized instance.
     */
    final protected ArrayList<SSAInstruction> calls = new ArrayList<>();

    /**
     * List of all instructions
     */
    protected final ArrayList<SSAInstruction> allInstructions = new ArrayList<>();

    private final SSAInstructionFactory insts = declaringClass.getClassLoader().getInstructionFactory();
    
    public SpecializedMethod(MethodReference method, IClass declaringClass, boolean isStatic, boolean isFactory) {
      super(method, declaringClass, isStatic, isFactory);
    }

    public SpecializedMethod(IMethod method, IClass declaringClass, boolean isStatic, boolean isFactory) {
      super(method, declaringClass, isStatic, isFactory);
    }

    /**
     * @param T type allocated by the instruction.   
     */
    protected void addInstruction(final TypeReference T, SSAInstruction instr, boolean isAllocation) {
      if (isAllocation) {
        if (typesAllocated.contains(T)) {
          return;
        } else {
          typesAllocated.add(T);
        }
      }

      allInstructions.add(instr);
      if (isAllocation) {
        allocations.add(instr);
      }
    }

    /**
     * @param t type of object to allocate
     * @return value number of the newly allocated object
     */
    protected int addStatementsForConcreteSimpleType(final TypeReference t) {
      // assert we haven't allocated this type already.
      assert !typesAllocated.contains(t);
      if (DEBUG) {
        System.err.println(("addStatementsForConcreteType: " + t));
      }
      NewSiteReference ref = NewSiteReference.make(getNewSiteForType(t), t);
      int alloc = getLocalForType(t);

      if (t.isArrayType()) {
        // for now, just allocate an array of size 1 in each dimension.
        int dims = 0;
        int dim = t.getDerivedMask();
        if ((dim&ElementMask) == PrimitiveMask) {
          dim >>= 2;
        }
        while ((dim&ElementMask) == ArrayMask) {
          dims++;
          dim >>=2;
        }
        
        int[] extents = new int[dims];
        Arrays.fill(extents, 1);
        SSANewInstruction a = insts.NewInstruction(allInstructions.size(), alloc, ref, extents);
        addInstruction(t, a, true);
      } else {
        SSANewInstruction a = insts.NewInstruction(allInstructions.size(), alloc, ref);
        addInstruction(t, a, true);
        addCtorInvokeInstruction(t, alloc);
      }

      SSAReturnInstruction r = insts.ReturnInstruction(allInstructions.size(), alloc, false);
      addInstruction(t, r, false);
      return alloc;
    }

    /**
     * Add an instruction to invoke the default constructor on the object of value number alloc of type t.
     */
    protected void addCtorInvokeInstruction(final TypeReference t, int alloc) {
      MethodReference init = MethodReference.findOrCreate(t, MethodReference.initAtom, MethodReference.defaultInitDesc);
      CallSiteReference site = CallSiteReference.make(getCallSiteForType(t), init, IInvokeInstruction.Dispatch.SPECIAL);
      int[] params = new int[1];
      params[0] = alloc;
      int exc = getExceptionsForType(t);
      SSAInvokeInstruction s = insts.InvokeInstruction(allInstructions.size(), params, exc, site, null);
      calls.add(s);
      allInstructions.add(s);
    }
  }
}
