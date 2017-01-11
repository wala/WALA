/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Sep 28, 2005
 */
package com.ibm.wala.cast.java.types;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.util.collections.HashMapFactory;

public class JavaPrimitiveTypeMap {
  public static final Map<String, JavaPrimitiveType> primNameMap = HashMapFactory.make();

  public static class JavaPrimitiveType implements CAstType.Primitive {
    String fLongName;

    String fShortName;

    private JavaPrimitiveType(String longName, String shortName) {
      fLongName = longName;
      fShortName = shortName;
    }

    @Override
    public String getName() {
      return fShortName;
    }

    public String getLongName() {
      return fLongName;
    }

    @Override
    public Collection<CAstType> getSupertypes() {
      return Collections.EMPTY_LIST;
    }
  }

  public static String getShortName(String longName) {
    return primNameMap.get(longName).getName();
  }

  public static JavaPrimitiveType lookupType(String longName) {
    return primNameMap.get(longName);
  }

  public static final JavaPrimitiveType VoidType = new JavaPrimitiveType("void", "V");

  static {
    primNameMap.put("int", new JavaPrimitiveType("int", "I"));
    primNameMap.put("long", new JavaPrimitiveType("long", "J"));
    primNameMap.put("short", new JavaPrimitiveType("short", "S"));
    primNameMap.put("char", new JavaPrimitiveType("char", "C"));
    primNameMap.put("byte", new JavaPrimitiveType("byte", "B"));
    primNameMap.put("boolean", new JavaPrimitiveType("boolean", "Z"));
    primNameMap.put("float", new JavaPrimitiveType("float", "F"));
    primNameMap.put("double", new JavaPrimitiveType("double", "D"));
    primNameMap.put("void", VoidType);
  }
}
