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
 * This just exists to enforce strong typing.
 */
public interface ContextKey {

  /**
   * A property of contexts that might be generally useful: the "caller" method ... used for call-string context schemes.
   */
  public final static ContextKey CALLER = new ContextKey() {
  };

  /**
   * A property of contexts that might be generally useful: the "call site" method ... used for call-string context schemes.
   */
  public final static ContextKey CALLSITE = new ContextKey() {
  };

  /**
   * A property of contexts that might be generally useful: an identifier for the receiver object ... used for object-sensitivity
   * context policies.
   * 
   * Known implementations (ContextItems) for RECEIVER include TypeAbstraction and InstanceKey
   */
  public final static ContextKey RECEIVER = new ContextKey() {
  };

  public static class ParameterKey implements ContextKey {
    public final int index;

    public ParameterKey(int index) {
      super();
      this.index = index;
    }
  }
  
  public static final ContextKey PARAMETERS[] = new ContextKey[]{
    new ParameterKey(0),
    new ParameterKey(1),
    new ParameterKey(2),
    new ParameterKey(3),
    new ParameterKey(4),
    new ParameterKey(5),
    new ParameterKey(6),
    new ParameterKey(7),
    new ParameterKey(8),
    new ParameterKey(9),
    new ParameterKey(10),
    new ParameterKey(11),
    new ParameterKey(12),
    new ParameterKey(13),
    new ParameterKey(14),
    new ParameterKey(15),
    new ParameterKey(16),
    new ParameterKey(17),
    new ParameterKey(18),
    new ParameterKey(19)
  };
}
