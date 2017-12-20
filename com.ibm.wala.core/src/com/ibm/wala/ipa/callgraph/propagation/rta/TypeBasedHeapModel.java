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
package com.ibm.wala.ipa.callgraph.propagation.rta;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.ClassBasedInstanceKeys;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DefaultPointerKeyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * A trivial field-based heap model, which only uses the information of which types (classes) are live.
 * 
 * Note that this heap model is based on ssa value numbers for locals, since we will build a pointer flow graph based on this heap
 * model when resolving reflection.
 * 
 * This is an inefficient prototype.
 */
public class TypeBasedHeapModel implements HeapModel {

  private final static boolean DEBUG = false;

  final DefaultPointerKeyFactory pointerKeys = new DefaultPointerKeyFactory();

  private final ClassBasedInstanceKeys iKeyFactory;

  private final Collection<IClass> klasses;

  private final CallGraph cg;

  private final Collection<CGNode> nodesHandled = HashSetFactory.make();

  /**
   * Map: &lt;PointerKey&gt; -&gt; thing, where thing is a FilteredPointerKey or an InstanceKey representing a constant.
   * 
   * computed lazily
   */
  private Map<PointerKey, Object> pKeys;

  /**
   * @param klasses Collection&lt;IClass&gt;
   * @throws IllegalArgumentException if cg is null
   */
  public TypeBasedHeapModel(AnalysisOptions options, Collection<IClass> klasses, CallGraph cg) {
    if (cg == null) {
      throw new IllegalArgumentException("cg is null");
    }
    iKeyFactory = new ClassBasedInstanceKeys(options, cg.getClassHierarchy());
    this.klasses = klasses;
    this.cg = cg;
  }

  private void initAllPKeys() {
    if (pKeys == null) {
      pKeys = HashMapFactory.make();
    }
    for (IClass klass : klasses) {
      pKeys.putAll(computePointerKeys(klass));
    }
    for (CGNode node : cg) {
      initPKeysForNode(node);
    }
  }

  private void initPKeysForNode(CGNode node) {
    if (pKeys == null) {
      pKeys = HashMapFactory.make();
    }
    if (!nodesHandled.contains(node)) {
      nodesHandled.add(node);
      pKeys.putAll(computePointerKeys(node));
    }
  }

  private Map<PointerKey, Object> computePointerKeys(CGNode node) {

    if (DEBUG) {
      System.err.println("computePointerKeys " + node);
    }

    IR ir = node.getIR();
    if (ir == null) {
      return Collections.emptyMap();
    }
    Map<PointerKey, Object> result = HashMapFactory.make();
    SymbolTable s = ir.getSymbolTable();
    if (s == null) {
      return Collections.emptyMap();
    }
    TypeInference ti = TypeInference.make(ir, false);

    for (int i = 1; i <= s.getMaxValueNumber(); i++) {
      if (DEBUG) {
        System.err.print(i);
      }
      if (s.isConstant(i)) {
        if (s.isStringConstant(i)) {
          TypeReference type = node.getMethod().getDeclaringClass().getClassLoader().getLanguage().getConstantType(
              s.getStringValue(i));
          result.put(pointerKeys.getPointerKeyForLocal(node, i), getInstanceKeyForConstant(type, s.getConstantValue(i)));
        }
      } else {
        TypeAbstraction t = ti.getType(i);
        if (DEBUG) {
          System.err.println(" type " + t);
        }
        if (t.getType() != null && t.getType().isReferenceType()) {
          result.put(pointerKeys.getPointerKeyForLocal(node, i), pointerKeys.getFilteredPointerKeyForLocal(node, i,
              new FilteredPointerKey.SingleClassFilter(t.getType())));
        }
      }
    }
    return result;
  }

  private Map<PointerKey, Object> computePointerKeys(IClass klass) {
    Map<PointerKey, Object> result = HashMapFactory.make();
    if (klass.isArrayClass()) {
      ArrayClass a = (ArrayClass) klass;
      if (a.getElementClass() != null && a.getElementClass().isReferenceType()) {
        PointerKey p = pointerKeys.getPointerKeyForArrayContents(new ConcreteTypeKey(a));
        result.put(p, p);
      }
    } else {
      for (IField f : klass.getAllFields()) {
        if (!f.getFieldTypeReference().isPrimitiveType()) {
          if (f.isStatic()) {
            PointerKey p = pointerKeys.getPointerKeyForStaticField(f);
            result.put(p, p);
          } else {
            PointerKey p = pointerKeys.getPointerKeyForInstanceField(new ConcreteTypeKey(klass), f);
            result.put(p, p);
          }
        }
      }
    }
    return result;
  }

  @Override
  public Iterator<PointerKey> iteratePointerKeys() {
    initAllPKeys();
    return IteratorUtil.filter(pKeys.values().iterator(), PointerKey.class);
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return iKeyFactory.getClassHierarchy();
  }

  @Override
  public InstanceKey getInstanceKeyForAllocation(CGNode node, NewSiteReference allocation) throws UnimplementedError {
    return iKeyFactory.getInstanceKeyForAllocation(node, allocation);
  }

  @Override
  public InstanceKey getInstanceKeyForMultiNewArray(CGNode node, NewSiteReference allocation, int dim) throws UnimplementedError {
    return iKeyFactory.getInstanceKeyForMultiNewArray(node, allocation, dim);
  }

  @Override
  public InstanceKey getInstanceKeyForConstant(TypeReference type, Object S) {
    return iKeyFactory.getInstanceKeyForConstant(type, S);
  }

  public String getStringConstantForInstanceKey() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public InstanceKey getInstanceKeyForPEI(CGNode node, ProgramCounter instr, TypeReference type) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public InstanceKey getInstanceKeyForMetadataObject(Object obj, TypeReference objType) throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /**
   * Note that this always returns a {@link FilteredPointerKey}, since the {@link TypeBasedPointerAnalysis} relies on the type
   * filter to compute points to sets.
   * 
   * @see com.ibm.wala.ipa.callgraph.propagation.PointerKeyFactory#getPointerKeyForLocal(com.ibm.wala.ipa.callgraph.CGNode, int)
   */
  @Override
  public FilteredPointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    initPKeysForNode(node);
    PointerKey p = pointerKeys.getPointerKeyForLocal(node, valueNumber);
    Object result = pKeys.get(p);
    if (result == null) {
      // a null constant
      return null;
    }
    if (result instanceof FilteredPointerKey) {
      return (FilteredPointerKey) result;
    } else {
      if (result instanceof ConcreteTypeKey) {
        ConcreteTypeKey c = (ConcreteTypeKey) result;
        if (c.getConcreteType().getReference().equals(TypeReference.JavaLangString)) {
          // a string constant;
          return pointerKeys.getFilteredPointerKeyForLocal(node, valueNumber, new FilteredPointerKey.SingleClassFilter(c
              .getConcreteType()));
        } else {
          Assertions.UNREACHABLE("need to handle " + result.getClass());
          return null;
        }
      } else {
        Assertions.UNREACHABLE("need to handle " + result.getClass());
        return null;
      }
    }
  }

  @Override
  public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, FilteredPointerKey.TypeFilter filter)
      throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public PointerKey getPointerKeyForReturnValue(CGNode node) {
    return pointerKeys.getPointerKeyForReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return pointerKeys.getPointerKeyForExceptionalReturnValue(node);
  }

  @Override
  public PointerKey getPointerKeyForStaticField(IField f) {
    return pointerKeys.getPointerKeyForStaticField(f);
  }

  @Override
  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField field) {
    return pointerKeys.getPointerKeyForInstanceField(I, field);
  }

  @Override
  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    return pointerKeys.getPointerKeyForArrayContents(I);
  }

  protected ClassBasedInstanceKeys getIKeyFactory() {
    return iKeyFactory;
  }
}
