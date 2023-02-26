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

/** Common implementation for {@link InstanceFieldPointerKey} implementations. */
public abstract class AbstractFieldPointerKey extends AbstractPointerKey
    implements InstanceFieldPointerKey {
  protected final InstanceKey instance;

  protected AbstractFieldPointerKey(InstanceKey container) {
    if (container == null) {
      throw new IllegalArgumentException("container is null");
    }
    this.instance = container;
  }

  @Override
  public InstanceKey getInstanceKey() {
    return instance;
  }
}
