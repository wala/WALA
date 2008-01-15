/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ssa;

import java.util.Collection;

import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Exceptions;
import com.ibm.wala.util.debug.Assertions;

/**
 * @author sfink
 */
public class SSANewInstruction extends SSAInstruction {
  private final int result;

  private final NewSiteReference site;

  /**
   * The value numbers of the arguments passed to the call. If params == null,
   * this should be a static this statement allocates a scalar. if params !=
   * null, then params[i] is the size of the ith dimension of the array.
   */
  private final int[] params;

  /**
   * Create a new instruction to allocate a scalar.
   */
  public SSANewInstruction(int result, NewSiteReference site) throws IllegalArgumentException {
    super();
    if (site == null) {
      throw new IllegalArgumentException("site cannot be null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(!site.getDeclaredType().isArrayType()
          || site.getDeclaredType().getClassLoader().getLanguage() != ClassLoaderReference.Java);
    }
    this.result = result;
    this.site = site;
    this.params = null;
  }

  /**
   * Create a new instruction to allocate an array.
   * 
   * @throws IllegalArgumentException
   *             if site is null
   * @throws IllegalArgumentException
   *             if params is null
   */
  public SSANewInstruction(int result, NewSiteReference site, int[] params) {

    super();
    if (params == null) {
      throw new IllegalArgumentException("params is null");
    }
    if (site == null) {
      throw new IllegalArgumentException("site is null");
    }
    if (Assertions.verifyAssertions) {
      Assertions._assert(site.getDeclaredType().isArrayType()
          || site.getDeclaredType().getClassLoader().getLanguage() != ClassLoaderReference.Java);
    }
    this.result = result;
    this.site = site;
    this.params = new int[params.length];
    System.arraycopy(params, 0, this.params, 0, params.length);
  }

  @Override
  public SSAInstruction copyForSSA(int[] defs, int[] uses) {
    if (params == null) {
      return new SSANewInstruction(defs == null ? result : defs[0], site);
    } else {
      return new SSANewInstruction(defs == null ? result : defs[0], site, uses == null ? params : uses);
    }
  }

  @Override
  public String toString(SymbolTable symbolTable) {
    return getValueString(symbolTable, result) + " = new " + site.getDeclaredType() + "@" + site.getProgramCounter()
        + (params == null ? "" : " dims: " + params.length);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#visit(IVisitor)
   * @throws IllegalArgumentException
   *             if v is null
   */
  @Override
  public void visit(IVisitor v) {
    if (v == null) {
      throw new IllegalArgumentException("v is null");
    }
    v.visitNew(this);
  }

  /**
   * @see com.ibm.wala.ssa.SSAInstruction#getDef()
   */
  @Override
  public boolean hasDef() {
    return true;
  }

  @Override
  public int getDef() {
    return result;
  }

  @Override
  public int getDef(int i) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(i == 0);
    }
    return result;
  }

  @Override
  public int getNumberOfDefs() {
    return 1;
  }

  /**
   * @return TypeReference
   */
  public TypeReference getConcreteType() {
    return site.getDeclaredType();
  }

  public NewSiteReference getNewSite() {
    return site;
  }

  @Override
  public int hashCode() {
    return result * 7529 + site.getDeclaredType().hashCode();
  }

  @Override
  public int getNumberOfUses() {
    return params == null ? 0 : params.length;
  }

  @Override
  public int getUse(int j) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(params != null, "expected params but got null");
      Assertions._assert(params.length > j, "found too few parameters");
    }
    return params[j];
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isPEI()
   */
  @Override
  public boolean isPEI() {
    return true;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#isFallThrough()
   */
  @Override
  public boolean isFallThrough() {
    return true;
  }

  /*
   * @see com.ibm.wala.ssa.Instruction#getExceptionTypes()
   */
  @Override
  public Collection<TypeReference> getExceptionTypes() {
    return site.getDeclaredType().isArrayType() ? Exceptions.getNewArrayExceptions() : Exceptions.getNewScalarExceptions();
  }
}
