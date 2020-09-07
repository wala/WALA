/*
 * Copyright (c) 2002 - 2020 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import java.util.Objects;

/** This {@link Context} consists of an {@link AllocationString} that records n allocation sites */
public class AllocationStringContext implements Context {

  private final AllocationString allocationString;

  public AllocationStringContext(AllocationString allocationString) {
    this.allocationString = allocationString;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AllocationStringContext)) {
      return false;
    }
    AllocationStringContext that = (AllocationStringContext) o;
    return allocationString.equals(that.allocationString);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allocationString);
  }

  @Override
  public ContextItem get(ContextKey name) {
    if (name == nObjContextSelector.ALLOCATION_STRING_KEY) {
      return allocationString;
    }
    return null;
  }

  @Override
  public String toString() {
    return "AllocationStringContext{" + "allocationString=" + allocationString + '}';
  }
}
