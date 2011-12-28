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

import java.util.Arrays;

import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;

/**
 * Represents a Java 5.0 class file annotation
 */
public class Annotation {
  
  private final TypeReference type;
  private final Pair<TypeReference, Object>[] arguments;
  
  private Annotation(TypeReference type, Pair<TypeReference, Object>[] arguments) {
    this.type = type;
    this.arguments = arguments;
  }
  
  public static Annotation make(TypeReference t, Pair<TypeReference, Object>[] arguments) {
    return new Annotation(t, arguments);
  }

  public static Annotation make(TypeReference t) {
    return make(t, null);
  }

  @Override
  public String toString() {
    StringBuffer sb = new StringBuffer("Annotation type " + type);
    if (arguments != null) {
      sb.append("[");
      for(Pair<TypeReference, Object> arg : arguments) {
        sb.append(" " + arg.fst.getName().getClassName() + ":" + arg.snd);
      }
      sb.append(" ]");
    }
    return sb.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + Arrays.hashCode(arguments);
    result = prime * result + ((type == null) ? 0 : type.hashCode());
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
    Annotation other = (Annotation) obj;
    if (!Arrays.equals(arguments, other.arguments))
      return false;
    if (type == null) {
      if (other.type != null)
        return false;
    } else if (!type.equals(other.type))
      return false;
    return true;
  }

  public Pair<TypeReference, Object>[] getArguments() {
    return arguments;
  }

  public TypeReference getType() {
    return type;
  }

}
