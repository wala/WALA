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

/** An pointer key which represents a unique set for a field associated with a set of instances. */
public class InstanceFieldKey extends AbstractFieldPointerKey {
  private final IField field;

  public InstanceFieldKey(InstanceKey instance, IField field) {

    super(instance);
    if (field == null) {
      throw new IllegalArgumentException("field is null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("instance is null");
    }
    this.field = field;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof InstanceFieldKey) {
      if (obj.getClass().equals(getClass())) {
        InstanceFieldKey other = (InstanceFieldKey) obj;
        boolean result = field.equals(other.field) && instance.equals(other.instance);
        return result;
      } else {
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 6229 * field.hashCode() + instance.hashCode();
  }

  @Override
  public String toString() {
    return "[" + instance + ',' + field + ']';
  }

  public IField getField() {
    return field;
  }
}
