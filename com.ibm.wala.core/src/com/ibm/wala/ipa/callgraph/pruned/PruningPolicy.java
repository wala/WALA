package com.ibm.wala.ipa.callgraph.pruned;

import com.ibm.wala.ipa.callgraph.CGNode;

/**
 * Policy which decides which branch of a call graph is going to be pruned.
 * @author Martin Mohr
 *
 */
public interface PruningPolicy {
  /**
   * Returns whether the given node shall be kept.
   * @param n node to be checked
   * @return {@code true}, if this node shall be kept, {@code false} otherwise
   */
  boolean check(CGNode n);
}
