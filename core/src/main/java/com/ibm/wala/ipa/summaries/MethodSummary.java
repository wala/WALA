/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.summaries;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.core.util.warnings.Warning;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import java.util.ArrayList;
import java.util.Map;

/** Summary information for a method. */
public class MethodSummary {

  protected static final SSAInstruction[] NO_STATEMENTS = new SSAInstruction[0];

  /** The method summarized */
  private final MethodReference method;

  /** List of statements that define this method summary */
  private ArrayList<SSAInstruction> statements;

  /** Map: value number -&gt; constant */
  private Map<Integer, ConstantValue> constantValues;

  /** Some reason this method summary indicates a problem. */
  private String poison;

  /** An indication of how severe the poison problem is. */
  private byte poisonLevel;

  /** Is this a static method? */
  private boolean isStatic = false;

  /** Is this a "factory" method? */
  private boolean isFactory = false;

  private final int numberOfParameters;

  /** Known names for values */
  private Map<Integer, Atom> valueNames = null;

  public MethodSummary(MethodReference method) {
    this(method, -1);
  }

  public MethodSummary(MethodReference method, int numberOfParameters) {
    if (method == null) {
      throw new IllegalArgumentException("null method");
    }
    this.numberOfParameters = numberOfParameters;
    this.method = method;
  }

  public void setValueNames(Map<Integer, Atom> nameTable) {
    this.valueNames = nameTable;
  }

  public Map<Integer, Atom> getValueNames() {
    return valueNames;
  }

  public Atom getValue(Integer v) {
    return valueNames != null && valueNames.containsKey(v) ? valueNames.get(v) : null;
  }

  public int getNumberOfStatements() {
    return (statements == null ? 0 : statements.size());
  }

  public void addStatement(SSAInstruction statement) {
    if (statements == null) {
      statements = new ArrayList<>();
    }
    statements.add(statement);
  }

  public void addConstant(Integer vn, ConstantValue value) {
    if (constantValues == null) constantValues = HashMapFactory.make(5);
    constantValues.put(vn, value);
  }

  /**
   * Returns the method.
   *
   * @return MethodReference
   */
  public MethodReference getMethod() {
    return method;
  }

  public boolean isNative() {
    // TODO implement this.
    return false;
  }

  public void addPoison(String reason) {
    this.poison = reason;
  }

  public boolean hasPoison() {
    return poison != null;
  }

  public String getPoison() {
    return poison;
  }

  public void setPoisonLevel(byte b) {
    poisonLevel = b;
    assert b == Warning.MILD || b == Warning.MODERATE || b == Warning.SEVERE;
  }

  public byte getPoisonLevel() {
    return poisonLevel;
  }

  public SSAInstruction[] getStatements() {
    return statements == null ? NO_STATEMENTS : statements.toArray(new SSAInstruction[0]);
  }

  public Map<Integer, ConstantValue> getConstants() {
    return constantValues;
  }

  /** @return the number of parameters, including the implicit 'this' */
  public int getNumberOfParameters() {
    if (numberOfParameters >= 0) {
      return numberOfParameters;
    } else {
      return isStatic() ? method.getNumberOfParameters() : method.getNumberOfParameters() + 1;
    }
  }

  public boolean isStatic() {
    return isStatic;
  }

  public void setStatic(boolean b) {
    isStatic = b;
  }

  public TypeReference getReturnType() {
    return method.getReturnType();
  }

  @Override
  public String toString() {
    return "[Summary: " + method + ']';
  }

  /** Note that by convention, getParameterType(0) == this for non-static methods. */
  public TypeReference getParameterType(int i) {
    if (isStatic()) {
      return method.getParameterType(i);
    } else {
      if (i == 0) {
        return method.getDeclaringClass();
      } else {
        return method.getParameterType(i - 1);
      }
    }
  }

  /**
   * Record if this is a "factory" method; meaning it returns some object which we know little about
   * ... usually we'll resolve this based on downstream uses of the object
   */
  public void setFactory(boolean b) {
    this.isFactory = b;
  }

  public boolean isFactory() {
    return isFactory;
  }
}
