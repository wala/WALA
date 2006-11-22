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
package com.ibm.wala.examples.drivers;

import java.util.Iterator;
import java.util.Map;

import com.ibm.wala.util.system.Environment;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * Test the functionality to read the OS environment
 * 
 * @author sfink
 */
public class GetEnv {

  public static void main(String[] args) {
    try {
      Map<String, String> m = Environment.readEnv();

      for (Iterator<Map.Entry<String, String>> it = m.entrySet().iterator(); it.hasNext();) {
        System.out.println(it.next());
      }
    } catch (WalaException e) {
      e.printStackTrace();
    }
  }
}
