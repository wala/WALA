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

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SyntheticClass;
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
import com.ibm.wala.util.strings.Atom;

/**
 * A synthetic class for the fake root method
 */
public class FakeRootClass extends SyntheticClass {
  public static final TypeReference FAKE_ROOT_CLASS = TypeReference.findOrCreate(ClassLoaderReference.Primordial, TypeName
      .string2TypeName("Lcom/ibm/wala/FakeRootClass"));

  private Map<Atom, IField> fakeRootStaticFields = null;

  private Set<IMethod> methods = HashSetFactory.make();

  public FakeRootClass(IClassHierarchy cha) {
    this(FAKE_ROOT_CLASS, cha);
  }

  public FakeRootClass(TypeReference typeRef, IClassHierarchy cha) {
    super(typeRef, cha);
  }

  public void addMethod(IMethod m) {
    methods.add(m);
  }

  public void addStaticField(final Atom name, final TypeReference fieldType) {
    if (fakeRootStaticFields == null) {
      fakeRootStaticFields = HashMapFactory.make(2);
    }

    fakeRootStaticFields.put(name, new IField() {
      public IClassHierarchy getClassHierarchy() {
        return FakeRootClass.this.getClassHierarchy();
      }

      public TypeReference getFieldTypeReference() {
        return fieldType;
      }

      public IClass getDeclaringClass() {
        return FakeRootClass.this;
      }

      public Atom getName() {
        return name;
      }

      public boolean isStatic() {
        return true;
      }

      public boolean isVolatile() {
        return false;
      }

      public FieldReference getReference() {
        return FieldReference.findOrCreate(FAKE_ROOT_CLASS, name, fieldType);
      }

      public boolean isFinal() {
        return false;
      }

      public boolean isPrivate() {
        return true;
      }

      public boolean isProtected() {
        return false;
      }

      public boolean isPublic() {
        return false;
      }

      public Collection<Annotation> getAnnotations() {
        return Collections.emptySet();
      }
    });
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getModifiers()
   */
  public int getModifiers() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getSuperclass()
   */
  public IClass getSuperclass() throws UnsupportedOperationException {
    return getClassHierarchy().getRootClass();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllImplementedInterfaces()
   */
  public Collection<IClass> getAllImplementedInterfaces() throws UnsupportedOperationException {
    return Collections.emptySet();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllAncestorInterfaces()
   */
  public Collection<IClass> getAllAncestorInterfaces() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.classLoader.Selector)
   */
  public IMethod getMethod(Selector selector) throws UnsupportedOperationException {
    for (IMethod m : methods) {
      if (m.getSelector().equals(selector)) {
        return m;
      }
    }
    return null;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getMethod(com.ibm.wala.classLoader.Selector)
   */
  public IField getField(Atom name) {
    if (fakeRootStaticFields != null) {
      return fakeRootStaticFields.get(name);
    } else {
      return null;
    }
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getClassInitializer()
   */
  public IMethod getClassInitializer() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredMethods()
   */
  public Collection<IMethod> getDeclaredMethods() throws UnsupportedOperationException {
    return Collections.unmodifiableCollection(methods);
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredInstanceFields()
   */
  public Collection<IField> getDeclaredInstanceFields() throws UnsupportedOperationException {
    return Collections.emptySet();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDeclaredStaticFields()
   */
  public Collection<IField> getDeclaredStaticFields() {
    if (fakeRootStaticFields != null) {
      return fakeRootStaticFields.values();
    } else {
      return Collections.emptySet();
    }
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#isReferenceType()
   */
  public boolean isReferenceType() {
    return getReference().isReferenceType();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getDirectInterfaces()
   */
  public Collection<IClass> getDirectInterfaces() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllInstanceFields()
   */
  public Collection<IField> getAllInstanceFields()  {
    return Collections.emptySet();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllStaticFields()
   */
  public Collection<IField> getAllStaticFields() {
    return getDeclaredStaticFields();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllMethods()
   */
  public Collection<IMethod> getAllMethods()  {
    throw new UnsupportedOperationException();
  }

  /*
   * @see com.ibm.wala.classLoader.IClass#getAllFields()
   */
  public Collection<IField> getAllFields()  {
    return getDeclaredStaticFields();
  }

  public boolean isPublic() {
    return false;
  }
  
  public boolean isPrivate() {
    return false;
  }

  @Override
  public InputStream getSource() {
    return null;
  }
}
