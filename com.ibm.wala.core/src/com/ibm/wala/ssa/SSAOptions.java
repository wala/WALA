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

/**
 *
 * Options that govern SSA construction
 * 
 * @author sfink
 */
public class SSAOptions {

  /**
   *  While SSA form makes the not-unreasonable assumption that values
   * must be defined before they are used, many languages permit using
   * undefined variables and simply give them some default value.  Rather
   * than requiring an IR used in SSA conversion to add bogus assignments
   * of the default that will get copy propagated away, this interface
   * is a way to specify what the default values are: this object will
   * be invoked whenever SSA conversion needs to read a value with an
   * no definition.
   *
   * @author Julian Dolby (dolby@us.ibm.com)
   *
   */
  public interface DefaultValues {
    int getDefaultValue(SymbolTable symtab, int valueNumber);
  }

  /**
   * use pi nodes when building IR
   */
  private boolean usePiNodes = false;
  private boolean conversionPreservesNames = false;
  private DefaultValues defaultValues = null;

  private final static SSAOptions defaultOptions = new SSAOptions();
  
  
  /**
   * @return true iff these options indicate pi nodes should be used
   */
  public boolean getUsePiNodes() {
    return usePiNodes;
  }

  /**
   * @param b whether to use pi nodes when constructing ir
   */
  public void setUsePiNodes(boolean b) {
    this.usePiNodes = b;
  }

  /**
   * @return whether to use Pi nodes to preserve distinct value numbers
   *  for variables with different names at the source level.
   */
  public boolean getPreserveNames() {
    return conversionPreservesNames;
  }

  /**
   * @param b whether to use Pi nodes to preserve distinct value numbers
   *  for variables with different names at the source level.
   */
  public void setPreserveNames(boolean b) {
    conversionPreservesNames = b;
  }

  public void setDefaultValues(DefaultValues defaultValues) {
    this.defaultValues = defaultValues;
  }

  public DefaultValues getDefaultValues() {
    return defaultValues;
  }

  /**
   * @return the default SSA Options
   */
  public static SSAOptions defaultOptions() {
    return defaultOptions;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  public boolean equals(Object arg0) {
    if (arg0 == null) {
      return false;
    }
    if (getClass().equals(arg0.getClass())) {
      SSAOptions other = (SSAOptions)arg0;
      return usePiNodes == other.usePiNodes;
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return (usePiNodes) ? 9277 : 7207;
  }
}
