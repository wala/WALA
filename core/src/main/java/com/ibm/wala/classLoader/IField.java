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
package com.ibm.wala.classLoader;

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.TypeReference;

/** */
public interface IField extends IMember {

  /** @return the canonical TypeReference of the declared type of the field */
  TypeReference getFieldTypeReference();

  /** @return canonical FieldReference representing this field */
  FieldReference getReference();

  /** Is this field final? */
  boolean isFinal();

  boolean isPrivate();

  boolean isProtected();

  boolean isPublic();

  @Override
  boolean isStatic();

  /** Is this member volatile? */
  boolean isVolatile();
}
