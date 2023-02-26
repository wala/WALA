/*
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.java.ssa;

import com.ibm.wala.classLoader.JavaLanguage;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.TypeReference;
import java.util.Collection;

// A new instruction with an explicit outer class, i.e. "Inner inner = outer.new Inner();"
public class AstJavaNewEnclosingInstruction extends SSANewInstruction {

  int enclosing;

  @Override
  public int getNumberOfUses() {
    return 1;
  }

  @Override
  public int getUse(int i) {
    assert i == 0;
    return enclosing;
  }

  public AstJavaNewEnclosingInstruction(
      int iindex, int result, NewSiteReference site, int enclosing)
      throws IllegalArgumentException {
    super(iindex, result, site);
    this.enclosing = enclosing;
  }

  public int getEnclosing() {
    return this.enclosing;
  }

  @Override
  public String toString() {
    return super.toString() + " ENCLOSING v" + enclosing;
  }

  @Override
  public SSAInstruction copyForSSA(SSAInstructionFactory insts, int[] defs, int[] uses) {
    return ((AstJavaInstructionFactory) insts)
        .JavaNewEnclosingInstruction(
            iIndex(),
            defs == null ? getDef(0) : defs[0],
            getNewSite(),
            uses == null ? enclosing : uses[0]);
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return JavaLanguage.getNewScalarExceptions();
  }
}
