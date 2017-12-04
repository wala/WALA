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
import com.ibm.wala.util.debug.UnimplementedError;

/**
 * A module which is a wrapper around a file in the filesystem
 */
public abstract class FileModule implements Module, ModuleEntry {

  private final File file;

  private final Module container;
  
  public FileModule(File f, Module container) throws IllegalArgumentException {
    if (f == null) {
      throw new IllegalArgumentException("f is null");
    }
    this.file = f;
    this.container = container;
    if (!f.exists()) {
      throw new IllegalArgumentException("bad file " + f.getAbsolutePath());
    }
  }

  public String getAbsolutePath() {
    return file.getAbsolutePath();
  }

  /*
   * @see com.ibm.wala.classLoader.Module#getEntries()
   */
  @Override
  public Iterator<ModuleEntry> getEntries() {
    return new NonNullSingletonIterator<>(this);
  }

  @Override
  public int hashCode() {
    return file.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (o.getClass().equals(getClass())) {
      FileModule other = (FileModule) o;
      return getName().equals(other.getName());
    } else {
      return false;
    }
  }

  /*
   * @see com.ibm.wala.classLoader.ModuleEntry#getName()
   */
  @Override
  public String getName() {
    return file.getName();
  }

  /*
   * @see com.ibm.wala.classLoader.ModuleEntry#getInputStream()
   */
  @Override
  public InputStream getInputStream() {
    try {
      if (!file.exists()) {
        System.err.println("PANIC: File does not exist! " + file);
      }
      return new FileInputStream(file);
    } catch (FileNotFoundException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE("could not read " + file);
      return null;
    }
  }

  /*
   * @see com.ibm.wala.classLoader.ModuleEntry#isModuleFile()
   */
  @Override
  public boolean isModuleFile() {
    return false;
  }

  /**
   * @return Returns the file.
   */
  public File getFile() {
    return file;
  }

  @Override
  public Module asModule() throws UnimplementedError {
    Assertions.UNREACHABLE("implement me");
    return null;
  }
  
  @Override
  public Module getContainer() {
    return container;
  }

}
