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

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeReference;

public class JavaScriptNewInstruction extends SSANewInstruction {
  
  public JavaScriptNewInstruction(int result, NewSiteReference site) {
    super(result, site);
  }

  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    return
      new JavaScriptNewInstruction(
        defs==null? getDef(): defs[0],
	getNewSite());
  }

  public Collection<TypeReference> getExceptionTypes() {
    return Util.typeErrorExceptions();
  }

}
