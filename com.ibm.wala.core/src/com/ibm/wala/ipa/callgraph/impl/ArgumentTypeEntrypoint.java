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
import java.util.Set;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;

/**
 * An entrypoint which chooses some valid (non-interface) concrete type for each argument, if one is available.
 */
public class ArgumentTypeEntrypoint extends Entrypoint {

  final private TypeReference[][] paramTypes;

  private final IClassHierarchy cha;

  /**
   * @throws IllegalArgumentException if method == null
   */
  protected TypeReference[][] makeParameterTypes(IMethod method) throws IllegalArgumentException {
    if (method == null) {
      throw new IllegalArgumentException("method == null");
    }
    TypeReference[][] result = new TypeReference[method.getNumberOfParameters()][];
    for (int i = 0; i < result.length; i++) {
      TypeReference t = method.getParameterType(i);
      if (!t.isPrimitiveType()) {
        IClass klass = cha.lookupClass(t);
        if (klass == null) {
          t = null;
        } else if (!klass.isInterface() && klass.isAbstract()) {
          // yes, I've seen classes that are marked as "abstract interface" :)
          t = chooseAConcreteSubClass(klass);
        } else if (klass.isInterface()) {
          t = chooseAnImplementor(klass);
        } else if (klass.isArrayClass()) {
          ArrayClass arrayKlass = (ArrayClass) klass;
          IClass innermost = arrayKlass.getInnermostElementClass();
          if (innermost != null && innermost.isInterface()) {
            TypeReference impl = chooseAnImplementor(innermost);
            if (impl == null) {
              t = null;
            } else {
              t = TypeReference.findOrCreateArrayOf(impl);
              for (int dim = 1; dim < arrayKlass.getDimensionality(); dim++) {
                t = TypeReference.findOrCreateArrayOf(t);
              }
            }
          }
        }
      }

      result[i] = (t == null) ? new TypeReference[0] : new TypeReference[] { t };
    }
    return result;
  }

  private TypeReference chooseAnImplementor(IClass iface) {
    Set implementors = cha.getImplementors(iface.getReference());
    if (!implementors.isEmpty()) {
      return ((IClass) implementors.iterator().next()).getReference();
    } else {
      return null;
    }
  }

  private TypeReference chooseAConcreteSubClass(IClass klass) {
    Collection<IClass> subclasses = cha.computeSubClasses(klass.getReference());
    for (IClass c : subclasses) {
      if (!c.isAbstract()) {
        return c.getReference();
      }
    }
    return null;
  }

  public ArgumentTypeEntrypoint(IMethod method, IClassHierarchy cha) {
    super(method);
    this.cha = cha;
    paramTypes = makeParameterTypes(method);
  }

  @Override
  public TypeReference[] getParameterTypes(int i) {
    return paramTypes[i];
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.Entrypoint#getNumberOfParameters()
   */
  @Override
  public int getNumberOfParameters() {
    return paramTypes.length;
  }

}
