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
package com.ibm.wala.ipa.cha;

import com.ibm.wala.util.WalaException;

/** An exception that means something went wrong when constructing a {@link ClassHierarchy}. */
public class ClassHierarchyException extends WalaException {
  public static final long serialVersionUID = 381093189198391L;

  public ClassHierarchyException(String string) {
    super(string);
  }

  public ClassHierarchyException(String s, Throwable cause) {
    super(s, cause);
  }
}
