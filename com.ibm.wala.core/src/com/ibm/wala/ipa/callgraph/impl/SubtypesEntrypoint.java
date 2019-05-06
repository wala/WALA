/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

/** An entrypoint whose parameter types are cones based on declared types. */
public class SubtypesEntrypoint extends DefaultEntrypoint {

  public SubtypesEntrypoint(MethodReference method, IClassHierarchy cha) {
    super(method, cha);
  }

  public SubtypesEntrypoint(IMethod method, IClassHierarchy cha) {
    super(method, cha);
  }

  @Override
  protected TypeReference[][] makeParameterTypes(IMethod method) {
    TypeReference[][] result = new TypeReference[method.getNumberOfParameters()][];
    Arrays.setAll(result, i -> makeParameterTypes(method, i));

    return result;
  }

  @Override
  protected TypeReference[] makeParameterTypes(IMethod method, int i) {
    TypeReference nominal = method.getParameterType(i);
    if (nominal.isPrimitiveType() || nominal.isArrayType()) return new TypeReference[] {nominal};
    else {
      IClass nc = getCha().lookupClass(nominal);
      if (nc == null) {
        throw new IllegalStateException("Could not resolve in cha: " + nominal);
      }
      Collection<IClass> subcs =
          nc.isInterface()
              ? getCha().getImplementors(nominal)
              : getCha().computeSubClasses(nominal);
      Set<TypeReference> subs = HashSetFactory.make();
      for (IClass cs : subcs) {
        if (!cs.isAbstract() && !cs.isInterface()) {
          subs.add(cs.getReference());
        }
      }
      return subs.toArray(new TypeReference[0]);
    }
  }
}
