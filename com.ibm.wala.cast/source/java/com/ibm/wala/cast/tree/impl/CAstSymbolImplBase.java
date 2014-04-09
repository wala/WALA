/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.CAstSymbol;

public abstract class CAstSymbolImplBase implements CAstSymbol {
  private final String _name;
  private final boolean _isFinal;
  private final boolean _isCaseInsensitive;
  private final Object _defaultInitValue;

  public CAstSymbolImplBase(String name) {
    this(name, false);
  }

  public CAstSymbolImplBase(String name, boolean isFinal) {
    this(name, isFinal, false);
  }

  public CAstSymbolImplBase(String name,  boolean isFinal, boolean isCaseSensitive) {
    this(name, isFinal, isCaseSensitive, null);
  }

  public CAstSymbolImplBase(String name, Object defaultInitValue) {
    this(name, false, defaultInitValue);
  }

  public CAstSymbolImplBase(String name, boolean isFinal, Object defaultInitValue) {
    this(name, isFinal, false, defaultInitValue);
  }

  public CAstSymbolImplBase(String name,  boolean isFinal, boolean isCaseSensitive, Object defaultInitValue) {
    this._name= name;
    this._isFinal= isFinal;
    this._isCaseInsensitive= isCaseSensitive;
    this._defaultInitValue= defaultInitValue;
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
