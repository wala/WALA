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

import com.ibm.wala.cast.types.*;
import com.ibm.wala.types.*;
import com.ibm.wala.util.Atom;

public class JavaScriptMethods extends AstMethodReference {

  public final static String ctorAtomStr = "ctor";
  public final static Atom ctorAtom = Atom.findOrCreateUnicodeAtom(ctorAtomStr);
  public final static String ctorDescStr = "()LRoot;";
  public final static Descriptor ctorDesc = Descriptor.findOrCreateUTF8(ctorDescStr);
  public final static MethodReference ctorReference =
    MethodReference.findOrCreate(JavaScriptTypes.CodeBody, ctorAtom, ctorDesc);

  public static MethodReference makeCtorReference(TypeReference cls) {
    return MethodReference.findOrCreate(cls, ctorAtom, ctorDesc);
  }

}
