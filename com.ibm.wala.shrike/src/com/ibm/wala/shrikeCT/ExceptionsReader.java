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
 * This class reads Exceptions attributes.
 */
public final class ExceptionsReader extends AttributeReader {
  /**
   * Build a reader for the attribute 'iter'.
   */
  public ExceptionsReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "Exceptions");

    checkSize(attr, 8);
    int count = cr.getUShort(attr + 6);
    checkSizeEquals(attr + 8, 2 * count);
  }

  /**
   * @return the indices of the constant pool items for the exceptions
   */
  public int[] getRawTable() {
    int count = cr.getUShort(attr + 6);
    int[] r = new int[count];
    for (int i = 0; i < r.length; i++) {
      r[i] = cr.getUShort(attr + 8 + i * 2);
    }
    return r;
  }

  /**
   * @return the classes of exceptions that can be thrown by the method
   */
  public String[] getClasses() throws InvalidClassFileException {
    int count = cr.getUShort(attr + 6);
    String[] r = new String[count];
    ConstantPoolParser cp = cr.getCP();
    for (int i = 0; i < r.length; i++) {
      r[i] = cp.getCPClass(cr.getUShort(attr + 8 + i * 2));
    }
    return r;
  }
}
