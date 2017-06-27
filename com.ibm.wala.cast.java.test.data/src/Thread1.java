/******************************************************************************
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
class R implements Runnable {
  public int i;
	
  R(int i) { this.i = i; }

  
  @Override
  public void run() {
    return;
  }

}

public class Thread1 {
  
  private void test() {
    R r = new R(2);
    Thread t = new Thread(r);
    t.start();
  }

  public static void main(String[] a) {
    (new Thread1()).test();
  }

}


