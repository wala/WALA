/*
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
public class Monitor2 {
  private static boolean randomIsolate() {
    return Math.random() > .5;
  }
  
  int i = 0;

  public Monitor2() { }

  public void incr() { synchronized(this) { i++; } }

  public static void main(String[] a) {
    randomIsolate();
    new Monitor2().incr();
  }
}
