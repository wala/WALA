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
package com.ibm.wala.fixpoint;

/**
 * Constants used in the fixed-point solver framework
 */
public interface FixedPointConstants {

  /**
   * A return value which indicates that a lhs has changed, and the statement might
   * need to be evaluated again.
   */
  final static byte CHANGED = 1;
  /**
   * A return value which indicates that lhs has not changed, and the statement might
   * need to be evaluated again.
   */
  final static byte NOT_CHANGED = 0;
  /**
   * A return value which indicates that lhs has changed, and the statement need not
   * be evaluated again.
   */
  final static byte CHANGED_AND_FIXED = 3;
  /**
   * A return value which indicates that lhs has not changed, and the statement need
   * not be evaluated again.
   */
  final static byte NOT_CHANGED_AND_FIXED = 2;
  
  /**
   * The bit-mask which defines the "CHANGED" flag
   */
  final static int CHANGED_MASK = 0x1;
  /**
   * The bit-mask which defines the "FIXED" flag
   */
  final static int FIXED_MASK = 0x2;
  /**
   * The bit-mask which defines the "SIDE EFFECT" flag
   */
  final static int SIDE_EFFECT_MASK = 0x4;
}
