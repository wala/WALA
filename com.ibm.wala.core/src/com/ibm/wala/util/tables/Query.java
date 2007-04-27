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
import java.util.Properties;

import com.ibm.wala.util.collections.HashSetFactory;

/**
 * Misc SQL-like support for queries on tables
 * 
 * @author sjfink
 *
 */
public class Query {

  /**
   * SELECT * from t where column="value"
   */
  public static Collection<Properties> selectStarWhereEquals(Table t, String column, String value) {
    Collection<Properties> result = new ArrayList<Properties>();
    for (int i = 0 ; i < t.getNumberOfRows(); i++) {
      Properties p = t.row2Properties(i);
      if (p.getProperty(column).equals(value)) {
        result.add(p);
      }
    }
    return result;
  }

  /**
   * SELECT attribute FROM t where column="value"
   */
  public static Collection<String> selectWhereEquals(Table t, String attribute, String column, String value) {
    Collection<Properties> rows = selectStarWhereEquals(t, column, value);
    Collection<String> result = HashSetFactory.make();
    for (Properties p : rows) {
      result.add(p.getProperty(attribute));
    }
    return result;
  }

  public static Table viewWhereEquals(Table t, String column, String value) {
    Collection<Properties> c = selectStarWhereEquals(t, column, value);
    Table result = new Table(t);
    for (Properties p : c) {
      result.addRow(p);
    }
    return result;
  }
}
