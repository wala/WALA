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

import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import java.util.Arrays;

/** An entrypoint whose parameter types are the declared types. */
public class DefaultEntrypoint extends Entrypoint {
  private final TypeReference[][] paramTypes;

  private final IClassHierarchy cha;

  public DefaultEntrypoint(IMethod method, IClassHierarchy cha) {

    super(method);
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    this.cha = cha;
    paramTypes = makeParameterTypes(method);
    assert paramTypes != null : method.toString();
  }

  public DefaultEntrypoint(MethodReference method, IClassHierarchy cha) {
    super(method, cha);
    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    this.cha = cha;
    paramTypes = makeParameterTypes(getMethod());
    assert paramTypes != null : method.toString();
  }

  protected TypeReference[][] makeParameterTypes(IMethod method) {
    TypeReference[][] result = new TypeReference[method.getNumberOfParameters()][];
    Arrays.setAll(result, i -> makeParameterTypes(method, i));

    return result;
  }

  protected TypeReference[] makeParameterTypes(IMethod method, int i) {
    return new TypeReference[] {method.getParameterType(i)};
  }

  @Override
  public TypeReference[] getParameterTypes(int i) {
    return paramTypes[i];
  }

  public void setParameterTypes(int i, TypeReference[] types) {
    paramTypes[i] = types;
  }

  @Override
  public int getNumberOfParameters() {
    return paramTypes.length;
  }

  public IClassHierarchy getCha() {
    return cha;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Arrays.deepHashCode(paramTypes);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    final DefaultEntrypoint other = (DefaultEntrypoint) obj;
    if (!Arrays.deepEquals(paramTypes, other.paramTypes)) return false;
    return true;
  }
}
