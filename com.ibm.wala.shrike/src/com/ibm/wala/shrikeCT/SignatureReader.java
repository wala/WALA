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
 * This class reads Signature attributes.
 */
public final class SignatureReader extends AttributeReader {
  /**
   * Build a reader for the attribute 'iter'.
   */
  public SignatureReader(ClassReader.AttrIterator iter) throws InvalidClassFileException {
    super(iter, "Signature");

    checkSizeEquals(attr + 6, 2);
  }

  /**
   * @return the index of the constant pool item holding the value
   */
  public int getSignatureCPIndex() {
    return cr.getUShort(attr + 6);
  }

  public String getSignature() throws InvalidClassFileException {
    return cr.getCP().getCPUtf8(getSignatureCPIndex());
  }
}
