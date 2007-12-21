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

import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchyDweller;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;

/**
 *
 * Basic interface for an object that represents a single Java class
 * for analysis purposes, including array classes.
 * 
 * @author sfink
 */
public interface IClass extends IClassHierarchyDweller {

  /**
   * Return the object that represents the defining class loader
   * for this class.
   * @return the object that represents the defining class loader
   * for this class.
   */
  IClassLoader getClassLoader();

  /**
   * Is this class a Java interface?
   * @return boolean
   */
  boolean isInterface();

  /**
   *  @return true iff this class is abstract
   */
  boolean isAbstract();
  
  /**
   *  @return true iff this class is public
   */
  boolean isPublic();
  
  /**
   *  @return true iff this class is static
   */
  boolean isStatic();

  /**
   * Return the integer that encodes the class's modifiers,
   * as defined by the JVM specification
   * @return the integer that encodes the class's modifiers,
   * as defined by the JVM specification
   */
  int getModifiers();

  /**
   * @return the superclass, or null if java.lang.Object
   */
  IClass getSuperclass() throws ClassHierarchyException;

  /**
   * @return Collection of (IClass) interfaces this class directly implements
   * If this class is an interface, returns the interfaces it immediately extends.
   */
  Collection<IClass> getDirectInterfaces() throws ClassHierarchyException;
  
  /**
   * @return Collection of (IClass) interfaces this class implements, including
   * all ancestors of interfaces immediately implemented.
   */
  Collection<IClass> getAllImplementedInterfaces() throws ClassHierarchyException;
  

  /**
   * Finds method matching signature.  Delegates to superclass if not
   * found.
   * 
   * @param selector a method signature
   * @return IMethod from this class matching the signature; null
   *         if not found in this class or any superclass.
   */
  IMethod getMethod(Selector selector);
  
  /**
   * Finds a field.
   */
  IField getField(Atom name);

  /**
   * @return canonical TypeReference corresponding to this class
   */
  TypeReference getReference();

  /**
   * @return String holding the name of the source file that defined
   * this class, or null if none found
   */
  String getSourceFileName();

  /**
   * @return the method that is this class's initializer, or null if none
   */
  IMethod getClassInitializer();

  /**
   *  @return true iff the class is an array class.
   */
  boolean isArrayClass();

  /**
   * @return an Iterator of the IMethods declared by this class.
   */
  Collection<IMethod> getDeclaredMethods();

  /**
   * Compute the instance fields declared by this class or any of
   * its superclasses.
   * 
   * @return Collection of IFields
   * @throws ClassHierarchyException 
   */
  Collection<IField> getAllInstanceFields() throws ClassHierarchyException;
  
  /**
   * Compute the static fields declared by this class or any of
   * its superclasses.
   * 
   * @return Collection of IFields
   * @throws ClassHierarchyException 
   */
  Collection<IField> getAllStaticFields() throws ClassHierarchyException;
  
  /**
   * Compute the instance and static fields declared by this class or
   * any of its superclasses.
   * 
   * @return Collection of IFields
   * @throws ClassHierarchyException 
   */
  Collection<IField> getAllFields() throws ClassHierarchyException;
  
  /**
   * Compute the methods declared by this class or
   * any of its superclasses.
   * 
   * @return Collection of IMethods
   * @throws ClassHierarchyException 
   */
  Collection<IMethod> getAllMethods() throws ClassHierarchyException;
  
  /**
   * Compute the instance fields declared by this class.
   * 
   * @return Collection of IFields
   */
  Collection<IField> getDeclaredInstanceFields();

  /**
   * @return Collection of IField
   */
  Collection<IField> getDeclaredStaticFields();

  /**
   * @return the TypeName for this class
   */
  TypeName getName();

  /**
   * Does 'this' refer to a reference type?  If not, then it refers
   * to a primitive type.
   */
  boolean isReferenceType();


}
