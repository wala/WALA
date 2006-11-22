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
package com.ibm.wala.util.io;

import java.util.Properties;

/**
 *
 * utilities for parsing a command line
 * 
 * @author sfink
 */
public class CommandLine {
 
  /**
   * create a Properties object representing the properties set by the command line args.
   * if args[i] is "-foo" and args[i+1] is "bar", then the result will define a property
   * with key "foo" and value "bar"
   */
  public static Properties parse(String[] args) {
    Properties result = new Properties(); 
    for (int i = 0; i<args.length-1; i++) {
      String key = parseForKey(args[i]);
      if (key != null) {
        result.put(key,args[i+1]);
        i++;
      }
    }
    return result;
  }

  /**
   * if string is of the form "-foo", return "foo".
   * else return null.
   */
  private static String parseForKey(String string) {
    if (string.charAt(0) == '-') {
      return string.substring(1);
    } else {
      return null;
    }
  }
}
