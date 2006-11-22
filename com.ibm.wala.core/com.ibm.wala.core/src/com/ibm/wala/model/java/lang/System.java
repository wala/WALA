/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.model.java.lang;

/**
 * @author sfink

 */
public class System {

  /**
   * A simple model of object-array copy
   */
  static void arraycopy(Object src, Object dest) {
    Object[] A = (Object[])src;
    Object[] B = (Object[])dest;
    for (int i = 0; i<A.length; i++) {
      B[i] = A[i];
    }
  }
}
