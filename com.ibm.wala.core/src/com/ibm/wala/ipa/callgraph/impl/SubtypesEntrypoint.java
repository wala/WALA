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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * 
 * An entrypoint whose parameter types are cones based on declared types.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 * @author Mandana Vaziri (mvaziri@us.ibm.com)
 */
public class SubtypesEntrypoint extends DefaultEntrypoint {

  public SubtypesEntrypoint(MethodReference method, IClassHierarchy cha) {
    super(method, cha);
  }

  public SubtypesEntrypoint(IMethod method, IClassHierarchy cha) {
    super(method, cha);
  }

  /**
   * @param method
   */
  protected TypeReference[][] makeParameterTypes(MethodReference method) {
    TypeReference[][] result = new TypeReference[method.getNumberOfParameters()][];
    for (int i = 0; i < result.length; i++) {
      result[i] = makeParameterTypes(method, i);
    }

    return result;
  }

  protected TypeReference[] makeParameterTypes(MethodReference method, int i) {
    TypeReference nominal = method.getParameterType(i);
    if (nominal.isPrimitiveType() || nominal.isArrayType())
      return new TypeReference[] { nominal };
    else {
      IClass nc = getCha().lookupClass(nominal);
      Collection subcs = nc.isInterface() ? getCha().getImplementors(nominal) : getCha().computeSubClasses(nominal);
      Set<TypeReference> subs = new HashSet<TypeReference>();
      for (Iterator I = subcs.iterator(); I.hasNext();) {
        IClass cs = (IClass) I.next();
        if (!cs.isAbstract() && !cs.isInterface())
          subs.add(cs.getReference());
      }
      return subs.toArray(new TypeReference[subs.size()]);
    }
  }
}
