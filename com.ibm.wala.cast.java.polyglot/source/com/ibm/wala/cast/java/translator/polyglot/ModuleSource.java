/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Oct 6, 2005
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import polyglot.frontend.FileSource;
import polyglot.frontend.Resource;

import com.ibm.wala.classLoader.SourceModule;

/**
 * A Polyglot Source whose input comes from an InputStream.<br>
 * Currently extends FileSource since that's all that the Polyglot Compiler class
 * will accept.
 * @author rfuhrer
 */
public class ModuleSource extends FileSource {
  private final SourceModule module;
  
  SourceModule getModule() {
    return module;
  }
  
  public ModuleSource(final SourceModule module) throws IOException {
    super(new Resource() {
      public File file() {
        return new File(module.getURL().getFile());
      }

      public InputStream getInputStream() throws IOException {
        return module.getInputStream();
      }

      public String name() {
        String fullPath = module.getURL().getFile();
        int idx= fullPath.lastIndexOf(File.separatorChar);
        return (idx > 0) ? fullPath.substring(idx+1) : fullPath;
      }
      @Override
      public String toString() {
        return module.getName();
      }
    }, true);
    this.module = module;
  }
}
