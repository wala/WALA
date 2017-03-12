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
 * This class builds serializable ConstantValue attributes. These attributes are associated with final fields.
 * 
 * After constructing a ConstantValueWriter, you must call setValueCPIndex.
 */
public final class ConstantValueWriter extends ClassWriter.Element {
  final private int attrID;

  private int index = -1;

  final private ClassWriter w;

  /**
   * Build an empty writer.
   * 
   * @throws IllegalArgumentException if w is null
   */
  public ConstantValueWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    this.w = w;
    attrID = w.addCPUtf8("ConstantValue");
  }

  /**
   * Build an writer for a 'long' constant value.
   */
  public ConstantValueWriter(ClassWriter w, long v) {
    this(w);
    setLong(v);
  }

  /**
   * Build an writer for an 'int' constant value.
   */
  public ConstantValueWriter(ClassWriter w, int v) {
    this(w);
    setInt(v);
  }

  /**
   * Build an writer for a 'float' constant value.
   */
  public ConstantValueWriter(ClassWriter w, float v) {
    this(w);
    setFloat(v);
  }

  /**
   * Build an writer for a 'double' constant value.
   */
  public ConstantValueWriter(ClassWriter w, double v) {
    this(w);
    setDouble(v);
  }

  /**
   * Build an writer for a 'String' constant value.
   */
  public ConstantValueWriter(ClassWriter w, String v) {
    this(w);
    setString(v);
  }

  private void verify() {
    if (index < 0) {
      throw new IllegalArgumentException("The value's constant pool index is not set");
    }
  }

  @Override
  public int getSize() {
    verify();
    return 8;
  }

  @Override
  public int copyInto(byte[] buf, int offset) throws IllegalArgumentException {
    verify();
    ClassWriter.setUShort(buf, offset, attrID);
    ClassWriter.setInt(buf, offset + 2, 2);
    ClassWriter.setUShort(buf, offset + 6, index);
    return offset + 8;
  }

  /**
   * Set the constant value to a long.
   */
  public void setLong(long value) {
    this.index = w.addCPLong(value);
  }

  /**
   * Set the constant value to a double.
   */
  public void setDouble(double value) {
    this.index = w.addCPDouble(value);
  }

  /**
   * Set the constant value to an int.
   */
  public void setInt(int value) {
    this.index = w.addCPInt(value);
  }

  /**
   * Set the constant value to a float.
   */
  public void setFloat(float value) {
    this.index = w.addCPFloat(value);
  }

  /**
   * Set the constant value to a String.
   */
  public void setString(String value) {
    if (value == null) {
      throw new IllegalArgumentException("null value");
    }
    this.index = w.addCPString(value);
  }

  /**
   * Set the index of the constant pool item holding the constant value.
   */
  public void setValueCPIndex(int index) throws IllegalArgumentException {
    if (index < 1 || index > 0xFFFF) {
      throw new IllegalArgumentException("Invalid CP index: " + index);
    }

    this.index = index;
  }
}
