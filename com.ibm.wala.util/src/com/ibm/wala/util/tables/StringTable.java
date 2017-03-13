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
import java.util.StringTokenizer;

import com.ibm.wala.util.collections.SimpleVector;
import com.ibm.wala.util.debug.Assertions;

/**
 */
public class StringTable extends Table<String> implements Cloneable {

  /**
   * create an empty table
   */
  public StringTable() {
    super();
  }

  /**
   * create an empty table with the same column headings as t
   */
  public StringTable(StringTable t) {
    super(t);
  }

  /**
   * create an empty table with the given column headings
   */
  public StringTable(String[] columns) {
    super(columns);
  }

  /**
   * read from a direct (native) text file
   * 
   * @throws IOException
   * @throws FileNotFoundException
   * @throws IllegalArgumentException if fileName is null
   * 
   */
  public static StringTable readFromDirectTextFile(String fileName, Character comment) throws FileNotFoundException, IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("fileName is null");
    }
    File f = new File(fileName);
    return readFromTextFile(f, comment);
  }

//  /**
//   * read from a text file obtained as a resource
//   */
//  public static StringTable readFromTextFile(String fileName, Character comment) throws IOException {
//    if (fileName == null) {
//      throw new IllegalArgumentException("null fileName");
//    }
//    File f = FileProvider.getFile(fileName);
//    return readFromTextFile(f, comment);
//  }

  /**
   * @param f a file containing a table in text format, whitespace delimited
   * @throws IOException
   * @throws FileNotFoundException
   */
  public static StringTable readFromTextFile(File f, Character comment) throws FileNotFoundException, IOException {
    if (f == null) {
      throw new IllegalArgumentException("null f");
    }
    try (final FileInputStream in = new FileInputStream(f)) {
      return readFromStream(in, comment);
    }
  }

  /**
   * @param s a stream containing a table in text format, whitespace delimited
   * @throws IOException
   * @throws IllegalArgumentException if s is null
   */
  public static StringTable readFromStream(InputStream s, Character commentToken) throws IOException {
    return readFromStream(s, commentToken, null);
  }

  /**
   * @param s a stream containing a table in text format, whitespace delimited
   * @throws IOException
   * @throws IllegalArgumentException if s is null
   */
  public static StringTable readFromStream(InputStream s, Character commentToken, Character delimiter) throws IOException {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    StringTable result = new StringTable();

    LineNumberReader reader = new LineNumberReader(new InputStreamReader(s));

    // LineNumberReader reader = new LineNumberReader(new
    // InputStreamReader(new FileInputStream(f)));
    String line = readNextNonCommentLine(reader, commentToken);
    if (line == null) {
      throw new IOException("first line expected to be column headings");
    }
    result.populateColumnHeadings(line, delimiter);

    line = readNextNonCommentLine(reader, commentToken);
    int row = 0;
    while (line != null) {
      result.populateRow(row, line, delimiter);
      line = readNextNonCommentLine(reader, commentToken);
      row++;
    }

    return result;
  }

  public static String readNextNonCommentLine(LineNumberReader reader, Character commentToken) throws IOException {
    if (reader == null) {
      throw new IllegalArgumentException("reader is null");
    }
    String line = reader.readLine();
    while (line != null && isCommented(line, commentToken)) {
      line = reader.readLine();
    }
    return line;
  }

  private static boolean isCommented(String line, Character commentToken) {
    if (line.length() == 0) {
      return true;
    }
    if (commentToken == null) {
      return false;
    }
    return line.charAt(0) == commentToken;
  }

  private void populateRow(int row, String line, Character delimiter) {
    StringTokenizer st = delimiter == null ? new StringTokenizer(line) : new StringTokenizer(line, delimiter.toString());
    int nColumns = st.countTokens();
    Assertions.productionAssertion(nColumns == getNumberOfColumns(), "expected " + getNumberOfColumns() + " got " + nColumns
        + " row " + row + " " + line.length() + " " + line);
    SimpleVector<String> r = new SimpleVector<>();
    rows.add(row, r);
    for (int i = 0; i < nColumns; i++) {
      r.set(i, (String) st.nextElement());
    }
  }

  /**
   * @param line a whitespace-delimited string of column names
   */
  private void populateColumnHeadings(String line, Character delimiter) {
    StringTokenizer st = delimiter == null ? new StringTokenizer(line) : new StringTokenizer(line, delimiter.toString());
    int nColumns = st.countTokens();
    for (int i = 0; i < nColumns; i++) {
      columnHeadings.set(i, (String) st.nextElement());
    }
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    StringTable result = new StringTable(this);
    for (int i = 0; i < getNumberOfRows(); i++) {
      result.addRow(row2Map(i));
    }
    return result;
  }
}
