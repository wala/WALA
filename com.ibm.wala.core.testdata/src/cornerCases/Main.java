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

package cornerCases;

import javax.swing.tree.RowMapper;

/**
 * @author sfink
 *     <p>TODO To change the template for this generated type comment go to Window - Preferences -
 *     Java - Code Style - Code Templates
 */
public class Main {

  public static void main(String[] args) {
    testCastToString();
  }

  /** Test bug 38496: propagation of a string constant to a checkcast */
  private static void testCastToString() {
    Object o = "a constant string";
    String s = (String) o;
    s.toString();
  }

  public static class YuckyField {
    RowMapper f;
  }

  /** Bug 38540: type inference crashed on this method when class FontSupport was not found */
  public static Object foo() {
    getRowMapper();
    return new YuckyField().f;
  }

  public static RowMapper getRowMapper() {
    return null;
  }
}
