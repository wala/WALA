/*
 * Copyright (c) 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.intset;

public class NumberUtility {

  static boolean isByte(int number) {
    if (number >= Byte.MIN_VALUE && number <= Byte.MAX_VALUE) {
      return true;
    }
    return false;
  }

  static boolean isShort(int number) {
    if (!isByte(number) && number >= Short.MIN_VALUE && number <= Short.MAX_VALUE) {
      return true;
    }
    return false;
  }
}
