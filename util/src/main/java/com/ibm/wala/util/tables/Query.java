/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.tables;

import static com.google.common.base.Preconditions.checkArgument;

import com.ibm.wala.util.collections.HashSetFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import org.jspecify.annotations.NullUnmarked;
import org.jspecify.annotations.Nullable;

/** Misc SQL-like support for queries on tables */
public class Query {

  /**
   * SELECT * from t where column=value
   *
   * @throws IllegalArgumentException if t == null
   */
  @NullUnmarked
  public static <T> Collection<Map<String, @Nullable T>> selectStarWhereEquals(
      Table<T> t, String column, T value) throws IllegalArgumentException {
    checkArgument(t != null, "t == null");
    Collection<Map<String, @Nullable T>> result = new ArrayList<>();
    for (int i = 0; i < t.getNumberOfRows(); i++) {
      Map<String, @Nullable T> p = t.row2Map(i);
      if (p.get(column).equals(value)) {
        result.add(p);
      }
    }
    return result;
  }

  /** SELECT attribute FROM t where column=value */
  @SuppressWarnings("NullAway") // to enable JSpecifyExperimental
  public static <T> Collection<@Nullable T> selectWhereEquals(
      Table<T> t, String attribute, String column, T value) {
    Collection<Map<String, @Nullable T>> rows = selectStarWhereEquals(t, column, value);
    Collection<@Nullable T> result = HashSetFactory.make();
    for (Map<String, @Nullable T> p : rows) {
      result.add(p.get(attribute));
    }
    return result;
  }

  /** SELECT attribute FROM t where P(column) */
  @SuppressWarnings("NullAway") // to enable JSpecifyExperimental
  public static <T> Collection<Map<String, @Nullable T>> selectStarWhere(
      Table<T> t, String column, Predicate<@Nullable T> P) {
    checkArgument(t != null, "t == null");
    Collection<Map<String, @Nullable T>> c = new ArrayList<>();
    for (int i = 0; i < t.getNumberOfRows(); i++) {
      Map<String, @Nullable T> p = t.row2Map(i);
      T s = p.get(column);
      if (P.test(s)) {
        c.add(p);
      }
    }
    return c;
  }

  public static <T> Table<T> viewWhereEquals(Table<T> t, String column, T value) {
    Collection<Map<String, @Nullable T>> c = selectStarWhereEquals(t, column, value);
    Table<T> result = new Table<>(t);
    for (Map<String, @Nullable T> p : c) {
      result.addRow(p);
    }
    return result;
  }

  public static StringTable viewWhereEquals(StringTable t, String column, String value) {
    Collection<Map<String, @Nullable String>> c = selectStarWhereEquals(t, column, value);
    StringTable result = new StringTable(t);
    for (Map<String, @Nullable String> p : c) {
      result.addRow(p);
    }
    return result;
  }
}
