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
package com.ibm.wala.cast.js.types;

import com.ibm.wala.cast.types.AstTypeReference;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public class JavaScriptTypes extends AstTypeReference {

  public static final String jsNameStr = "JavaScript";

  public static final String jsLoaderNameStr = "JavaScriptLoader";

  public static final Atom jsName = Atom.findOrCreateUnicodeAtom(jsNameStr);

  public static final Atom jsLoaderName = Atom.findOrCreateUnicodeAtom(jsLoaderNameStr);

  public static final ClassLoaderReference jsLoader = new ClassLoaderReference(jsLoaderName, jsName, null);

  public static final TypeReference Root = TypeReference.findOrCreate(jsLoader, rootTypeName);

  public static final TypeReference Undefined = TypeReference.findOrCreate(jsLoader, "LUndefined");

  public static final TypeReference Null = TypeReference.findOrCreate(jsLoader, "LNull");

  public static final TypeReference Array = TypeReference.findOrCreate(jsLoader, "LArray");

  public static final TypeReference Object = TypeReference.findOrCreate(jsLoader, "LObject");

  public static final TypeReference CodeBody = TypeReference.findOrCreate(jsLoader, functionTypeName);

  public static final TypeReference Function = TypeReference.findOrCreate(jsLoader, "LFunction");

  public static final TypeReference Script = TypeReference.findOrCreate(jsLoader, "LScript");

  public static final TypeReference ReferenceError = TypeReference.findOrCreate(jsLoader, "LReferenceError");

  public static final TypeReference TypeError = TypeReference.findOrCreate(jsLoader, "LTypeError");

  public static final TypeReference Primitives = TypeReference.findOrCreate(jsLoader, "LPrimitives");

  public static final TypeReference FakeRoot = TypeReference.findOrCreate(jsLoader, "LFakeRoot");

  public static final TypeReference Boolean = TypeReference.findOrCreate(jsLoader, "LBoolean");

  public static final TypeReference String = TypeReference.findOrCreate(jsLoader, "LString");

  public static final TypeReference Number = TypeReference.findOrCreate(jsLoader, "LNumber");

  public static final TypeReference Date = TypeReference.findOrCreate(jsLoader, "LDate");

  public static final TypeReference RegExp = TypeReference.findOrCreate(jsLoader, "LRegExp");

  public static final TypeReference BooleanObject = TypeReference.findOrCreate(jsLoader, "LBooleanObject");

  public static final TypeReference StringObject = TypeReference.findOrCreate(jsLoader, "LStringObject");

  public static final TypeReference NumberObject = TypeReference.findOrCreate(jsLoader, "LNumberObject");

  public static final TypeReference DateObject = TypeReference.findOrCreate(jsLoader, "LDateObject");

  public static final TypeReference RegExpObject = TypeReference.findOrCreate(jsLoader, "LRegExpObject");

}
