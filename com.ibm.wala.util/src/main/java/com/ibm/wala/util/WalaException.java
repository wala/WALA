/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util;

/** An exception to raise for some WALA failure */
public class WalaException extends Exception {

  private static final long serialVersionUID = 3959226859263419122L;
  /** @param s a message describing the failure */
  public WalaException(String s, Throwable cause) {
    super(s, cause);
  }
  /** @param string a message describing the failure */
  public WalaException(String string) {
    super(string);
  }
}
