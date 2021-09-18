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
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/** A synthetic class for the fake root method. */
public class FakeRootClass extends SyntheticClass {
  public static final TypeReference fakeRootClass(ClassLoaderReference clr) {
    return TypeReference.findOrCreate(clr, TypeName.string2TypeName("Lcom/ibm/wala/FakeRootClass"));
  }

  private Map<Atom, IField> fakeRootStaticFields = null;

  private final Set<IMethod> methods = HashSetFactory.make();

  public FakeRootClass(ClassLoaderReference clr, IClassHierarchy cha) {
    this(fakeRootClass(clr), cha);
  }

  public FakeRootClass(TypeReference typeRef, IClassHierarchy cha) {
    super(typeRef, cha);
  }

  @Override
  public IClassLoader getClassLoader() {
    return getClassHierarchy().getLoader(getReference().getClassLoader());
  }

  public void addMethod(IMethod m) {
    methods.add(m);
  }

  public void addStaticField(final Atom name, final TypeReference fieldType) {
    if (fakeRootStaticFields == null) {
      fakeRootStaticFields = HashMapFactory.make(2);
    }

    fakeRootStaticFields.put(
        name,
        new IField() {
          @Override
          public IClassHierarchy getClassHierarchy() {
            return FakeRootClass.this.getClassHierarchy();
          }

          @Override
          public TypeReference getFieldTypeReference() {
            return fieldType;
          }

          @Override
          public IClass getDeclaringClass() {
            return FakeRootClass.this;
          }

          @Override
          public Atom getName() {
            return name;
          }

          @Override
          public boolean isStatic() {
            return true;
          }

          @Override
          public boolean isVolatile() {
            return false;
          }

          @Override
          public FieldReference getReference() {
            return FieldReference.findOrCreate(FakeRootClass.this.getReference(), name, fieldType);
          }

          @Override
          public boolean isFinal() {
            return false;
          }

          @Override
          public boolean isPrivate() {
            return true;
          }

          @Override
          public boolean isProtected() {
            return false;
          }

          @Override
          public boolean isPublic() {
            return false;
          }

          @Override
          public Collection<Annotation> getAnnotations() {
            return Collections.emptySet();
          }
        });
  }

  /** @see com.ibm.wala.classLoader.IClass#getModifiers() */
  @Override
  public int getModifiers() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /** @see com.ibm.wala.classLoader.IClass#getSuperclass() */
  @Override
  public IClass getSuperclass() throws UnsupportedOperationException {
    return getClassHierarchy().getRootClass();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces() */
  @Override
  public Collection<IClass> getAllImplementedInterfaces() throws UnsupportedOperationException {
    return Collections.emptySet();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces() */
  public Collection<IClass> getAllAncestorInterfaces() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /** @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.types.Selector) */
  @Override
  public IMethod getMethod(Selector selector) throws UnsupportedOperationException {
    for (IMethod m : methods) {
      if (m.getSelector().equals(selector)) {
        return m;
      }
    }
    return null;
  }

  /** @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.types.Selector) */
  @Override
  public IField getField(Atom name) {
    if (fakeRootStaticFields != null) {
      return fakeRootStaticFields.get(name);
    } else {
      return null;
    }
  }

  /** @see com.ibm.wala.classLoader.IClass#getClassInitializer() */
  @Override
  public IMethod getClassInitializer() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /** @see com.ibm.wala.classLoader.IClass#getDeclaredMethods() */
  @Override
  public Collection<IMethod> getDeclaredMethods() throws UnsupportedOperationException {
    return Collections.unmodifiableCollection(methods);
  }

  /** @see com.ibm.wala.classLoader.IClass#getDeclaredInstanceFields() */
  @Override
  public Collection<IField> getDeclaredInstanceFields() throws UnsupportedOperationException {
    return Collections.emptySet();
  }

  /** @see com.ibm.wala.classLoader.IClass#getDeclaredStaticFields() */
  @Override
  public Collection<IField> getDeclaredStaticFields() {
    if (fakeRootStaticFields != null) {
      return fakeRootStaticFields.values();
    } else {
      return Collections.emptySet();
    }
  }

  /** @see com.ibm.wala.classLoader.IClass#isReferenceType() */
  @Override
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  /** @see com.ibm.wala.classLoader.IClass#getDirectInterfaces() */
  @Override
  public Collection<IClass> getDirectInterfaces() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllInstanceFields() */
  @Override
  public Collection<IField> getAllInstanceFields() {
    return Collections.emptySet();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllStaticFields() */
  @Override
  public Collection<IField> getAllStaticFields() {
    return getDeclaredStaticFields();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllMethods() */
  @Override
  public Collection<IMethod> getAllMethods() {
    throw new UnsupportedOperationException();
  }

  /** @see com.ibm.wala.classLoader.IClass#getAllFields() */
  @Override
  public Collection<IField> getAllFields() {
    return getDeclaredStaticFields();
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public Reader getSource() {
    return null;
  }
}
