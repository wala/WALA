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
 * This class reads LocalVariableTable attributes.
 * 
 * Instead of constructing a LocalVariableTable directly, consider just calling LocalVariableTable.makeVarMap for convenient access
 * to aggregate local variable data from all the LocalVariableTable attributes for a given Code.
 */
public final class LocalVariableTableReader extends AttributeReader {
  public LocalVariableTableReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "LocalVariableTable");

    int offset = attr + 6;
    checkSize(offset, 2);
    int count = cr.getUShort(offset);
    offset += 2;
    checkSize(offset, count * 10);
  }

  /**
   * @return the raw line number table data, a flattened sequence of (startPC, PClength, nameIndex, typeIndex, var) tuples
   */
  public int[] getRawTable() {
    int count = cr.getUShort(attr + 6);
    int[] r = new int[count * 5];
    int offset = attr + 8;
    for (int i = 0; i < r.length; i++) {
      r[i] = cr.getUShort(offset);
      offset += 2;
    }
    return r;
  }

  private static int[] makeVarVector(int[] curVector, int varIndex, int nameIndex, int typeIndex) {
    int[] newVector;
    if (curVector == null) {
      newVector = new int[(varIndex + 1) * 2];
    } else {
      newVector = new int[Math.max(curVector.length, (varIndex + 1) * 2)];
      System.arraycopy(curVector, 0, newVector, 0, curVector.length);
    }
    newVector[varIndex * 2] = nameIndex;
    newVector[varIndex * 2 + 1] = typeIndex;
    return newVector;
  }

  /**
   * @return an array mapping bytecode offsets to arrays representing the local variable maps for each offset; a local variable map
   *         is represented as an array of localVars*2 elements, containing a pair (nameIndex, typeIndex) for each local variable; a
   *         pair (0,0) indicates there is no information for that local variable at that offset
   */
  public static int[][] makeVarMap(CodeReader code) throws InvalidClassFileException, IllegalArgumentException {
    if (code == null) {
      throw new IllegalArgumentException();
    }

    int[][] r = null;
    ClassReader cr = code.getClassReader();

    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
    code.initAttributeIterator(iter);
    for (; iter.isValid(); iter.advance()) {
      if (iter.getName().equals("LocalVariableTable")) {
        if (r == null) {
          r = new int[code.getBytecodeLength()][];
        }

        // check length
        @SuppressWarnings("unused")
        LocalVariableTableReader localVariableTableReader = new LocalVariableTableReader(iter);
        int attr = iter.getRawOffset();
        int count = cr.getUShort(attr + 6);
        int offset = attr + 8;
        for (int j = 0; j < count; j++) {
          int startPC = cr.getUShort(offset);
          int length = cr.getUShort(offset + 2);
          int nameIndex = cr.getUShort(offset + 4);
          int typeIndex = cr.getUShort(offset + 6);
          int varIndex = cr.getUShort(offset + 8);
          offset += 10;

          if (varIndex < 0) {
            throw new InvalidClassFileException(offset, "Invalid variable index " + varIndex + " in LocalVariableTable");
          } else if (startPC < 0) {
            throw new InvalidClassFileException(offset, "Invalid startPC " + startPC + " in LocalVariableTable");
          } else if (startPC + length > r.length) {
            throw new InvalidClassFileException(offset, "Invalid startPC+length " + (startPC + length) + " > " + r.length
                + " in LocalVariableTable");
          }

          for (int k = startPC; k < startPC + length; k++) {
            int[] newVector = makeVarVector(r[k], varIndex, nameIndex, typeIndex);
            r[k] = newVector;
          }
        }
      }
    }

    return r;
  }
}
