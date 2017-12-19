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
 * InvalidPositionException.java
 *
 * Created on 21. Juni 2005, 13:26
 */

package com.ibm.wala.sourcepos;

/**
 * An exception for invalid positions.
 * 
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
class InvalidPositionException extends Exception {

  private static final long serialVersionUID = 7949660405524028349L;

  /** possible causes for this exception */
  enum Cause {
    LINE_NUMBER_ZERO, COLUMN_NUMBER_ZERO, LINE_NUMBER_OUT_OF_RANGE, COLUMN_NUMBER_OUT_OF_RANGE
  }

  /** the cause for this exception */
  private Cause cause;

  /**
   * Constructs an instance of <code>InvalidRangeException</code> with the
   * specified cause.
   * 
   * @param c
   *          the cause
   */
  InvalidPositionException(Cause c) {
    cause = c;
  }

  /**
   * Returns the cause for this exception.
   * 
   * @return the cause for this exception
   */
  Cause getThisCause() {
    return cause;
  }
}
