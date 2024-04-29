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
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;
import java.util.Collections;

class Util {
  private static final Collection<TypeReference> TYPE_ERROR_EXCEPTIONS =
      Collections.singleton(JavaScriptTypes.TypeError);

  public static Collection<TypeReference> typeErrorExceptions() {
    return TYPE_ERROR_EXCEPTIONS;
  }

  public static Collection<TypeReference> noExceptions() {
    return Collections.emptySet();
  }
}
