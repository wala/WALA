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
package com.ibm.wala.cast.js.ssa;

import com.ibm.wala.cast.ir.ssa.AstPropertyWrite;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;

public class JavaScriptPropertyWrite extends AstPropertyWrite {

  public JavaScriptPropertyWrite(int iindex, int objectRef, int memberRef, int value) {
    super(iindex, objectRef, memberRef, value);
  }

  /*
   * (non-Javadoc)
   * @see com.ibm.domo.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return Util.typeErrorExceptions();
  }
}
