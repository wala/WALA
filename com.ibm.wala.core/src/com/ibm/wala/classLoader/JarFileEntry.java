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

import java.io.InputStream;
import java.util.jar.JarFile;

import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileSuffixes;

/**
 *
 * An entry in a Jar file.
 * 
 * @author sfink
 */
public class JarFileEntry implements ModuleEntry {

  private final String entryName;
  private final JarFileModule jarFileModule;
  private final JarFile jarFile;
  JarFileEntry(String entryName, JarFileModule jarFile) {
    this.entryName = entryName;
    this.jarFileModule = jarFile;
    this.jarFile = jarFile.getJarFile();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#getName()
   */
  public String getName() {
    return entryName;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#isClassFile()
   */
  public boolean isClassFile() {
    return FileSuffixes.isClassFile(getName());
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#getInputStream()
   */
  public InputStream getInputStream() {
    try {
      return jarFile.getInputStream(jarFile.getEntry(entryName));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#getSize()
   */
  public long getSize() {
    // TODO: cache this?
    return jarFile.getEntry(entryName).getSize();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    // TODO Auto-generated method stub
    return jarFile.getName() + ":" + getName();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#isModuleFile()
   */
  public boolean isModuleFile() {
    return FileSuffixes.isJarFile(getName()) || FileSuffixes.isWarFile(getName());
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#asModule()
   */
  public Module asModule() {
    return new NestedJarFileModule(jarFileModule,jarFile.getEntry(entryName));
  }

  public JarFile getJarFile() {
    return jarFile;
  }

  @Override
  public int hashCode() { 
    return entryName.hashCode() * 5059 + jarFile.hashCode();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#getClassName()
   */
  public String getClassName() {
    return FileSuffixes.stripSuffix(getName());
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#isSourceFile()
   */
  public boolean isSourceFile() {
    return FileSuffixes.isSourceFile(getName());
  }
  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }
}
