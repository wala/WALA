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
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.propagation.AbstractPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

public class ObjectPropertyCatalogKey extends AbstractPointerKey {
  private final InstanceKey object;

  public String getName() {
    return "catalog of " + object.toString();
  }

  public ObjectPropertyCatalogKey(InstanceKey object) {
    this.object = object;
  }

  @Override
  public boolean equals(Object x) {
    return (x instanceof ObjectPropertyCatalogKey)
        && ((ObjectPropertyCatalogKey) x).object.equals(object);
  }

  @Override
  public int hashCode() {
    return object.hashCode();
  }

  @Override
  public String toString() {
    return '[' + getName() + ']';
  }

  public InstanceKey getObject() {
    return object;
  }
}
