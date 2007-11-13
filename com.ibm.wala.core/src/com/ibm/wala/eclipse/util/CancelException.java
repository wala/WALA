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
package com.ibm.wala.eclipse.util;

/**
 * An exception for when work is canceled in eclipse.
 * 
 * @author sjfink
 * 
 */
public class CancelException extends Exception {

  private CancelException(String msg) {
    super(msg);
  }

  public static CancelException make(String msg) {
    return new CancelException(msg);
  }

}
