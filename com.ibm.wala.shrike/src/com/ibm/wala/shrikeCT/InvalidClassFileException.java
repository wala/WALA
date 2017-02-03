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
 * This exception is thrown when we detect that the incoming class file data was not a valid class file.
 */
public class InvalidClassFileException extends Exception {

  private static final long serialVersionUID = -6224203694783674259L;

  final private int offset;

  /**
   * The incoming class file is invalid.
   * 
   * @param offset the offset within the data where the invalidity was detected
   * @param s the reason the data is invalid
   */
  public InvalidClassFileException(int offset, String s) {
    super("Class file invalid at " + offset + ": " + s);
    this.offset = offset;
  }

  /**
   * @return the offset within the data where the problem was detected
   */
  public int getOffset() {
    return offset;
  }
}
