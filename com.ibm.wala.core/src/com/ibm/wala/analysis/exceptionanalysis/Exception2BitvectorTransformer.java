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

import java.util.HashSet;
import java.util.Set;

import com.ibm.wala.fixpoint.BitVectorVariable;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.OrdinalSetMapping;

public class Exception2BitvectorTransformer {
  private OrdinalSetMapping<TypeReference> values;

  public OrdinalSetMapping<TypeReference> getValues() {
    return values;
  }

  public Exception2BitvectorTransformer(Set<TypeReference> exceptions) {
    createValues(exceptions);
    for (TypeReference exception : exceptions) {
      BitVector bv = new BitVector(values.getSize());
      bv.set(values.getMappedIndex(exception));
    }
  }

  private void createValues(Set<TypeReference> exceptions) {
    TypeReference[] exceptionsArray = new TypeReference[exceptions.size()];
    exceptions.toArray(exceptionsArray);
    values = new ObjectArrayMapping<>(exceptionsArray);
  }

  public BitVector computeBitVector(Set<TypeReference> exceptions) {
    BitVector result = new BitVector(values.getSize());
    for (TypeReference exception : exceptions) {
      int pos = values.getMappedIndex(exception);
      if (pos != -1) {
        result.set(pos);
      } else {
        throw new IllegalArgumentException("Got exception I don't know about,"
            + "make sure only to use exceptions given to the constructor ");
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
