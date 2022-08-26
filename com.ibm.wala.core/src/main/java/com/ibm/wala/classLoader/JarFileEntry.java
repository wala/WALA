/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.classLoader;

import com.ibm.wala.core.util.io.FileSuffixes;
import com.ibm.wala.util.debug.Assertions;
import java.io.InputStream;
import java.util.jar.JarFile;

/** An entry in a Jar file. */
public class JarFileEntry implements ModuleEntry {

  private final String entryName;

  private final JarFileModule jarFileModule;

  protected JarFileEntry(String entryName, JarFileModule jarFile) {
    this.entryName = entryName;
    this.jarFileModule = jarFile;
  }

  @Override
  public String getName() {
    return entryName;
  }

  @Override
  public boolean isClassFile() {
    return FileSuffixes.isClassFile(getName());
  }

  @Override
  public InputStream getInputStream() {
    try {
      JarFile jarFile = jarFileModule.getJarFile();
      return jarFile.getInputStream(jarFile.getEntry(entryName));
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public long getSize() {
    // TODO: cache this?
    return jarFileModule.getJarFile().getEntry(entryName).getSize();
  }

  @Override
  public String toString() {
    return jarFileModule.getJarFile().getName() + ':' + getName();
  }

  @Override
  public boolean isModuleFile() {
    return FileSuffixes.isJarFile(getName()) || FileSuffixes.isWarFile(getName());
  }

  @Override
  public Module asModule() {
    return new NestedJarFileModule(jarFileModule, jarFileModule.getJarFile().getEntry(entryName));
  }

  public JarFile getJarFile() {
    return jarFileModule.getJarFile();
  }

  @Override
  public JarFileModule getContainer() {
    return jarFileModule;
  }

  @Override
  public int hashCode() {
    return entryName.hashCode() * 5059 + jarFileModule.getJarFile().hashCode();
  }

  @Override
  public String getClassName() {
    return FileSuffixes.stripSuffix(getName());
  }

  @Override
  public boolean isSourceFile() {
    return FileSuffixes.isSourceFile(getName());
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }
}
