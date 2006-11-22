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

public class BinaryDirectoryTreeModule extends DirectoryTreeModule {

  public BinaryDirectoryTreeModule(File root) {
    super(root);
  }

  protected boolean includeFile(File file) {
	  return file.getName().endsWith("class");
  }
  
  protected FileModule makeFile(File file) {
    return new ClassFileModule(file);
  }

}
