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

import java.io.File;

/** a module representing a directory tree of source files. */
public class SourceDirectoryTreeModule extends DirectoryTreeModule {

  /** file extension of source files in directory tree (defaults to "java" for Java source files) */
  String fileExt = "java";

  public SourceDirectoryTreeModule(File root) {
    super(root);
  }

  public SourceDirectoryTreeModule(File root, String fileExt) {
    super(root);
    if (fileExt != null) this.fileExt = fileExt;
  }

  @Override
  protected boolean includeFile(File file) {
    return file.getName().endsWith(fileExt);
  }

  @Override
  protected FileModule makeFile(File file) {
    String rootPath = root.getAbsolutePath();
    if (!rootPath.endsWith(File.separator)) {
      rootPath += File.separator;
    }

    String filePath = file.getAbsolutePath();

    assert filePath.startsWith(rootPath);

    return new SourceFileModule(file, filePath.substring(rootPath.length()), this);
  }

  @Override
  public String toString() {
    return "SourceDirectoryTreeModule:" + getPath();
  }
}
