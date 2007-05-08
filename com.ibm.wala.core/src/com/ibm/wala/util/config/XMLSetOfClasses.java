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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ecore.common.EContainer;
import com.ibm.wala.emf.wrappers.EUtil;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.PatternSetUtil;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.warnings.WalaException;

/**
 * 
 * An object which represents a set of classes read from an XML file.
 * 
 * @author sfink
 */
public class XMLSetOfClasses extends SetOfClasses {

  private static final boolean DEBUG = false;

  private Pattern pattern;

  private String regex;

  private boolean needsCompile = true;

  public XMLSetOfClasses(String xmlFile, ClassLoader loader) {
    super();

    EContainer c = null;
    try {
      c = (EContainer) EUtil.readEObjects(xmlFile, loader).get(0);
    } catch (WalaException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }

    regex = PatternSetUtil.composeRegularExpression(c);
    needsCompile = true;
  }

  private void compile() {
    pattern = Pattern.compile(regex);
    needsCompile = false;
  }

  public boolean contains(TypeReference klass) {
    return contains(klass.getName().toString());
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.impl.SetOfClasses#contains(java.lang.String)
   */
  public boolean contains(String klassName) {
    if (needsCompile) {
      compile();
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
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.impl.SetOfClasses#add(com.ibm.wala.classLoader.IClass)
   */
  public void add(IClass klass) {
    if (klass == null) {
      throw new IllegalArgumentException("klass is null");
    }
    regex = regex + '|' + klass.getReference().getName().toString();
    needsCompile = true;
  }
}