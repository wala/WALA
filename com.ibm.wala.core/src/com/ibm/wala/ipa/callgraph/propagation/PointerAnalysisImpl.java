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
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * General representation of the results of pointer analysis
 */
public class PointerAnalysisImpl extends AbstractPointerAnalysis {

  /**
   * mapping from PointerKey to PointsToSetVariable
   */
  private final PointsToMap pointsToMap;

  /**
   * Meta-data regarding heap abstractions
   */
  private final HeapModel H;

  /**
   * An object that abstracts how to model pointers in the heap.
   */
  protected final PointerKeyFactory pointerKeys;

  /**
   * An object that abstracts how to model instances in the heap.
   */
  private final InstanceKeyFactory iKeyFactory;

  protected final PropagationCallGraphBuilder builder;

  public PointerAnalysisImpl(PropagationCallGraphBuilder builder, CallGraph cg, PointsToMap pointsToMap,
      MutableMapping<InstanceKey> instanceKeys, PointerKeyFactory pointerKeys, InstanceKeyFactory iKeyFactory) {
    super(cg, instanceKeys);
    this.builder = builder;
    this.pointerKeys = pointerKeys;
    this.iKeyFactory = iKeyFactory;
    this.pointsToMap = pointsToMap;
    if (iKeyFactory == null) {
      throw new IllegalArgumentException("null iKeyFactory");
    }
    H = makeHeapModel();
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer("PointerAnalysis:\n");
    for (PointerKey p : Iterator2Iterable.make(pointsToMap.iterateKeys())) {
      OrdinalSet<InstanceKey> O = getPointsToSet(p);
      result.append("  ").append(p).append(" ->\n");
      for (InstanceKey instanceKey : O) {
        result.append("     ").append(instanceKey).append("\n");
      }
    }
    return result.toString();
  }

  protected HeapModel makeHeapModel() {
    return new HModel();
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis#getPointsToSet(com.ibm.wala.ipa.callgraph.propagation.PointerKey)
   */
  @Override
  @SuppressWarnings("unchecked")
  public OrdinalSet<InstanceKey> getPointsToSet(PointerKey key) {
    if (pointsToMap.isImplicit(key)) {
      return computeImplicitPointsToSet(key);
    }

    // special logic to handle contents of char[] from string constants.
    if (key instanceof InstanceFieldKey) {
      InstanceFieldKey ifk = (InstanceFieldKey) key;
      if (ifk.getInstanceKey() instanceof ConstantKey) {
        ConstantKey<?> i = (ConstantKey<?>) ifk.getInstanceKey();
        if (i.getValue() instanceof String && i.getConcreteType().getClassLoader().getLanguage().equals(Language.JAVA)) {
          StringConstantCharArray contents = StringConstantCharArray.make((ConstantKey<String>) i);
          instanceKeys.add(contents);
          Collection<InstanceKey> singleton = HashSetFactory.make();
          singleton.add(contents);
          return OrdinalSet.toOrdinalSet(singleton, instanceKeys);
        }
      }
    }

    PointsToSetVariable v = pointsToMap.getPointsToSet(key);

    if (v == null) {
      return OrdinalSet.empty();
    } else {
      IntSet S = v.getValue();
      return new OrdinalSet<>(S, instanceKeys);
    }
  }

  /**
   * did the pointer analysis use a type filter for a given points-to set? (this is ugly).
   */
  @Override
  public boolean isFiltered(PointerKey key) {
    if (pointsToMap.isImplicit(key)) {
      return false;
    }
    PointsToSetVariable v = pointsToMap.getPointsToSet(key);
    if (v == null) {
      return false;
    } else {
      return v.getPointerKey() instanceof FilteredPointerKey;
    }
  }

  public static class ImplicitPointsToSetVisitor extends SSAInstruction.Visitor {
    protected final PointerAnalysisImpl analysis;

    protected final CGNode node;

    protected final LocalPointerKey lpk;

    protected OrdinalSet<InstanceKey> pointsToSet = null;

    protected ImplicitPointsToSetVisitor(PointerAnalysisImpl analysis, LocalPointerKey lpk) {
      this.lpk = lpk;
      this.node = lpk.getNode();
      this.analysis = analysis;
    }

    @Override
    public void visitNew(SSANewInstruction instruction) {
      pointsToSet = OrdinalSet.empty();
    }

    @Override
    public void visitInvoke(SSAInvokeInstruction instruction) {
      pointsToSet = analysis.computeImplicitPointsToSetAtCall(lpk, node, instruction);
    }

    @Override
    public void visitCheckCast(SSACheckCastInstruction instruction) {
      pointsToSet = analysis.computeImplicitPointsToSetAtCheckCast(node, instruction);
    }

    @Override
    public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
      pointsToSet = analysis.computeImplicitPointsToSetAtCatch(node, instruction);
    }

    @Override
    public void visitGet(SSAGetInstruction instruction) {
      pointsToSet = analysis.computeImplicitPointsToSetAtGet(node, instruction);
    }

    @Override
    public void visitPhi(SSAPhiInstruction instruction) {
      pointsToSet = analysis.computeImplicitPointsToSetAtPhi(node, instruction);
    }

    @Override
    public void visitPi(SSAPiInstruction instruction) {
      pointsToSet = analysis.computeImplicitPointsToSetAtPi(node, instruction);
    }

    @Override
    public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
      pointsToSet = analysis.computeImplicitPointsToSetAtALoad(node, instruction);
    }
  }

  protected ImplicitPointsToSetVisitor makeImplicitPointsToVisitor(LocalPointerKey lpk) {
    return new ImplicitPointsToSetVisitor(this, lpk);
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSet(PointerKey key) {
    if (key instanceof LocalPointerKey) {
      LocalPointerKey lpk = (LocalPointerKey) key;
      CGNode node = lpk.getNode();
      IR ir = node.getIR();
      DefUse du = node.getDU();
      if (((SSAPropagationCallGraphBuilder) builder).contentsAreInvariant(ir.getSymbolTable(), du, lpk.getValueNumber())) {
        // cons up the points-to set for invariant contents
        InstanceKey[] ik = ((SSAPropagationCallGraphBuilder) builder).getInvariantContents(ir.getSymbolTable(), du, node, lpk
            .getValueNumber(), H, true);
        return toOrdinalSet(ik);
      } else {
        SSAInstruction def = du.getDef(lpk.getValueNumber());
        if (def != null) {
          ImplicitPointsToSetVisitor v = makeImplicitPointsToVisitor(lpk);
          def.visit(v);
          if (v.pointsToSet != null) {
            return v.pointsToSet;
          } else {
            Assertions.UNREACHABLE("saw " + key + ": time to implement for " + def.getClass());
            return null;
          }
        } else {
          Assertions.UNREACHABLE("unexpected null def for " + key);
          return null;
        }
      }
    } else {
      Assertions.UNREACHABLE("unexpected implicit key " + key + " that's not a local pointer key");
      return null;
    }
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSetAtPi(CGNode node, SSAPiInstruction instruction) {
    MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
    for (int i = 0; i < instruction.getNumberOfUses(); i++) {
      int vn = instruction.getUse(i);
      if (vn != -1) {
        PointerKey lpk = pointerKeys.getPointerKeyForLocal(node, vn);
        OrdinalSet pointees = getPointsToSet(lpk);
        IntSet set = pointees.getBackingSet();
        if (set != null) {
          S.addAll(set);
        }
      }
    }
    return new OrdinalSet<>(S, instanceKeys);
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSetAtPhi(CGNode node, SSAPhiInstruction instruction) {
    MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
    for (int i = 0; i < instruction.getNumberOfUses(); i++) {
      int vn = instruction.getUse(i);
      if (vn != -1) {
        PointerKey lpk = pointerKeys.getPointerKeyForLocal(node, vn);
        OrdinalSet pointees = getPointsToSet(lpk);
        IntSet set = pointees.getBackingSet();
        if (set != null) {
          S.addAll(set);
        }
      }
    }
    return new OrdinalSet<>(S, instanceKeys);
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSetAtALoad(CGNode node, SSAArrayLoadInstruction instruction) {
    PointerKey arrayRef = pointerKeys.getPointerKeyForLocal(node, instruction.getArrayRef());
    MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
    OrdinalSet<InstanceKey> refs = getPointsToSet(arrayRef);
    for (InstanceKey ik : refs) {
      PointerKey key = pointerKeys.getPointerKeyForArrayContents(ik);
      OrdinalSet pointees = getPointsToSet(key);
      IntSet set = pointees.getBackingSet();
      if (set != null) {
        S.addAll(set);
      }
    }
    return new OrdinalSet<>(S, instanceKeys);
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSetAtGet(CGNode node, SSAGetInstruction instruction) {
    return computeImplicitPointsToSetAtGet(node, instruction.getDeclaredField(), instruction.getRef(), instruction.isStatic());
  }

  public OrdinalSet<InstanceKey> computeImplicitPointsToSetAtGet(CGNode node, FieldReference field, int refVn, boolean isStatic) {
    IField f = getCallGraph().getClassHierarchy().resolveField(field);
    if (f == null) {
      return OrdinalSet.empty();
    }
    if (isStatic) {
      PointerKey fKey = pointerKeys.getPointerKeyForStaticField(f);
      return getPointsToSet(fKey);
    } else {
      PointerKey ref = pointerKeys.getPointerKeyForLocal(node, refVn);
      MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
      OrdinalSet<InstanceKey> refs = getPointsToSet(ref);
      for (InstanceKey ik : refs) {
        PointerKey fkey = pointerKeys.getPointerKeyForInstanceField(ik, f);
        if (fkey != null) {
          OrdinalSet pointees = getPointsToSet(fkey);
          IntSet set = pointees.getBackingSet();
          if (set != null) {
            S.addAll(set);
          }
        }
      }
      return new OrdinalSet<>(S, instanceKeys);
    }
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSetAtCatch(CGNode node, SSAGetCaughtExceptionInstruction instruction) {
    IR ir = node.getIR();
    List<ProgramCounter> peis = SSAPropagationCallGraphBuilder.getIncomingPEIs(ir, ir.getBasicBlockForCatch(instruction));
    Set<IClass> caughtTypes = SSAPropagationCallGraphBuilder.getCaughtExceptionTypes(instruction, ir);
    MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
    // add the instances from each incoming pei ...
    for (ProgramCounter peiLoc : peis) {
      SSAInstruction pei = ir.getPEI(peiLoc);
      PointerKey e = null;
      // first deal with exception variables from calls and throws.
      if (pei instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction s = (SSAAbstractInvokeInstruction) pei;
        e = pointerKeys.getPointerKeyForLocal(node, s.getException());
      } else if (pei instanceof SSAThrowInstruction) {
        SSAThrowInstruction s = (SSAThrowInstruction) pei;
        e = pointerKeys.getPointerKeyForLocal(node, s.getException());
      }
      if (e != null) {
        OrdinalSet<InstanceKey> ep = getPointsToSet(e);
        for (InstanceKey ik : ep) {
          if (PropagationCallGraphBuilder.catches(caughtTypes, ik.getConcreteType(), getCallGraph().getClassHierarchy())) {
            S.add(instanceKeys.getMappedIndex(ik));
          }
        }
      }

      // Account for those exceptions for which we do not actually have a
      // points-to set for
      // the pei, but just instance keys
      Collection<TypeReference> types = pei.getExceptionTypes();
      if (types != null) {
        for (TypeReference type : types) {
          if (type != null) {
            InstanceKey ik = SSAPropagationCallGraphBuilder.getInstanceKeyForPEI(node, peiLoc, type, iKeyFactory);
            ConcreteTypeKey ck = (ConcreteTypeKey) ik;
            IClass klass = ck.getType();
            if (PropagationCallGraphBuilder.catches(caughtTypes, klass, getCallGraph().getClassHierarchy())) {
              S.add(instanceKeys.getMappedIndex(SSAPropagationCallGraphBuilder
                  .getInstanceKeyForPEI(node, peiLoc, type, iKeyFactory)));
            }
          }
        }
      }
    }
    return new OrdinalSet<>(S, instanceKeys);
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSetAtCheckCast(CGNode node, SSACheckCastInstruction instruction) {
    PointerKey rhs = pointerKeys.getPointerKeyForLocal(node, instruction.getVal());
    OrdinalSet<InstanceKey> rhsSet = getPointsToSet(rhs);
    MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
    for (TypeReference t : instruction.getDeclaredResultTypes()) {
      IClass klass = getCallGraph().getClassHierarchy().lookupClass(t);
      if (klass == null) {
        // could not find the type. conservatively assume Object
        return rhsSet;
      } else {
        if (klass.isInterface()) {
          for (InstanceKey ik : rhsSet) {
            if (getCallGraph().getClassHierarchy().implementsInterface(ik.getConcreteType(), klass)) {
              S.add(getInstanceKeyMapping().getMappedIndex(ik));
            }
          }
        } else {
          for (InstanceKey ik : rhsSet) {
            if (getCallGraph().getClassHierarchy().isSubclassOf(ik.getConcreteType(), klass)) {
              S.add(getInstanceKeyMapping().getMappedIndex(ik));
            }
          }
        }
      }
    }
    return new OrdinalSet<>(S, instanceKeys);
  }

  private OrdinalSet<InstanceKey> computeImplicitPointsToSetAtCall(LocalPointerKey lpk, CGNode node, SSAInvokeInstruction call) {
    int exc = call.getException();
    if (lpk.getValueNumber() == exc) {
      return computeImplicitExceptionsForCall(node, call);
    } else {
      Assertions.UNREACHABLE("time to implement me.");
      return null;
    }
  }

  private OrdinalSet<InstanceKey> toOrdinalSet(InstanceKey[] ik) {
    MutableSparseIntSet s = MutableSparseIntSet.makeEmpty();
    for (InstanceKey element : ik) {
      int index = instanceKeys.getMappedIndex(element);
      if (index != -1) {
        s.add(index);
      } else {
        assert index != -1 : "instance " + element + " not mapped!";
      }
    }
    return new OrdinalSet<>(s, instanceKeys);
  }

  /**
   * @return the points-to set for the exceptional return values from a particular call site
   */
  private OrdinalSet<InstanceKey> computeImplicitExceptionsForCall(CGNode node, SSAInvokeInstruction call) {
    MutableSparseIntSet S = MutableSparseIntSet.makeEmpty();
    for (CGNode target : getCallGraph().getPossibleTargets(node, call.getCallSite())) {
      PointerKey retVal = pointerKeys.getPointerKeyForExceptionalReturnValue(target);
      IntSet set = getPointsToSet(retVal).getBackingSet();
      if (set != null) {
        S.addAll(set);
      }
    }
    return new OrdinalSet<>(S, instanceKeys);
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis#getHeapModel()
   */
  @Override
  public HeapModel getHeapModel() {
    return H;
  }

  protected class HModel implements HeapModel {

    @Override
    public Iterator<PointerKey> iteratePointerKeys() {
      return pointsToMap.iterateKeys();
    }

    @Override
    public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) {
      return iKeyFactory.getInstanceKeyForAllocation(node, allocation);
    }

    @Override
    public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) {
      return iKeyFactory.getInstanceKeyForMultiNewArray(node, allocation, dim);
    }

    @Override
    public <T> InstanceKey getInstanceKeyForConstant(TypeReference type, T S) {
      return iKeyFactory.getInstanceKeyForConstant(type, S);
    }

    @Override
    public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter peiLoc, TypeReference type) {
      return iKeyFactory.getInstanceKeyForPEI(node, peiLoc, type);
    }

    @Override
    public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) {
      return iKeyFactory.getInstanceKeyForMetadataObject(obj, objType);
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory#getPointerKeyForLocal(com.ibm.detox.ipa.callgraph.CGNode, int)
     */
    @Override
    public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
      return pointerKeys.getPointerKeyForLocal(node, valueNumber);
    }

    @Override
    public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter) {
      return pointerKeys.getFilteredPointerKeyForLocal(node, valueNumber, filter);
    }

    /*
     * @see com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory#getPointerKeyForReturnValue(com.ibm.detox.ipa.callgraph.CGNode)
     */
    @Override
    public PointerKey getPointerKeyForReturnValue(CGNode node) {
      return pointerKeys.getPointerKeyForReturnValue(node);
    }

    /*
     * @see
     * com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory#getPointerKeyForExceptionalReturnValue(com.ibm.detox.ipa.callgraph
     * .CGNode)
     */
    @Override
    public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
      return pointerKeys.getPointerKeyForExceptionalReturnValue(node);
    }

    /*
     * @see
     * com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory#getPointerKeyForStaticField(com.ibm.wala.classLoader.FieldReference)
     */
    @Override
    public PointerKey getPointerKeyForStaticField(IField f) {
      return pointerKeys.getPointerKeyForStaticField(f);
    }

    /*
     * @see
     * com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory#getPointerKeyForInstance(com.ibm.wala.ipa.callgraph.propagation.
     * InstanceKey, com.ibm.wala.classLoader.FieldReference)
     */
    @Override
    public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
      assert field != null;
      return pointerKeys.getPointerKeyForInstanceField(I, field);
    }

    /*
     * @see
     * com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory#getPointerKeyForArrayContents(com.ibm.wala.ipa.callgraph.propagation
     * .InstanceKey)
     */
    @Override
    public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
      return pointerKeys.getPointerKeyForArrayContents(I);
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return getCallGraph().getClassHierarchy();
    }
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis#iteratePointerKeys()
   */
  @Override
  public Iterable<PointerKey> getPointerKeys() {
    return Iterator2Iterable.make(pointsToMap.iterateKeys());
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return builder.getClassHierarchy();
  }
}
