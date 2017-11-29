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

import java.io.UnsupportedEncodingException;

public class SourceDebugExtensionWriter extends ClassWriter.Element {
  final private int attrID;

  private byte[] table;

  public SourceDebugExtensionWriter(ClassWriter w) {
    if (w == null) {
      throw new IllegalArgumentException("w is null");
    }
    attrID = w.addCPUtf8("SourceDebugExtension");
  }

  @Override
  public int getSize() {
    return table == null ? 6 : 6 + table.length;
  }

  @Override
  public int copyInto(byte[] buf, int offset) throws IllegalArgumentException {
    ClassWriter.setUShort(buf, offset, attrID);
    ClassWriter.setInt(buf, offset + 2, getSize() - 6);
    offset += 6;
    if (table != null) {
      for (byte element : table) {
        ClassWriter.setUByte(buf, offset, element);
        offset++;
      }
    }
    return offset;
  }

  public void setRawTable(byte[] sourceDebug) {
    if (sourceDebug == null) {
      throw new IllegalArgumentException("sourceDebug is null");
    }
    for (byte element : sourceDebug) {
      if (element < 1) {
        throw new IllegalArgumentException("Invalid CP index: " + element);
      }
    }
    this.table = sourceDebug;
  }

  public void setDebugInfo(String sourceDebug) {
    if (sourceDebug == null) {
      throw new IllegalArgumentException("sourceDebug is null");
    }
    try {
      byte[] bytes = sourceDebug.getBytes("UTF8");
      setRawTable(bytes);
    } catch (UnsupportedEncodingException e) {
      System.err.println(e);
    }
  }
}
