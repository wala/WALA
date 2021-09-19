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
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

/** A synthetic implementation of a class */
public class BypassSyntheticClass extends SyntheticClass {

  /**
   * @param T a type reference
   * @return a synthetic class name to represent the synthetic form of this type
   * @throws IllegalArgumentException if T is null
   */
  public static TypeName getName(TypeReference T) {
    if (T == null) {
      throw new IllegalArgumentException("T is null");
    }
    String s = "L$" + T.getName().toString().substring(1);
    return TypeName.string2TypeName(s);
  }

  /** The original "real" type corresponding to this synthetic type. */
  private final IClass realType;

  private final IClassLoader loader;

  public BypassSyntheticClass(IClass realType, IClassLoader loader, IClassHierarchy cha)
      throws NullPointerException, NullPointerException {
    super(TypeReference.findOrCreate(loader.getReference(), getName(realType.getReference())), cha);
    this.loader = loader;
    this.realType = realType;
  }

  /** @see com.ibm.wala.classLoader.IClass#getClassLoader() */
  @Override
  public IClassLoader getClassLoader() {
    return loader;
  }

  /** @see com.ibm.wala.classLoader.IClass#getSuperclass() */
  @Override
  public IClass getSuperclass() {
    if (realType.isInterface()) {
      IClass result = loader.lookupClass(TypeReference.JavaLangObject.getName());
      if (result != null) {
        return result;
      } else {
        throw new IllegalStateException("could not find java.lang.Object");
      }
    } else return realType;
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces() */
  @Override
  public Collection<IClass> getAllImplementedInterfaces() {
    Collection<IClass> realIfaces = realType.getAllImplementedInterfaces();
    if (realType.isInterface()) {
      HashSet<IClass> result = HashSetFactory.make(realIfaces);
      result.add(realType);
      return result;
    } else {
      return realIfaces;
    }
  }

  /** @see com.ibm.wala.classLoader.IClass#getMethod(Selector) */
  @Override
  public IMethod getMethod(Selector selector) {
    return realType.getMethod(selector);
  }

  /** @see com.ibm.wala.classLoader.IClass#getMethod(Selector) */
  @Override
  public IField getField(Atom name) {
    return realType.getField(name);
  }

  /** @see com.ibm.wala.classLoader.IClass#getSourceFileName() */
  @Override
  public String getSourceFileName() {
    return realType.getSourceFileName();
  }

  /** @see com.ibm.wala.classLoader.IClass#getClassInitializer() */
  @Override
  public IMethod getClassInitializer() {
    return null;
  }

  /** @see com.ibm.wala.classLoader.IClass#getDeclaredMethods() */
  @Override
  public Collection<? extends IMethod> getDeclaredMethods() {
    return realType.getDeclaredMethods();
  }

  /** @see com.ibm.wala.classLoader.IClass#getDeclaredInstanceFields() */
  @Override
  public Collection<IField> getDeclaredInstanceFields() {
    return realType.getDeclaredInstanceFields();
  }

  /** @see com.ibm.wala.classLoader.IClass#getDeclaredStaticFields() */
  @Override
  public Collection<IField> getDeclaredStaticFields() {
    return realType.getDeclaredStaticFields();
  }

  /** @see com.ibm.wala.classLoader.IClass#isInterface() */
  public boolean isSyntheticImplentor() {
    return realType.isInterface();
  }

  @Override
  public String toString() {
    return "<Synthetic "
        + (realType.isInterface() ? "Implementor" : "Subclass")
        + ' '
        + realType
        + '>';
  }

  public IClass getRealType() {
    return realType;
  }

  @Override
  public boolean equals(Object arg0) {
    if (arg0 == null) {
      return false;
    }
    if (arg0.getClass().equals(getClass())) {
      return realType.equals(((BypassSyntheticClass) arg0).realType);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return realType.hashCode() * 1621;
  }

  /** @see com.ibm.wala.classLoader.IClass#getModifiers() */
  @Override
  public int getModifiers() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return 0;
  }

  /** @see com.ibm.wala.classLoader.IClass#isReferenceType() */
  @Override
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  /** @see com.ibm.wala.classLoader.IClass#getDirectInterfaces() */
  @Override
  public Collection<IClass> getDirectInterfaces() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllInstanceFields() */
  @Override
  public Collection<IField> getAllInstanceFields() {
    return realType.getAllInstanceFields();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllStaticFields() */
  @Override
  public Collection<IField> getAllStaticFields() {
    return realType.getAllStaticFields();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllMethods() */
  @Override
  public Collection<? extends IMethod> getAllMethods() {
    return realType.getAllMethods();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllFields() */
  @Override
  public Collection<IField> getAllFields() {
    return realType.getAllFields();
  }

  @Override
  public boolean isPublic() {
    return realType.isPublic();
  }

  @Override
  public boolean isPrivate() {
    return realType.isPrivate();
  }

  @Override
  public Reader getSource() {
    return null;
  }

  @Override
  public Collection<Annotation> getAnnotations() {
    return Collections.emptySet();
  }
}
