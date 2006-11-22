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
package com.ibm.wala.util.system;

import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.dynamic.BasicLauncher;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WalaException;

/**
 * Read the system environment as a Map
 */
public class Environment  {

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.capa.core.impl.EAnalysisEngineImpl#processImpl()
   */
  public static Map<String, String> readEnv() throws WalaException {
    BasicLauncher b = new BasicLauncher();
    if (File.separatorChar == '\\')
      b.setCmd("cmd.exe /Cset");
    else
      b.setCmd("printenv");
    b.setCaptureOutput(true);
    b.launch();
    return parseOutput(b.getOutput());
  }

  /**
   * @param b
   *          a set of lines of the form KEY=value
   */
  private static Map<String, String> parseOutput(byte[] b) {
    String s = new String(b);
    HashMap<String, String> result = new HashMap<String, String>();
    BufferedReader br = new BufferedReader(new StringReader(s));
    try {
      String line = br.readLine();
      while (line != null) {
        int i = line.indexOf('=');
        String key = line.substring(0, i);
        String value = line.substring(i + 1);
        result.put(key, value);
        line = br.readLine();
      }
    } catch (Exception e) {
      Assertions.UNREACHABLE();
    }
    return result;
  }
} // Environment
