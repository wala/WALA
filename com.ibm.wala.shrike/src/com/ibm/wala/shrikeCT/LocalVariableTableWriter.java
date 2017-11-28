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

import java.util.Arrays;

import com.ibm.wala.shrikeBT.Compiler;

/**
 * This class helps emit LocalVariableTable attributes.
 */
public final class LocalVariableTableWriter extends ClassWriter.Element {
  final private int attrID;

  private int[] rawTable = emptyTable;

  private static final int[] emptyTable = new int[0];

  /**
   * Create a blank LocalVariableTable.
   * 
   * @throws IllegalArgumentException if w is null
   */
  public LocalVariableTableWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    attrID = w.addCPUtf8("LocalVariableTable");
  }

  /**
   * Set the raw local variable table values. Consider using LocalVariableTableWriter.makeRawTable to build the raw values.
   * 
   * @param table the raw values, a flattened sequence of (startPC, length, nameIndex, typeIndex, var) tuples
   */
  public void setRawTable(int[] table) {
    if (table == null) {
      table = emptyTable;
    }

    if (table.length % 5 != 0) {
      throw new IllegalArgumentException("Local variable table has bad length: " + table.length);
    }
    if (table.length / 5 > 0xFFFF) {
      throw new IllegalArgumentException("Too many local variable table entries: " + table.length / 5);
    }
    for (int i = 0; i < table.length; i++) {
      int v = table[i];
      if (v < 0 || v > 0xFFFF) {
        throw new IllegalArgumentException("Bad local variable table entry at " + i + ": " + v);
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
    ClassWriter.setUShort(buf, offset + 6, rawTable.length / 5);
    offset += 8;
    for (int element : rawTable) {
      ClassWriter.setUShort(buf, offset, element);
      offset += 2;
    }

    return offset;
  }

  /**
   * Build a raw local variable table from a formatted variable map.
   * 
   * @param varMap an array mapping bytecode offsets to a variable map for each offset; a variable map is a array of 2*localVars
   *          elements, containing a (nameIndex, typeIndex) for each local variable; the pair (0,0) indicates that there is no
   *          information about that local variable at that offset
   * @throws IllegalArgumentException if varMap == null
   */
  public static int[] makeRawTable(int[][] varMap, Compiler.Output output) throws IllegalArgumentException {
    if (varMap == null) {
      throw new IllegalArgumentException("varMap == null");
    }
    try {
      int varCount = 0;
      for (int[] element : varMap) {
        if (element != null) {
          varCount = Math.max(varCount, element.length);
        }
      }
      varCount /= 2;

      int[] entries = new int[20];
      int[] varEnd = new int[varCount];
      Arrays.fill(varEnd, -1);
      int[] lastVector = null;
      int entryCount = 0;
      for (int i = 0; i < varMap.length; i++) {
        if (varMap[i] != lastVector) {
          lastVector = varMap[i];

          if (lastVector != null) {
            for (int k = 0; k < lastVector.length / 2 && k < output.getMaxLocals(); k++) {
              if (lastVector[k * 2] > 0 && i >= varEnd[k]) {
                int entryOffset = entryCount * 5;
                entryCount++;
                if (entryCount * 5 > entries.length) {
                  int[] newEntries = new int[entries.length * 2];
                  System.arraycopy(entries, 0, newEntries, 0, entries.length);
                  entries = newEntries;
                }
                int nameIndex = lastVector[k * 2];
                int typeIndex = lastVector[k * 2 + 1];
                int end = i + 1;
                while (end < varMap.length) {
                  if (varMap[end] == null || k * 2 >= varMap[end].length || varMap[end][k * 2] != nameIndex
                      || varMap[end][k * 2 + 1] != typeIndex) {
                    break;
                  }
                  end++;
                }
                varEnd[k] = end;
                entries[entryOffset] = i;
                entries[entryOffset + 1] = end - i;
                entries[entryOffset + 2] = nameIndex;
                entries[entryOffset + 3] = typeIndex;
                entries[entryOffset + 4] = k;
              }
            }
          }
        }
      }

      if (entryCount == 0) {
        return null;
      } else {
        int[] r = new int[entryCount * 5];
        System.arraycopy(entries, 0, r, 0, r.length);
        return r;
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("malformed varMap", e);
    }
  }
}
