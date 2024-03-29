/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.classLoader;

import com.ibm.wala.shrike.shrikeBT.ExceptionHandler;
import com.ibm.wala.shrike.shrikeBT.IndirectionData;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import java.util.Collection;
import java.util.Iterator;

/** A method which originated in bytecode, decoded by Shrike */
public interface IBytecodeMethod<I> extends IMethod {

  /**
   * @return the bytecode index corresponding to instruction i in the getInstructions() array
   */
  int getBytecodeIndex(int i) throws InvalidClassFileException;

  /**
   * @return the instuction index i in the getInstructions() array corresponding to the bytecode
   *     index bcIndex
   */
  int getInstructionIndex(int bcIndex) throws InvalidClassFileException;

  /**
   * @return the Shrike representation of the exception handlers
   */
  ExceptionHandler[][] getHandlers() throws InvalidClassFileException;

  /**
   * @return the Shrike instructions decoded from the bytecode
   */
  I[] getInstructions() throws InvalidClassFileException;

  /**
   * there
   *
   * @return the call sites declared in the bytecode for this method
   */
  Collection<CallSiteReference> getCallSites() throws InvalidClassFileException;

  Iterator<FieldReference> getFieldsRead() throws InvalidClassFileException;

  Iterator<FieldReference> getFieldsWritten() throws InvalidClassFileException;

  Iterator<TypeReference> getArraysWritten() throws InvalidClassFileException;

  /**
   * @return the new sites declared in the bytecode for this method
   */
  Collection<NewSiteReference> getNewSites() throws InvalidClassFileException;

  /**
   * @return information about any indirect uses of local variables
   */
  IndirectionData getIndirectionData();

  Collection<Annotation>[] getParameterAnnotations();

  Collection<Annotation> getAnnotations(boolean runtimeInvisible) throws InvalidClassFileException;
}
