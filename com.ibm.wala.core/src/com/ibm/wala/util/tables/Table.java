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
package com.ibm.wala.util.tables;

import java.util.Map;

import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.intset.SimpleVector;

/**
 * @author sfink
 * @author Eran Yahav
 * @author Alexey Loginov
 */
public class Table<T> implements Cloneable {

  // table is implemented as a SimpleVector of rows. Each row is a SimpleVector<T>.
  protected final SimpleVector<SimpleVector<T>> rows = new SimpleVector<SimpleVector<T>>();

  // SimpleVector<String> ... headings of columns
  protected final SimpleVector<String> columnHeadings = new SimpleVector<String>();

  /**
   * create an empty table
   */
  public Table() {
  }

  /**
   * create an empty table with the same column headings as t
   */
  public Table(Table<T> t) {
    for (int i = 0; i < t.getNumberOfColumns(); i++) {
      columnHeadings.set(i, t.getColumnHeading(i));
    }
  }

  /**
   * create an empty table with the given column headings
   */
  public Table(String[] columns) {
    for (int i = 0; i < columns.length; i++) {
      columnHeadings.set(i, columns[i]);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    int[] format = computeColumnWidths();
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < getNumberOfColumns(); i++) {
      StringBuffer heading = new StringBuffer(getColumnHeading(i));
      StringStuff.padWithSpaces(heading, format[i]);
      result.append(heading);
    }
    result.append("\n");
    for (int j = 0; j < getNumberOfRows(); j++) {
      for (int i = 0; i < getNumberOfColumns(); i++) {
        StringBuffer element = new StringBuffer(getElement(j, i).toString());
        StringStuff.padWithSpaces(element, format[i]);
        result.append(element);
      }
      result.append("\n");
    }
    return result.toString();
  }

  public T getElement(int row, int column) {
    SimpleVector<T> r = rows.get(row);
    return r.get(column);
  }

  /**
   * Note that column indices start at zero
   */
  public String getColumnHeading(int i) {
    return columnHeadings.get(i);
  }

  private int[] computeColumnWidths() {
    int[] result = new int[getNumberOfColumns()];
    for (int i = 0; i < getNumberOfColumns(); i++) {
      result[i] = columnHeadings.get(i).toString().length() + 1;
    }
    for (int j = 0; j < getNumberOfRows(); j++) {
      for (int i = 0; i < getNumberOfColumns(); i++) {
        result[i] = Math.max(result[i], getElement(j, i).toString().length() + 1);
      }
    }
    return result;
  }

  public int getNumberOfColumns() {
    return columnHeadings.getMaxIndex() + 1;
  }

  public int getNumberOfRows() {
    return rows.getMaxIndex() + 1;
  }

  public Map<String,T> row2Map(int row) {
    Map<String,T> result = HashMapFactory.make();
    for (int j = 0; j < getNumberOfColumns(); j++) {
      result.put(getColumnHeading(j), getElement(row, j));
    }
    return result;
  }

  public void addRow(Map<String,T> p) {
    SimpleVector<T> r = new SimpleVector<T>();
    rows.set(rows.getMaxIndex() + 1, r);
    for (int i = 0; i < getNumberOfColumns(); i++) {
      r.set(i, (T) p.get(getColumnHeading(i)));
    }
  }
}
