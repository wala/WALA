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
 * This class builds serializable Exceptions attributes.
 */
public final class ExceptionsWriter extends ClassWriter.Element {
  final private int attrID;

  private int[] table;

  /**
   * Build an empty writer.
   * 
   * @throws IllegalArgumentException if w is null
   */
  public ExceptionsWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    attrID = w.addCPUtf8("Exceptions");
  }

  @Override
  public int getSize() {
    return table == null ? 8 : 8 + table.length * 2;
  }

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
   * Set the list of exceptions that can be thrown.
   * 
   * @param exceptions an array of indices to constant pool Class entries
   * @throws IllegalArgumentException if exceptions is null
   */
  public void setRawTable(int[] exceptions) {
    if (exceptions == null) {
      throw new IllegalArgumentException("exceptions is null");
    }
    for (int exception : exceptions) {
      if (exception < 1 || exception > 0xFFFF) {
        throw new IllegalArgumentException("Invalid CP index: " + exception);
      }
    }

    this.table = exceptions;
  }
}
