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
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.util.collections.HashSetFactory;

/**
 * A module containing files under some directory.
 */
public abstract class DirectoryTreeModule implements Module {

  protected final File root;

  /**
   * @param root a directory
   */
  DirectoryTreeModule(File root) throws IllegalArgumentException {
    this.root = root;
    if (root == null) {
      throw new IllegalArgumentException("null root");
    }
    if (!root.exists()) {
      throw new IllegalArgumentException("root does not exist " + root);
    }
    if (!root.isDirectory()) {
      throw new IllegalArgumentException("root is not a directory " + root);
    }
  }

  /**
   * returns null if unsuccessful in creating FileModule
   */
  protected abstract FileModule makeFile(File file);

  protected abstract boolean includeFile(File file);

  private Set<FileModule> getEntriesRecursive(File dir) {
    Set<FileModule> result = HashSetFactory.make();
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory()) {
          result.addAll(getEntriesRecursive(file));
        } else if (includeFile(file)) {
          FileModule fileModule = makeFile(file);
          if (fileModule != null) {
            result.add(fileModule);
          }
        }
      }
    } else {
      // TODO: replace this with a real warning when the WarningSets are
      // revamped
      System.err.println(("Warning: failed to retrieve files in " + dir));
    }

    return result;
  }

  @Override
  public Iterator<FileModule> getEntries() {
    return getEntriesRecursive(root).iterator();
  }

  public String getPath() {
    return root.getAbsolutePath();
  }

  @Override
  public String toString() {
    return getClass().getName() + ":" + getPath();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((root == null) ? 0 : root.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    final DirectoryTreeModule other = (DirectoryTreeModule) obj;
    if (root == null) {
      if (other.root != null)
        return false;
    } else if (!root.equals(other.root))
      return false;
    return true;
  }
}
