package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.propagation.AllocationSite;
import java.util.Arrays;

/** @author genli */
public class AllocationString implements ContextItem {

  private final AllocationSite[] allocationSites;

  public AllocationString(AllocationSite allocationSite) {
    if (allocationSite == null) {
      throw new IllegalArgumentException("null allocationSite");
    }
    allocationSites = new AllocationSite[] {allocationSite};
  }

  public AllocationString(AllocationSite[] allocationSites) {
    this.allocationSites = allocationSites;
  }

  public AllocationSite[] getAllocationSites() {
    return allocationSites;
  }

  public int getLength() {
    return allocationSites.length;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AllocationString)) {
      return false;
    }
    AllocationString that = (AllocationString) o;
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
