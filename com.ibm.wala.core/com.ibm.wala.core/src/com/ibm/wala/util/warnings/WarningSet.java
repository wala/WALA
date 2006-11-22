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

import java.io.PrintStream;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.Filter;
import com.ibm.wala.util.graph.traverse.BFSPathFinder;

/**
 * 
 * a Set of Warnings.
 * 
 * @author sfink
 */
public class WarningSet extends HashSet<Warning> {
  public static final long serialVersionUID = 998857762163892376L;

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Collection#add(java.lang.Object)
   */
  public boolean add(Warning o) {
    return super.add(o);
  }
  private final static int MAX_PATHS = 200;

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    TreeSet<Warning> T = new TreeSet<Warning>(new DescendingComparator());
    T.addAll(this);
    Iterator it = T.iterator();
    StringBuffer result = new StringBuffer();
    for (int i = 1; i <= size(); i++) {
      result.append(i).append(". ");
      result.append(it.next());
      result.append("\n");
    }
    return result.toString();
  }

  /**
   * This entrypoint will print paths for severe warnings. NB: This
   * implementation is slow.
   */
  public String toString(CallGraph cg) {
    TreeSet<Warning> T = new TreeSet<Warning>(new DescendingComparator());
    T.addAll(this);
    Iterator it = T.iterator();
    StringBuffer result = new StringBuffer();
    int nPaths = 0;
    if (size() == 0) {
      return "No warnings.";
    }
    for (int i = 1; i <= size(); i++) {
      result.append(i).append(". ");
      Warning w = (Warning) it.next();
      result.append(w);
      result.append("\n");
      if (w.getLevel() >= Warning.SEVERE && w instanceof MethodWarning) {
        final MethodReference m = ((MethodWarning) w).getMethod();
        Filter mFilter = new Filter() {
          public boolean accepts(Object o) {
            CGNode n = (CGNode)o;
            return n.getMethod().getReference().equals(m);
          }
        };
        if (nPaths++ < MAX_PATHS) {
          BFSPathFinder p = new BFSPathFinder<CGNode>(cg, cg.getFakeRootNode(), mFilter);
          List L = p.find();
          if (L == null) {
            result.append("   No path found\n");
          } else {
            result.append("   Path:\n");
            for (Iterator it2 = L.iterator(); it2.hasNext();) {
              result.append("      ").append(it2.next());
              result.append("\n");
            }
          }
        }
      }
    }
    return result.toString();
  }

  /* a copy of the method above; fix this! */
  public void dump(CallGraph cg, PrintStream out) {
    TreeSet<Warning> T = new TreeSet<Warning>(new DescendingComparator());
    T.addAll(this);
    Iterator it = T.iterator();
    for (int i = 1; i <= size(); i++) {
      out.print(i);
      out.print(". ");
      Warning w = (Warning) it.next();
      out.println(w);
      if (w.getLevel() >= Warning.SEVERE && w instanceof MethodWarning) {
        final MethodReference m = ((MethodWarning) w).getMethod();
        Filter mFilter = new Filter() {
          public boolean accepts(Object o) {
            CGNode n = (CGNode)o;
            return n.getMethod().getReference().equals(m);
          }
        };
        BFSPathFinder p = new BFSPathFinder<CGNode>(cg, cg.getFakeRootNode(), mFilter);
        List L = p.find();
        if (L == null) {
          out.println("   No path found");
        } else {
          out.println("   Path:");
          for (Iterator it2 = L.iterator(); it2.hasNext();) {
            out.print("      ");
            out.println(it2.next());
          }
        }
      }
    }
  }

  /**
   * 
   * @author sfink
   * 
   * This comparator reverses the "natural" notion of severity in Warnings.
   */
  public static class DescendingComparator implements Comparator<Warning> {
    /*
     * (non-Javadoc)
     * 
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Warning w1, Warning w2) {
      if (w1.getLevel() < w2.getLevel()) {
        return 1;
      } else if (w1.getLevel() > w2.getLevel()) {
        return -1;
      } else {
        return w1.getMsg().compareTo(w2.getMsg());
      }
    }
  }
}