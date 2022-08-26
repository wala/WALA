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

import com.ibm.wala.classLoader.ArrayClass;

/** A {@link PointerKey} which represents the contents of an array instance. */
public final class ArrayContentsKey extends AbstractFieldPointerKey implements FilteredPointerKey {
  public ArrayContentsKey(InstanceKey instance) {
    super(instance);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ArrayContentsKey) {
      ArrayContentsKey other = (ArrayContentsKey) obj;
      return instance.equals(other.instance);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 1061 * instance.hashCode();
  }

  @Override
  public String toString() {
    return "[" + instance + "[]]";
  }

  @Override
  public TypeFilter getTypeFilter() {
    return new SingleClassFilter(((ArrayClass) instance.getConcreteType()).getElementClass());
  }
}
