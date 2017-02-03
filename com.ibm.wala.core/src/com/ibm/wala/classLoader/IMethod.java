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
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;

/**
 * Basic interface for an object that represents a single Java method for analysis purposes.
 */
public interface IMethod extends IMember, ContextItem {

  /**
   * Is this method synchronized?
   */
  boolean isSynchronized();

  /**
   * Is this method a class initializer?
   */
  boolean isClinit();

  /**
   * Is this method an object initializer?
   */
  boolean isInit();

  /**
   * Is this method native?
   */
  boolean isNative();

  /**
   * Did someone synthesize this method? (As opposed to reading it from a class file)
   */
  boolean isSynthetic();

  /**
   * Is this method abstract?
   */
  boolean isAbstract();

  /**
   * Is this method private?
   */
  boolean isPrivate();

  /**
   * Is this method protected?
   */
  boolean isProtected();

  /**
   * Is this method public?
   */
  boolean isPublic();

  /**
   * Is this method final?
   */
  boolean isFinal();
  
  /**
   * Is this method a bridge method?  See JLS 3rd Edition 15.12.4.5
   */
  boolean isBridge();

  /**
   * @return canonical MethodReference corresponding to this method
   */
  MethodReference getReference();

  /**
   * @return true iff this method has at least one exception handler
   */
  boolean hasExceptionHandler();

  /**
   * By convention, for a non-static method, getParameterType(0) is the this pointer
   */
  TypeReference getParameterType(int i);

  /**
   * @return the name of the return type for this method
   */
  TypeReference getReturnType();

  /**
   * Method getNumberOfParameters. This result includes the "this" pointer if applicable
   */
  int getNumberOfParameters();

  /**
   * @return an array of the exception types declared by the throws clause for this method, or null if there are none
   * @throws InvalidClassFileException
   */
  TypeReference[] getDeclaredExceptions() throws InvalidClassFileException, UnsupportedOperationException;

  /**
   * @return the source line number corresponding to a particular bytecode index, or -1 if the information is not available.
   */
  int getLineNumber(int bcIndex);
/** BEGIN Custom change: precise positions */
  
  public interface SourcePosition extends Comparable {
    int getFirstLine();
    int getLastLine();
    int getFirstCol();
    int getLastCol();
    int getFirstOffset();
    int getLastOffset(); 
  }
  
  SourcePosition getSourcePosition(int instructionIndex) throws InvalidClassFileException;

  SourcePosition getParameterSourcePosition(int paramNum) throws InvalidClassFileException;
/** END Custom change: precise positions */
  
  /**
   * @return the (source code) name of the local variable of a given number at the specified program counter, or null if the
   *         information is not available.
   */
  String getLocalVariableName(int bcIndex, int localNumber);

  /**
   * something like: com.foo.bar.createLargeOrder(IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   */
  public String getSignature();

  /**
   * something like: foo(Ljava/langString;)Ljava/lang/Class;
   */
  public Selector getSelector();

  /**
   * something like: (IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   */
  Descriptor getDescriptor();

  /**
   * @return true iff the local variable table information for this method is available
   */
  boolean hasLocalVariableTable();
}
