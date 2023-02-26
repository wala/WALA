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

package com.ibm.wala.classLoader;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cha.IClassHierarchyDweller;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import java.io.Reader;
import java.util.Collection;
import java.util.NoSuchElementException;

/**
 * Basic interface for an object that represents a single Java class for analysis purposes,
 * including array classes.
 */
public interface IClass extends IClassHierarchyDweller {

  /**
   * Return the object that represents the defining class loader for this class.
   *
   * @return the object that represents the defining class loader for this class.
   */
  IClassLoader getClassLoader();

  /** Is this class a Java interface? */
  boolean isInterface();

  /** @return true iff this class is abstract */
  boolean isAbstract();

  /** @return true iff this class is public */
  boolean isPublic();

  /** @return true iff this class is private */
  boolean isPrivate();

  /** @return true iff this class is synthetic, i.e., compiler-generated */
  boolean isSynthetic();

  /**
   * Return the integer that encodes the class's modifiers, as defined by the JVM specification
   *
   * @return the integer that encodes the class's modifiers, as defined by the JVM specification
   */
  int getModifiers() throws UnsupportedOperationException;

  /**
   * @return the superclass, or null if java.lang.Object
   * @throws IllegalStateException if there's some problem determining the superclass
   */
  IClass getSuperclass();

  /**
   * @return Collection of (IClass) interfaces this class directly implements. If this class is an
   *     interface, returns the interfaces it immediately extends.
   */
  Collection<? extends IClass> getDirectInterfaces();

  /**
   * @return Collection of (IClass) interfaces this class implements, including all ancestors of
   *     interfaces immediately implemented. If this class is an interface, it returns all
   *     super-interfaces.
   */
  Collection<IClass> getAllImplementedInterfaces();

  /**
   * Finds method matching signature. Delegates to superclass if not found.
   *
   * @param selector a method signature
   * @return IMethod from this class matching the signature; null if not found in this class or any
   *     superclass.
   */
  IMethod getMethod(Selector selector);

  /**
   * Finds a field.
   *
   * @throws IllegalStateException if the class contains multiple fields with name {@code name}.
   */
  IField getField(Atom name);

  /** Finds a field, given a name and a type. Returns {@code null} if not found. */
  IField getField(Atom name, TypeName type);

  /** @return canonical TypeReference corresponding to this class */
  TypeReference getReference();

  /**
   * @return String holding the name of the source file that defined this class, or null if none
   *     found
   * @throws NoSuchElementException if this class was generated from more than one source file The
   *     assumption that a class is generated from a single source file is java specific, and will
   *     change in the future. In place of this API, use the APIs in IClassLoader. SJF .. we should
   *     think about this deprecation. postponing deprecation for now.
   */
  String getSourceFileName() throws NoSuchElementException;

  /**
   * @return String representing the source file holding this class, or null if not found
   * @throws NoSuchElementException if this class was generated from more than one source file The
   *     assumption that a class is generated from a single source file is java specific, and will
   *     change in the future. In place of this API, use the APIs in IClassLoader. SJF .. we should
   *     think about this deprecation. postponing deprecation for now.
   */
  Reader getSource() throws NoSuchElementException;

  /** @return the method that is this class's initializer, or null if none */
  IMethod getClassInitializer();

  /** @return true iff the class is an array class. */
  boolean isArrayClass();

  /** @return an Iterator of the IMethods declared by this class. */
  Collection<? extends IMethod> getDeclaredMethods();

  /** Compute the instance fields declared by this class or any of its superclasses. */
  Collection<IField> getAllInstanceFields();

  /** Compute the static fields declared by this class or any of its superclasses. */
  Collection<IField> getAllStaticFields();

  /** Compute the instance and static fields declared by this class or any of its superclasses. */
  Collection<IField> getAllFields();

  /** Compute the methods declared by this class or any of its superclasses. */
  Collection<? extends IMethod> getAllMethods();

  /**
   * Compute the instance fields declared by this class.
   *
   * @return Collection of IFields
   */
  Collection<IField> getDeclaredInstanceFields();

  /** @return Collection of IField */
  Collection<IField> getDeclaredStaticFields();

  /** @return the TypeName for this class */
  TypeName getName();

  /** Does 'this' refer to a reference type? If not, then it refers to a primitive type. */
  boolean isReferenceType();

  /** get annotations, if any */
  Collection<Annotation> getAnnotations();
}
