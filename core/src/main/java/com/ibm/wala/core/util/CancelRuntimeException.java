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
package com.ibm.wala.core.util;

import com.ibm.wala.util.CancelException;

/**
 * An exception for when work is canceled in eclipse. This is identical to {@link CancelException},
 * but this one extends {@link RuntimeException}, so it need not be threaded through every API that
 * uses it.
 */
public class CancelRuntimeException extends RuntimeException {

  private static final long serialVersionUID = 5859062345002606705L;

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
