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
 * This class helps emit LineNumberTable attributes.
 */
public final class LineNumberTableWriter extends ClassWriter.Element {
  final private int attrID;

  private int[] rawTable = emptyTable;

  private static final int[] emptyTable = new int[0];

  /**
   * Build an empty LineNumberTable.
   * 
   * @throws IllegalArgumentException if w is null
   */
  public LineNumberTableWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    attrID = w.addCPUtf8("LineNumberTable");
  }

  /**
   * Set the raw table entries. Consider calling LineNumberTableWriter.makeRawTable to build the raw entries.
   * 
   * @param table a flattened sequence of (startPC, lineNumber) pairs
   */
  public void setRawTable(int[] table) {
    if (table == null) {
      table = emptyTable;
    }

    if (table.length % 2 != 0) {
      throw new IllegalArgumentException("Line number table has bad length: " + table.length);
    }
    if (table.length / 2 > 0xFFFF) {
      throw new IllegalArgumentException("Too many line number table entries: " + table.length / 2);
    }
    for (int i = 0; i < table.length; i++) {
      int v = table[i];
      if (v < 0 || v > 0xFFFF) {
        throw new IllegalArgumentException("Bad line number table entry at " + i + ": " + v);
      }
    }

    rawTable = table;
  }

  @Override
  public int getSize() {
    return 8 + rawTable.length * 2;
  }

  @Override
  public int copyInto(byte[] buf, int offset) throws IllegalArgumentException {
    ClassWriter.setUShort(buf, offset, attrID);
    ClassWriter.setInt(buf, offset + 2, 2 + rawTable.length * 2);
    ClassWriter.setUShort(buf, offset + 6, rawTable.length / 2);
    offset += 8;
    for (int element : rawTable) {
      ClassWriter.setUShort(buf, offset, element);
      offset += 2;
    }

    return offset;
  }

  /**
   * @param newLineMap an array indexed by bytecode offset, mapping each bytecode offset to its line number (or 0 if there is no
   *          line or it's not known)
   * @return the line numbers in "raw" format, a flattened sequence of (startPC, lineNumber) pairs
   * @throws IllegalArgumentException if newLineMap == null
   */
  public static int[] makeRawTable(int[] newLineMap) throws IllegalArgumentException {
    if (newLineMap == null) {
      throw new IllegalArgumentException("newLineMap == null");
    }
    int rawCount = 0;
    int last = -1;
    for (int next : newLineMap) {
      if (next != last) {
        rawCount++;
      }
    }
    int[] rawTable = new int[rawCount * 2];
    last = -1;
    int index = 0;
    for (int i = 0; i < newLineMap.length; i++) {
      int next = newLineMap[i];
      if (next != last) {
        rawTable[index] = i;
        rawTable[index + 1] = next;
        index += 2;
      }
    }
    return rawTable;
  }
}
