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

package com.ibm.wala.ipa.callgraph.impl;

import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * This class represents the union of two sets of {@link Entrypoint}s.
 */
public class ComposedEntrypoints implements Iterable<Entrypoint> {

  final private Set<Entrypoint> entrypoints = HashSetFactory.make();
  
  public ComposedEntrypoints(Iterable<Entrypoint> A, Iterable<Entrypoint> B) {
    if (A == null) {
      throw new IllegalArgumentException("A is null");
    }
    if (B == null) {
      throw new IllegalArgumentException("B is null");
    }
    for (Entrypoint entrypoint : A) {
      entrypoints.add(entrypoint);
    }
    for (Entrypoint entrypoint : B) {
      entrypoints.add(entrypoint);
    }
  }

  @Override
  public Iterator<Entrypoint> iterator() {
    return entrypoints.iterator();
  }

}
