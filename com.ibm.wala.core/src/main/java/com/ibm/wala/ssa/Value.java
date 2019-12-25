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
package com.ibm.wala.ssa;

/**
 * Representation of a particular value which appears in an SSA IR.
 *
 * <p>Clients probably shouldn't use this; it's only public (for now) due to Java's package-based
 * weak module system.
 */
public interface Value {

  /** Is this value a string constant? */
  public boolean isStringConstant();

  /** Is this value a null constant? */
  public boolean isNullConstant();
}
