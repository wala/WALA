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
 * This class builds serializable SourceFile attributes.
 * 
 * After constructing a SourceFileWriter, you must call setSourceFileCPIndex.
 */
public final class SourceFileWriter extends ClassWriter.Element {
  final private int attrID;

  private int index = -1;

  /**
   * Build an empty writer.
   * 
   * @throws IllegalArgumentException if w is null
   */
  public SourceFileWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    attrID = w.addCPUtf8("SourceFile");
  }

  private void verify() {
    if (index < 0) {
      throw new IllegalArgumentException("The value's constant pool index is not set");
    }
  }

  @Override
  public int getSize() throws IllegalArgumentException {
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
   * Set the index of the constant pool item holding the source file name.
   */
  public void setSourceFileCPIndex(int index) throws IllegalArgumentException {
    if (index < 1 || index > 0xFFFF) {
      throw new IllegalArgumentException("Invalid CP index: " + index);
    }

    this.index = index;
  }
}
