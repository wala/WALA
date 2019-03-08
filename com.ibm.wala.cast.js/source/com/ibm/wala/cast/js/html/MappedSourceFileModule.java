/*
 * Copyright (c) 2002 - 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.js.html;

import com.ibm.wala.classLoader.SourceFileModule;
import java.io.File;

public class MappedSourceFileModule extends SourceFileModule implements MappedSourceModule {
  private final FileMapping fileMapping;

  public MappedSourceFileModule(File f, String fileName, FileMapping fileMapping) {
    super(f, fileName, null);
    this.fileMapping = fileMapping;
  }

  public MappedSourceFileModule(File f, SourceFileModule clonedFrom, FileMapping fileMapping) {
    super(f, clonedFrom);
    this.fileMapping = fileMapping;
  }

  @Override
  public FileMapping getMapping() {
    return fileMapping;
  }
}
