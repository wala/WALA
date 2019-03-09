/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util;

/**
 * An exception for when work is canceled in eclipse. This version forces every API that uses it to
 * declare it. Use {@code CancelRuntimeException} to avoid the need to declare a cancel exception.
 */
public class CancelException extends Exception {

  private static final long serialVersionUID = 3728159810629412928L;

  protected CancelException(String msg) {
    super(msg);
  }

  public CancelException(Exception cause) {
    super(cause);
  }

  public static CancelException make(String msg) {
    return new CancelException(msg);
  }
}
