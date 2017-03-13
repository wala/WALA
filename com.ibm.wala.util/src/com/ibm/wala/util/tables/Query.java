/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.tables;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * Misc SQL-like support for queries on tables
 */
public class Query {

  /**
   * SELECT * from t where column=value
   * @throws IllegalArgumentException  if t == null
   */
  public static <T> Collection<Map<String,T>> selectStarWhereEquals(Table<T> t, String column, T value) throws IllegalArgumentException {
    if (t == null) {
      throw new IllegalArgumentException("t == null");
    }
    Collection<Map<String,T>> result = new ArrayList<>();
    for (int i = 0 ; i < t.getNumberOfRows(); i++) {
      Map<String,T> p = t.row2Map(i);
      if (p.get(column).equals(value)) {
        result.add(p);
      }
    }
    return result;
  }

  /**
   * SELECT attribute FROM t where column=value
   */
  public static <T> Collection<T> selectWhereEquals(Table<T> t, String attribute, String column, T value) {
    Collection<Map<String,T>> rows = selectStarWhereEquals(t, column, value);
    Collection<T> result = HashSetFactory.make();
    for (Map<String,T> p : rows) {
      result.add(p.get(attribute));
    }
    return result;
  }
  
  /**
   * SELECT attribute FROM t where P(column)
   */
  public static <T> Collection<Map<String,T>> selectStarWhere(Table<T> t, String column, Predicate<T> P) {
    if (t == null) {
      throw new IllegalArgumentException("t == null");
    }
    Collection<Map<String,T>> c = new ArrayList<>();
    for (int i = 0 ; i < t.getNumberOfRows(); i++) {
      Map<String,T> p = t.row2Map(i);
      T s = p.get(column);
      if (P.test(s)) {
        c.add(p);
      }
    }
    return c;
  }
 

  public static <T> Table<T> viewWhereEquals(Table<T> t, String column, T value) {
    Collection<Map<String,T>> c = selectStarWhereEquals(t, column, value);
    Table<T> result = new Table<>(t);
    for (Map<String,T> p : c) {
      result.addRow(p);
    }
    return result;
  }

  public static StringTable viewWhereEquals(StringTable t, String column, String value) {
    Collection<Map<String,String>> c = selectStarWhereEquals(t, column, value);
    StringTable result = new StringTable(t);
    for (Map<String,String> p : c) {
      result.addRow(p);
    }
    return result;
  }
}
