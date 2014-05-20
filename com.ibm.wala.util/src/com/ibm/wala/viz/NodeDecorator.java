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
 * @param <T> the node type
 */
public interface NodeDecorator<T> {
  
  /**
   * @param n
   * @return the String label for node n
   */
  String getLabel(T n) throws WalaException;
  
}
