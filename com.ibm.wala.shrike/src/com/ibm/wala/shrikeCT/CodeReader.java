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
 * This attribute reader reads Code attributes from methods.
 */
public final class CodeReader extends AttributeReader {
  final private int codeLen;

  final private int exnTableLen;

  public CodeReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "Code");

    int offset = attr + 6;
    checkSize(offset, 8);
    codeLen = cr.getInt(offset + 4);
    offset += 8;

    checkSize(offset, codeLen + 2);
    offset += codeLen;
    exnTableLen = cr.getUShort(offset);
    offset += 2;
    checkSize(offset, exnTableLen * 8 + 2);
    offset += exnTableLen * 8;
    int attrCount = cr.getUShort(offset);
    offset += 2;
    for (int i = 0; i < attrCount; i++) {
      checkSize(offset, 6);
      int len = cr.getInt(offset + 2);
      offset += 6;
      checkSize(offset, len);
      offset += len;
    }
  }

  /**
   * @return the maximum stack size used by the code, in words
   */
  public int getMaxStack() {
    return cr.getUShort(attr + 6);
  }

  /**
   * @return the maximum local variable size used by the code, in words
   */
  public int getMaxLocals() {
    return cr.getUShort(attr + 8);
  }

  /**
   * @return the length of the bytecode array, in bytes
   */
  public int getBytecodeLength() {
    return codeLen;
  }

  /**
   * @return the bytecode bytes
   */
  public byte[] getBytecode() {
    byte[] r = new byte[codeLen];
    System.arraycopy(cr.getBytes(), attr + 14, r, 0, r.length);
    return r;
  }

  /**
   * @return the raw exception handler data, a flattened sequence of (startPC, endPC, catchClassIndex, catchPC) tuples
   */
  public int[] getRawHandlers() {
    int[] r = new int[exnTableLen * 4];
    int offset = attr + 14 + codeLen + 2;
    for (int i = 0; i < r.length; i++) {
      r[i] = cr.getUShort(offset);
      offset += 2;
    }
    return r;
  }

  /**
   * Point iter at the list of attributes for this code.
   * 
   * @throws IllegalArgumentException if iter is null
   */
  public void initAttributeIterator(ClassReader.AttrIterator iter) {
    if (iter == null) {
      throw new IllegalArgumentException("iter is null");
    }
    iter.init(cr, attr + 14 + codeLen + 2 + exnTableLen * 8);
  }
}
