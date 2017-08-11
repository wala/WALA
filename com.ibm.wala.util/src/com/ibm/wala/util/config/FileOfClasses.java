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
package com.ibm.wala.util.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An object which represents a set of classes read from a text file.
 */
public class FileOfClasses extends SetOfClasses {

  /* Serial version */
  private static final long serialVersionUID = -3256390509887654322L;  

  private static final boolean DEBUG = false;

  private Pattern pattern = null;

  private String regex = null;

  private boolean needsCompile = false;
  
  public FileOfClasses(InputStream input) throws IOException {
    if (input == null) {
      throw new IllegalArgumentException("null input");
    }
    try (final BufferedReader is = new BufferedReader(new InputStreamReader(input))) {
  
      StringBuffer regex = null;
      String line;
      while ((line = is.readLine()) != null) {
  
        if (line.startsWith("#")) continue;
  
        if (regex == null) {
          regex = new StringBuffer("(" + line + ")");
        } else {
          regex.append("|(" + line + ")");
        }
      }
  
      if (regex != null) {
        this.regex = regex.toString();
        needsCompile = true;
      }
    }
  }

  private void compile() {
    pattern = Pattern.compile(regex);
    needsCompile = false;
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.impl.SetOfClasses#contains(java.lang.String)
   */
  @Override
  public boolean contains(String klassName) {
    if (needsCompile) {
      compile();
    }
    if (pattern == null) {
      return false;
    }
    Matcher m = pattern.matcher(klassName);
    if (DEBUG) {
      if (m.matches()) {
        System.err.println(klassName + " " + true);
      } else {
        System.err.println(klassName + " " + false);
      }
    }
    return m.matches();
  }

  /*
   * @see com.ibm.wala.ipa.callgraph.impl.SetOfClasses#add(com.ibm.wala.classLoader.IClass)
   */
  @Override
  public void add(String klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    if (regex == null) {
      regex = klass;
    } else {
      regex = regex + '|' + klass;
    }
    needsCompile = true;
  }
  
  @Override
  public String toString() {
    return this.regex;
  }
  
}
