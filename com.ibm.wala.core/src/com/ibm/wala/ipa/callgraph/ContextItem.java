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

package com.ibm.wala.ipa.callgraph;

/**
 * A placeholder for strong typing.
 */
public interface ContextItem {

  public class Value<T> implements ContextItem {
    private final T v;
    
    public Value(T v) {
      this.v = v;
    }
    
    public T getValue() {
      return v;
    }

    public static <T> Value make(T v) {
      return new Value<T>(v);
    }

  }
}
