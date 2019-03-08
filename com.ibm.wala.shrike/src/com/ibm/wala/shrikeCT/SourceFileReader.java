/*
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrikeCT;

/** This class reads SourceFile attributes. */
public final class SourceFileReader extends AttributeReader {
  /** Build a reader for the attribute 'iter'. */
  public SourceFileReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "SourceFile");

    checkSizeEquals(attr + 6, 2);
  }

  /** @return the index of the constant pool item holding the value */
  public int getSourceFileCPIndex() {
    return cr.getUShort(attr + 6);
  }

  public String getSourceFile() throws IllegalArgumentException, InvalidClassFileException {
    return cr.getCP().getCPUtf8(getSourceFileCPIndex());
  }
}
