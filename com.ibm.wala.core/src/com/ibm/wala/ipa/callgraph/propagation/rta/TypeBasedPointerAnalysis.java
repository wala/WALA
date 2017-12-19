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
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractPointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.ArrayContentsKey;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKeyWithFilter;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.ReturnValueKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ExceptionReturnValueKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BimodalMutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * A trivial field-based pointer analysis solution, which only uses the information of which types (classes) are live.
 */
public class TypeBasedPointerAnalysis extends AbstractPointerAnalysis {

  private final Collection<IClass> klasses;

  private final TypeBasedHeapModel heapModel;

  /**
   * Map: IClass -&gt; OrdinalSet
   */
  private final Map<IClass, OrdinalSet<InstanceKey>> pointsTo = HashMapFactory.make();

  /**
   * @param klasses Collection<IClass>
   * @throws AssertionError if klasses is null
   */
  private TypeBasedPointerAnalysis(AnalysisOptions options, Collection<IClass> klasses, CallGraph cg) throws AssertionError {
    super(cg, makeInstanceKeys(klasses));
    this.klasses = klasses;
    heapModel = new TypeBasedHeapModel(options, klasses, cg);
  }

  /**
   * @param c Collection<IClass>
   */
  private static MutableMapping<InstanceKey> makeInstanceKeys(Collection<IClass> c) {
    if (c == null) {
      throw new IllegalArgumentException("null c");
    }
    MutableMapping<InstanceKey> result = MutableMapping.make();
    for (IClass klass : c) {
      if (!klass.isAbstract() && !klass.isInterface()) {
        result.add(new ConcreteTypeKey(klass));
      }
    }
    return result;
  }

  public static TypeBasedPointerAnalysis make(AnalysisOptions options, Collection<IClass> klasses, CallGraph cg)
      throws AssertionError {
    return new TypeBasedPointerAnalysis(options, klasses, cg);
  }

  @Override
  public OrdinalSet<InstanceKey> getPointsToSet(PointerKey key) throws IllegalArgumentException {
    if (key == null) {
      throw new IllegalArgumentException("key == null");
    }
    IClass type = inferType(key);
    if (type == null) {
      return OrdinalSet.empty();
    } else {
      OrdinalSet<InstanceKey> result = pointsTo.get(type);
      if (result == null) {
        result = computeOrdinalInstanceSet(type);
        pointsTo.put(type, result);
      }
      return result;
    }
  }

  /**
   * Compute the set of {@link InstanceKey}s which may represent a particular type.
   */
  private OrdinalSet<InstanceKey> computeOrdinalInstanceSet(IClass type) {
    Collection<IClass> klasses = null;
    if (type.isInterface()) {
      klasses = getCallGraph().getClassHierarchy().getImplementors(type.getReference());
    } else {
      Collection<IClass> sc = getCallGraph().getClassHierarchy().computeSubClasses(type.getReference());
      klasses = HashSetFactory.make();
      for (IClass c : sc) {
        if (!c.isInterface()) {
          klasses.add(c);
        }
      }
    }
    Collection<IClass> c = HashSetFactory.make();
    for (IClass klass : klasses) {
      if (klass.isArrayClass()) {
        TypeReference elementType = klass.getReference().getArrayElementType();
        if (elementType.isPrimitiveType()) {
          c.add(klass);
        } else {
          // just add Object[], since with array typing rules we have no idea
          // the exact type of array the reference is pointing to
          c.add(klass.getClassHierarchy().lookupClass(TypeReference.JavaLangObject.getArrayTypeForElementType()));
        }
      } else if (this.klasses.contains(klass)) {
        c.add(klass);
      }

    }
    OrdinalSet<InstanceKey> result = toOrdinalInstanceKeySet(c);
    return result;
  }

  private OrdinalSet<InstanceKey> toOrdinalInstanceKeySet(Collection<IClass> c) {
    BimodalMutableIntSet s = new BimodalMutableIntSet();
    for (IClass klass : c) {
      int index = getInstanceKeyMapping().add(new ConcreteTypeKey(klass));
      s.add(index);
    }
    return new OrdinalSet<>(s, getInstanceKeyMapping());
  }

  private IClass inferType(PointerKey key) {
    if (key instanceof LocalPointerKeyWithFilter) {
      LocalPointerKeyWithFilter lpk = (LocalPointerKeyWithFilter) key;
      FilteredPointerKey.TypeFilter filter = lpk.getTypeFilter();
      assert filter instanceof FilteredPointerKey.SingleClassFilter;
      return ((FilteredPointerKey.SingleClassFilter) filter).getConcreteType();
    } else if (key instanceof StaticFieldKey) {
      StaticFieldKey s = (StaticFieldKey) key;
      return getCallGraph().getClassHierarchy().lookupClass(s.getField().getFieldTypeReference());
    } else if (key instanceof InstanceFieldKey) {
      InstanceFieldKey i = (InstanceFieldKey) key;
      return getCallGraph().getClassHierarchy().lookupClass(i.getField().getFieldTypeReference());
    } else if (key instanceof ArrayContentsKey) {
      ArrayContentsKey i = (ArrayContentsKey) key;
      FilteredPointerKey.TypeFilter filter = i.getTypeFilter();
      assert filter instanceof FilteredPointerKey.SingleClassFilter;
      return ((FilteredPointerKey.SingleClassFilter) filter).getConcreteType();
    } else if (key instanceof ExceptionReturnValueKey) {
      return getCallGraph().getClassHierarchy().lookupClass(TypeReference.JavaLangException);
    } else if (key instanceof ReturnValueKey) {
      ReturnValueKey r = (ReturnValueKey) key;
      return getCallGraph().getClassHierarchy().lookupClass(r.getNode().getMethod().getReturnType());
    } else {
      Assertions.UNREACHABLE("inferType " + key.getClass());
      return null;
    }
  }

  @Override
  public HeapModel getHeapModel() {
    return heapModel;
  }

  @Override
  public Collection<PointerKey> getPointerKeys() {
    return Iterator2Collection.toSet(heapModel.iteratePointerKeys());
  }

  @Override
  public boolean isFiltered(PointerKey pk) {
    return false;
  }

  @Override
  public IClassHierarchy getClassHierarchy() {
    return heapModel.getClassHierarchy();
  }

}
