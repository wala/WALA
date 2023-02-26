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
package com.ibm.wala.core.util.shrike;

import com.ibm.wala.core.util.warnings.Warning;
import com.ibm.wala.types.MemberReference;

/** Utility class to help deal with analysis of exceptions. */
public class Exceptions {

  /** A warning for when we fail to resolve the type for a checkcast */
  public static class MethodResolutionFailure extends Warning {

    final MemberReference method;

    MethodResolutionFailure(byte code, MemberReference method) {
      super(code);
      this.method = method;
    }

    @Override
    public String getMsg() {
      return getClass().toString() + " : " + method;
    }

    public static MethodResolutionFailure moderate(MemberReference method) {
      return new MethodResolutionFailure(Warning.MODERATE, method);
    }

    public static MethodResolutionFailure severe(MemberReference method) {
      return new MethodResolutionFailure(Warning.SEVERE, method);
    }
  }
}
