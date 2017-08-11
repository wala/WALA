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
 * This class reads LineNumberTable attributes.
 * 
 * Instead of constructing a LineNumberTableReader directly, consider just calling LineNumberTableReader.makeBytecodeToSourceMap for
 * convenient access to aggregate line number data from all the LineNumberTable attributes for a given Code.
 */
public final class LineNumberTableReader extends AttributeReader {
  /**
   * Build a reader for a LineNumberTable attribute.
   */
  public LineNumberTableReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "LineNumberTable");

    int offset = attr + 6;
    checkSize(offset, 2);
    int count = cr.getUShort(offset);
    offset += 2;
    checkSize(offset, count * 4);
  }

  /**
   * @return the raw line number table data, a flattened sequence of (startPC, lineNumber) pairs
   */
  public int[] getRawTable() {
    int count = cr.getUShort(attr + 6);
    int[] r = new int[count * 2];
    int offset = attr + 8;
    for (int i = 0; i < r.length; i++) {
      r[i] = cr.getUShort(offset);
      offset += 2;
    }
    return r;
  }

  /**
   * Construct a "bytecode to source" map for the given code. This method aggregates all the LineNumberTable attributes for the code
   * into one handy data structure.
   * 
   * @return an array mapping each byte of the bytecode bytes to the line number that that byte belongs to, or null if there is no
   *         line number data in the Code
   */
  public static int[] makeBytecodeToSourceMap(CodeReader code) throws InvalidClassFileException, IllegalArgumentException {

    if (code == null) {
      throw new IllegalArgumentException();
    }
    int[] r = null;
    ClassReader cr = code.getClassReader();

    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
    code.initAttributeIterator(iter);
    for (; iter.isValid(); iter.advance()) {
      if (iter.getName().equals("LineNumberTable")) {
        if (r == null) {
          r = new int[code.getBytecodeLength()];
        }

        // check length
        @SuppressWarnings("unused")
        LineNumberTableReader lineNumberTableReader = new LineNumberTableReader(iter);
        int attr = iter.getRawOffset();
        int count = cr.getUShort(attr + 6);
        int offset = attr + 8;
        for (int j = 0; j < count; j++) {
          int startPC = cr.getUShort(offset);
          int lineNum = cr.getUShort(offset + 2);
          offset += 4;

          if (startPC < 0 || startPC >= r.length) {
            throw new InvalidClassFileException(offset, "Invalid bytecode offset " + startPC + " in LineNumberTable");
          }
          r[startPC] = lineNum;
        }
      }
    }

    if (r != null) {
      // fill in gaps in the old line number map
      int last = 0;
      for (int i = 0; i < r.length; i++) {
        int cur = r[i];
        if (cur == 0) {
          r[i] = last;
        } else {
          last = cur;
        }
      }
    }

    return r;
  }
}
