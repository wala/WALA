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
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;

public class TransitivePrototypeKey extends AbstractFieldPointerKey {

  public String getName() {
    return "transitive prototype of " + getInstanceKey().toString();
  }

  public TransitivePrototypeKey(InstanceKey object) {
    super(object);
  }

  @Override
  public boolean equals(Object x) {
    return (x instanceof TransitivePrototypeKey)
        && ((TransitivePrototypeKey) x).getInstanceKey().equals(getInstanceKey());
  }

  @Override
  public int hashCode() {
    return getInstanceKey().hashCode();
  }

  @Override
  public String toString() {
    return "<proto:" + getName() + '>';
  }
}
