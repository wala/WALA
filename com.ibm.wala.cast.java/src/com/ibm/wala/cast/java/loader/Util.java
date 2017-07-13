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
package com.ibm.wala.cast.java.loader;


import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.util.strings.Atom;

public class Util {

  public static Selector methodEntityToSelector(CAstEntity methodEntity) {
    Atom name= Atom.findOrCreateUnicodeAtom(methodEntity.getName());
    CAstType.Function signature= (CAstType.Function) methodEntity.getType();
    // Use signature to determine # of args; (entity's args includes 'this')
    TypeName retTypeName= 
      TypeName.string2TypeName(signature.getReturnType().getName());
    TypeName[] argTypeNames= 
      (signature.getArgumentCount() == 0) ? 
      null :
      new TypeName[signature.getArgumentCount()];

    int i= 0;
    for (CAstType argType : signature.getArgumentTypes()) {
      argTypeNames[i]= TypeName.string2TypeName(argType.getName());
      ++i;
    }

    Descriptor desc= Descriptor.findOrCreate(argTypeNames, retTypeName);
    
    return new Selector(name, desc);
  }

  public static Atom fieldEntityToAtom(CAstEntity fieldEntity) {
    return Atom.findOrCreateUnicodeAtom(fieldEntity.getName());
  }
}
