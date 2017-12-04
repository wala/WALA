/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * An implementation of {@link SSAIndirectionData} specialized for IRs originated from Shrike.
 */
public class ShrikeIndirectionData implements SSAIndirectionData<ShrikeIndirectionData.ShrikeLocalName> {

  /**
   * In Shrike, the only "source" level entities which have names relevant to indirect pointer operations are bytecode locals.
   */
  public static class ShrikeLocalName implements com.ibm.wala.ssa.SSAIndirectionData.Name {
    private final int bytecodeLocalNumber;

    public ShrikeLocalName(int bytecodeLocalNumber) {
      this.bytecodeLocalNumber = bytecodeLocalNumber;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + bytecodeLocalNumber;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ShrikeLocalName other = (ShrikeLocalName) obj;
      if (bytecodeLocalNumber != other.bytecodeLocalNumber)
        return false;
      return true;
    }

    @Override
    public String toString() {
      return "(local:" + bytecodeLocalNumber + ")";
    }
  }

  private final Map<ShrikeLocalName, Integer>[] defs;

  private final Map<ShrikeLocalName, Integer>[] uses;

  @SuppressWarnings("unchecked")
  public ShrikeIndirectionData(int instructionArrayLength) {
    defs = new HashMap[instructionArrayLength];
    uses = new HashMap[instructionArrayLength];
  }

  @Override
  public int getDef(int instructionIndex, ShrikeLocalName name) {
    if (defs[instructionIndex] == null || !defs[instructionIndex].containsKey(name)) {
      return -1;
    } else {
      return defs[instructionIndex].get(name);
    }
  }

  @Override
  public int getUse(int instructionIndex, ShrikeLocalName name) {
    if (uses[instructionIndex] == null || !uses[instructionIndex].containsKey(name)) {
      return -1;
    } else {
      return uses[instructionIndex].get(name);
    }
  }

  @Override
  public void setDef(int instructionIndex, ShrikeLocalName name, int newDef) {
    if (defs[instructionIndex] == null) {
      defs[instructionIndex] = new HashMap<>(2);
    }

    defs[instructionIndex].put(name, newDef);
  }

  @Override
  public void setUse(int instructionIndex, ShrikeLocalName name, int newUse) {
    if (uses[instructionIndex] == null) {
      uses[instructionIndex] = new HashMap<>(2);
    }

    uses[instructionIndex].put(name, newUse);
  }

  @Override
  public Collection<ShrikeLocalName> getNames() {
    HashSet<ShrikeLocalName> result = new HashSet<>();
    for (int i = 0; i < uses.length; i++) {
      if (uses[i] != null) {
        result.addAll(uses[i].keySet());
      }
      if (defs[i] != null) {
        result.addAll(defs[i].keySet());
      }
    }
    return result;
  }

  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < defs.length; i++) {
      if (defs[i] != null) {
        result.append(i + " <- " + defs[i] + "\n");
      }
      if (uses[i] != null) {
        result.append(i + " -> " + uses[i] + "\n");
      }
    }
    return result.toString();
  }
}
