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
package com.ibm.wala.cast.js.types;

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

public class JavaScriptMethods extends AstMethodReference {

  public static final String ctorAtomStr = "ctor";
  public static final Atom ctorAtom = Atom.findOrCreateUnicodeAtom(ctorAtomStr);
  public static final String ctorDescStr = "()LRoot;";
  public static final Descriptor ctorDesc =
      Descriptor.findOrCreateUTF8(JavaScriptLoader.JS, ctorDescStr);
  public static final MethodReference ctorReference =
      MethodReference.findOrCreate(JavaScriptTypes.CodeBody, ctorAtom, ctorDesc);

  public static MethodReference makeCtorReference(TypeReference cls) {
    return MethodReference.findOrCreate(cls, ctorAtom, ctorDesc);
  }

  public static final String dispatchAtomStr = "dispatch";
  public static final Atom dispatchAtom = Atom.findOrCreateUnicodeAtom(dispatchAtomStr);
  public static final String dispatchDescStr = "()LRoot;";
  public static final Descriptor dispatchDesc =
      Descriptor.findOrCreateUTF8(JavaScriptLoader.JS, dispatchDescStr);
  public static final MethodReference dispatchReference =
      MethodReference.findOrCreate(JavaScriptTypes.CodeBody, dispatchAtom, dispatchDesc);
}
