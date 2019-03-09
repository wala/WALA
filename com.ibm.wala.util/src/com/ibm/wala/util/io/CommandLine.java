/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.util.io;

import java.util.Properties;

/** utilities for parsing a command line */
public class CommandLine {

  /**
   * create a Properties object representing the properties set by the command line args. if args[i]
   * is "-foo" and args[i+1] is "bar", then the result will define a property with key "foo" and
   * value "bar"
   *
   * @throws IllegalArgumentException if args == null
   */
  public static Properties parse(String[] args) throws IllegalArgumentException {
    if (args == null) {
      throw new IllegalArgumentException("args == null");
    }
    Properties result = new Properties();
    for (int i = 0; i < args.length; i++) {
      if (args[i] == null) {
        // skip it
        continue;
      }
      String key = parseForKey(args[i]);
      if (key != null) {
        if (args[i].contains("=")) {
          result.put(key, args[i].substring(args[i].indexOf('=') + 1));
        } else {
          if ((i + 1) >= args.length || args[i + 1].charAt(0) == '-') {
            throw new IllegalArgumentException(
                "Malformed command-line.  Must be of form -key=value or -key value");
          }
          result.put(key, args[i + 1]);
          i++;
        }
      }
    }
    return result;
  }

  /** if string is of the form "-foo" or "-foo=", return "foo". else return null. */
  private static String parseForKey(String string) {
    if (string.charAt(0) == '-') {
      if (string.contains("=")) {
        return string.substring(1, string.indexOf('='));
      } else {
        return string.substring(1);
      }
    } else {
      return null;
    }
  }
}
