/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeCT;

/**
 * This class reads RuntimeInvisibleAnnotations attributes.
 * 
 * @author sjfink
 */
public final class RuntimeInvisibleAnnotationsReader extends AttributeReader {

  /**
   * offset in class file where this attribute begins
   */
  private final int beginOffset;

  public RuntimeInvisibleAnnotationsReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "RuntimeInvisibleAnnotations");
    beginOffset = attr;
  }

  /**
   * @return number of annotations in this attribute
   * @throws InvalidClassFileException
   */
  public int getAnnotationCount() throws InvalidClassFileException {
    int offset = beginOffset + 6;
    checkSize(offset, 2);
    return cr.getUShort(offset);
  }

  /**
   * @return total length of this attribute in bytes, <bf>including</bf> the
   *         first 6 bytes
   * @throws InvalidClassFileException
   */
  public int getAttributeSize() throws InvalidClassFileException {
    int offset = beginOffset + 2;
    checkSize(offset, 4);
    return cr.getInt(offset) + 6;
  }

  /**
   * @return the offsets into the class file of the annotations of this
   *         attribute
   * @throws InvalidClassFileException
   * @throws UnimplementedException 
   */
  public int[] getAnnotationOffsets() throws InvalidClassFileException, UnimplementedException {
    int[] result = new int[getAnnotationCount()];
    int offset = beginOffset + 8;
    for (int i = 0; i < result.length; i++) {
      result[i] = offset;
      offset += getAnnotationSize(offset);
    }
    return result;
  }

  /**
   * @param begin
   *          offset in the constant pool
   * @return the size, in bytes, of the annotation structure starting at a given
   *         offset
   * @throws InvalidClassFileException
   * @throws UnimplementedException 
   */
  private int getAnnotationSize(int begin) throws InvalidClassFileException, UnimplementedException {
    int offset = begin + 2;
    checkSize(offset, 2);
    int numElementValuePairs = cr.getUShort(offset);
    offset += 2;
    for (int i = 0; i < numElementValuePairs; i++) {
      offset += 2;
      offset += getElementValueSize(offset);
    }
    return offset - begin;
  }

  /**
   * @return the size, in bytes, of the element-value structure starting at a
   *         given offset
   * @throws UnimplementedException 
   * 
   */
  private int getElementValueSize(int begin) throws UnimplementedException {
    throw new UnimplementedException();
  }

  /**
   * temporary migration aid until I've implemented everything.
   * 
   * @author sjfink
   * 
   */
  public static class UnimplementedException extends Exception {
  }

  /**
   * @return the type of the annotation stating at a given offset 
   * @throws InvalidClassFileException 
   */
  public String getAnnotationType(int offset) throws InvalidClassFileException {
    checkSize(offset, 2);
    int cpOffset = cr.getUShort(offset);
    return cr.getCP().getCPUtf8(cpOffset);
  }
}