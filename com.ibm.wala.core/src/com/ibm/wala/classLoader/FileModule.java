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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Iterator;

import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * A module which is a wrapper around a file in the filesystem
 * 
 * @author sfink
 */
public abstract class FileModule implements Module, ModuleEntry {

  private final File file;

  public FileModule(File f) {
    this.file = f;
  }

  public String getAbsolutePath() {
    return file.getAbsolutePath();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.Module#getEntries()
   */
  public Iterator<ModuleEntry> getEntries() {
    return new NonNullSingletonIterator<ModuleEntry>(this);
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return file.hashCode();
  }
  

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.FileModule#equals(java.lang.Object)
   */
  public boolean equals(Object o) {
    if (o.getClass().equals(getClass())) {
      FileModule other = (FileModule)o;
      return getName().equals(other.getName());
    } else {
      return false;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#getName()
   */
  public String getName() {
    return file.getName();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#getInputStream()
   */
  public InputStream getInputStream() {
    try {
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE("could not read " + file);
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#isModuleFile()
   */
  public boolean isModuleFile() {
    return false;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.ModuleEntry#asModule()
   */
  public Module asModule() {
    Assertions.UNREACHABLE();
    return null;
  }
  /**
   * @return Returns the file.
   */
  public File getFile() {
    return file;
  }
}
