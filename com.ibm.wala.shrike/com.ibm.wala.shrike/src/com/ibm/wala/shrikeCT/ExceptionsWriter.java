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
  private int attrID;
  private int[] table;

  /**
   * Build an empty writer.
   */
  public ExceptionsWriter(ClassWriter w) {
    attrID = w.addCPUtf8("Exceptions");
  }

  public int getSize() {
    return table == null ? 8 : 8 + table.length * 2;
  }

  public int copyInto(byte[] buf, int offset) {
    ClassWriter.setUShort(buf, offset, attrID);
    ClassWriter.setInt(buf, offset + 2, getSize() - 6);
    ClassWriter.setUShort(buf, offset + 6, table == null ? 0 : table.length);
    offset += 8;
    if (table != null) {
      for (int i = 0; i < table.length; i++) {
        ClassWriter.setUShort(buf, offset, table[i]);
        offset += 2;
      }
    }
    return offset;
  }

  /**
   * Set the list of exceptions that can be thrown.
   * 
   * @param exceptions
   *          an array of indices to constant pool Class entries
   */
  public void setRawTable(int[] exceptions) {
    for (int i = 0; i < exceptions.length; i++) {
      if (exceptions[i] < 1 || exceptions[i] > 0xFFFF) {
        throw new IllegalArgumentException("Invalid CP index: " + exceptions[i]);
      }
    }

    this.table = exceptions;
  }
}