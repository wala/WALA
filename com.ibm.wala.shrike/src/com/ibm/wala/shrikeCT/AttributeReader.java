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
 * This is a base class for "attribute readers", the classes which provide access to the contents of attributes.
 */
public abstract class AttributeReader {
  protected final ClassReader cr;

  final protected int attr;

  final protected int length;

  /**
   * Construct a reader for a particular attribute.
   * 
   * @param attr a valid attribute iterator pointing at the attribute to read
   * @param expectedName the name the attribute must have
   */
  protected AttributeReader(ClassReader.AttrIterator attr, String expectedName) throws InvalidClassFileException {
    if (attr == null) {
      throw new IllegalArgumentException("attr cannot be null");
    }
    attr.verifyValid();
    this.cr = attr.cr;
    this.attr = attr.offset;
    this.length = attr.size;

    String n = attr.getName();
    if (expectedName != n && !expectedName.equals(n)) {
      throw new IllegalArgumentException("Attribute " + n + " is not a " + expectedName + " attribute");
    }
  }

  /**
   * @return the class reader the attribute belongs to
   */
  public final ClassReader getClassReader() {
    return cr;
  }

  /**
   * @return the offset of the raw attribute data (including the attribute header)
   */
  public final int getRawOffset() {
    return attr;
  }

  /**
   * @return the size of the raw attribute data (including the attribute header)
   */
  public final int getRawSize() {
    return length;
  }

  /**
   * Ensure that the len bytes starting at offset fall within the attribute data.
   * 
   * @throws InvalidClassFileException if the bytes fall outside the data
   */
  protected final void checkSize(int offset, int len) throws InvalidClassFileException {
    if (length < offset - attr + len) {
      throw new InvalidClassFileException(offset, "Attribute data too short, expected " + len + " bytes, got "
          + (length + attr - offset));
    }
  }

  /**
   * Ensure that the len bytes starting at offset end at the end of the attribute data.
   * 
   * @throws InvalidClassFileException if the bytes do not end at the end of the attribute
   */
  protected final void checkSizeEquals(int offset, int len) throws InvalidClassFileException {
    if (length != offset - attr + len) {
      throw new InvalidClassFileException(offset, "Attribute data invalid length, expected " + len + " bytes, got "
          + (length + attr - offset));
    }
  }
}
