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
package com.ibm.wala.ssa;

/** Options that govern SSA construction */
public class SSAOptions {

  /**
   * While SSA form makes the not-unreasonable assumption that values must be defined before they
   * are used, many languages permit using undefined variables and simply give them some default
   * value. Rather than requiring an IR used in SSA conversion to add bogus assignments of the
   * default that will get copy propagated away, this interface is a way to specify what the default
   * values are: this object will be invoked whenever SSA conversion needs to read a value with an
   * no definition.
   */
  public interface DefaultValues {
    int getDefaultValue(SymbolTable symtab, int valueNumber);
  }

  /** policy for pi node insertion. */
  private SSAPiNodePolicy piNodePolicy;

  private DefaultValues defaultValues = null;

  private static final SSAOptions defaultOptions = new SSAOptions();

  /** return a policy that enables all built-in pi node policies */
  public static SSAPiNodePolicy getAllBuiltInPiNodes() {
    return CompoundPiPolicy.createCompoundPiPolicy(
        InstanceOfPiPolicy.createInstanceOfPiPolicy(), NullTestPiPolicy.createNullTestPiPolicy());
  }

  public void setDefaultValues(DefaultValues defaultValues) {
    this.defaultValues = defaultValues;
  }

  public DefaultValues getDefaultValues() {
    return defaultValues;
  }

  /** @return the default SSA Options */
  public static SSAOptions defaultOptions() {
    return defaultOptions;
  }

  public SSAPiNodePolicy getPiNodePolicy() {
    return piNodePolicy;
  }

  public void setPiNodePolicy(SSAPiNodePolicy piNodePolicy) {
    this.piNodePolicy = piNodePolicy;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((piNodePolicy == null) ? 0 : piNodePolicy.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final SSAOptions other = (SSAOptions) obj;
    if (piNodePolicy == null) {
      if (other.piNodePolicy != null) return false;
    } else if (!piNodePolicy.equals(other.piNodePolicy)) return false;
    return true;
  }
}
