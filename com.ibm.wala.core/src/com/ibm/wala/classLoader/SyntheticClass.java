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

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;

/**
 *
 * A Class that exists nowhere in bytecode.
 * 
 * @author sfink
 */
public abstract class SyntheticClass implements IClass {

  private final TypeReference T;
  
  private final ClassHierarchy cha;
  /**
   * @param T type reference describing this class
   */
  public SyntheticClass(TypeReference T, ClassHierarchy cha) {
    super();
    this.T = T;
    this.cha = cha;
  }

  /**
   * By default, a synthetic class is "loaded" by the primordial loader.
   * Subclasses may override as necessary.
   * @see com.ibm.wala.classLoader.IClass#getClassLoader()
   */
  public IClassLoader getClassLoader() {
    return cha.getLoader(ClassLoaderReference.Primordial);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#isInterface()
   */
  public boolean isInterface() {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#isAbstract()
   */
  public boolean isAbstract() {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getReference()
   */
  public TypeReference getReference() {
    return T;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getSourceFileName()
   */
  public String getSourceFileName() {
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#isArrayClass()
   */
  public boolean isArrayClass() {
    return false;
  }
}
