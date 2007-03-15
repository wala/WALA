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
import com.ibm.wala.util.debug.Trace;

/**
 * A module containing files under some directory.
 * 
 * @author julian (i think?)
 * 
 */
public abstract class DirectoryTreeModule implements Module {

  protected final File root;

  /**
   * @param root a directory
   */
  DirectoryTreeModule(File root) {
    this.root = root;
    assert root.isDirectory();
  }

  protected abstract FileModule makeFile(File file);

  protected abstract boolean includeFile(File file);

  private Set<ModuleEntry> getEntriesRecursive(File dir) {
    Set<ModuleEntry> result = HashSetFactory.make();
    File[] files = dir.listFiles();
    if (files != null) {
      for (int i = 0; i < files.length; i++) {
        if (files[i].isDirectory()) {
          result.addAll(getEntriesRecursive(files[i]));
        } else if (includeFile(files[i])) {
          result.add(makeFile(files[i]));
        }
      }
    } else {
      // TODO: replace this with a real warning when the WarningSets are
      // revamped
      Trace.println("Warning: failed to retrieve files in " + dir);
    }

    return result;
  }

  public Iterator<ModuleEntry> getEntries() {
    return getEntriesRecursive(root).iterator();
  }

  public String getPath() {
    return root.getAbsolutePath();
  }
}
