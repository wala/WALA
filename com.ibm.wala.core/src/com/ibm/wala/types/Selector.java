/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.types;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.util.strings.Atom;

/**
 * A method selector; something like: foo(Ljava/lang/String;)Ljava/lang/Class;
 * 
 * TODO: Canonicalize these?
 */
public final class Selector {

  private final Atom name;

  private final Descriptor descriptor;
  
  private final String s;
  
  private static final Map<String,Selector> CACHE = new HashMap<String,Selector>();
  
  public static Selector make(String selectorStr) {
    Selector ret = CACHE.get(selectorStr);
    if (ret == null) {
      ret = make(Language.JAVA, selectorStr);
      CACHE.put(selectorStr, ret);
    }
    return ret;
  }

  public static Selector make(Language l, String selectorStr) {
    if (selectorStr == null) {
      throw new IllegalArgumentException("null selectorStr");
    }
    try {
      String methodName = selectorStr.substring(0, selectorStr.indexOf('('));
      String desc = selectorStr.substring(selectorStr.indexOf('('));
      return new Selector(Atom.findOrCreateUnicodeAtom(methodName), Descriptor.findOrCreateUTF8(l, desc));
    } catch (StringIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid selectorStr: " + selectorStr);
    }
  }

  public Selector(Atom name, Descriptor descriptor) {
    this.name = name;
    this.descriptor = descriptor;
    this.s = name.toString() + descriptor.toString();
    if (name == null) {
      throw new IllegalArgumentException("null name");
    }
    if (descriptor == null) {
      throw new IllegalArgumentException("null descriptor");
    }
  }

  @Override
  public boolean equals(Object obj) {
    // using instanceof is OK because Selector is final
    if (obj instanceof Selector) {
      Selector other = (Selector) obj;
      return name.equals(other.name) && descriptor.equals(other.descriptor);
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    return 19 * name.hashCode() + descriptor.hashCode();
  }

  @Override
  public String toString() {
    return s;
  }

  public Descriptor getDescriptor() {
    return descriptor;
  }

  public Atom getName() {
    return name;
  }

}
