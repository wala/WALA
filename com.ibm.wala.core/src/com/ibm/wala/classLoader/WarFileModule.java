/*******************************************************************************
 * Copyright (c) 2015 Google Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Google Inc - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.classLoader;

import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * A module which is a wrapper around a War file
 */
public class WarFileModule extends JarFileModule {

  public WarFileModule(JarFile f) {
    super(f);
  }

  @Override
  public String toString() {
    return "WarFileModule:" + getJarFile().getName();
  }

  @Override
  protected ModuleEntry createEntry(ZipEntry z) {
    return new WarFileEntry(z.getName(), this);
  }
}
