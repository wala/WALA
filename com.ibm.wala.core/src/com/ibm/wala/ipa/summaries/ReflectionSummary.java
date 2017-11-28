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
package com.ibm.wala.ipa.summaries;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.SetType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * Reflection summary information for a method.
 */
public class ReflectionSummary {


  private final Map<Integer, Set<TypeReference>> map = HashMapFactory.make();


  public void addType(int bcIndex, TypeReference T) {
    Set<TypeReference> S = findOrCreateSetForBCIndex(bcIndex);
    S.add(T);
  }

  private Set<TypeReference> findOrCreateSetForBCIndex(int bcIndex) {
    Integer I = new Integer(bcIndex);
    Set<TypeReference> result = map.get(I);
    if (result == null) {
      result = HashSetFactory.make(10);
      map.put(I, result);
    }
    return result;
  }

  public TypeAbstraction getTypeForNewInstance(int bcIndex, IClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("null cha");
    }
    Set<TypeReference> S = map.get(new Integer(bcIndex));
    if (S == null) {
      return null;
    } else {
      PointType[] p = new PointType[S.size()];
      Iterator<TypeReference> it = S.iterator();
      for (int i = 0; i < p.length; i++) {
        TypeReference T = it.next();
        IClass klass = cha.lookupClass(T);
        assert klass != null : "null type for " + T;
        p[i] = new PointType(klass);
      }
      return new SetType(p);
    }
  }

  public Set<TypeReference> getTypesForProgramLocation(int bcIndex) {
    return map.get(new Integer(bcIndex));
  }
}
