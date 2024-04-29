package com.ibm.wala.ipa.callgraph.util;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;

/**
 * Utility methods for searching call graphs, e.g., to find particular {@link CGNode}s or types of
 * statements within a node
 */
public class CallGraphSearchUtil {

  private CallGraphSearchUtil() {}

  /**
   * Find the main method in a call graph
   *
   * @param cg call graph
   * @return CGNode for the main method
   * @throws com.ibm.wala.util.debug.UnimplementedError if no main method is not found
   */
  public static CGNode findMainMethod(CallGraph cg) {
    Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
    Atom name = Atom.findOrCreateUnicodeAtom("main");
    return findMethod(cg, d, name);
  }

  /**
   * Find a method in a call graph
   *
   * @param cg the call graph
   * @param d descriptor for the desired method
   * @param name name of the desired method
   * @return the corresponding CGNode. If multiple CGNodes exist for the descriptor and name,
   *     returns one of them arbitrarily
   * @throws com.ibm.wala.util.debug.UnimplementedError if no matching CGNode is found
   */
  public static CGNode findMethod(CallGraph cg, Descriptor d, Atom name) {
    for (CGNode n : Iterator2Iterable.make(cg.getSuccNodes(cg.getFakeRootNode()))) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    // if it's not a successor of fake root, just iterate over everything
    for (CGNode n : cg) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }

  /**
   * Find method with some name in a call graph
   *
   * @param cg the call graph
   * @param name desired method name
   * @return matching {@link CGNode}
   * @throws com.ibm.wala.util.debug.UnimplementedError if no matching CGNode is found
   */
  public static CGNode findMethod(CallGraph cg, String name) {
    Atom a = Atom.findOrCreateUnicodeAtom(name);
    for (CGNode n : cg) {
      if (n.getMethod().getName().equals(a)) {
        return n;
      }
    }
    System.err.println("call graph " + cg);
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }
}
