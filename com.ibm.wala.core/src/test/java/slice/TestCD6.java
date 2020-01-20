/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package slice;

public class TestCD6 {

  public static void main(String[] args) {
    int i = 0;
    if (someBool()) {
      i = 3;
    } else {
      i = 4;
    }
    doNothing(i);
  }

  public static void doNothing(int i) {}

  public static boolean someBool() {
    return false;
  }
}
