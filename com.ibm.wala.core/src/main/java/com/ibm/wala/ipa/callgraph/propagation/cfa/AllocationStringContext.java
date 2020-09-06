package com.ibm.wala.ipa.callgraph.propagation.cfa;

import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import java.util.Objects;

/** @author genli */
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
