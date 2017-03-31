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

import java.util.ArrayList;
import java.util.Map;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.intset.BitVector;

/**
 */
public class Table<T> {

  // table is implemented as an ArrayList of rows. Each row is a SimpleVector<T>.
  protected final ArrayList<SimpleVector<T>> rows = new ArrayList<>();

  // SimpleVector<String> ... headings of columns
  protected final SimpleVector<String> columnHeadings = new SimpleVector<>();

  /**
   * create an empty table
   */
  public Table() {
  }

  /**
   * create an empty table with the same column headings as t
   * 
   * @throws IllegalArgumentException if t == null
   */
  public Table(Table<T> t) throws IllegalArgumentException {
    if (t == null) {
      throw new IllegalArgumentException("t == null");
    }
    for (int i = 0; i < t.getNumberOfColumns(); i++) {
      columnHeadings.set(i, t.getColumnHeading(i));
    }
  }

  /**
   * create an empty table with the given column headings
   * 
   * @throws IllegalArgumentException if columns == null, or columns[i] == null for some i
   */
  public Table(String[] columns) throws IllegalArgumentException {
    if (columns == null) {
      throw new IllegalArgumentException("columns == null");
    }
    for (int i = 0; i < columns.length; i++) {
      if (columns[i] == null) {
        throw new IllegalArgumentException("columns[" + i + "] is null");
      }
      columnHeadings.set(i, columns[i]);
    }
  }

  @Override
  public String toString() {
    int[] format = computeColumnWidths();
    StringBuffer result = new StringBuffer();
    for (int i = 0; i < getNumberOfColumns(); i++) {
      StringBuffer heading = new StringBuffer(getColumnHeading(i));
      padWithSpaces(heading, format[i]);
      result.append(heading);
    }
    result.append("\n");
    for (int j = 0; j < getNumberOfRows(); j++) {
      for (int i = 0; i < getNumberOfColumns(); i++) {
        T e = getElement(j, i);
        StringBuffer element = e == null ? new StringBuffer() : new StringBuffer(e.toString());
        padWithSpaces(element, format[i]);
        result.append(element);
      }
      result.append("\n");
    }
    return result.toString();
  }

  public synchronized T getElement(int row, int column) {
    try {
      SimpleVector<T> r = rows.get(row);
      return r.get(column);
    } catch (IndexOutOfBoundsException e) {
      throw new IllegalArgumentException("row: " + row + " column: " + column, e);
    }
  }

  /**
   * Note that column indices start at zero
   */
  public synchronized String getColumnHeading(int i) {
    return columnHeadings.get(i);
  }

  public int[] computeColumnWidths() {
    int[] result = new int[getNumberOfColumns()];
    for (int i = 0; i < getNumberOfColumns(); i++) {
      result[i] = columnHeadings.get(i).length() + 1;
    }
    for (int j = 0; j < getNumberOfRows(); j++) {
      for (int i = 0; i < getNumberOfColumns(); i++) {
        T element = getElement(j, i);
        result[i] = element == null ? result[i] : Math.max(result[i], element.toString().length() + 1);
      }
    }
    return result;
  }

  public synchronized int getNumberOfColumns() {
    return columnHeadings.getMaxIndex() + 1;
  }

  public synchronized int getNumberOfRows() {
    return rows.size();
  }

  public synchronized Map<String, T> row2Map(int row) {
    Map<String, T> result = HashMapFactory.make();
    for (int j = 0; j < getNumberOfColumns(); j++) {
      result.put(getColumnHeading(j), getElement(row, j));
    }
    return result;
  }

  public synchronized void addRow(Map<String, T> p) {
    if (p == null) {
      throw new IllegalArgumentException("null p " + p);
    }
    SimpleVector<T> r = new SimpleVector<>();
    rows.add(r);
    for (int i = 0; i < getNumberOfColumns(); i++) {
      r.set(i, p.get(getColumnHeading(i)));
    }
  }

  public synchronized void removeRow(Map<String, T> p) {
    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    BitVector toRemove = new BitVector();
    for (int i = 0; i < rows.size(); i++) {
      Map<String, T> row = row2Map(i);
      if (row.equals(p)) {
        toRemove.set(i);
      }
    }
    for (int i = 0; i < rows.size(); i++) {
      if (toRemove.get(i)) {
        rows.remove(i);
      }
    }

  }
  
  public static void padWithSpaces(StringBuffer b, int length) {
    if (b == null) {
      throw new IllegalArgumentException("b is null");
    }
    if (b.length() < length) {
      for (int i = b.length(); i < length; i++) {
        b.append(" ");
      }
    }
  }
}
