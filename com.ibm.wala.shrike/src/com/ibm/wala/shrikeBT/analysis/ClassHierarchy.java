/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT.analysis;

import java.util.HashSet;
import java.util.Iterator;

import com.ibm.wala.shrikeBT.Constants;

/**
 * This class takes the raw information from a ClassHierarchyProvider and computes type operations (subtype check, type union). All
 * operations are static.
 * 
 * Because ClassHierarchyProvider sometimes only provides partial information, these routines sometimes answer "don't know".
 */
public final class ClassHierarchy {
  private ClassHierarchy() {
  }

  /**
   * Equals Constants.NO
   */
  public static final int NO = Constants.NO;

  /**
   * Equals Constants.YES
   */
  public static final int YES = Constants.YES;

  /**
   * Equals Constants.MAYBE
   */
  public static final int MAYBE = Constants.MAYBE;

  private static int checkSuperinterfacesContain(ClassHierarchyProvider hierarchy, String t1, String t2, HashSet<String> visited) {
    String[] ifaces = hierarchy.getSuperInterfaces(t1);
    if (ifaces == null) {
      return MAYBE;
    }

    int r = NO;
    for (String iface2 : ifaces) {
      String iface = iface2;
      if (!visited.contains(iface)) {
        visited.add(iface);
        if (iface.equals(t2)) {
          return YES;
        } else {
          int v = checkSuperinterfacesContain(hierarchy, iface, t2, visited);
          if (v == YES) {
            return YES;
          } else if (v == MAYBE) {
            r = MAYBE;
          }
        }
      }
    }
    return r;
  }

  private static int checkSupertypesContain(ClassHierarchyProvider hierarchy, String t1, String t2) {
    int r = NO;

    String c = t1;
    while (true) {
      String sup = hierarchy.getSuperClass(c);
      if (sup == null) {
        if (!c.equals(Constants.TYPE_Object)) {
          r = MAYBE;
        }
        break;
      }

      if (sup.equals(t2)) {
        return YES;
      }

      c = sup;
    }

    if (hierarchy.isInterface(t2) != NO) {
      HashSet<String> visited = new HashSet<>();

      for (c = t1; c != null; c = hierarchy.getSuperClass(c)) {
        int v = checkSuperinterfacesContain(hierarchy, c, t2, visited);
        if (v == YES) {
          return YES;
        } else if (v == MAYBE) {
          r = MAYBE;
        }
      }
    }

    return r;
  }

  private static int checkSubtypesContain(ClassHierarchyProvider hierarchy, String t1, String t2, HashSet<String> visited) {
    // No interface is a subclass of a real class
    if (hierarchy.isInterface(t1) == NO && hierarchy.isInterface(t2) == YES) {
      return NO;
    }

    String[] subtypes = hierarchy.getSubClasses(t1);
    if (subtypes == null) {
      return MAYBE;
    }

    int r = NO;
    for (String subtype : subtypes) {
      String subt = subtype;
      if (!visited.contains(subt)) {
        visited.add(subt);
        if (subt.equals(t2)) {
          return YES;
        } else {
          int v = checkSubtypesContain(hierarchy, subt, t2, visited);
          if (v == YES) {
            return YES;
          } else if (v == MAYBE) {
            r = MAYBE;
          }
        }
      }
    }
    return r;
  }

  private static int checkSubtypeOfHierarchy(ClassHierarchyProvider hierarchy, String t1, String t2) {
    if (t2.equals(Constants.TYPE_Object)) {
      return YES;
    } else {
       int v = checkSupertypesContain(hierarchy, t1, t2);
      if (v == MAYBE) {
        v = checkSubtypesContain(hierarchy, t2, t1, new HashSet<String>());
      }
      return v;
    }
  }

  /**
   * Perform subtype check.
   * 
   * @param hierarchy the hierarchy information to use for the decision
   * @param t1 a type in JVM format
   * @param t2 a type in JVM format
   * @return whether t1 is a subtype of t2 (YES, NO, MAYBE)
   */
  public static int isSubtypeOf(ClassHierarchyProvider hierarchy, String t1, String t2) {
    if (t1 == null || t2 == null) {
      return NO;
    } else if (t1.equals(t2)) {
      return YES;
    } else if (t1.equals(Constants.TYPE_unknown) || t2.equals(Constants.TYPE_unknown)) {
      return MAYBE;
    } else {
      switch (t1.charAt(0)) {
      case 'L':
        if (t1.equals(Constants.TYPE_null)) {
          return YES;
        } else if (t2.startsWith("[")) {
          return NO;
        } else if (hierarchy == null) {
          return MAYBE;
        } else {
          return checkSubtypeOfHierarchy(hierarchy, t1, t2);
        }
      case '[':
        if (t2.equals(Constants.TYPE_Object) || t2.equals("Ljava/io/Serializable;") || t2.equals("Ljava/lang/Cloneable;")) {
          return YES;
        } else if (t2.startsWith("[")) {
          return isSubtypeOf(hierarchy, t1.substring(1), t2.substring(1));
        } else {
          return NO;
        }
      default:
        return NO;
      }
    }
  }

  private static boolean insertSuperInterfaces(ClassHierarchyProvider hierarchy, String t, HashSet<String> supers) {
    String[] ifaces = hierarchy.getSuperInterfaces(t);
    if (ifaces == null) {
      return false;
    } else {
      boolean r = true;
      for (int i = 0; i < ifaces.length; i++) {
        String iface = ifaces[i];
        if (!supers.contains(iface)) {
          supers.add(iface);
          if (!insertSuperInterfaces(hierarchy, ifaces[i], supers)) {
            r = false;
          }
        }
      }
      return r;
    }
  }

  private static boolean insertSuperClasses(ClassHierarchyProvider hierarchy, String t, HashSet<String> supers) {
    String last = t;

    for (String c = t; c != null; c = hierarchy.getSuperClass(c)) {
      supers.add(c);
      last = c;
    }

    return last.equals(Constants.TYPE_Object);
  }

  private static boolean insertSuperClassInterfaces(ClassHierarchyProvider hierarchy, String t, HashSet<String> supers) {
    boolean r = true;

    for (String c = t; c != null; c = hierarchy.getSuperClass(c)) {
      if (!insertSuperInterfaces(hierarchy, c, supers)) {
        r = false;
      }
    }

    return r;
  }

  private static boolean collectDominatingSuperClasses(ClassHierarchyProvider hierarchy, String t, HashSet<String> matches,
      HashSet<String> supers) {
    String last = t;

    for (String c = t; c != null; c = hierarchy.getSuperClass(c)) {
      if (matches.contains(c)) {
        supers.add(c);
        return true;
      }
      last = c;
    }

    return last.equals(Constants.TYPE_Object);
  }

  private static boolean collectDominatingSuperInterfacesFromClass(ClassHierarchyProvider hierarchy, String t,
      HashSet<String> supers) {
    String[] ifaces = hierarchy.getSuperInterfaces(t);
    if (ifaces == null) {
      return false;
    } else {
      boolean r = true;
      for (int i = 0; i < ifaces.length; i++) {
        String iface = ifaces[i];
        if (!supers.contains(iface)) {
          supers.add(iface);
          if (!insertSuperInterfaces(hierarchy, ifaces[i], supers)) {
            r = false;
          }
        }
      }
      return r;
    }
  }

  private static boolean collectDominatingSuperInterfaces(ClassHierarchyProvider hierarchy, String t, HashSet<String> supers) {
    boolean r = true;

    for (String c = t; c != null && !supers.contains(c); c = hierarchy.getSuperClass(c)) {
      if (!collectDominatingSuperInterfacesFromClass(hierarchy, c, supers)) {
        r = false;
      }
    }

    return r;
  }

  private static String findCommonSupertypeHierarchy(ClassHierarchyProvider hierarchy, String t1, String t2) {    
    if (isSubtypeOf(hierarchy, t1, t2) == YES) {
      return t2;
    } else if (isSubtypeOf(hierarchy, t2, t1) == YES) {
      return t1;
    }

    HashSet<String> t1Supers = new HashSet<>();
    t1Supers.add(Constants.TYPE_Object);
    boolean t1ExactClasses = insertSuperClasses(hierarchy, t1, t1Supers);
    int t1ClassCount = t1Supers.size();
    boolean t1ExactInterfaces = insertSuperClassInterfaces(hierarchy, t1, t1Supers);

    HashSet<String> t2Supers = new HashSet<>();
    boolean t2ExactClasses = collectDominatingSuperClasses(hierarchy, t2, t1Supers, t2Supers);

    if (t2Supers.size() == 0) {
      // we didn't find a common superclass, so we don't know what it might be
      return Constants.TYPE_unknown;
    } // otherwise we know for sure what the common superclass is

    boolean t2ExactInterfaces;
    if (t1ExactInterfaces && t1ExactClasses && t1Supers.size() - t1ClassCount == 0) {
      // t1 doesn't have any interfaces so we don't need to search t2's
      // interfaces
      t2ExactInterfaces = true;
    } else {
      t2ExactInterfaces = collectDominatingSuperInterfaces(hierarchy, t2, t2Supers);
      if (!t1ExactInterfaces && t2Supers.size() != 1) {
        // we found an interface; it might also apply to t1; must bail
        return Constants.TYPE_unknown;
      }
    }

    if (!t2ExactClasses || !t2ExactInterfaces) {
      // there may be some common interfaces that we don't know about
      return "";
    }

    for (Iterator<String> iter = t2Supers.iterator(); iter.hasNext();) {
      String element = iter.next();
      boolean subsumed = false;

      for (String element2 : t2Supers) {
        if (element != element2 && isSubtypeOf(hierarchy, element2, element) == YES) {
          subsumed = true;
          break;
        }
      }

      if (subsumed) {
        iter.remove();
      }
    }

    if (t2Supers.size() == 1) {
      return t2Supers.iterator().next();
    } else if (t2Supers.size() == 0) {
      return Constants.TYPE_Object;
    } else {
      return Constants.TYPE_unknown; // some non-representable combination of
      // class and interfaces
    }
  }

  /**
   * Compute the most specific common supertype.
   * 
   * @param hierarchy the hierarchy information to use for the decision
   * @param t1 a type in JVM format
   * @param t2 a type in JVM format
   * @return the most specific common supertype of t1 and t2, or TYPE_unknown if it cannot be determined or cannot be represented as
   *         a Java type, or null if there is no common supertype
   */
  public static String findCommonSupertype(ClassHierarchyProvider hierarchy, String t1, String t2) {
    if (t1 == null || t2 == null) {
      return null;
    } else if (t1.equals(t2)) {
      return t1;
    } else if (t1.equals(Constants.TYPE_unknown) || t2.equals(Constants.TYPE_unknown)) {
      return Constants.TYPE_unknown;
    } else {
      if (t2.charAt(0) == '[') { // put array into t1
        String t = t1;
        t1 = t2;
        t2 = t;
      }

      switch (t1.charAt(0)) {
      case 'L':
        // two non-array types
        // if either one is constant null, return the other one
        if (t1.equals(Constants.TYPE_null)) {
          return t2;
        } else if (t2.equals(Constants.TYPE_null)) {
          return t1;
        } else if (hierarchy == null) {
          // don't have a class hierarchy
          return Constants.TYPE_unknown;
        } else {
          return findCommonSupertypeHierarchy(hierarchy, t1, t2);
        }
      case '[': {
        char ch2 = t2.charAt(0);
        if (ch2 == '[') {
          char ch1_1 = t1.charAt(1);
          if (ch1_1 == '[' || ch1_1 == 'L') {
            return "[" + findCommonSupertype(hierarchy, t1.substring(1), t2.substring(1));
          } else {
            return Constants.TYPE_Object;
          }
        } else if (ch2 == 'L') {
          if (t2.equals(Constants.TYPE_null)) {
            return t1;
          } else if (t2.equals("Ljava/io/Serializable;") || t2.equals("Ljava/lang/Cloneable;")) {
            return t2;
          } else {
            return Constants.TYPE_Object;
          }
        } else {
          return null;
        }
      }
      default:
        return null;
      }
    }
  }
}
