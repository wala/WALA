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
package com.ibm.wala.ipa.cha;

import com.ibm.wala.util.warnings.Warning;

/**
 * A warning for when we get a class not found exception
 */
public class ClassHierarchyWarning extends Warning {

  final String message;

  ClassHierarchyWarning(String message) {
    super(Warning.SEVERE);
    this.message = message;
  }

  @Override
  public String getMsg() {
    return getClass().toString() + " : " + message;
  }

  public static ClassHierarchyWarning create(String message) {
    return new ClassHierarchyWarning(message);
  }
}
