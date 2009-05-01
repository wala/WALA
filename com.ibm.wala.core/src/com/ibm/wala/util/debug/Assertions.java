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
package com.ibm.wala.util.debug;

/**
 * WALA-specific assertion checking.
 * 
 * This may go away in favor of Java language-level assertions.
 */
public class Assertions {

  @Deprecated
  public static final boolean verifyAssertions = true;

  /**
   * @throws UnimplementedError if b == false
   */
  @Deprecated
  public static void _assert(boolean b) throws UnimplementedError {
    assert b;
  }

  /**
   * @throws UnimplementedError if b == false
   */
  @Deprecated
  public static void _assert(boolean b, String string) throws UnimplementedError {
    assert b : string;
  }

  /**
   * An assertion which does not need to be guarded by verifyAssertions. These assertions will be enabled in production!
   * 
   * @throws UnimplementedError if b == false
   */
  public static void productionAssertion(boolean b, String string) throws UnimplementedError {
    if (!b)
      throw new UnimplementedError(string);
  }

  /**
   * An assertion which does not need to be guarded by verifyAssertions. These assertions will be enabled in production!
   * 
   * @throws UnimplementedError if b == false
   */
  public static void productionAssertion(boolean b) throws UnimplementedError {
    if (!b)
      throw new UnimplementedError();
  }

  /**
   * An assertion to call when reaching a point that should not be reached.
   * 
   * @throws UnimplementedError unconditionally
   */
  public static void UNREACHABLE() {
    throw new UnimplementedError();
  }

  /**
   * An assertion to call when reaching a point that should not be reached.
   * 
   * @throws UnimplementedError unconditionally
   */
  public static void UNREACHABLE(String string) {
    throw new UnimplementedError(string);
  }

  /**
   * An assertion to call when reaching a point that should not be reached.
   * 
   * @throws UnimplementedError unconditionally
   */
  public static void UNREACHABLE(Object o) {
    throw new UnimplementedError(o == null ? "" : o.toString());
  }

  /**
   * This is only a convenience method, identical to _assert. Allows the programmer to distinguish preconditions from other
   * assertions.
   * 
   * @throws UnimplementedError if b == false
   */
  @Deprecated
  public static void precondition(boolean b) throws UnimplementedError {
    assert b;
  }

  /**
   * This is only a convenience method, identical to _assert. It allows the programmer to distinguish preconditions from other
   * assertions.
   * 
   * @throws UnimplementedError if b == false
   */
  @Deprecated
  public static void precondition(boolean b, String string) throws UnimplementedError {
    assert b : string;
  }

  /**
   * This is only a convenience method, identical to _assert. Allows the programmer to distinguish postconditions from other
   * assertions.
   * 
   * @throws UnimplementedError if b == false
   */
  @Deprecated
  public static void postcondition(boolean b) throws UnimplementedError {
    assert b;
  }

  /**
   * This is only a convenience method, identical to _assert. It allows the programmer to distinguish postconditions from other
   * assertions.
   * 
   * @throws UnimplementedError if b == false
   */
  @Deprecated
  public static void postcondition(boolean b, String string) throws UnimplementedError {
    assert b : string;
  }
}
