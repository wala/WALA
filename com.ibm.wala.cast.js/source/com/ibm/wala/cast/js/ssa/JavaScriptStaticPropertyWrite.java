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
package com.ibm.wala.cast.js.ssa;

import java.util.Collection;

import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.util.Atom;

public class JavaScriptStaticPropertyWrite extends SSAPutInstruction {
  
  public JavaScriptStaticPropertyWrite(int objectRef,
				       FieldReference memberRef,
				       int value) {
    super(objectRef, value, memberRef);
  }

  public JavaScriptStaticPropertyWrite(int objectRef,
				       String fieldName,
				       int value)
  {
    this(objectRef, 
	 FieldReference.findOrCreate(
	   JavaScriptTypes.Root,
	   Atom.findOrCreateUnicodeAtom(fieldName),
	   JavaScriptTypes.Root),
	 value);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return
      new JavaScriptStaticPropertyWrite(
        uses==null? getRef(): uses[0],
	getDeclaredField(),
	uses==null? getVal(): uses[1]);
  }

  /* (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  public Collection getExceptionTypes() {
    return Util.typeErrorExceptions();
  }

}
