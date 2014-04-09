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

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;

public class JavaScriptMethods extends AstMethodReference {

  public final static String ctorAtomStr = "ctor";
  public final static Atom ctorAtom = Atom.findOrCreateUnicodeAtom(ctorAtomStr);
  public final static String ctorDescStr = "()LRoot;";
  public final static Descriptor ctorDesc = Descriptor.findOrCreateUTF8(JavaScriptLoader.JS, ctorDescStr);
  public final static MethodReference ctorReference =
    MethodReference.findOrCreate(JavaScriptTypes.CodeBody, ctorAtom, ctorDesc);

  public static MethodReference makeCtorReference(TypeReference cls) {
    return MethodReference.findOrCreate(cls, ctorAtom, ctorDesc);
  }

  public final static String dispatchAtomStr = "dispatch";
  public final static Atom dispatchAtom = Atom.findOrCreateUnicodeAtom(dispatchAtomStr);
  public final static String dispatchDescStr = "()LRoot;";
  public final static Descriptor dispatchDesc = Descriptor.findOrCreateUTF8(JavaScriptLoader.JS, dispatchDescStr);
  public final static MethodReference dispatchReference =
    MethodReference.findOrCreate(JavaScriptTypes.CodeBody, dispatchAtom, dispatchDesc);

}
