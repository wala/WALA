/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util;

/** Runtime exception for some WALA failure. */
public class WalaRuntimeException extends RuntimeException {

  private static final long serialVersionUID = -272544923431659418L;
  /** @param s a message describing the failure */
  public WalaRuntimeException(String s, Throwable cause) {
    super(s, cause);
  }
  /** @param string a message describing the failure */
  public WalaRuntimeException(String string) {
    super(string);
  }
}
