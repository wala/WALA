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
package com.ibm.wala.util.warnings;

import com.ibm.wala.types.MethodReference;

/**
 *
 * A warning associated with a method
 * 
 * @author sfink
 */
public abstract class MethodWarning extends Warning {

  private final MethodReference method;
  /**
   * @param level
   */
  public MethodWarning(byte level, MethodReference method) {
    super(level);
    this.method = method;
  }
  
  public MethodWarning(MethodReference method) {
    super();
    this.method = method;
  }

  public MethodReference getMethod() {
    return method;
  }
}
