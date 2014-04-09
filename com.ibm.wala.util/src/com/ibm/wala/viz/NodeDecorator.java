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
package com.ibm.wala.viz;

import com.ibm.wala.util.WalaException;

/**
 */
public interface NodeDecorator {
  
  public static final NodeDecorator DEFAULT = new NodeDecorator() {
    public String getLabel(Object o) {
      return o.toString();
    } };
  
  /**
   * @param o
   * @return the String label for node o
   */
  String getLabel(Object o) throws WalaException;
  
}
