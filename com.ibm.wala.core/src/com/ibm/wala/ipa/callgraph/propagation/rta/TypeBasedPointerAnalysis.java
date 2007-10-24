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
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.AbstractPointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.ArrayInstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.HeapModel;
import com.ibm.wala.ipa.callgraph.propagation.InstanceFieldKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.LocalPointerKeyWithFilter;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Collection;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.BimodalMutableIntSet;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.OrdinalSet;

/**
 * 
 * A trivial field-based pointer analysis solution, which only uses the
 * information of which types (classes) are live.
 * 
 * @author sfink
 */
public class TypeBasedPointerAnalysis extends AbstractPointerAnalysis {

  private final Collection<IClass> klasses;

  private final TypeBasedHeapModel heapModel;

  /**
   * Map: IClass -> OrdinalSet
   */
  private final Map<IClass, OrdinalSet<InstanceKey>> pointsTo = HashMapFactory.make();

  /**
   * @param klasses
   *          Collection<IClass>
   * @throws AssertionError  if klasses is null
   */
  public TypeBasedPointerAnalysis(AnalysisOptions options, Collection<IClass> klasses, CallGraph cg) throws AssertionError {
    super(cg, makeInstanceKeys(klasses));
    this.klasses = klasses;
    heapModel = new TypeBasedHeapModel(options, klasses, cg);
  }

  /**
   * @param c
   *          Collection<IClass>
   */
  private static MutableMapping<InstanceKey> makeInstanceKeys(Collection<IClass> c) {
    assert c != null;
    MutableMapping<InstanceKey> result = MutableMapping.make();
    for (Iterator<IClass> it = c.iterator(); it.hasNext();) {
      IClass klass = it.next();
      if (!klass.isAbstract() && !klass.isInterface()) {
        result.add(new ConcreteTypeKey(klass));
      }
    }
    return result;
  }

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

  private OrdinalSet<InstanceKey> computeOrdinalInstanceSet(IClass type) {
    Collection<IClass> klasses = null;
    if (type.isInterface()) {
      klasses = getCallGraph().getClassHierarchy().getImplementors(type.getReference());
    } else {
      klasses = getCallGraph().getClassHierarchy().computeSubClasses(type.getReference());
    }
    klasses = HashSetFactory.make(klasses);
    klasses.retainAll(this.klasses);
    OrdinalSet<InstanceKey> result = toOrdinalInstanceKeySet(klasses);
    return result;
  }

  /**
   * @param c
   *          Collection<IClass>
   */
  private OrdinalSet<InstanceKey> toOrdinalInstanceKeySet(Collection c) {
    BimodalMutableIntSet s = new BimodalMutableIntSet();
    for (Iterator it = c.iterator(); it.hasNext();) {
      IClass klass = (IClass) it.next();
      int index = getInstanceKeyMapping().getMappedIndex(new ConcreteTypeKey(klass));
      if (index >= 0) {
        s.add(index);
      }
    }
    return new OrdinalSet<InstanceKey>(s, getInstanceKeyMapping());
  }

  private IClass inferType(PointerKey key) {
    if (key instanceof LocalPointerKeyWithFilter) {
      LocalPointerKeyWithFilter lpk = (LocalPointerKeyWithFilter) key;
      FilteredPointerKey.TypeFilter filter = lpk.getTypeFilter();
      Assertions._assert(filter instanceof FilteredPointerKey.SingleClassFilter);
      return ((FilteredPointerKey.SingleClassFilter)filter).getConcreteType();
    } else if (key instanceof StaticFieldKey) {
      StaticFieldKey s = (StaticFieldKey) key;
      return getCallGraph().getClassHierarchy().lookupClass(s.getField().getFieldTypeReference());
    } else if (key instanceof InstanceFieldKey) {
      InstanceFieldKey i = (InstanceFieldKey) key;
      return getCallGraph().getClassHierarchy().lookupClass(i.getField().getFieldTypeReference());
    } else if (key instanceof ArrayInstanceKey) {
      ArrayInstanceKey i = (ArrayInstanceKey) key;
      FilteredPointerKey.TypeFilter filter = i.getTypeFilter();
      Assertions._assert(filter instanceof FilteredPointerKey.SingleClassFilter);
      return ((FilteredPointerKey.SingleClassFilter)filter).getConcreteType();
    } else {
      Assertions.UNREACHABLE("inferType " + key.getClass());
      return null;
    }
  }

  public HeapModel getHeapModel() {
    return heapModel;
  }

  public Collection<PointerKey> getPointerKeys() {
    return Iterator2Collection.toCollection(heapModel.iteratePointerKeys());
  }

  public boolean isFiltered(PointerKey pk) {
    return false;
  }

  public IClassHierarchy getClassHierarchy() {
    return heapModel.getClassHierarchy();
  }

}
