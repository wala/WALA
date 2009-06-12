/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.types.annotations;

import com.ibm.wala.types.TypeReference;

/**
 * Represents a Java 5.0 class file annotation
 */
public class Annotation {
  
  private final TypeReference type;
  
  private Annotation(TypeReference type) {
    this.type = type;
  }

  public static Annotation make(TypeReference t) {
    return new Annotation(t);
  }

  @Override
  public String toString() {
    return "Annotation type " + type;
  }

  @Override
  public int hashCode() {
    final int PRIME = 31;
    int result = 1;
    result = PRIME * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final Annotation other = (Annotation) obj;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

  
  public TypeReference getType() {
    return type;
  }

}
