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

import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

/**
 *
 * Basic interface for an object that represents a single Java method for
 * analysis purposes.
 * 
 * @author sfink
 */
public interface IMethod extends IMember, ContextItem {

  /**
   * Is this method synchronized?
   * 
   * @return boolean
   */
  boolean isSynchronized();

  /**
   * Is this method a class initializer?
   * 
   * @return boolean
   */
  boolean isClinit();

  /**
   * Is this method an object initializer?
   * 
   * @return boolean
   */
  boolean isInit();

  /**
   * Is this method native?
   * 
   * @return boolean
   */
  boolean isNative();

  /**
   * Did someone synthesize this method? (As opposed to reading it from a class
   * file)
   * 
   * @return boolean
   */
  boolean isSynthetic();

  /**
   * Is this method abstract?
   * 
   * @return boolean
   */
  boolean isAbstract();

  /**
   * Is this method private?
   * 
   * @return boolean
   */
  boolean isPrivate();

  /**
   * Is this method protected?
   * 
   * @return boolean
   */
  boolean isProtected();

  /**
   * Is this method public?
   * 
   * @return boolean
   */
  boolean isPublic();

  /**
   * Is this method final?
   * 
   * @return boolean
   */
  boolean isFinal();

  /**
   * @return canonical MethodReference corresponding to this method
   */
  MethodReference getReference();

  /**
   * @return maximum number of JVM locals used by this method
   */
  int getMaxLocals();

  /**
   * @return maximum height of JVM stack used by this method
   */
  int getMaxStackHeight();

  /**
   * @return true iff this method has at least one exception handler
   */
  boolean hasExceptionHandler();

  /**
   * Method getParameterType. By convention, for a non-static method,
   * getParameterType(0) is the this pointer
   */
  TypeReference getParameterType(int i);
  
  /**
   * @return the name of the return type for this method
   */
  TypeReference getReturnType();

  /**
   * Method getNumberOfParameters. This result includes the "this" pointer if
   * applicable
   */
  int getNumberOfParameters();

  /**
   * @return an array of the exception types declared by the throws clause for
   *         this method, or null if there are none
   */
  TypeReference[] getDeclaredExceptions();

  /**
   * @return the source line number corresponding to a particular bytecode
   *         index, or -1 if the information is not available.
   */
  int getLineNumber(int bcIndex);

  /**
   * @return the (source code) name of the local variable of a given number at
   *         the specified program counter, or null if the information is not
   *         available.
   */
  String getLocalVariableName(int bcIndex, int localNumber);

  /**
   * something like:
   * com.foo.bar.createLargeOrder(IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   * 
   * @return String
   */
  public String getSignature();

  /**
   * something like:
   * foo(Ljava/langString;)Ljava/lang/Class;
   * 
   * @return String
   */
  public Selector getSelector();

  /**
   * Method getDescriptor. something like:
   * (IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   * 
   * @return Descriptor
   */
  Descriptor getDescriptor();

  /**
   * @return true iff the local variable table information for this method is
   *         available
   */
  boolean hasLocalVariableTable();
}
