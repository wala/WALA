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
package com.ibm.wala.util.ssa;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.ssa.SSAInstruction;

public class InstructionByIIndexMap<Instruction extends SSAInstruction, T> implements Map<Instruction, T> {
  private Map<InstructionByIIndexWrapper<Instruction>, T> map;

  public InstructionByIIndexMap(Map<InstructionByIIndexWrapper<Instruction>, T> map) {
    this.map = map;
  }

  public InstructionByIIndexMap() {
    this.map = new LinkedHashMap<>();
  }

  @Override
  public int size() {
    return map.size();
  }

  @Override
  public boolean isEmpty() {
    return map.isEmpty();
  }

  @Override
  public boolean containsKey(Object key) {
    if (key instanceof SSAInstruction) {
      SSAInstruction instruction = (SSAInstruction) key;
      if (instruction.iindex >= 0) {
        return map.containsKey(new InstructionByIIndexWrapper<>(instruction));
      }
    }
    return false;
  }

  @Override
  public boolean containsValue(Object value) {
    return map.containsValue(value);
  }

  @Override
  public T get(Object key) {
    if (key instanceof SSAInstruction) {
      SSAInstruction instruction = (SSAInstruction) key;
      if (instruction.iindex >= 0) {
        return map.get(new InstructionByIIndexWrapper<>(instruction));
      }
    }
    return null;
  }

  @Override
  public T put(Instruction key, T value) {
    return map.put(new InstructionByIIndexWrapper<>(key), value);
  }

  @Override
  public T remove(Object key) {
    if (key instanceof SSAInstruction) {
      SSAInstruction instruction = (SSAInstruction) key;
      if (instruction.iindex >= 0) {
        return map.remove(new InstructionByIIndexWrapper<>(instruction));
      }
    }
    return null;
  }

  @Override
  public void putAll(Map<? extends Instruction, ? extends T> m) {
    for (java.util.Map.Entry<? extends Instruction, ? extends T> entry:m.entrySet()) {
      this.put(entry.getKey(), entry.getValue());
    }
  }

  @Override
  public void clear() {
    map.clear();
  }

  @Override
  public Set<Instruction> keySet() {
    Set<Instruction> result = new LinkedHashSet<>();
    for (InstructionByIIndexWrapper<Instruction> wrapper : map.keySet()) {
      result.add(wrapper.getInstruction());
    }

    return result;
  }

  @Override
  public Collection<T> values() {
    return map.values();
  }

  @Override
  public Set<java.util.Map.Entry<Instruction, T>> entrySet() {
    Set<java.util.Map.Entry<Instruction, T>> result = new LinkedHashSet<>();
    for (java.util.Map.Entry<InstructionByIIndexWrapper<Instruction>, T> entry:map.entrySet()) {
      result.add(new AbstractMap.SimpleImmutableEntry<>(entry.getKey().getInstruction(), entry.getValue()));
    }

    return result;
  }
}
