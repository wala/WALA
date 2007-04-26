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

/**
 * 
 * A method selector; something like: foo(Ljava/langString;)Ljava/lang/Class;
 * 
 * TODO: Canonicalize these?
 * 
 * @author sfink
 * 
 */
public final class Selector {

  private final Atom name;

  private final Descriptor descriptor;

  public Selector(Atom name, Descriptor descriptor) {
    this.name = name;
    this.descriptor = descriptor;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object obj) {
    // using instanceof is OK because Selector is final
    if (obj instanceof Selector) {
      Selector other = (Selector) obj;
      return name.equals(other.name) && descriptor.equals(other.descriptor);
    } else {
      return false;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode() Cache??
   */
  public int hashCode() {
    return 19 * name.hashCode() + descriptor.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return name.toString() + descriptor.toString();
  }

  public Descriptor getDescriptor() {
    return descriptor;
  }

  public Atom getName() {
    return name;
  }

}
