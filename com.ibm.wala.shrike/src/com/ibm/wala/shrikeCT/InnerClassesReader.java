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
 * This class reads InnerClasses attributes.
 */
public final class InnerClassesReader extends AttributeReader {
  /**
   * Build a reader for the attribute 'iter'.
   */
  public InnerClassesReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "InnerClasses");

    checkSize(attr, 8);
    int count = cr.getUShort(attr + 6);
    checkSizeEquals(attr + 8, 8 * count);
  }

  /**
   * @return the raw values that make up this attribute
   */
  public int[] getRawTable() {
    int count = cr.getUShort(attr + 6);
    int[] r = new int[count * 4];
    for (int i = 0; i < r.length; i++) {
      r[i] = cr.getUShort(attr + 8 + i * 2);
    }
    return r;
  }

  /**
   * @return the names of inner classes this attribute holds information about.
   */
  public String[] getInnerClasses() throws InvalidClassFileException {
    int count = cr.getUShort(attr + 6);
    String[] r = new String[count];
    ConstantPoolParser cp = cr.getCP();
    for (int i = 0; i < r.length; i++) {
      r[i] = cp.getCPClass(cr.getUShort(attr + 8 + i * 8));
    }
    return r;
  }

  /**
   * return the name of the outer class recorded as the enclosing class for a class named s. return null if not found.
   * 
   * @throws InvalidClassFileException
   */
  public String getOuterClass(String s) throws InvalidClassFileException {
    String[] inner = getInnerClasses();
    for (int i = 0; i < inner.length; i++) {
      if (inner[i].equals(s)) {
        int x = cr.getUShort(attr + 8 + i * 8 + 2);
        if (x != 0) {
          ConstantPoolParser cp = cr.getCP();
          return cp.getCPClass(cr.getUShort(attr + 8 + i * 8 + 2));
        }
      }
    }
    return null;
  }

  /**
   * return the mask of flags recorded in the InnerClasses attribute for a class named s. return 0 if not found.
   * 
   * @throws InvalidClassFileException
   */
  public int getAccessFlags(String s) throws InvalidClassFileException {
    String[] inner = getInnerClasses();
    for (int i = 0; i < inner.length; i++) {
      if (inner[i].equals(s)) {
        return cr.getUShort(attr + 8 + i * 8 + 6);
      }
    }
    return 0;
  }

}
