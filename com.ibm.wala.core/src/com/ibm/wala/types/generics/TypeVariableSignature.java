/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.types.generics;

/**
 * TypeVariableSignature: T identifier ;
 * 
 * @author sjfink
 * 
 */
public class TypeVariableSignature extends TypeSignature {

  private TypeVariableSignature(String s) throws IllegalArgumentException {
    super(s);
    if (s.length() == 0) {
      throw new IllegalArgumentException();
    }
    if (s.charAt(s.length() - 1) != ';') {
      throw new IllegalArgumentException(s);
    }
  }

  public static TypeVariableSignature make(String s) throws IllegalArgumentException {
    return new TypeVariableSignature(s);
  }

  @Override
  public boolean isClassTypeSignature() {
    return false;
  }

  @Override
  public boolean isTypeVariable() {
    return true;
  }

  @Override
  public boolean isArrayTypeSignature() {
    return false;
  }

  public String getIdentifier() {
    return rawString().substring(1, rawString().length() - 1);
  }
  
  @Override
  public boolean isBaseType() {
    return false;
  }
}
