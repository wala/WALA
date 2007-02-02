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

import java.util.ArrayList;
import java.util.List;


public class StringSymbol extends Symbol implements IValueSymbol {
  public StringSymbol(String name) {
    super(name);
  }
  
  public Object value() {
    return new String(getName());
  }
  
  public List toCharSymbols() {
    List l = new ArrayList();
    String s = getName();
    char cs[] = s.toCharArray();
    if (cs.length == 0) {
      l.add(new CharSymbol(""));
    }
    else {
      for (int i = 0; i < cs.length; i++) {
        /*
        if (cs[i] == '\\') {
          String ss = Character.toString(cs[i]) + Character.toString(cs[i+1]);
          CharSymbol sym = new CharSymbol(ss);
          l.add(sym);
          i++;
        }
        else {
          CharSymbol sym = new CharSymbol(cs[i]);
          l.add(sym);
        }
        */
        CharSymbol sym = new CharSymbol(cs[i]);
        l.add(sym);
      }
    }
    return l;
  }
  
  static public List toCharSymbols(String s) {
    return (new StringSymbol(s)).toCharSymbols();
  }
}
