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

import com.ibm.wala.cast.tree.CAstType;

public class CAstSymbolImpl extends CAstSymbolImplBase {
  public CAstSymbolImpl(String _name, CAstType type) {
    super(_name, type);
  }

  public CAstSymbolImpl(String _name, CAstType type, boolean _isFinal) {
    super(_name, type, _isFinal);
  }

  public CAstSymbolImpl(String _name, CAstType type, boolean _isFinal, boolean _isCaseInsensitive) {
    super(_name, type, _isFinal, _isCaseInsensitive);
  }

  public CAstSymbolImpl(String _name, CAstType type, Object _defaultInitValue) {
    super(_name, type, _defaultInitValue);
  }

  public CAstSymbolImpl(String _name, CAstType type, boolean _isFinal, Object _defaultInitValue) {
    super(_name, type, _isFinal, _defaultInitValue);
  }

  public CAstSymbolImpl(
      String _name,
      CAstType type,
      boolean _isFinal,
      boolean _isCaseInsensitive,
      Object _defaultInitValue) {
    super(_name, type, _isFinal, _isCaseInsensitive, _defaultInitValue);
  }

  @Override
  public boolean isInternalName() {
    return false;
  }
}
