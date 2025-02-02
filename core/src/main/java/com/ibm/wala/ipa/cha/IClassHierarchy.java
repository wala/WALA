/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.cha;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.Set;

/** General interface for a type hierarchy */
public interface IClassHierarchy extends Iterable<IClass> {

  ClassLoaderFactory getFactory();

  AnalysisScope getScope();

  IClassLoader[] getLoaders();

  IClassLoader getLoader(ClassLoaderReference loaderRef);

  /**
   * @return true if the add succeeded; false if it failed for some reason
   * @throws IllegalArgumentException if klass is null
   */
  boolean addClass(IClass klass);

  /**
   * @return The number of classes present in the class hierarchy.
   */
  int getNumberOfClasses();

  boolean isRootClass(IClass c);

  IClass getRootClass();

  int getNumber(IClass c);

  /* BEGIN Custom change: remember unresolved classes */
  Set<TypeReference> getUnresolvedClasses();

  /* END Custom change: remember unresolved classes */
  /**
   * Find the possible targets of a call to a method reference
   *
   * @param ref method reference
   * @return the set of IMethods that this call can resolve to.
   * @throws IllegalArgumentException if ref is null
   */
  Set<IMethod> getPossibleTargets(MethodReference ref);

  /**
   * Find the possible targets of a call to a method reference where the receiver is of a certain
   * type
   *
   * @param receiverClass the class of the receiver
   * @param ref method reference
   * @return the set of IMethods that this call can resolve to.
   */
  Set<IMethod> getPossibleTargets(IClass receiverClass, MethodReference ref);

  /**
   * Return the unique receiver of an invocation of method on an object of type {@code
   * m.getDeclaredClass()}. Note that for Java, {@code m.getDeclaredClass()} must represent a
   * <em>class</em>, <em>not</em> an interface. This method does not work for finding inherited
   * methods in interfaces.
   *
   * @return IMethod, or null if no appropriate receiver is found.
   * @throws IllegalArgumentException if m is null
   */
  IMethod resolveMethod(MethodReference m);

  /**
   * @return the canonical IField that represents a given field , or null if none found
   * @throws IllegalArgumentException if f is null
   */
  IField resolveField(FieldReference f);

  /**
   * @return the canonical IField that represents a given field , or null if none found
   * @throws IllegalArgumentException if f is null
   * @throws IllegalArgumentException if klass is null
   */
  IField resolveField(IClass klass, FieldReference f);

  /**
   * Return the unique target of an invocation of method on an object of type receiverClass
   *
   * @param receiverClass type of receiver. Note that for Java, {@code receiverClass} must represent
   *     a <em>class</em>, <em>not</em> an interface. This method does not work for finding
   *     inherited methods in interfaces.
   * @param selector method signature
   * @return Method resolved method abstraction
   * @throws IllegalArgumentException if receiverClass is null
   */
  IMethod resolveMethod(IClass receiverClass, Selector selector);

  /**
   * Load a class using one of the loaders specified for this class hierarchy
   *
   * @return null if can't find the class.
   * @throws IllegalArgumentException if A is null
   */
  IClass lookupClass(TypeReference A);

  // public boolean isSyntheticClass(IClass c);

  boolean isInterface(TypeReference type);

  IClass getLeastCommonSuperclass(IClass A, IClass B);

  TypeReference getLeastCommonSuperclass(TypeReference A, TypeReference B);

  /**
   * Is c a subclass of T?
   *
   * @throws IllegalArgumentException if c is null
   */
  boolean isSubclassOf(IClass c, IClass T);

  /**
   * Does c implement i?
   *
   * @return true iff i is an interface and c is a class that implements i, or c is an interface
   *     that extends i.
   */
  boolean implementsInterface(IClass c, IClass i);

  /** Return set of all subclasses of type in the Class Hierarchy */
  Collection<IClass> computeSubClasses(TypeReference type);

  /**
   * Solely for optimization; return a Collection&lt;TypeReference&gt; representing the subclasses
   * of Error
   *
   * <p>kind of ugly. a better scheme?
   */
  Collection<TypeReference> getJavaLangErrorTypes();

  /**
   * Solely for optimization; return a Collection&lt;TypeReference&gt; representing the subclasses
   * of {@link RuntimeException}
   *
   * <p>kind of ugly. a better scheme?
   */
  Collection<TypeReference> getJavaLangRuntimeExceptionTypes();

  /**
   * @param type an interface
   * @return Set of IClass that represent implementors of the interface
   */
  Set<IClass> getImplementors(TypeReference type);

  /**
   * @return the number of classes that immediately extend klass.
   */
  int getNumberOfImmediateSubclasses(IClass klass);

  /**
   * @return the classes that immediately extend klass.
   */
  Collection<IClass> getImmediateSubclasses(IClass klass);

  /**
   * Does an expression c1 x := c2 y typecheck?
   *
   * <p>i.e. is c2 a subtype of c1?
   *
   * @throws IllegalArgumentException if c1 is null
   * @throws IllegalArgumentException if c2 is null
   */
  boolean isAssignableFrom(IClass c1, IClass c2);

  /**
   * Clear internal caches that may be invalidated by addition of new classes, e.g., a cache of the
   * results of {@link #getPossibleTargets(MethodReference)}.
   */
  void clearCaches();
}
