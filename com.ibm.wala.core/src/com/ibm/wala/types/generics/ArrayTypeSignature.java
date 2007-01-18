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

public class ArrayTypeSignature extends TypeSignature {

  ArrayTypeSignature(String s) {
    super(s);
    assert(s.charAt(0) == '[');
  }

  @Override
  public boolean isClassTypeSignature() {
    return false;
  }

  @Override
  public boolean isTypeVariable() {
    return false;
  }
  
  public static ArrayTypeSignature make(String s) {
    return new ArrayTypeSignature(s);
  }

  @Override
  public boolean isArrayTypeSignature() {
    return true;
  }
  
  public TypeSignature getContents() {
    return TypeSignature.make(rawString().substring(1));
  }
  
  @Override
  public boolean isBaseType() {
    return false;
  }

}
