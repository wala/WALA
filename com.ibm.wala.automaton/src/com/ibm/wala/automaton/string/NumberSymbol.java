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


public class NumberSymbol extends Symbol implements IValueSymbol {
  public NumberSymbol(String num) {
    super(num);
  }
  
  public NumberSymbol(int num) {
    super(Integer.toString(num));
  }
  
  public NumberSymbol(Integer num) {
    super(num.toString());
  }
  
  public NumberSymbol(long num) {
    super(Long.toString(num));
  }
  
  public NumberSymbol(Long num) {
    super(num.toString());
  }
  
  public NumberSymbol(float num) {
    super(Float.toString(num));
  }
  
  public NumberSymbol(Float num) {
    super(num.toString());
  }
  
  public NumberSymbol(double num) {
    super(Double.toString(num));
  }
  
  public NumberSymbol(Double num) {
    super(num.toString());
  }
  
  public String getIntegerName() {
    String s = getName();
    int idx = s.indexOf('.');
    if (idx == 0) {
      return "0";
    }
    if (idx > 0) {
      return s.substring(0, idx);
    }
    else {
      return s;
    }
  }
  
  public Object value() {
    return new Double(doubleValue());
  }
  
  public int intValue() {
    return Integer.parseInt(getIntegerName());
  }
  
  public long longValue() {
    return Long.parseLong(getIntegerName());
  }
  
  public float floatValue() {
    return Float.parseFloat(getName());
  }
  
  public double doubleValue() {
    return Double.parseDouble(getName());
  }
}
