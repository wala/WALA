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
package com.ibm.wala.cast.types;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;

public class AstMethodReference {

  public static final String fnAtomStr = "do";
  public static final Atom fnAtom = Atom.findOrCreateUnicodeAtom(fnAtomStr);
  public static final Descriptor fnDesc =
      Descriptor.findOrCreate(new TypeName[0], AstTypeReference.rootTypeName);
  public static final Selector fnSelector = new Selector(fnAtom, fnDesc);

  public static MethodReference fnReference(TypeReference cls) {
    return MethodReference.findOrCreate(cls, fnAtom, fnDesc);
  }
}
