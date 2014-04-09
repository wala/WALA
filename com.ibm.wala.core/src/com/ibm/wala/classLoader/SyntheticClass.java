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

package com.ibm.wala.classLoader;

import java.io.InputStream;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

/**
 * An {@link IClass} that exists nowhere in bytecode.
 */
public abstract class SyntheticClass implements IClass {

  private final TypeReference T;

  private final IClassHierarchy cha;
  /**
   * @param T type reference describing this class
   */
  public SyntheticClass(TypeReference T, IClassHierarchy cha) {
    super();
    this.T = T;
    this.cha = cha;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((T == null) ? 0 : T.hashCode());
    result = prime * result + ((cha == null) ? 0 : cha.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof SyntheticClass))
      return false;
    final SyntheticClass other = (SyntheticClass) obj;
    if (T == null) {
      if (other.T != null)
        return false;
    } else if (!T.equals(other.T))
      return false;
    if (cha == null) {
      if (other.cha != null)
        return false;
    } else if (!cha.equals(other.cha))
      return false;
    return true;
  }

  /**
   * By default, a synthetic class is "loaded" by the primordial loader.
   * Subclasses may override as necessary.
   * @see com.ibm.wala.classLoader.IClass#getClassLoader()
   */
  public IClassLoader getClassLoader() {
    return cha.getLoader(ClassLoaderReference.Primordial);
  }

  /* 
   * @see com.ibm.wala.classLoader.IClass#isInterface()
   */
  public boolean isInterface() {
    return false;
  }

  /* 
   * @see com.ibm.wala.classLoader.IClass#isAbstract()
   */
  public boolean isAbstract() {
    return false;
  }

  /* 
   * @see com.ibm.wala.classLoader.IClass#getReference()
   */
  public TypeReference getReference() {
    return T;
  }

  /* 
   * @see com.ibm.wala.classLoader.IClass#getSourceFileName()
   */
  public String getSourceFileName() {
    return null;
  }
  
  public InputStream getSource() {
    return null;
  }

  /* 
   * @see com.ibm.wala.classLoader.IClass#isArrayClass()
   */
  public boolean isArrayClass() {
    return false;
  }
  
  public IClassHierarchy getClassHierarchy() {
    return cha;
  }
  
  public TypeName getName() {
    return getReference().getName();
  }

  /**
   * we assume synthetic classes do not need to have multiple fields with the same name.  
   */
  public IField getField(Atom name, TypeName typeName) {
    return getField(name);
  }
}
