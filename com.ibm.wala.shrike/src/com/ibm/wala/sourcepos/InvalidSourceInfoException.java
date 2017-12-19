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
 * InvalidSourceInfoException.java
 *
 * Created on 22. Juni 2005, 22:19
 */

package com.ibm.wala.sourcepos;

/**
 * An {@code InvalidSourceInfoException} is thrown if {@code SourceInfo} could
 * not be initialized. Reasons are an invalid bytecode and a missing
 * CharacterRangeTable.
 * 
 * @author Siegfried Weber
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public class InvalidSourceInfoException extends Exception {

  private static final long serialVersionUID = -5895195422989965097L;

  /**
   * Creates a new instance of <code>InvalidSourceInfoException</code> without
   * detail message.
   */
  public InvalidSourceInfoException() {
  }

  /**
   * Constructs an instance of <code>InvalidSourceInfoException</code> with the
   * specified detail message.
   * 
   * @param msg
   *          the detail message.
   */
  public InvalidSourceInfoException(String msg) {
    super(msg);
  }
}
