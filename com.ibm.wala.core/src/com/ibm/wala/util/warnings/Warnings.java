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
package com.ibm.wala.util.warnings;

import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;

import com.ibm.wala.util.collections.HashSetFactory;

/**
 * A global, static dictionary of warnings
 * 
 * @author sfink
 */
public class Warnings {
  
  private final static Collection<Warning> warnings = HashSetFactory.make();
  
  public static synchronized boolean add(Warning w) {
    return warnings.add(w);
  }
  
  public static synchronized void clear() {
    warnings.clear();
  }

//  private final static int MAX_PATHS = 200;

  public static String asString() {
    TreeSet<Warning> T = new TreeSet<Warning>();
    T.addAll(warnings);
    Iterator<Warning> it = T.iterator();
    StringBuffer result = new StringBuffer();
    for (int i = 1; i <= T.size(); i++) {
      result.append(i).append(". ");
      result.append(it.next());
      result.append("\n");
    }
    return result.toString();
  }

  public static Iterator<Warning> iterator() {
    return warnings.iterator();
  }

//  /**
//   * This method will print paths for severe warnings. NB: This
//   * implementation is slow.
//   */
//  public String toString(CallGraph cg) {
//    TreeSet<Warning> T = new TreeSet<Warning>();
//    T.addAll(this);
//    Iterator<Warning> it = T.iterator();
//    StringBuffer result = new StringBuffer();
//    int nPaths = 0;
//    if (size() == 0) {
//      return "No warnings.";
//    }
//    for (int i = 1; i <= size(); i++) {
//      result.append(i).append(". ");
//      Warning w = it.next();
//      result.append(w);
//      result.append("\n");
//      if (w.getLevel() >= Warning.SEVERE && w instanceof MethodWarning) {
//        final MemberReference m = ((MethodWarning) w).getMethod();
//        Filter mFilter = new Filter() {
//          public boolean accepts(Object o) {
//            CGNode n = (CGNode)o;
//            return n.getMethod().getReference().equals(m);
//          }
//        };
//        if (nPaths++ < MAX_PATHS) {
//          BFSPathFinder<CGNode> p = new BFSPathFinder<CGNode>(cg, cg.getFakeRootNode(), mFilter);
//          List<CGNode> L = p.find();
//          if (L == null) {
//            result.append("   No path found\n");
//          } else {
//            result.append("   Path:\n");
//            for (Iterator<CGNode> it2 = L.iterator(); it2.hasNext();) {
//              result.append("      ").append(it2.next());
//              result.append("\n");
//            }
//          }
//        }
//      }
//    }
//    return result.toString();
//  }
//
//  /* a copy of the method above; fix this! */
//  public void dump(CallGraph cg, PrintStream out) {
//    TreeSet<Warning> T = new TreeSet<Warning>();
//    T.addAll(this);
//    Iterator<Warning> it = T.iterator();
//    for (int i = 1; i <= size(); i++) {
//      out.print(i);
//      out.print(". ");
//      Warning w = it.next();
//      out.println(w);
//      if (w.getLevel() >= Warning.SEVERE && w instanceof MethodWarning) {
//        final MemberReference m = ((MethodWarning) w).getMethod();
//        Filter mFilter = new Filter() {
//          public boolean accepts(Object o) {
//            CGNode n = (CGNode)o;
//            return n.getMethod().getReference().equals(m);
//          }
//        };
//        BFSPathFinder<CGNode> p = new BFSPathFinder<CGNode>(cg, cg.getFakeRootNode(), mFilter);
//        List<CGNode> L = p.find();
//        if (L == null) {
//          out.println("   No path found");
//        } else {
//          out.println("   Path:");
//          for (Iterator<CGNode> it2 = L.iterator(); it2.hasNext();) {
//            out.print("      ");
//            out.println(it2.next());
//          }
//        }
//      }
//    }
//  }
}