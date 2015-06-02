/*******************************************************************************
 * Copyright (c) 2015 Google Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Google Inc - initial API and implementation
 *******************************************************************************/

package com.ibm.wala.classLoader;

import com.ibm.wala.util.io.FileSuffixes;

/**
 * An entry in a War file.
 */
public class WarFileEntry extends JarFileEntry {

  private static final String WAR_CLASS_PREFIX = "WEB-INF/classes/";

  protected WarFileEntry(String entryName, JarFileModule jarFile) {
    super(entryName, jarFile);
  }

  /*
   * @see com.ibm.wala.classLoader.ModuleEntry#getClassName()
   */
  @Override
  public String getClassName() {
    String className = FileSuffixes.stripSuffix(getName());
    if (className.startsWith(WAR_CLASS_PREFIX)) {
      className = className.substring(WAR_CLASS_PREFIX.length());
    }
    return className;
  }
}
