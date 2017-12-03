/*******************************************************************************
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * Refinement Analysis Tools is Copyright (c) 2007 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.util.collections;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.Permission;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Miscellaneous utility functions.
 */
public class Util {

  /** The empty {@link BitSet}. */
  public static final BitSet EMPTY_BITSET = new BitSet();

  /**
   * Get a {@link String} representation of a {@link Throwable}.
   * 
   * @throws IllegalArgumentException if thrown == null
   */
  public static String str(Throwable thrown) throws IllegalArgumentException {
    if (thrown == null) {
      throw new IllegalArgumentException("thrown == null");
    }
    // create a memory buffer to which to dump the trace
    ByteArrayOutputStream traceDump = new ByteArrayOutputStream();
    try (final PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(traceDump, StandardCharsets.UTF_8)))) {
      thrown.printStackTrace(w);
    }
    return traceDump.toString();
  }
  
  /**
   * Return those elements of <code>c</code> that are assignable to <code>klass</code>.
   */
  @SuppressWarnings("unchecked")
  public static <S, T> Set<T> filterByType(Iterable<S> c, Class<T> klass) {
    Set<T> result = HashSetFactory.make();
    for(S s : c)
      if(klass.isAssignableFrom(s.getClass()))
        result.add((T)s);
    return result;
  }

  /**
   * Test whether <em>some</em> element of the given {@link Collection} satisfies the given {@link Predicate}.
   * 
   * @throws IllegalArgumentException if c == null
   */
  public static <T> boolean forSome(Collection<T> c, Predicate<T> p) throws IllegalArgumentException {
    if (c == null) {
      throw new IllegalArgumentException("c == null");
    }
    for (T t : c) {
      if (p.test(t)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Test whether <em>some</em> element of the given {@link Collection} satisfies the given {@link Predicate}.
   * 
   * @return The first element satisfying the predicate; otherwise null.
   * @throws IllegalArgumentException if c == null
   */
  public static <T> T find(Collection<T> c, Predicate<T> p) throws IllegalArgumentException {
    if (c == null) {
      throw new IllegalArgumentException("c == null");
    }
    for (T obj : c) {
      if (p.test(obj))
        return obj;
    }

    return null;
  }

  /**
   * Test whether <em>all</em> elements of the given {@link Collection} satisfy the given {@link Predicate}.
   * 
   * @throws NullPointerException if c == null
   */
  public static <T> boolean forAll(Collection<T> c, Predicate<T> p) throws NullPointerException {
    for (T t : c) {
      if (!p.test(t))
        return false;
    }
    return true;
  }

  /**
   * Perform an action for all elements in a collection.
   * 
   * @param c the collection
   * @param v the visitor defining the action
   * @throws IllegalArgumentException if c == null
   */
  public static <T> void doForAll(Collection<T> c, ObjectVisitor<T> v) throws IllegalArgumentException {
    if (c == null) {
      throw new IllegalArgumentException("c == null");
    }
    for (T t : c)
      v.visit(t);
  }

  /**
   * Map a list: generate a new list with each element mapped. The new list is always an {@link ArrayList}; it would have been more
   * precise to use {@link java.lang.reflect reflection} to create a list of the same type as 'srcList', but reflection works really
   * slowly in some implementations, so it's best to avoid it.
   * 
   * @throws IllegalArgumentException if srcList == null
   */
  public static <T, U> List<U> map(List<T> srcList, Function<T, U> f) throws IllegalArgumentException {
    if (srcList == null) {
      throw new IllegalArgumentException("srcList == null");
    }
    ArrayList<U> result = new ArrayList<>();
    for (T t : srcList) {
      result.add(f.apply(t));
    }
    return result;
  }

  /**
   * Map a set: generate a new set with each element mapped. The new set is always a {@link HashSet}; it would have been more
   * precise to use {@link java.lang.reflect reflection} to create a set of the same type as 'srcSet', but reflection works really
   * slowly in some implementations, so it's best to avoid it.
   * 
   * @throws IllegalArgumentException if srcSet == null
   */
  public static <T, U> Set<U> mapToSet(Collection<T> srcSet, Function<T, U> f) throws IllegalArgumentException {
    if (srcSet == null) {
      throw new IllegalArgumentException("srcSet == null");
    }
    HashSet<U> result = HashSetFactory.make();
    for (T t : srcSet) {
      result.add(f.apply(t));
    }
    return result;
  }

  /*
   * Grow an int[] -- i.e. allocate a new array of the given size, with the initial segment equal to this int[].
   */
  public static int[] realloc(int[] data, int newSize) throws IllegalArgumentException {
    if (data == null) {
      throw new IllegalArgumentException("data == null");
    }
    if (data.length < newSize) {
      int[] newData = new int[newSize];
      System.arraycopy(data, 0, newData, 0, data.length);
      return newData;
    } else
      return data;
  }

  /** Generate strings with fully qualified names or not */
  public static final boolean FULLY_QUALIFIED_NAMES = false;

  /**
   * Write object fields to string
   * 
   * @throws IllegalArgumentException if obj == null
   */
  public static String objectFieldsToString(Object obj) throws IllegalArgumentException {
    if (obj == null) {
      throw new IllegalArgumentException("obj == null");
    }
    // Temporarily disable the security manager
    SecurityManager oldsecurity = System.getSecurityManager();
    System.setSecurityManager(new SecurityManager() {
      @Override
      public void checkPermission(Permission perm) {
      }
    });

    Class<?> c = obj.getClass();
    StringBuffer buf = new StringBuffer(FULLY_QUALIFIED_NAMES ? c.getName() : removePackageName(c.getName()));
    while (c != Object.class) {
      Field[] fields = c.getDeclaredFields();

      if (fields.length > 0)
        buf = buf.append(" (");

      for (int i = 0; i < fields.length; i++) {
        // Make this field accessible
        fields[i].setAccessible(true);

        try {
          Class<?> type = fields[i].getType();
          String name = fields[i].getName();
          Object value = fields[i].get(obj);

          // name=value : type
          buf = buf.append(name);
          buf = buf.append("=");
          buf = buf.append(value == null ? "null" : value.toString());
          buf = buf.append(" : ");
          buf = buf.append(FULLY_QUALIFIED_NAMES ? type.getName() : removePackageName(type.getName()));
        } catch (IllegalAccessException e) {
          e.printStackTrace();
        }

        buf = buf.append(i + 1 >= fields.length ? ")" : ",");
      }
      c = c.getSuperclass();
    }
    // Reinstate the security manager
    System.setSecurityManager(oldsecurity);

    return buf.toString();
  }

  /** Remove the package name from a fully qualified class name */
  public static String removePackageName(String fully_qualified_name_) {
    if (fully_qualified_name_ == null)
      return null;

    int lastdot = fully_qualified_name_.lastIndexOf('.');

    if (lastdot < 0) {
      return "";
    } else {
      return fully_qualified_name_.substring(lastdot + 1);
    }
  }

  /**
   * checks if two sets have a non-empty intersection
   * 
   * @return <code>true</code> if the sets intersect; <code>false</code> otherwise
   */
  public static <T> boolean intersecting(final Set<T> s1, final Set<T> s2) {
    return forSome(s1, s2::contains);
  }

  /**
   * given the name of a class C, returns the name of the top-most enclosing class of class C. For example, given A$B$C, the method
   * returns A
   * 
   * @return String name of top-most enclosing class
   * @throws IllegalArgumentException if typeStr == null
   */
  public static String topLevelTypeString(String typeStr) throws IllegalArgumentException {
    if (typeStr == null) {
      throw new IllegalArgumentException("typeStr == null");
    }
    int dollarIndex = typeStr.indexOf('$');
    String topLevelTypeStr = dollarIndex == -1 ? typeStr : typeStr.substring(0, dollarIndex);
    return topLevelTypeStr;
  }

  public static <T> void addIfNotNull(T val, Collection<T> vals) {
    if (val != null) {
      vals.add(val);
    }
  }

  /**
   * @return the amount of memory currently being used, in bytes. Often inaccurate, but there's no better thing to do from within
   *         the JVM.
   */
  public static long getUsedMemory() {
    gc();
    long totalMemory = Runtime.getRuntime().totalMemory();
    gc();
    long freeMemory = Runtime.getRuntime().freeMemory();
    long usedMemory = totalMemory - freeMemory;
    return usedMemory;
  }

  private static void gc() {
    try {
      for (int i = 0; i < 2; i++) {
        System.gc();
        Thread.sleep(100);
        System.runFinalization();
        Thread.sleep(100);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

} // class Util
