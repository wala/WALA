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
package com.ibm.wala.util.debug;

import java.io.*;
import java.util.Collection;
import java.util.Iterator;

/**
 * Simple utility for printing trace messages.
 * 
 * TODO: should this be nuked and replaced with some java.util.Logging
 * functions?
 * 
 * @author sfink
 * 
 */
public class Trace {

  private static final String TRACEFILE_KEY = "com.ibm.wala.tracefile";

  private static String traceFile = null;

  private static PrintStream out = null;

  public synchronized static void setTraceFile(String fileName) {
    System.setProperty(TRACEFILE_KEY, fileName);
  }
  
  public static PrintStream getTraceStream() {
    setTraceFile();
    return out;
  }

  /**
   * @return true iff we can print to the tracefile
   */
  private synchronized static boolean setTraceFile() {
    String fileName = System.getProperty(TRACEFILE_KEY);
    if (fileName == null) {
      if (traceFile != null) {
        traceFile = null;
        if (out != null) {
          out.close();
        }
        out = null;
      }
      return false;
    } else {
      if (traceFile != null) {
        if (traceFile.equals(fileName)) {
          // tracefile already initialized
          return true;
        } else {
          // change in tracefile
          traceFile = null;
          if (out != null) {
            out.close();
          }
          out = null;
        }
      }
      // open the new tracefile
      traceFile = fileName;
      File f = new File(fileName);
      try {
        out = new PrintStream(new FileOutputStream(f));
        return true;
      } catch (FileNotFoundException e) {
        System.err.println("Error: file not found: " + fileName);
        Assertions.UNREACHABLE("Invalid trace file: " + f);
        return false;
      }
    }
  }

  /**
   * Method println.
   * 
   * @param string
   */
  public static synchronized void println(String string) {
    if (setTraceFile()) {
      out.println(string);
    }
  }

  /**
   * @param o
   * @throws IllegalArgumentException  if o is null
   */
  public static synchronized void println(Object o) {
    if (o == null) {
      throw new IllegalArgumentException("o is null");
    }
    Trace.println(o.toString());
  }

  /**
   * @param string
   */
  public static synchronized void print(String string) {
    if (setTraceFile()) {
      out.print(string);
    }
  }

  /**
   * @param string
   * @param args
   */
  public static synchronized void format(String format, Object ... args) {
    /**
     * @param string
     */
    if (setTraceFile()) {
      out.format(format, args);
    }
  }

  public static void flush() {
    if (setTraceFile()) {
      out.flush();
    }
  }

  public static PrintWriter getTraceWriter() {
    if (setTraceFile()) {
      return new PrintWriter(out);
    } else {
      return null;
    }
  }

  /**
   * print S iff s contains substring
   * 
   * @param S
   * @param substring
   * @return true if something is printed, false otherwise
   */
  public static boolean guardedPrintln(String S, String substring) {
    if (substring == null || S.indexOf(substring) > -1) {
      println(S);
      return true;
    } else {
      return false;
    }
  }

  /**
   * print S iff s contains substring
   * 
   * @param S
   * @param substring
   * @return true if something is printed, false otherwise
   */
  public static boolean guardedPrint(String S, String substring) {
    if (substring == null || S.indexOf(substring) > -1) {
      print(S);
      return true;
    } else {
      return false;
    }
  }

  /**
   * @param string
   * @param c
   * @throws IllegalArgumentException  if c is null
   */
  public static void printCollection(String string, Collection c) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    println(string);
    if (c.isEmpty()) {
      println("none\n");
    } else {
      for (Iterator it = c.iterator(); it.hasNext();) {
        println(it.next().toString());
      }
      println("\n");
    }
  }

  /**
   * @return Returns the traceFile.
   */
  public synchronized static String getTraceFile() {
    setTraceFile();
    return traceFile;
  }
}
