package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.CAstSymbol;

public class CAstSymbolImpl implements CAstSymbol {

  private final String _name;
  private final boolean _isFinal;
  private final boolean _isCaseInsensitive;
  private final Object _defaultInitValue;

  public CAstSymbolImpl(String _name) {
    this._name = _name;
    this._isFinal = false;
    this._isCaseInsensitive = false;
    this._defaultInitValue = null;
  }

  public CAstSymbolImpl(String _name, boolean _isFinal) {
    this._name = _name;
    this._isFinal = _isFinal;
    this._isCaseInsensitive = false;
    this._defaultInitValue = null;
  }

  public CAstSymbolImpl(String _name, boolean _isFinal, boolean _isCaseInsensitive) {
    this._name = _name;
    this._isFinal = _isFinal;
    this._isCaseInsensitive = _isCaseInsensitive;
    this._defaultInitValue = null;
  }

  public CAstSymbolImpl(String _name, Object _defaultInitValue) {
    this._name = _name;
    this._isFinal = false;
    this._isCaseInsensitive = false;
    this._defaultInitValue = _defaultInitValue;
  }

  public CAstSymbolImpl(String _name, boolean _isFinal, Object _defaultInitValue) {
    this._name = _name;
    this._isFinal = _isFinal;
    this._isCaseInsensitive = false;
    this._defaultInitValue = _defaultInitValue;
  }

  public CAstSymbolImpl(String _name, boolean _isFinal, boolean _isCaseInsensitive, Object _defaultInitValue) {
    this._name = _name;
    this._isFinal = _isFinal;
    this._isCaseInsensitive = _isCaseInsensitive;
    this._defaultInitValue = _defaultInitValue;
  }

  public String name() { return _name; }

  public boolean isFinal() { return _isFinal; }

  public boolean isCaseInsensitive() { return _isCaseInsensitive; }

  public Object defaultInitValue() { return _defaultInitValue; }

  public String toString() {
      return _name;
  }
}
