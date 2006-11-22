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
package com.ibm.wala.types;

import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Assertions;

public abstract class MemberReference {

  /**
   * The type reference
   */
  protected final TypeReference declaredClass;

  /**
   * The member name
   */
  protected final Atom name;

  /**
   * Cached hash code for efficiency
   */
  private final int hash;

  /**
   * @param type
   * @param name
   * @param hash
   */
  protected MemberReference(TypeReference type, Atom name, int hash) {
    this.declaredClass = type;
    this.name = name;
    this.hash = hash;
  }

  /**
   * @return the type reference component of this member reference
   */
  public final TypeReference getType() {
    if (Assertions.verifyAssertions) {
      Assertions._assert(declaredClass != null);
    }
    return declaredClass;
  }

  /**
   * @return the member name component of this member reference
   */
  public final Atom getName() {
    return name;
  }

  public abstract String getSignature();

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public final int hashCode() {
    return hash;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public final boolean equals(Object other) {
    // These are canonical
    return this == other;
  }

}
