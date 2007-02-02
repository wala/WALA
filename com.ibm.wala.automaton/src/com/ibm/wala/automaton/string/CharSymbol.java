/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.automaton.string;


public class CharSymbol extends Symbol implements IValueSymbol {
  public CharSymbol(String name) {
    super(name);
    char cs[] = name.toCharArray();
    if (cs.length>1) {
      throw(new AssertionError("a single character is expected."));
    }
  }
  
  public CharSymbol(char c) {
    this(Character.toString(c));
  }
  
  public CharSymbol(byte b) {
    this(Character.toString((char)b));
  }
  
  public Object value() {
    return new Character(charValue());
  }
  
  public byte byteValue() {
    return (byte) getName().charAt(0);
  }
  
  public char charValue() {
    return getName().charAt(0);
  }
}
