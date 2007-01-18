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
 * UNDER CONSTRUCTION.
 * 
 * MethodTypeSignature:
 *    FormalTypeParameters? (TypeSignature*) ReturnType ThrowsSignature*
 * 
 * @author sjfink
 *
 */
public class MethodTypeSignature extends Signature {
  
  public MethodTypeSignature(String s) {
    super(s);
  }

  public static MethodTypeSignature make(String genericsSignature) {
    return new MethodTypeSignature(genericsSignature);
  }

  public TypeSignature[] getArguments() {
    String typeSig = rawString().replaceAll(".*\\(","\\(").replaceAll("\\).*", "\\)");
    String[] args = TypeSignature.parseForTypeSignatures(typeSig);
    TypeSignature[] result = new TypeSignature[args.length];
    for (int i = 0; i<args.length; i++) {
      result[i] = TypeSignature.make(args[i]);
    }
    return result;
  }

}
