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
package com.ibm.wala.j2ee.client.impl;

import com.ibm.wala.j2ee.client.IClass;
import com.ibm.wala.util.ImmutableByteArray;
import com.ibm.wala.util.StringStuff;

/**
 * 
 * A representation of a class used to communicate analysis results.
 * 
 * @author sfink
 */
public class ClassImpl implements IClass {

  private final String classLoader;
  private final String name;
  private final String pack;

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return name;
  }

  public ClassImpl(com.ibm.wala.classLoader.IClass klass) {
    this(klass.getReference().getName().toString(),klass.getClassLoader().getName().toString());
  }
  
  public ClassImpl(String name, String classLoader) {
    this.classLoader = classLoader;
    this.name = name;
    String canon = StringStuff.deployment2CanonicalTypeString(name);
    ImmutableByteArray b = StringStuff.parseForPackage(new ImmutableByteArray(canon.getBytes()));
    this.pack = (b == null) ? null : b.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.atk.Method#getClassLoaderName()
   */

  public String getClassLoaderName() {
    return classLoader;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.atk.Method#getName()
   */
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return getClassLoaderName().hashCode() * 4003 + getName().hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object arg0) {
    if (arg0 == null) {
      return false;
    }
    if (getClass().equals(arg0.getClass())) {
      ClassImpl other = (ClassImpl) arg0;
      return getName().equals(other.getName()) && getClassLoaderName().equals(other.getClassLoaderName());
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.client.IClass#getPackage()
   */
  public String getPackage() {
    return pack;
  }
}