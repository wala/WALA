/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.CAstSymbol;
import com.ibm.wala.cast.tree.CAstType;

public abstract class CAstSymbolImplBase implements CAstSymbol {
  private final String _name;
  private final boolean _isFinal;
  private final boolean _isCaseInsensitive;
  private final Object _defaultInitValue;
  private final CAstType type;

  public CAstSymbolImplBase(String name, CAstType type) {
    this(name, type, false);
  }

  public CAstSymbolImplBase(String name, CAstType type, boolean isFinal) {
    this(name, type, isFinal, false);
  }

  public CAstSymbolImplBase(String name, CAstType type, boolean isFinal, boolean isCaseSensitive) {
    this(name, type, isFinal, isCaseSensitive, null);
  }

  public CAstSymbolImplBase(String name, CAstType type, Object defaultInitValue) {
    this(name, type, false, defaultInitValue);
  }

  public CAstSymbolImplBase(String name, CAstType type, boolean isFinal, Object defaultInitValue) {
    this(name, type, isFinal, false, defaultInitValue);
  }

  public CAstSymbolImplBase(
      String name,
      CAstType type,
      boolean isFinal,
      boolean isCaseSensitive,
      Object defaultInitValue) {
    this._name = name;
    this.type = type;
    this._isFinal = isFinal;
    this._isCaseInsensitive = isCaseSensitive;
    this._defaultInitValue = defaultInitValue;

    assert name != null;
    assert type != null;
  }

  @Override
  public CAstType type() {
    return type;
  }

  @Override
  public String name() {
    return _name;
  }

  @Override
  public boolean isFinal() {
    return _isFinal;
  }

  @Override
  public boolean isCaseInsensitive() {
    return _isCaseInsensitive;
  }

  @Override
  public Object defaultInitValue() {
    return _defaultInitValue;
  }

  @Override
  public abstract boolean isInternalName();

  @Override
  public String toString() {
    return _name;
  }
}
