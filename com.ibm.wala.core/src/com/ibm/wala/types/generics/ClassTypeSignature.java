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

import java.util.StringTokenizer;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

/**
 * Under construction.
 * 
 * ClassTypeSignature:
 *   L PackageSpecifier* SimpleClassTypeSignature ClassTypeSignatureSuffix* ;
 *   
 * SimpleClassTypeSignature:
 *   Identifier TypeArguments?
 *   
 * TypeArguments:
 *   &lt;TypeArguments+&gt;
 * 
 * @author sjfink
 * 
 */
public class ClassTypeSignature extends TypeSignature {

  ClassTypeSignature(String s) throws IllegalArgumentException {
    super(s);
    if (s.length() == 0) {
      throw new IllegalArgumentException();
    }
    if (s.charAt(0) != 'L') {
      throw new IllegalArgumentException(s);
    }
    if (s.charAt(s.length()-1) != ';') {
      throw new IllegalArgumentException(s);
    }
  }

  public static ClassTypeSignature makeClassTypeSig(String s) throws IllegalArgumentException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    return new ClassTypeSignature(s);
  }

  @Override
  public boolean isTypeVariable() {
    return false;
  }

  @Override
  public boolean isClassTypeSignature() {
    return true;
  }
  
  @Override
  public boolean isArrayTypeSignature() {
    return false;
  }

  /**
   * Return the name of the raw type for this signature
   */
  public TypeName getRawName() {
    // note: need to handle type arguments for raw signatures like the following:
    // Ljava/util/IdentityHashMap<TK;TV;>.IdentityHashMapIterator<TV;>;
    StringBuffer s = new StringBuffer();
    StringTokenizer t = new StringTokenizer(rawString(),".");
    while (t.hasMoreTokens()) {
      String x = t.nextToken();
      s.append(x.replaceAll("<.*>","").replace(";",""));
      if (t.hasMoreElements()) {
        // note that '$' is the canonical separator for inner class names
        s.append('$');
      }
    }
    return TypeName.string2TypeName(s.toString());
  }

  public TypeArgument[] getTypeArguments() {
    // note: need to handle type arguments for raw signatures like the following:
    // Ljava/util/IdentityHashMap<TK;TV;>.IdentityHashMapIterator<TV;>;
    int lastDot = rawString().lastIndexOf('.');
    if (rawString().indexOf('<',lastDot) == -1) {
      return null;
    } else {
      int start = rawString().indexOf('<',lastDot);
      int end = endOfTypeArguments();
      return TypeArgument.make(rawString().substring(start,end));
    }
  }
  
  private int endOfTypeArguments() {
    // note: need to handle type arguments for raw signatures like the following:
    // Ljava/util/IdentityHashMap<TK;TV;>.IdentityHashMapIterator<TV;>;
    int lastDot = rawString().lastIndexOf('.');
    int i = rawString().indexOf('<',lastDot) + 1;
    assert (i > 0);
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
  
  @Override
  public boolean isBaseType() {
    return false;
  }
  
  public static IClass lookupClass(IClassHierarchy cha, ClassTypeSignature sig) {
    if (sig == null) {
      throw new IllegalArgumentException("sig is null");
    }
    if (cha == null) {
      throw new IllegalArgumentException("cha is null");
    }
    TypeReference t = TypeReference.findOrCreate(ClassLoaderReference.Application, sig.getRawName());
    return cha.lookupClass(t);
  }

}
