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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.Properties;
import java.util.StringTokenizer;

import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.config.FileProvider;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.SimpleVector;
import com.ibm.wala.util.warnings.WalaException;

/**
 * @author sfink
 * @author Eran Yahav
 */
public class Table implements Cloneable {

  // table is implemented as a SimpleVector of rows. Each row is a SimpleVector.
  private final SimpleVector<SimpleVector> rows = new SimpleVector<SimpleVector>();

  // SimpleVector<String> ... headings of columns
  private final SimpleVector<String> columnHeadings = new SimpleVector<String>();

  /**
   * create an empty table
   */
  public Table() {
  }

  /**
   * create an empty table with the same column headings as t
   */
  public Table(Table t) {
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

  /**
   * read from a direct (native) text file
   * @param fileName
   * @return
   * @throws WalaException
   */
  public static Table readFromDirectTextFile(String fileName) throws WalaException {
    File f = new File(fileName);
    return readFromTextFile(f);
  }

  /**
   * read from a text file obtained as a resource
   * @param fileName
   * @return
   * @throws IOException
   * @throws WalaException
   */
  public static Table readFromTextFile(String fileName) throws IOException, WalaException {
    File f = FileProvider.getFile(fileName);
    return readFromTextFile(f);
  }

  /**
   * @param f
   *          a file containing a table in text format, whitespace delimited
   * @throws WalaException
   */
  private static Table readFromTextFile(File f) throws WalaException {
    try {
      return readFromStream(new FileInputStream(f));
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      throw new WalaException("readFromTextFile failed");
    }
  }
  
  /**
   * @param s
   *          a stream  containing a table in text format, whitespace delimited
   * @throws WalaException
   */
  public static Table readFromStream(InputStream s) throws WalaException {
    try {
      Table result = new Table();

      LineNumberReader reader = new LineNumberReader(new InputStreamReader(s));

      // LineNumberReader reader = new LineNumberReader(new
      // InputStreamReader(new FileInputStream(f)));
      String line = readNextNonCommentLine(reader);
      if (line == null) {
        throw new IOException("first line expected to be column headings");
      }
      result.populateColumnHeadings(line);

      line = readNextNonCommentLine(reader);
      int row = 0;
      while (line != null) {
        result.populateRow(row, line);
        line = readNextNonCommentLine(reader);
        row++;
      }

      return result;
    } catch (IOException e) {
      e.printStackTrace();
      throw new WalaException("readFromStream failed");
    }
  }

  public static String readNextNonCommentLine(LineNumberReader reader) throws IOException {
    String line = reader.readLine();
    while (line != null && isCommented(line)) {
      line = reader.readLine();
    }
    return line;
  }

  private static boolean isCommented(String line) {
    return line.charAt(0) == Constants.COMMENT;
  }

  private void populateRow(int row, String line) {
    StringTokenizer st = new StringTokenizer(line);
    int nColumns = st.countTokens();
    Assertions.productionAssertion(nColumns == getNumberOfColumns(), "expected " + getNumberOfColumns() + " got " + nColumns
        + " row " + row);
    SimpleVector<Object> r = new SimpleVector<Object>();
    rows.set(row, r);
    for (int i = 0; i < nColumns; i++) {
      r.set(i, st.nextElement());
    }

  }

  /**
   * @param line
   *          a whitespace-delimited string of column names
   */
  private void populateColumnHeadings(String line) {
    StringTokenizer st = new StringTokenizer(line);
    int nColumns = st.countTokens();
    for (int i = 0; i < nColumns; i++) {
      columnHeadings.set(i, (String)st.nextElement());
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

  public Object getElement(int row, int column) {
    SimpleVector r = (SimpleVector) rows.get(row);
    return r.get(column);
  }

  /**
   * Note that column indices start at zero
   */
  public String getColumnHeading(int i) {
    return (String) columnHeadings.get(i);
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

  public Properties row2Properties(int row) {
    Properties result = new Properties();
    for (int j = 0; j < getNumberOfColumns(); j++) {
      result.put(getColumnHeading(j), getElement(row, j));
    }
    return result;
  }

  public void addRow(Properties p) {
    SimpleVector<Object> r = new SimpleVector<Object>();
    rows.set(rows.getMaxIndex() + 1, r);
    for (int i = 0; i < getNumberOfColumns(); i++) {
      r.set(i, p.getProperty(getColumnHeading(i)));
    }
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    Table result = new Table(this);
    for (int i = 0; i < getNumberOfRows(); i++) {
      result.addRow(row2Properties(i));
    }
    return result;
  }
}
