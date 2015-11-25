/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.exceptionanalysis;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.OrdinalSetMapping;

public class ExceptionToBitvectorTransformer {

  private Map<TypeReference, BitVector> includingExceptions;
  private OrdinalSetMapping<TypeReference> values;

  public OrdinalSetMapping<TypeReference> getValues() {
    return values;
  }

  public ExceptionToBitvectorTransformer(Set<TypeReference> exceptions) {
    includingExceptions = new HashMap<TypeReference, BitVector>();
    createValues(exceptions);
    for (TypeReference exception : exceptions) {
      BitVector bv = new BitVector(values.getSize());
      bv.set(values.getMappedIndex(exception));
      includingExceptions.put(exception, bv);
    }
  }

  public ExceptionToBitvectorTransformer(Set<TypeReference> exceptions, ClassHierarchy cha) {
    // TODO
    throw new UnsupportedOperationException();
  }

  private void createValues(Set<TypeReference> exceptions) {
    TypeReference[] exceptionsArray = new TypeReference[exceptions.size()];
    exceptions.toArray(exceptionsArray);
    values = new ObjectArrayMapping<TypeReference>(exceptionsArray);
  }

  public BitVector computeBitVector(Set<TypeReference> exceptions) {
    BitVector result = new BitVector(values.getSize());
    for (TypeReference exception : exceptions) {
      // if (!includingExceptions.containsKey(exception)) {
      // throw new IllegalArgumentException("Got exception I don't know about,"
      // + "make sure only to use exceptions given to the constructor ");
      // }
      if (includingExceptions.containsKey(exception)) {
        result.or(includingExceptions.get(exception));
      }
    }
    return result;
  }

  public Set<TypeReference> computeExceptions(BitVector bitVector) {
    assert bitVector.length() == values.getSize();
    Set<TypeReference> result = new HashSet<>();
    for (int i = 0; i < bitVector.length(); i++) {
      if (bitVector.get(i)) {
        result.add(values.getMappedObject(i));
      }
    }
    return result;
  }

  public Set<TypeReference> computeExceptions(BitVectorVariable bitVector) {
    Set<TypeReference> result = new HashSet<>();
    for (int i = 0; i < values.getSize(); i++) {
      if (bitVector.get(i)) {
        result.add(values.getMappedObject(i));
      }
    }
    return result;
  }
}
