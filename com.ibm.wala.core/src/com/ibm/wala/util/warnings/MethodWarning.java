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

import com.ibm.wala.types.MemberReference;

/**
 * A warning associated with a method
 */
@Deprecated
public abstract class MethodWarning extends Warning {

  private final MemberReference method;

  public MethodWarning(byte level, MemberReference method) {
    super(level);
    this.method = method;
  }
  
  public MethodWarning(MemberReference method) {
    super();
    this.method = method;
  }

  public MemberReference getMethod() {
    return method;
  }

  
}
