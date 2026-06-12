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

import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import java.util.Arrays;

/**
 * This is a {@link ContextItem} that records n allocation sites, the 0th element represents the
 * most recently used receiver obj, which is an {@code AllocationSiteInNode}
 */
public record AllocationString(AllocationSite[] allocationSites) implements ContextItem {

  public AllocationString(AllocationSite allocationSite) {
    this(new AllocationSite[] {allocationSite});
  }

  public AllocationString {
    if (allocationSites == null) {
      throw new IllegalArgumentException("null allocationSites");
    }
  }

  /**
   * @deprecated Use {@link #allocationSites()} instead
   */
  @Deprecated(forRemoval = true, since = "1.8.0")
  public AllocationSite[] getAllocationSites() {
    return allocationSites();
  }

  public int getLength() {
    return allocationSites.length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AllocationString that)) {
      return false;
    }
    return Arrays.equals(getAllocationSites(), that.getAllocationSites());
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(getAllocationSites());
  }

  @Override
  public String toString() {
    return "AllocationString{" + "allocationSites=" + Arrays.toString(allocationSites) + '}';
  }
}
