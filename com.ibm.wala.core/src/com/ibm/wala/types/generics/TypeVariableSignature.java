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
 * TypeVariableSignature:
 *    T identifier ;
 * 
 * @author sjfink
 *
 */
public class TypeVariableSignature extends TypeSignature {

  private TypeVariableSignature(String s) {
    super(s);
    assert (s.charAt(s.length()-1) == ';');
  }

  public static TypeVariableSignature make(String s) {
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

  public String getIdentifier() {
    return rawString().substring(1, rawString().length()-1);
  }
}
