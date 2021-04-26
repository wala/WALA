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
/*
 * Created on Sep 1, 2005
 */
package com.ibm.wala.cast.tree;

import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Set;

public class CAstQualifier {
  public static final Set /* <CAstQualifier> */<CAstQualifier> sQualifiers = HashSetFactory.make();

  public static final CAstQualifier CONST = new CAstQualifier("const");
  public static final CAstQualifier STRICTFP = new CAstQualifier("strictfp");
  public static final CAstQualifier VOLATILE = new CAstQualifier("volatile");
  public static final CAstQualifier ABSTRACT = new CAstQualifier("abstract");
  public static final CAstQualifier INTERFACE = new CAstQualifier("interface");
  public static final CAstQualifier NATIVE = new CAstQualifier("native");
  public static final CAstQualifier TRANSIENT = new CAstQualifier("transient");
  public static final CAstQualifier FINAL = new CAstQualifier("final");
  public static final CAstQualifier STATIC = new CAstQualifier("static");
  public static final CAstQualifier PRIVATE = new CAstQualifier("private");
  public static final CAstQualifier PROTECTED = new CAstQualifier("protected");
  public static final CAstQualifier PUBLIC = new CAstQualifier("public");
  public static final CAstQualifier SYNCHRONIZED = new CAstQualifier("synchronized");
  public static final CAstQualifier ANNOTATION = new CAstQualifier("@annotation");
  public static final CAstQualifier ENUM = new CAstQualifier("enum");
  public static final CAstQualifier MODULE = new CAstQualifier("module");

  static {
    sQualifiers.add(ANNOTATION);
    sQualifiers.add(PUBLIC);
    sQualifiers.add(PROTECTED);
    sQualifiers.add(PRIVATE);
    sQualifiers.add(STATIC);
    sQualifiers.add(FINAL);
    sQualifiers.add(SYNCHRONIZED);
    sQualifiers.add(TRANSIENT);
    sQualifiers.add(NATIVE);
    sQualifiers.add(INTERFACE);
    sQualifiers.add(ABSTRACT);
    sQualifiers.add(VOLATILE);
    sQualifiers.add(STRICTFP);
    sQualifiers.add(CONST);
    sQualifiers.add(ENUM);
    sQualifiers.add(MODULE);
  }

  private static int sNextBitNum = 0;

  private final String fName;

  private final long fBit;

  public CAstQualifier(String name) {
    super();
    fBit = 1L << sNextBitNum++;
    fName = name;
    sQualifiers.add(this);
  }

  public long getBit() {
    return fBit;
  }

  public String getName() {
    return fName;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof CAstQualifier)) return false;
    CAstQualifier other = (CAstQualifier) o;
    return other.fName.equals(fName) && (fBit == other.fBit);
  }

  @Override
  public int hashCode() {
    int result = 37;
    result = result * 13 + fName.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return fName;
  }
}
