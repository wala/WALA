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
 * Under construction.
 * 
 * ClassSignature: 
 *    (<FormalTypeParameter+>)? SuperclassSignature SuperinterfaceSignature*
 * 
 * SuperclassSignature:
 *    ClassTypeSignature
 * 
 * @author sjfink
 * 
 */
public class ClassSignature extends Signature {

  private ClassSignature(String sig) {
    super(sig);
  }

  public static ClassSignature make(String sig) {
    return new ClassSignature(sig);
  }

  public FormalTypeParameter[] getFormalTypeParameters() {
    if (rawString().charAt(0) != '<') {
      // no formal type parameters
      return null;
    }
    int index = endOfFormalTypeParameters();
    String[] args = FormalTypeParameter.parseForFormalTypeParameters(rawString().substring(0,index));
    FormalTypeParameter[] result = new FormalTypeParameter[args.length];
    for (int i = 0; i < args.length; i++) {
      result[i] = FormalTypeParameter.make(args[i]);
    }
    return result;
  }
  
  public ClassTypeSignature getSuperclassSignature() {
    String s = rawString().substring(endOfFormalTypeParameters());
    assert s.charAt(0) == 'L';
    int i = 1;
    int depth = 0;
    while (depth > 0 || s.charAt(i) != ';') {
      if (s.charAt(i) == '<') {
        depth++;
      }
      if (s.charAt(i) == '>') {
        depth--;
      }
      i++;
    }
    return ClassTypeSignature.makeClassTypeSig(s.substring(0,i+1));
  }
  
  private int endOfFormalTypeParameters() {
    if (rawString().charAt(0) != '<') {
      return 0;
    }
    int i = 1;
    int depth = 1;
    while (depth > 0) {
      if (rawString().charAt(i) == '>') {
        depth--;
      }
      if (rawString().charAt(i) == '<') {
        depth++;
      }
      i++;
    }
    return i;
  }
}
