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
public class Casts {

  public static void main(String[] args) {
    (new Casts()).test(args);
  }

  private void test(String[] args) {
    long l1 = Long.parseLong(args[0]);
    int i1 = Integer.parseInt(args[1]);
    short s1 = Short.parseShort(args[2]);
    float f1 = Float.parseFloat(args[3]);
    double d1 = Double.parseDouble(args[4]);
    
    double d2 = d1 + f1;
    double d3 = d1 + l1;
    double d4 = d1 + i1;

    float f2 = f1 + i1;
    float f3 = f1 + s1;

    long l2 = l1 + i1;
    long l3 = l1 + s1;
    
    int i2 = i1 + s1;
  }

}
