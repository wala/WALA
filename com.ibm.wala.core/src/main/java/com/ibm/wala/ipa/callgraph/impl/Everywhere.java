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
package com.ibm.wala.ipa.callgraph.impl;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;

/** An object that represent the context everywhere; used for context-insensitive analysis */
public class Everywhere implements Context {

  public static final Everywhere EVERYWHERE = new Everywhere();

  private Everywhere() {}

  /** This context gives no information. */
  @Override
  public ContextItem get(ContextKey name) {
    return null;
  }

  @Override
  public String toString() {
    return "Everywhere";
  }

  /** Don't use default hashCode (java.lang.Object) as it's nondeterministic. */
  @Override
  public int hashCode() {
    return 9851;
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }
}
