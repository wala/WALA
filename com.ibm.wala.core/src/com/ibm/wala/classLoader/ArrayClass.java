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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * Implementation of IClass for array classes.
 * 
 * @author Alan Donovan
 * @author sfink
 */
public class ArrayClass implements IClass, Constants {

  private final ClassHierarchy cha;
  
  /**
   * Package-visible constructor; only for use by ArrayClassLoader class.
   * 'loader' must be the Primordial IClassLoader.
   * 
   * [WHY? -- array classes are loaded by the element classloader??]
   */
  ArrayClass(TypeReference type, IClassLoader loader, ClassHierarchy cha) {
    this.type = type;
    this.loader = loader;
    this.cha = cha;
    if (Assertions.verifyAssertions) {
      TypeReference elementType = type.getInnermostElementType();
      if (!elementType.isPrimitiveType()) {
        IClass klass = loader.lookupClass(elementType.getName(), cha);
        if (klass == null) {
          Assertions.UNREACHABLE("caller should not attempt to create an array with type " + type);
        }
      } else {
        Assertions._assert(loader.getReference().equals(ClassLoaderReference.Primordial));
      }
    }
  }

  private final TypeReference type;

  private final IClassLoader loader;

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
   * @see com.ibm.wala.classLoader.IClass#getName()
   */
  public TypeName getName() {
    return getReference().getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#isInterface()
   */
  public boolean isInterface() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#isAbstract()
   */
  public boolean isAbstract() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getModifiers()
   */
  public int getModifiers() {
    return ACC_PUBLIC | ACC_FINAL;
  }

  public String getQualifiedNameForReflection() {
    return type.getName().toString(); // XXX is this the right string?
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getSuperclass()
   */
  public IClass getSuperclass() {
    try {
      IClass elt = getElementClass();

      Assertions._assert(getReference().getArrayElementType().isPrimitiveType() || elt != null);

      // super is Ljava/lang/Object in two cases:
      // 1) [Ljava/lang/Object
      // 2) [? for primitive arrays (null from getElementClass)
      if (elt == null || elt.getReference() == TypeReference.JavaLangObject) {
        return loader.lookupClass(TypeReference.JavaLangObject.getName(), getClassHierarchy());
      }

      // else it is array of super of element type (yuck)
      else {
        TypeReference eltSuperRef = elt.getSuperclass().getReference();
        TypeReference superRef = TypeReference.findOrCreateArrayOf(eltSuperRef);
        return elt.getSuperclass().getClassLoader().lookupClass(superRef.getName(), getClassHierarchy());
      }
    } catch (ClassHierarchyException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.classLoader.Selector)
   */
  public IMethod getMethod(Selector sig) {
    return loader.lookupClass(TypeReference.JavaLangObject.getName(), getClassHierarchy()).getMethod(sig);
  }

  public IField getField(Atom name) {
    return getSuperclass().getField(name);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getDeclaredMethods()
   */
  public Iterator<IMethod> getDeclaredMethods() {
    return EmptyIterator.instance();
  }

  public int getNumberOfDeclaredMethods() {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getReference()
   */
  public TypeReference getReference() {
    return type;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getSourceFileName()
   */
  public String getSourceFileName() {
    return null;
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
   * @see com.ibm.wala.classLoader.IClass#isArrayClass()
   */
  public boolean isArrayClass() {
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return getReference().toString();
  }

  /**
   * @return the IClass that represents the array element type, or null if the
   *         element type is a primitive
   */
  public IClass getElementClass() {
    TypeReference elementType = getReference().getArrayElementType();
    if (elementType.isPrimitiveType()) {
      return null;
    }
    return loader.lookupClass(elementType.getName(), getClassHierarchy());
  }

  public int hashCode() {
    return type.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getDeclaredFields()
   */
  public Collection<IField> getDeclaredInstanceFields() {
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getDeclaredStaticFields()
   */
  public Collection<IField> getDeclaredStaticFields() {
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces()
   */
  public Collection<IClass> getAllImplementedInterfaces() {
    HashSet<IClass> result = HashSetFactory.make(2);
    result.add(loader.lookupClass(TypeReference.array_interfaces[0], getClassHierarchy()));
    result.add(loader.lookupClass(TypeReference.array_interfaces[1], getClassHierarchy()));
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getAllAncestorInterfaces()
   */
  public Collection<IClass> getAllAncestorInterfaces() {
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#isReferenceType()
   */
  public boolean isReferenceType() {
    return true;
  }

  public int getDimensionality() {
    return getReference().getDimensionality();
  }

  /**
   * @return the IClass that represents the innermost array element type, or
   *         null if the element type is a primitive
   */
  public IClass getInnermostElementClass() {
    TypeReference elementType = getReference().getInnermostElementType();
    if (elementType.isPrimitiveType()) {
      return null;
    }
    return loader.lookupClass(elementType.getName(), getClassHierarchy());
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
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    if (obj instanceof ArrayClass) {
      ArrayClass other = (ArrayClass) obj;
      return loader.equals(other.loader) && type.equals(other.type);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.IClass#getAllInstanceFields()
   */
  public Collection<IField> getAllInstanceFields() throws ClassHierarchyException {
    Assertions.UNREACHABLE();
    return null;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getAllStaticFields()
   */
  public Collection<IField> getAllStaticFields() throws ClassHierarchyException {
    Assertions.UNREACHABLE();
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getAllMethods()
   */
  public Collection getAllMethods() throws ClassHierarchyException {
	Assertions.UNREACHABLE();
	return null;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.IClass#getAllFields()
   */
  public Collection<IField> getAllFields() throws ClassHierarchyException {
	Assertions.UNREACHABLE();
	return null;
  }

  public ClassHierarchy getClassHierarchy() {
    return cha;
  }
}
