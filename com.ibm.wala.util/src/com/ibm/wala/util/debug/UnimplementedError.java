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

package com.ibm.wala.util.debug;

/** Something that's not implemented yet. */
public class UnimplementedError extends Error {
  public static final long serialVersionUID = 20981098918191L;

  public UnimplementedError() {
    super();
  }

  public UnimplementedError(String s) {
    super(s);
  }
}
