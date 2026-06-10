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
package com.ibm.wala.cast.ir.ssa;

import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Pair;
import java.util.Collection;
import java.util.Objects;

/**
 * This abstract class provides helper functionality for recording lexical uses and/or definitions.
 * It is used in lexical read and write instructions
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public abstract class AstLexicalAccess extends SSAInstruction {

  /**
   * A single lexical access.
   *
   * @author Julian Dolby (dolby@us.ibm.com)
   * @param variableName name being accessed
   * @param variableDefiner name of entity that defines the variable
   * @param type type of the lexical value
   * @param valueNumber value number used for name where access is being performed (not in the
   *     declaring entity)
   */
  public record Access(
      String variableName, String variableDefiner, TypeReference type, int valueNumber) {

    public Pair<String, String> getName() {
      return Pair.make(variableName, variableDefiner);
    }

    // intentional: omits variableDefiner and type
    @Override
    public int hashCode() {
      return variableName.hashCode() * valueNumber;
    }

    // intentional: omits type
    @Override
    public boolean equals(Object other) {
      return (other instanceof Access)
          && variableName.equals(((Access) other).variableName)
          && valueNumber == ((Access) other).valueNumber
          && Objects.equals(variableDefiner, ((Access) other).variableDefiner);
    }

    @Override
    public String toString() {
      return "Access(" + variableName + '@' + variableDefiner + ':' + valueNumber + ')';
    }
  }

  private Access[] accesses;

  AstLexicalAccess(int iindex, Access[] accesses) {
    super(iindex);
    setAccesses(accesses);
  }

  public void setAccesses(Access[] accesses) {
    this.accesses = accesses;
  }

  public Access[] getAccesses() {
    return accesses;
  }

  public Access getAccess(int i) {
    return accesses[i];
  }

  public int getAccessCount() {
    return accesses.length;
  }

  @Override
  public boolean isFallThrough() {
    return true;
  }

  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return null;
  }

  @Override
  public int hashCode() {
    int v = 1;
    for (Access accesse : accesses) v *= accesse.variableName.hashCode();

    return v;
  }
}
