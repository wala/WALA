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
package com.ibm.wala.j2ee;

import com.ibm.wala.util.warnings.Warning;

/**
 * A warning for when we can't load some reference
 */
public class LoadFailure extends Warning {

  final Object ref;
  LoadFailure(Object ref) {
    super(Warning.SEVERE);
    this.ref = ref;
  }
  public String getMsg() {
    return getClass().toString() + " : " + ref;
  }
  public static LoadFailure create(Object ref) {
    return new LoadFailure(ref);
  }
}