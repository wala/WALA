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
package com.ibm.wala.classLoader;

import java.io.File;

import com.ibm.wala.util.io.FileSuffixes;

/**
 * 
 * A module which is a wrapper around a .java file
 * 
 * @author sfink
 */
public class SourceFileModule extends FileModule implements Module, ModuleEntry {

  private final String fileName;
  public SourceFileModule(File f, String fileName) {
    super(f);
    this.fileName = fileName;
  }

  public SourceFileModule(File f, SourceFileModule clonedFrom) {
    super(f);
    this.fileName = clonedFrom.fileName;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "SourceFileModule:" + getFile().toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.ModuleEntry#isClassFile()
   */
  public boolean isClassFile() {
    return false;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.ModuleEntry#getClassName()
   */
  public String getClassName() {
    return FileSuffixes.stripSuffix(fileName).replace(File.separator.charAt(0), '/');
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.ModuleEntry#isSourceFile()
   */
  public boolean isSourceFile() {
    return true;
  }
 
}
