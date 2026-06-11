/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.types;

import com.ibm.wala.classLoader.Language;
import com.ibm.wala.core.util.strings.Atom;

/**
 * A method selector; something like: foo(Ljava/lang/String;)Ljava/lang/Class;
 *
 * <p>TODO: Canonicalize these?
 */
public record Selector(Atom name, Descriptor descriptor) {

  public static Selector make(String selectorStr) {
    return make(Language.JAVA, selectorStr);
  }

  public static Selector make(Language l, String selectorStr) {
    if (selectorStr == null) {
      throw new IllegalArgumentException("null selectorStr");
    }
    try {
      String methodName = selectorStr.substring(0, selectorStr.indexOf('('));
      String desc = selectorStr.substring(selectorStr.indexOf('('));
      return new Selector(
          Atom.findOrCreateUnicodeAtom(methodName), Descriptor.findOrCreateUTF8(l, desc));
    } catch (StringIndexOutOfBoundsException e) {
      throw new IllegalArgumentException("invalid selectorStr: " + selectorStr, e);
    }
  }

  public Selector(Atom name, Descriptor descriptor) {
    this.name = name;
    this.descriptor = descriptor;
    if (name == null) {
      throw new IllegalArgumentException("null name");
    }
    if (descriptor == null) {
      throw new IllegalArgumentException("null descriptor");
    }
  }

  @Override
  public String toString() {
    return name.toString() + descriptor;
  }

  /**
   * @deprecated Use {@link #descriptor()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public Descriptor getDescriptor() {
    return descriptor();
  }

  /**
   * @deprecated Use {@link #name()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public Atom getName() {
    return name();
  }
}
