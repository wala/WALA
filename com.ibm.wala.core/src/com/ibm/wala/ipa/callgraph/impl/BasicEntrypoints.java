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

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * 
 * Bare bones implementation of entrypoints
 * 
 * @author sfink
 */
public class BasicEntrypoints implements Iterable<Entrypoint> {

  private HashSet<Entrypoint> entrypoints = HashSetFactory.make();

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.Entrypoints#iterator()
   */
  public Iterator<Entrypoint> iterator() {
    return entrypoints.iterator();
  }

  public void add(Entrypoint e) {
    entrypoints.add(e);
  }

  public int size() {
    return entrypoints.size();
  }
}