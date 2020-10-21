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
package com.ibm.wala.model;

/** A bogus class to support returning "unknown" objects */
public class SyntheticFactory {

  /**
   * This method should be hijacked.
   *
   * @return some object by reflection
   */
  public static native Object getObject();
}
