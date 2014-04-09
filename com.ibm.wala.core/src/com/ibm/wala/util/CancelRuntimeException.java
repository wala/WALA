/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util;

/**
 * An exception for when work is canceled in eclipse. This is identical to {@link CancelException}, but this one extends
 * {@link RuntimeException}, so it need not be threaded through every API that uses it.
 */
public class CancelRuntimeException extends RuntimeException {

  protected CancelRuntimeException(String msg) {
    super(msg);
  }

  public CancelRuntimeException(Exception cause) {
    super(cause);
  }

  public static CancelRuntimeException make(String msg) {
    return new CancelRuntimeException(msg);
  }

}
