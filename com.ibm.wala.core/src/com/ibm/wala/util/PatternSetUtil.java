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
package com.ibm.wala.util;

import java.util.Iterator;

import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.ecore.regex.EPattern;

/**
 * @author sfink
 *
 */
public class PatternSetUtil {

  /**
   * @param c a Container of EPattern
   * @return a regular expression that is the OR of a set of regular expressions
   * contained the EContainer
   * @throws IllegalArgumentException  if c is null
   */
  public static String composeRegularExpression(EContainer c) {
    if (c == null) {
      throw new IllegalArgumentException("c is null");
    }
    StringBuffer result = new StringBuffer();
    for (Iterator it = c.getContents().iterator(); it.hasNext(); ) {
      EPattern pattern = (EPattern)it.next();
      String s = pattern.getPattern();
      result.append(s);
      if (it.hasNext()) {
        result.append('|');
      }
    }
    return result.toString();
  }

}
