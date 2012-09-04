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

  public String name() {
    return _name;
  }

  public boolean isFinal() {
    return _isFinal;
  }

  public boolean isCaseInsensitive() {
    return _isCaseInsensitive;
  }

  public Object defaultInitValue() {
    return _defaultInitValue;
  }

  public abstract boolean isInternalName();

  public String toString() {
    return _name;
  }
}
