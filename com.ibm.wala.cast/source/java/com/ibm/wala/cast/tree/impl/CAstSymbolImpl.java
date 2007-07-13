package com.ibm.wala.cast.tree.impl;

public class CAstSymbolImpl extends CAstSymbolImplBase {
  public CAstSymbolImpl(String _name) {
    super(_name);
  }

  public CAstSymbolImpl(String _name, boolean _isFinal) {
    super(_name, _isFinal);
  }

  public CAstSymbolImpl(String _name, boolean _isFinal, boolean _isCaseInsensitive) {
    super(_name, _isFinal, _isCaseInsensitive);
  }

  public CAstSymbolImpl(String _name, Object _defaultInitValue) {
    super(_name, _defaultInitValue);
  }

  public CAstSymbolImpl(String _name, boolean _isFinal, Object _defaultInitValue) {
    super(_name, _isFinal, _defaultInitValue);
  }

  public CAstSymbolImpl(String _name, boolean _isFinal, boolean _isCaseInsensitive, Object _defaultInitValue) {
    super(_name, _isFinal, _isCaseInsensitive, _defaultInitValue);
  }

  public boolean isInternalName() { return false; }
}
