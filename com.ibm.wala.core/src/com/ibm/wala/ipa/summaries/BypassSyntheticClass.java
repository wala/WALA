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

import java.util.Collection;
import java.util.HashSet;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * A synthetic implementation of a class
 * 
 * @author Julian Dolby
 */
public class BypassSyntheticClass extends SyntheticClass {

  /**
   * @param T
   *          a type reference
   * @return a synthetic class name to represent the synthetic form of this type
   */
  public static TypeName getName(TypeReference T) {
    String s = "L$" + T.getName().toString().substring(1);
    return TypeName.string2TypeName(s);
  }

  /**
   * The original "real" type corresponding to this synthetic type.
   */
  private final IClass realType;

  private final IClassLoader loader;

  public BypassSyntheticClass(IClass realType, IClassLoader loader, ClassHierarchy cha) {
    super(TypeReference.findOrCreate(loader.getReference(), getName(realType.getReference())), cha);
    this.loader = loader;
    this.realType = realType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getClassLoader()
   */
  public IClassLoader getClassLoader() {
    return loader;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getSuperclass()
   */
  public IClass getSuperclass() throws ClassHierarchyException {
    if (realType.isInterface()) {
      IClass result = loader.lookupClass(TypeReference.JavaLangObject.getName(), getClassHierarchy());
      if (result != null) {
        return result;
      } else {
        throw new ClassHierarchyException("could not find java.lang.Object");
      }
    } else
      return realType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getAllInterfaces()
   */
  public Collection<IClass> getAllImplementedInterfaces() throws ClassHierarchyException {
    Collection<IClass> realIfaces = realType.isInterface() ? realType.getAllAncestorInterfaces() : realType.getAllImplementedInterfaces();
    if (realType.isInterface()) {
      HashSet<IClass> result = new HashSet<IClass>(realIfaces);
      result.add(realType);
      return result;
    } else {
      return realIfaces;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getAllInterfaces()
   */
  public Collection<IClass> getAllAncestorInterfaces() throws ClassHierarchyException {
    if (Assertions.verifyAssertions) {
      Assertions._assert(realType.isInterface());
    }
    HashSet<IClass> result = new HashSet<IClass>(realType.getAllAncestorInterfaces().size() + 1);
    result.addAll(realType.getAllAncestorInterfaces());
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.classLoader.Selector)
   */
  public IMethod getMethod(Selector selector) {
    return realType.getMethod(selector);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.classLoader.Selector)
   */
  public IField getField(Atom name) {
    return realType.getField(name);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getSourceFileName()
   */
  public String getSourceFileName() {
    return realType.getSourceFileName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
   */
  public IMethod getClassInitializer() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getDeclaredMethods()
   */
  public Collection<IMethod> getDeclaredMethods() {
    return realType.getDeclaredMethods();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getDeclaredInstanceFields()
   */
  public Collection<IField> getDeclaredInstanceFields() {
    return realType.getDeclaredInstanceFields();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getDeclaredStaticFields()
   */
  public Collection<IField> getDeclaredStaticFields() {
    return realType.getDeclaredStaticFields();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#isInterface()
   */
  public boolean isSyntheticImplentor() {
    return realType.isInterface();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "<Synthetic " + (realType.isInterface() ? "Implementor" : "Subclass") + " " + realType.toString() + ">";
  }

  public IClass getRealType() {
    return realType;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object arg0) {
    if (arg0.getClass().equals(getClass())) {
      return realType.equals(((BypassSyntheticClass) arg0).realType);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return realType.hashCode() * 1621;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getModifiers()
   */
  public int getModifiers() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getName()
   */
  public TypeName getName() {
    return getReference().getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#isReferenceType()
   */
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getDirectInterfaces()
   */
  public Collection<IClass> getDirectInterfaces() {
    // TODO Auto-generated method stub
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getAllInstanceFields()
   */
  public Collection<IField> getAllInstanceFields() throws ClassHierarchyException {
    return realType.getAllInstanceFields();
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getAllStaticFields()
   */
  public Collection<IField> getAllStaticFields() throws ClassHierarchyException {
    return realType.getAllStaticFields();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getAllMethods()
   */
  public Collection getAllMethods() throws ClassHierarchyException {
	return realType.getAllMethods();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getAllFields()
   */
  public Collection<IField> getAllFields() throws ClassHierarchyException {
	return realType.getAllFields();
  }
}
