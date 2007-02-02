/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.js.types.*;

import java.util.*;

class Util {
  private static final Collection TYPE_ERROR_EXCEPTIONS =
    Collections.unmodifiableCollection(
      Collections.singleton( JavaScriptTypes.TypeError ));


  public static Collection typeErrorExceptions() {
    return TYPE_ERROR_EXCEPTIONS;
  }

  public static Collection noExceptions() {
    return Collections.EMPTY_SET;
  }

}
