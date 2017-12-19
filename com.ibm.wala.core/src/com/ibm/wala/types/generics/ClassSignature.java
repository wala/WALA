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

import java.util.ArrayList;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.ShrikeClass;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

/**
 * Under construction.
 * 
 * ClassSignature: 
 *    (&lt;FormalTypeParameter+&gt;)? SuperclassSignature SuperinterfaceSignature*
 * 
 * SuperclassSignature:
 *    ClassTypeSignature
 *    
 * SuperinterfaceSignature:
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
    if (sig == null || sig.length() == 0) {
      throw new IllegalArgumentException("empty or null sig");
    }
    return new ClassSignature(sig);
  }

  /**
   * @return the formal type parameters, or null if none
   */
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
  
  public ClassTypeSignature getSuperclassSignature() throws IllegalArgumentException {
    return ClassTypeSignature.makeClassTypeSig(rawString().substring(endOfFormalTypeParameters(),endOfClassTypeSig(endOfFormalTypeParameters())));
  }
  
  private int endOfClassTypeSig(int start) throws IllegalArgumentException {
    String s = rawString().substring(start);
    if (s.charAt(0) != 'L') {
      throw new IllegalArgumentException("malformed ClassSignature " + rawString());
    }
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
    return start + i + 1;
  }
  
  public ClassTypeSignature[] getSuperinterfaceSignatures() throws IllegalArgumentException {
    int start = endOfClassTypeSig(endOfFormalTypeParameters());
    ArrayList<ClassTypeSignature> result = new ArrayList<>();
    while (start < rawString().length() - 1) {
      int end = endOfClassTypeSig(start);
      result.add(ClassTypeSignature.makeClassTypeSig(rawString().substring(start,end)));
      start = end;
    }
    if (result.size() == 0) {
      return null;
    }
    ClassTypeSignature[] arr = new ClassTypeSignature[result.size()];
    return result.toArray(arr);
    
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
  
  /**
   * @param klass
   * @return the class signature, or null if none
   * @throws InvalidClassFileException
   */
  public static ClassSignature getClassSignature(IClass klass) throws InvalidClassFileException {
    if (klass instanceof ShrikeClass) {
      ShrikeClass sc = (ShrikeClass) klass;
      return sc.getClassSignature();
    } else {
      return null;
    }
  }
}
