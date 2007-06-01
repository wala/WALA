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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.emf.wrappers.EUtil;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * An object which represents a set of strings, defined intensionally as
 * the language accepted by any of the regular expressions in a file which
 * holds an EContainer of EPattern.
 * 
 * @author sfink
 */
public class IntensionalSetOfStrings {

  private static final boolean DEBUG = false;

  private Pattern pattern;
  final private String regex;
  private boolean needsCompile = true;

  public IntensionalSetOfStrings(String xmlFile, ClassLoader loader) throws WalaException {
    super();

    EContainer c = (EContainer)EUtil.readEObjects(xmlFile, loader).get(0);

    regex = PatternSetUtil.composeRegularExpression(c);
    needsCompile = true;
  }

  private void compile() {
    pattern = Pattern.compile(regex);
    needsCompile = false;
  }

  public boolean contains(String signature) {
    if (needsCompile) {
      compile();
    }
    Matcher m = pattern.matcher(signature);
    if (DEBUG) {
      if (m.matches()) {
        System.err.println(signature + " " + true);
      } else {
        System.err.println(signature + " " + false);
      }
    }
    return m.matches();
  }
}