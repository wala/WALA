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
 * This class helps emit Code elements.
 * 
 * After constructing a CodeWriter, at least the max stack, max locals and bytecode bytes must be set before it can be used.
 */
public final class CodeWriter extends ClassWriter.Element {
  final private int attrID;

  private int maxLocals = -1;

  private int maxStack = -1;

  private byte[] code;

  private int[] exnHandlers;

  private ClassWriter.Element[] attributes;

  /**
   * Build an empty serializable Code attribute.
   * 
   * @throws IllegalArgumentException if w is null
   */
  public CodeWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    attrID = w.addCPUtf8("Code");
  }

  private void verify() {
    if (maxStack < 0) {
      throw new IllegalArgumentException("maxStack not set");
    }
    if (maxLocals < 0) {
      throw new IllegalArgumentException("maxLocals not set");
    }
    if (code == null) {
      throw new IllegalArgumentException("No bytecodes set");
    }
  }

  public int getCodeLength() throws IllegalStateException {
    if (code == null) {
      throw new IllegalStateException("code not initialized");
    }
    return code.length;
  }

  @Override
  public int getSize() throws IllegalArgumentException {
    verify();

    int size = 14 + code.length + 2 + (exnHandlers == null ? 0 : exnHandlers.length) * 2 + 2;
    if (attributes != null) {
      for (ClassWriter.Element attribute : attributes) {
        size += attribute.getSize();
      }
    }
    return size;
  }

  @Override
  public int copyInto(byte[] buf, int offset) throws IllegalArgumentException {
    verify();

    int start = offset;
    ClassWriter.setUShort(buf, offset, attrID);
    ClassWriter.setUShort(buf, offset + 6, maxStack);
    ClassWriter.setUShort(buf, offset + 8, maxLocals);
    ClassWriter.setInt(buf, offset + 10, code.length);
    offset += 14;
    System.arraycopy(code, 0, buf, offset, code.length);
    offset += code.length;
    ClassWriter.setUShort(buf, offset, (exnHandlers == null ? 0 : exnHandlers.length) / 4);
    offset += 2;
    if (exnHandlers != null) {
      for (int exnHandler : exnHandlers) {
        ClassWriter.setUShort(buf, offset, exnHandler);
        offset += 2;
      }
    }

    ClassWriter.setUShort(buf, offset, (attributes == null ? 0 : attributes.length));
    offset += 2;
    if (attributes != null) {
      for (ClassWriter.Element attribute : attributes) {
        offset = attribute.copyInto(buf, offset);
      }
    }
    ClassWriter.setInt(buf, start + 2, offset - start - 6);
    return offset;
  }

  /**
   * Set the bytecodes for this Code attribute.
   * 
   * @throws IllegalArgumentException if code is null
   */
  public void setCode(byte[] code) throws IllegalArgumentException {
    if (code == null) {
      throw new IllegalArgumentException("code is null");
    }
    if (code.length > 0xFFFF) {
      throw new IllegalArgumentException("Code array is too long: " + code.length);
    }
    if (code.length == 0) {
      throw new IllegalArgumentException("Code array is empty");
    }

    this.code = code;
  }

  /**
   * Set the raw handler data for this Code attribute.
   * 
   * @param exnHandlers a flattened sequence of (startPC, endPC, catchClassIndex, catchPC) tuples
   * @throws IllegalArgumentException if exnHandlers is null
   */
  public void setRawHandlers(int[] exnHandlers) {
    if (exnHandlers == null) {
      throw new IllegalArgumentException("exnHandlers is null");
    }
    if (exnHandlers.length % 4 != 0) {
      throw new IllegalArgumentException("Exception handlers array has bad length: " + exnHandlers.length);
    }
    if (exnHandlers.length / 4 > 0xFFFF) {
      throw new IllegalArgumentException("Too many exception handlers: " + exnHandlers.length / 4);
    }
    for (int i = 0; i < exnHandlers.length; i++) {
      int v = exnHandlers[i];
      if (v < 0 || v > 0xFFFF) {
        throw new IllegalArgumentException("Invalid exception handler entry at " + i);
      }
    }

    this.exnHandlers = exnHandlers;
  }

  /**
   * Set the maximum number of local variable space used, in words, by this Code.
   */
  public void setMaxLocals(int maxLocals) {
    this.maxLocals = maxLocals;
  }

  /**
   * Set the maximum stack size, in words, in this Code.
   */
  public void setMaxStack(int maxStack) {
    this.maxStack = maxStack;
  }

  /**
   * Set the attributes of this Code.
   */
  public void setAttributes(ClassWriter.Element[] attributes) {
    this.attributes = attributes;
  }
}
