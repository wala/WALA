/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package string;

public class SimpleStringOps {

  private static void whatever(String s) {
    System.out.println(s.substring(5) + " and other garbage");
  }

  public static void main(String[] args) {
    if (args.length > 0) {
      String s = args[0];
      for (int i = 1; i < args.length; i++) {
        s = s + args[i];
      }

      if (s.length() < 6) {
        s = "a silly prefix " + s;
      }

      whatever(s);
    }
  }
}
