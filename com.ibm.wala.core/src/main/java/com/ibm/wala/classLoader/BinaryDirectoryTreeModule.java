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

import com.ibm.wala.core.util.warnings.Warning;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import java.io.File;

/** Module representing a directory of .class files */
public class BinaryDirectoryTreeModule extends DirectoryTreeModule {

  public BinaryDirectoryTreeModule(File root) {
    super(root);
  }

  @Override
  protected boolean includeFile(File file) {
    return file.getName().endsWith("class");
  }

  @Override
  protected FileModule makeFile(final File file) {
    try {
      return new ClassFileModule(file, this);
    } catch (InvalidClassFileException e) {
      Warnings.add(
          new Warning(Warning.MODERATE) {

            @Override
            public String getMsg() {
              return "Invalid class file at path " + file.getAbsolutePath();
            }
          });
      return null;
    }
  }
}
