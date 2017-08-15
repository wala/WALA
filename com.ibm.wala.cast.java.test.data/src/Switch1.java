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
public class Switch1 {
  public static void main(String[] args) {
    Switch1 s1= new Switch1();
    s1.testOne(args);
    s1.testTwo(args);
    s1.testThree(args);
  }
   
  public void testOne(String[] args) {
    char ch;
    switch(Integer.parseInt(args[0])) {
    case 0: ch=Character.forDigit(Integer.parseInt(args[1]), 10); break;
    case 1: ch=Character.forDigit(Integer.parseInt(args[2]), 10); break;
    case 2: ch=Character.forDigit(Integer.parseInt(args[3]), 10); break;
    case 3: ch=Character.forDigit(Integer.parseInt(args[4]), 10); break;
    default: ch= '?'; break;
    }
    System.out.println(ch);
  }

  public char testTwo(String[] args) {
    switch(Integer.parseInt(args[0])) {
    case 0: return Character.forDigit(Integer.parseInt(args[1]), 10);
    case 1: return Character.forDigit(Integer.parseInt(args[2]), 10);
    case 2: return Character.forDigit(Integer.parseInt(args[3]), 10);
    case 3: return Character.forDigit(Integer.parseInt(args[4]), 10);
    default: return '?';
    }
  }

  @SuppressWarnings("incomplete-switch")
  public void testThree(String[] args) {
    char ch = '?';
    switch(Integer.parseInt(args[0])) {
    case 0: ch=Character.forDigit(Integer.parseInt(args[1]), 10); break;
    case 1: ch=Character.forDigit(Integer.parseInt(args[2]), 10); break;
    case 2: ch=Character.forDigit(Integer.parseInt(args[3]), 10); break;
    case 3: ch=Character.forDigit(Integer.parseInt(args[4]), 10); break;
    }
    System.out.println(ch);
  }

}
