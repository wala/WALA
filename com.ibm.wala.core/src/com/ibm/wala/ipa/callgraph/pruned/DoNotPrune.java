package com.ibm.wala.ipa.callgraph.pruned;

import com.ibm.wala.ipa.callgraph.CGNode;

public class DoNotPrune implements PruningPolicy {

  public static DoNotPrune INSTANCE = new DoNotPrune();
  @Override
  public boolean check(CGNode n) {
    return true;
  }

}
