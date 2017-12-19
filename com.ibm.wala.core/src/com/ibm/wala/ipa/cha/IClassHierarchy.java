/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.cha;

import java.util.Collection;
import java.util.Set;

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

/**
 * General interface for a type hierarchy
 */
public interface IClassHierarchy extends Iterable<IClass> {

  public ClassLoaderFactory getFactory();

  public AnalysisScope getScope();

  public IClassLoader[] getLoaders();

  public IClassLoader getLoader(ClassLoaderReference loaderRef);

  /**
   * @return true if the add succeeded; false if it failed for some reason
   * @throws IllegalArgumentException if klass is null
   */
  public boolean addClass(IClass klass);

  /**
   * @return The number of classes present in the class hierarchy.
   */
  public int getNumberOfClasses();

  public boolean isRootClass(IClass c);

  public IClass getRootClass();

  public int getNumber(IClass c);

/** BEGIN Custom change: remember unresolved classes */
  public Set<TypeReference> getUnresolvedClasses();
  
/** END Custom change: remember unresolved classes */
  /**
   * Find the possible targets of a call to a method reference
   * 
   * @param ref method reference
   * @return the set of IMethods that this call can resolve to.
   * @throws IllegalArgumentException if ref is null
   */
  public Set<IMethod> getPossibleTargets(MethodReference ref);

  /**
   * Find the possible targets of a call to a method reference where the receiver is of a certain type
   * 
   * @param receiverClass the class of the receiver
   * @param ref method reference
   * @return the set of IMethods that this call can resolve to.
   */
  public Set<IMethod> getPossibleTargets(IClass receiverClass, MethodReference ref);

  /**
   * Return the unique receiver of an invocation of method on an object of type m.getDeclaredClass
   * 
   * @return IMethod, or null if no appropriate receiver is found.
   * @throws IllegalArgumentException if m is null
   */
  public IMethod resolveMethod(MethodReference m);

  /**
   * @return the canonical IField that represents a given field , or null if none found
   * @throws IllegalArgumentException if f is null
   */
  public IField resolveField(FieldReference f);

  /**
   * @return the canonical IField that represents a given field , or null if none found
   * @throws IllegalArgumentException if f is null
   * @throws IllegalArgumentException if klass is null
   */
  public IField resolveField(IClass klass, FieldReference f);

  /**
   * Return the unique receiver of an invocation of method on an object of type declaringClass
   * 
   * @param receiverClass type of receiver
   * @param selector method signature
   * @return Method resolved method abstraction
   * @throws IllegalArgumentException if receiverClass is null
   */
  public IMethod resolveMethod(IClass receiverClass, Selector selector);

  /**
   * Load a class using one of the loaders specified for this class hierarchy
   * 
   * @return null if can't find the class.
   * @throws IllegalArgumentException if A is null
   */
  public IClass lookupClass(TypeReference A);

  // public boolean isSyntheticClass(IClass c);

  public boolean isInterface(TypeReference type);

  public IClass getLeastCommonSuperclass(IClass A, IClass B);

  public TypeReference getLeastCommonSuperclass(TypeReference A, TypeReference B);

  /**
   * Is c a subclass of T?
   * 
   * @throws IllegalArgumentException if c is null
   */
  public boolean isSubclassOf(IClass c, IClass T);

  /**
   * Does c implement i?
   * 
   * @return true iff i is an interface and c is a class that implements i, or c is an interface that extends i.
   */
  public boolean implementsInterface(IClass c, IClass i);

  /**
   * Return set of all subclasses of type in the Class Hierarchy
   */
  public Collection<IClass> computeSubClasses(TypeReference type);

  /**
   * Solely for optimization; return a Collection&lt;TypeReference&gt; representing the subclasses of Error
   * 
   * kind of ugly. a better scheme?
   */
  public Collection<TypeReference> getJavaLangErrorTypes();

  /**
   * Solely for optimization; return a Collection&lt;TypeReference&gt; representing the subclasses of {@link RuntimeException}
   * 
   * kind of ugly. a better scheme?
   */
  public Collection<TypeReference> getJavaLangRuntimeExceptionTypes();

  /**
   * @param type an interface
   * @return Set of IClass that represent implementors of the interface
   */
  public Set<IClass> getImplementors(TypeReference type);

  /**
   * @return the number of classes that immediately extend klass.
   */
  public int getNumberOfImmediateSubclasses(IClass klass);

  /**
   * @return the classes that immediately extend klass.
   */
  public Collection<IClass> getImmediateSubclasses(IClass klass);

  /**
   * Does an expression c1 x := c2 y typecheck?
   * 
   * i.e. is c2 a subtype of c1?
   * 
   * @throws IllegalArgumentException if c1 is null
   * @throws IllegalArgumentException if c2 is null
   */
  public boolean isAssignableFrom(IClass c1, IClass c2);

}
