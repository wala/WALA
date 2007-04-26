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
import com.ibm.wala.util.debug.*;

/**
 * 
 * By convention, symbol numbers start at 1 ... the "this" parameter will be
 * symbol number 1 in a virtual method.
 * 
 * @author sfink
 */
public class SymbolTable {

  /**
   * value numbers for parameters to this method
   */
  private int[] parameters;

  /**
   * Mapping from Constant -> value number
   */
  private HashMap<ConstantValue, Integer> constants = HashMapFactory.make(10);

  /**
   * Constructor.
   * 
   * @param numberOfParameters
   *          in the IR .. should be ir.getNumberOfParameters()
   */
  public SymbolTable(int numberOfParameters) {
    parameters = new int[numberOfParameters];
    for (int i = 0; i < parameters.length; i++) {
      parameters[i] = getNewValueNumber();
    }
  }

  /**
   * Values. Note: this class must maintain the following invariant:
   * values.length > nextFreeValueNumber.
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
   * @param o
   *          instance of a Java 'boxed-primitive' class, String or NULL.
   * @return value number for constant.
   */
  int findOrCreateConstant(Object o) {
    ConstantValue v = new ConstantValue(o);
    Integer result = constants.get(v);
    if (result == null) {
      int r = getNewValueNumber();
      result = new Integer(r);
      constants.put(v, result);
      values[r] = v;
    }
    return result.intValue();

  }

  public void setConstantValue(int vn, ConstantValue val) {
    values[vn] = val;
  }

  public void setDefaultValue(int vn, final Object defaultValue) {
    Assertions._assert(values[vn] == null);

    Trace.println("setting default for " + vn + " to " + defaultValue);

    values[vn] = new Value() {
      public boolean isStringConstant() { return false; }

      public boolean isNullConstant() { return false; }

      public int getDefaultValue(SymbolTable symtab) {
	return findOrCreateConstant( defaultValue );
      }
    };
  }

  /**
   * Method getNullConstant.
   * 
   * @return int
   */
  public int getNullConstant() {
    return findOrCreateConstant(null);
  }

  /**
   * Method getConstant.
   * 
   * @param i
   * @return int
   */
  public int getConstant(boolean b) {
    return findOrCreateConstant(new Boolean(b));
  }

  /**
   * Method getConstant.
   * 
   * @param i
   * @return int
   */
  public int getConstant(int i) {
    return findOrCreateConstant(new Integer(i));
  }

  /**
   * Method getConstant.
   * 
   * @param l
   * @return int
   */
  public int getConstant(long l) {
    return findOrCreateConstant(new Long(l));
  }

  /**
   * Method getConstant.
   * 
   * @param f
   * @return int
   */
  public int getConstant(float f) {
    return findOrCreateConstant(new Float(f));
  }

  /**
   * Method getConstant.
   * 
   * @param d
   * @return int
   */
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
   * 
   * @param i
   * @return int
   */
  public int getParameter(int i) throws IllegalArgumentException {
    if (parameters.length <= i) {
      throw new IllegalArgumentException("parameters too small for index " + i + ", length = " + parameters.length);
    }
    return parameters[i];
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
   * ensure that the symbol table has allocated space for the particular value
   * number
   * 
   * @param i
   *          a value number
   */
  public void ensureSymbol(int i) {
    if (i != -1) {
      if (i >= values.length || values[i] == null) {
        if (nextFreeValueNumber <= i) {
          nextFreeValueNumber = i + 1;
        }
        expandForNewValueNumber(i);
      }
    }
  }

  public String getValueString(int valueNumber) {
    if (valueNumber < 0 || valueNumber > getMaxValueNumber() || values[valueNumber] == null) {
      return "v" + valueNumber;
    } else {
      return "v" + valueNumber + ":" + values[valueNumber].toString();
    }
  }

  public boolean isConstant(int v) {
    return v < values.length && values[v] instanceof ConstantValue;
  }

  public boolean isZero(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isZeroConstant();
  }

  public boolean isOne(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isOneConstant();
  }

  public boolean isTrue(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isTrueConstant();
  }

  public boolean isZeroOrFalse(int v) {
    return isZero(v) || isFalse(v);
  }

  public boolean isOneOrTrue(int v) {
    return isOne(v) || isTrue(v);
  }

  public boolean isFalse(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).isFalseConstant();
  }

  public boolean isBooleanOrZeroOneConstant(int v) {
    return isBooleanConstant(v) || isZero(v) || isOne(v);
  }

  public boolean isBooleanConstant(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Boolean;
  }

  public boolean isIntegerConstant(int v) {
    return (values[v] instanceof ConstantValue) && (((ConstantValue) values[v]).getValue() instanceof Integer);
  }

  public boolean isLongConstant(int v) {
    return (values[v] instanceof ConstantValue) && (((ConstantValue) values[v]).getValue() instanceof Long);
  }

  public boolean isFloatConstant(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Float;
  }

  public boolean isDoubleConstant(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Double;
  }

  public boolean isNumberConstant(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof Number;
  }

  public boolean isStringConstant(int v) {
    return (values[v] instanceof ConstantValue) && ((ConstantValue) values[v]).getValue() instanceof String;
  }

  public boolean isNullConstant(int v) {
    return (values.length > v) && (values[v] instanceof ConstantValue) && (((ConstantValue) values[v]).getValue() == null);
  }

  /**
   * Method newPhi.
   * 
   * @param rhs
   * @return int
   */
  public int newPhi(int[] rhs) throws IllegalArgumentException {
    int result = getNewValueNumber();
    SSAPhiInstruction phi = new SSAPhiInstruction(result, (int[]) rhs.clone());
    values[result] = new PhiValue(phi);
    return result;
  }

  /**
   * Return the PhiValue that is associated with a given value number
   */
  public PhiValue getPhiValue(int valueNumber) {
    return (PhiValue) values[valueNumber];
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

  public double getDoubleValue(int v) throws IllegalArgumentException {
    if (!isNumberConstant(v)) {
      throw new IllegalArgumentException("value number " + v + " is not a numeric constant.");
    }

    return ((Number) ((ConstantValue) values[v]).getValue()).doubleValue();
  }

  public Object getConstantValue(int v) throws IllegalArgumentException{
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
   * @return the Value object for given value number or null if we have no
   *         special information about the value
   */
  public Value getValue(int valueNumber) {
    if (Assertions.verifyAssertions) {
      if (valueNumber < 1 || valueNumber >= values.length) {
        Assertions._assert(valueNumber >= 0, "Invalid value number " + valueNumber);
        Assertions._assert(valueNumber < values.length, "Invalid value number " + valueNumber);
      }
    }
    return values[valueNumber];
  }

  /**
   * @param valueNumber
   * @return true iff this valueNumber is a paramter
   */
  public boolean isParameter(int valueNumber) {
    return valueNumber <= getNumberOfParameters();
  }
}
