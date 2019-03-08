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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.IField;

/** An pointer key which represents a unique set for each static field. */
public final class StaticFieldKey extends AbstractPointerKey {
  private final IField field;

  public StaticFieldKey(IField field) {
    if (field == null) {
      throw new IllegalArgumentException("null field");
    }
    this.field = field;
  }

  @Override
  public boolean equals(Object obj) {
    // instanceof is OK because this class is final
    if (obj instanceof StaticFieldKey) {
      StaticFieldKey other = (StaticFieldKey) obj;
      return field.equals(other.field);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 1889 * field.hashCode();
  }

  @Override
  public String toString() {
    return "[" + field + ']';
  }

  public IField getField() {
    return field;
  }
}
