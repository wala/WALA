/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cfg;

public class MonitorTest {
  void sync1() {
    Object a = new Object();
    synchronized (this) {
      synchronized (a) {
        dummy();
      }
    }
  }

  void sync2() {
    Object a = new Object();
    synchronized (this) {
      synchronized (a) {
        // Nothing here.
      }
    }
  }

  void sync3() {
    Object a = new Object();
    Object b = new Object();
    Object c = new Object();
    synchronized (a) {
      synchronized (b) {
        synchronized (c) {
          dummy();
        }
      }
    }
  }

  void dummy() {
  }
}
