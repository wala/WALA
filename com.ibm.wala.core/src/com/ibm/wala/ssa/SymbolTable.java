/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.HashMap;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * By convention, symbol numbers start at 1 ... the "this" parameter will be symbol number 1 in a virtual method.
 */
public class SymbolTable {

  /**
   * value numbers for parameters to this method
   */
  final private int[] parameters;

  /**
   * Mapping from Constant -> value number
   */
  final private HashMap<ConstantValue, Integer> constants = HashMapFactory.make(10);

  /**
   * @param numberOfParameters in the IR .. should be ir.getNumberOfParameters()
   */
  public SymbolTable(int numberOfParameters) {
    if (numberOfParameters < 0) {
      throw new IllegalArgumentException("Illegal numberOfParameters: " + numberOfParameters);
    }
    parameters = new int[numberOfParameters];
    for (int i = 0; i < parameters.length; i++) {
      parameters[i] = getNewValueNumber();
    }
  }

  /**
   * Values. Note: this class must maintain the following invariant: values.length > nextFreeValueNumber.
   */
  private Value[] values = new Value[5];

  private int nextFreeValueNumber = 1;

  /**
   * Method newSymbol.
   * 
   * @return int
   */
  public int newSymbol() {
    return getNewValueNumber();
  }

  /**
   * Common part of getConstant functions.
   * 
   * @param o instance of a Java 'boxed-primitive' class, String or NULL.
   * @return value number for constant.
   */
  int findOrCreateConstant(Object o) {
    ConstantValue v = new ConstantValue(o);
    Integer result = constants.get(v);
    if (result == null) {
      int r = getNewValueNumber();
      result = Integer.valueOf(r);
      constants.put(v, result);
      values[r] = v;
    }
    return result.intValue();

  }

  public void setConstantValue(int vn, ConstantValue val) {
    try {
      values[vn] = val;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid vn: " + vn);
    }
  }

  public void setDefaultValue(int vn, final Object defaultValue) {
    try {
      Assertions._assert(values[vn] == null);

      values[vn] = new Value() {
        public boolean isStringConstant() {
          return false;
        }

        public boolean isNullConstant() {
          return false;
        }

        public int getDefaultValue(SymbolTable symtab) {
          return findOrCreateConstant(defaultValue);
        }
      };
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid ivn: " + vn);
    }
  }

  public int getNullConstant() {
    return findOrCreateConstant(null);
  }

  public int getConstant(boolean b) {
    return findOrCreateConstant(Boolean.valueOf(b));
  }

  public int getConstant(int i) {
    return findOrCreateConstant(Integer.valueOf(i));
  }

  public int getConstant(long l) {
    return findOrCreateConstant(Long.valueOf(l));
  }

  public int getConstant(float f) {
    return findOrCreateConstant(new Float(f));
  }

  public int getConstant(double d) {
    return findOrCreateConstant(new Double(d));
  }

  public int getConstant(String s) {
    return findOrCreateConstant(s);
  }

  /**
   * Return the value number of the ith parameter
   * 
   * By convention, for a non-static method, the 0th parameter is 'this'
   */
  public int getParameter(int i) throws IllegalArgumentException {
    try {
      return parameters[i];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid i: " + i);
    }
  }

  private void expandForNewValueNumber(int vn) {
    if (vn >= values.length) {
      Value[] temp = values;
      values = new Value[2 * vn];
      System.arraycopy(temp, 0, values, 0, temp.length);
    }
  }

  private int getNewValueNumber() {
    int result = nextFreeValueNumber++;
    expandForNewValueNumber(result);
    return result;
  }

  /**
   * ensure that the symbol table has allocated space for the particular value number
   * 
   * @param i a value number
   */
  public void ensureSymbol(int i) {
    try {
      if (i != -1) {
        if (i >= values.length || values[i] == null) {
          if (nextFreeValueNumber <= i) {
            nextFreeValueNumber = i + 1;
          }
          expandForNewValueNumber(i);
        }
      }
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid i: " + i);
    } 

  }

  public String getValueString(int valueNumber) {
    if (valueNumber < 0 || valueNumber > getMaxValueNumber() || values[valueNumber] == null
        || values[valueNumber] instanceof PhiValue) {
      return "v" + valueNumber;
    } else {
      return "v" + valueNumber + ":" + values[valueNumber].toString();
    }
  }

  public boolean isConstant(int v) {
    try {
      return v < values.length && values[v] instanceof ConstantValue;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isZero(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isZeroConstant();
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isOne(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isOneConstant();
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isTrue(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isTrueConstant();
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isZeroOrFalse(int v) {
    return isZero(v) || isFalse(v);
  }

  public boolean isOneOrTrue(int v) {
    return isOne(v) || isTrue(v);
  }

  public boolean isFalse(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isFalseConstant();
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isBooleanOrZeroOneConstant(int v) {
    return isBooleanConstant(v) || isZero(v) || isOne(v);
  }

  public boolean isBooleanConstant(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Boolean;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isIntegerConstant(int v) {
    try {
      return (values[v] instanceof ConstantValue) && (((ConstantValue) values[v]).getValue() instanceof Integer);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isLongConstant(int v) {
    try {
      return (values[v] instanceof ConstantValue) && (((ConstantValue) values[v]).getValue() instanceof Long);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isFloatConstant(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Float;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isDoubleConstant(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Double;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isNumberConstant(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Number;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isStringConstant(int v) {
    try {
      return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof String;
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  public boolean isNullConstant(int v) {
    try {
      return (values.length > v) && (values[v] instanceof ConstantValue) && (((ConstantValue) values[v]).getValue() == null);
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid v: " + v);
    }
  }

  /**
   * @throws IllegalArgumentException if rhs is null
   */
  public int newPhi(int[] rhs) throws IllegalArgumentException {
    if (rhs == null) {
      throw new IllegalArgumentException("rhs is null");
    }
    int result = getNewValueNumber();
    SSAPhiInstruction phi = new SSAPhiInstruction(result, rhs.clone());
    values[result] = new PhiValue(phi);
    return result;
  }

  /**
   * Return the PhiValue that is associated with a given value number
   */
  public PhiValue getPhiValue(int valueNumber) {
    try {
      return (PhiValue) values[valueNumber];
    } catch (ArrayIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid valueNumber: " + valueNumber);
    }
  }

  public int getMaxValueNumber() {
    // return values.length - 1;
    return nextFreeValueNumber - 1;
  }

  public int[] getParameterValueNumbers() {
    return parameters;
  }

  public int getNumberOfParameters() {
    return parameters.length;
  }

  public String getStringValue(int v) throws IllegalArgumentException {
    if (!isStringConstant(v)) {
      throw new IllegalArgumentException("not a string constant: value number " + v);
    }

    return (String) ((ConstantValue) values[v]).getValue();
  }

  public float getFloatValue(int v) throws IllegalArgumentException {
    if (!isNumberConstant(v)) {
      throw new IllegalArgumentException("value number " + v + " is not a numeric constant.");
    }
    return ((Number) ((ConstantValue) values[v]).getValue()).floatValue();
  }

  public double getDoubleValue(int v) throws IllegalArgumentException {
    if (!isNumberConstant(v)) {
      throw new IllegalArgumentException("value number " + v + " is not a numeric constant.");
    }
    return ((Number) ((ConstantValue) values[v]).getValue()).doubleValue();
  }

  public int getIntValue(int v) throws IllegalArgumentException {
    if (!isNumberConstant(v)) {
      throw new IllegalArgumentException("value number " + v + " is not a numeric constant.");
    }
    return ((Number) ((ConstantValue) values[v]).getValue()).intValue();
  }

  public long getLongValue(int v) throws IllegalArgumentException {
    if (!isNumberConstant(v)) {
      throw new IllegalArgumentException("value number " + v + " is not a numeric constant.");
    }
    return ((Number) ((ConstantValue) values[v]).getValue()).longValue();
  }

  public Object getConstantValue(int v) throws IllegalArgumentException {
    if (!isConstant(v)) {
      throw new IllegalArgumentException("value number " + v + " is not a constant.");
    }

    Object value = ((ConstantValue) values[v]).getValue();
    if (value == null) {
      return null;
    } else {
      return value;
    }
  }

  /**
   * @return the Value object for given value number or null if we have no special information about the value
   */
  public Value getValue(int valueNumber) {
    if (valueNumber < 1 || valueNumber >= values.length) {
      throw new IllegalArgumentException("Invalid value number " + valueNumber);
    }
    return values[valueNumber];
  }

  /**
   * @param valueNumber
   * @return true iff this valueNumber is a parameter
   */
  public boolean isParameter(int valueNumber) {
    return valueNumber <= getNumberOfParameters();
  }
}
