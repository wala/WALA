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
 * This class builds serializable InnerClasses attributes.
 */
public final class InnerClassesWriter extends ClassWriter.Element {
  final private int attrID;

  private int[] table;

  /**
   * Build an empty writer.
   * 
   * @throws IllegalArgumentException if w is null
   */
  public InnerClassesWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    attrID = w.addCPUtf8("InnerClasses");
  }

  @Override
  public int getSize() {
    return table == null ? 8 : 8 + table.length * 2;
  }

  /**
   * Copy the bytes into 'buf' at offset 'offset'.
   * 
   * @return the number of bytes copies, which must be equal to getSize()
   */
  @Override
  public int copyInto(byte[] buf, int offset) throws IllegalArgumentException {
    ClassWriter.setUShort(buf, offset, attrID);
    ClassWriter.setInt(buf, offset + 2, getSize() - 6);
    ClassWriter.setUShort(buf, offset + 6, table == null ? 0 : table.length);
    offset += 8;
    if (table != null) {
      for (int element : table) {
        ClassWriter.setUShort(buf, offset, element);
        offset += 2;
      }
    }
    return offset;
  }

  /**
   * Set the raw values that make up this attribute
   * 
   * @throws IllegalArgumentException if classes is null
   */
  public void setRawTable(int[] classes) throws NullPointerException {
    if (classes == null) {
      throw new IllegalArgumentException("classes is null");
    }
    if (classes.length % 4 != 0) {
      throw new IllegalArgumentException("Invalid raw table length: " + classes.length);
    }
    for (int i = 0; i < classes.length; i += 4) {
      if (classes[i] < 1 || classes[i] > 0xFFFF) {
        throw new IllegalArgumentException("Invalid CP index: " + classes[i]);
      }
      if (classes[i + 1] < 0 || classes[i + 1] > 0xFFFF) {
        throw new IllegalArgumentException("Invalid CP index: " + classes[i]);
      }
      if (classes[i + 2] < 0 || classes[i + 2] > 0xFFFF) {
        throw new IllegalArgumentException("Invalid CP index: " + classes[i]);
      }
    }

    this.table = classes;
  }
}
