/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.util.scope;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.util.collections.Filter;

/**
 * A file which accepts an IClass only if the package name matches
 * a regular expression
 * 
 * @author sjfink
 *
 */
public class PackageExpressionFilter implements Filter {

  private final Pattern pattern;

  public PackageExpressionFilter(String pattern) {
    this.pattern = Pattern.compile(pattern);
  }

  public boolean accepts(Object o) {
    IClass c = (IClass) o;
    if (c.getName().getPackage() == null) {
      return false;
    }
    CharSequence packageName =  c.getName().getPackage().toString();
    Matcher m = pattern.matcher(packageName);
    return m.matches();
  }

  @Override
  public String toString() {
    return "PackageExpressionFilter:" + pattern.pattern();
  }

  /**
   * @return the regular expression.
   */
  public Pattern getPattern() {
    return pattern;
  }
}
