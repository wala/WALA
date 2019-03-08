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
package com.ibm.wala.ipa.callgraph;

/**
 * A Context is a mapping from a name (ContextKey) to a value (ContextItem)
 *
 * <p>For example, for CFA-1, there is only one name ("caller"); and the context maps "caller" to an
 * IMethod
 *
 * <p>As another example, for CPA, there would be name for each parameter slot ("zero","one","two"),
 * and the Context provides a mapping from this name to a set of types. eg. "one" -&gt;
 * {java.lang.String, java.lang.Date}
 */
public interface Context extends ContextItem {
  /** @return the objects corresponding to a given name */
  ContextItem get(ContextKey name);

  /** @return whether this context has a specific type */
  default boolean isA(Class<? extends Context> type) {
    return type.isInstance(this);
  }
}
