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
package com.ibm.wala.types;

import com.ibm.wala.core.util.strings.Atom;

/** Abstract superclass of {@link MethodReference} and {@link FieldReference} */
public abstract class MemberReference {

  /** The type that declares this member */
  private final TypeReference declaringClass;

  /** The member name */
  private final Atom name;

  /** Cached hash code for efficiency */
  private final int hash;

  protected MemberReference(TypeReference type, Atom name, int hash) {
    this.declaringClass = type;
    this.name = name;
    this.hash = hash;
  }

  /** @return the member name component of this member reference */
  public final Atom getName() {
    return name;
  }

  public abstract String getSignature();

  @Override
  public final int hashCode() {
    return hash;
  }

  @Override
  public final boolean equals(Object other) {
    // These are canonical
    return this == other;
  }

  /** @return the type that declared this member */
  public TypeReference getDeclaringClass() {
    if (declaringClass == null) {
      // fail eagerly
      throw new NullPointerException();
    }
    return declaringClass;
  }
}
