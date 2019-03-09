/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package slice;

/** Test for bug described on mailing list by Ravi Chandhran. */
public class Slice8 {

  public static void main(String[] args) {
    process();
  }

  private static void process() {
    int x = getVal();
    doNothing(x);
  }

  private static int getVal() {
    return 3;
  }

  private static void doNothing(int x) {}
}
