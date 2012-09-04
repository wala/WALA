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

  /**
   * context key representing some parameter index, useful, e.g. for CPA-style
   * context-sensitivity policies.
   */
  public static class ParameterKey implements ContextKey {
    public final int index;

    public ParameterKey(int index) {
      super();
      this.index = index;
    }
  }
  
  /**
   * Generally useful constants for possible parameter indices 
   */
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
    new ParameterKey(19),
    new ParameterKey(20),
    new ParameterKey(21),
    new ParameterKey(22),
    new ParameterKey(23),
    new ParameterKey(24),
    new ParameterKey(25),
    new ParameterKey(26),
    new ParameterKey(27),
    new ParameterKey(28),
    new ParameterKey(29),
    new ParameterKey(30),
    new ParameterKey(31),
    new ParameterKey(32),
    new ParameterKey(33),
    new ParameterKey(34),
    new ParameterKey(35),
    new ParameterKey(36),
    new ParameterKey(37),
    new ParameterKey(38),
    new ParameterKey(39),
    new ParameterKey(40),
    new ParameterKey(41),
    new ParameterKey(42),
    new ParameterKey(43),
    new ParameterKey(44),
    new ParameterKey(45),
    new ParameterKey(46),
    new ParameterKey(47),
    new ParameterKey(48),
    new ParameterKey(49)
  };
}
