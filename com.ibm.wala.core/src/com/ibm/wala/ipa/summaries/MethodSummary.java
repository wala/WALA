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
package com.ibm.wala.ipa.summaries;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.warnings.Warning;

/**
 * Summary information for a method.
 */
public class MethodSummary {

  protected final static SSAInstruction[] NO_STATEMENTS = new SSAInstruction[0];

  /**
   * The method summarized
   */
  final private MethodReference method;

  /**
   * List of statements that define this method summary
   */
  private ArrayList<SSAInstruction> statements;

  /**
   * Map: value number -&gt; constant
   */
  private Map<Integer, ConstantValue> constantValues;

  /**
   * The next available program counter value.
   */
  private int nextProgramCounter = 0;

  /**
   * Some reason this method summary indicates a problem.
   */
  private String poison;

  /**
   * An indication of how severe the poison problem is.
   */
  private byte poisonLevel;

  /**
   * Is this a static method?
   */
  private boolean isStatic = false;

  /**
   * Is this a "factory" method?
   */
  private boolean isFactory = false;

  public MethodSummary(MethodReference method) {
    if (method == null) {
      throw new IllegalArgumentException("null method");
    }
    this.method = method;
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
    if (constantValues == null)
      constantValues = HashMapFactory.make(5);
    constantValues.put(vn, value);
  }

  /**
   * Returns the method.
   * 
   * @return MethodReference
   */
  public MemberReference getMethod() {
    return method;
  }

  public boolean isNative() {
    // TODO implement this.
    return false;
  }

  /**
   * @param reason
   */
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
    if (statements == null) {
      return NO_STATEMENTS;
    } else {
      SSAInstruction[] result = new SSAInstruction[statements.size()];
      Iterator<SSAInstruction> it = statements.iterator();
      for (int i = 0; i < result.length; i++) {
        result[i] = it.next();
      }
      return result;
    }
  }

  public Map<Integer, ConstantValue> getConstants() {
    return constantValues;
  }

  /**
   * @return the number of parameters, including the implicit 'this'
   */
  public int getNumberOfParameters() {
    return (isStatic()) ? method.getNumberOfParameters() : method.getNumberOfParameters() + 1;
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
    return "[Summary: " + method + "]";
  }

  /**
   * Note that by convention, getParameterType(0) == this for non-static methods.
   */
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

  public int getNextProgramCounter() {
    return nextProgramCounter++;
  }

  /**
   * Record if this is a "factory" method; meaning it returns some object which we know little about ... usually we'll resolve this
   * based on downstream uses of the object
   * 
   * @param b
   */
  public void setFactory(boolean b) {
    this.isFactory = b;
  }

  public boolean isFactory() {
    return isFactory;
  }

}
