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

import java.io.InputStream;

/** A ModuleEntry represents a wrapper around a file representation in a {@link Module}. */
public interface ModuleEntry {

  /** @return a String that represents the name of the file described by this object */
  String getName();

  /** @return true if the file is a class file. */
  boolean isClassFile();

  /** @return true if the file is a source file. */
  boolean isSourceFile();

  /** @return an InputStream which provides the contents of this logical file. */
  InputStream getInputStream();

  /**
   * @return true iff this module entry (file) represents a module in its own right. e.g., a jar
   *     file which is an entry in another jar file.
   */
  boolean isModuleFile();

  /**
   * Precondition: isModuleFile().
   *
   * @return a Module view of this entry.
   */
  Module asModule();

  /**
   * @return the name of the class represented by this entry
   * @throws UnsupportedOperationException if !isClassFile() and !isSourceFile()
   */
  String getClassName();

  /** the containing module */
  Module getContainer();
}
