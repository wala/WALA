/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/*
 * InvalidCRTDataException.java
 *
 * Created on 7. Juni 2005, 11:37
 */

package com.ibm.wala.sourcepos;

import java.util.LinkedList;

/**
 * An exception for invalid data in the CharacterRangeTable.
 * 
 * @see CRTable
 * @see CRTData
 * @see CRTFlags
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
class InvalidCRTDataException extends Exception {

  private static final long serialVersionUID = 1088484553652342438L;

  /** Stores additional information */
  private LinkedList<Object> data;

  /**
   * Creates a new instance of <code>InvalidCRTDataException</code> without
   * detail message.
   */
  InvalidCRTDataException() {
  }

  /**
   * Constructs an instance of <code>InvalidCRTDataException</code> with the
   * specified detail message.
   * 
   * @param msg
   *          the detail message.
   */
  InvalidCRTDataException(String msg) {
    super(msg);
  }

  /**
   * Constructs an instance of <code>InvalidCRTDataException</code> with the
   * specified detail message and additional information.
   * 
   * @param msg
   *          the detail message.
   * @param data
   *          additional information.
   */
  InvalidCRTDataException(String msg, Object... data) {
    super(msg);
    this.data = new LinkedList<>();
    for (Object o : data)
      this.data.add(o);
  }

  /**
   * Returns additional information.
   * 
   * @return additional information
   */
  LinkedList<Object> getData() {
    return data;
  }
}
