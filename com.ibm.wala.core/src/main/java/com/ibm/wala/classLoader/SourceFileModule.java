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

import com.ibm.wala.core.util.io.FileSuffixes;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;

/** A {@link Module} which is a wrapper around a source file */
public class SourceFileModule extends FileModule implements Module, ModuleEntry, SourceModule {

  private final String fileName;
  /** cache result of {@link #getURL()}, for performance */
  private URL url;

  public SourceFileModule(File f, String fileName, Module container) {
    super(f, container);
    this.fileName = fileName;
  }

  public SourceFileModule(File f, SourceFileModule clonedFrom) {
    super(f, clonedFrom.getContainer());
    this.fileName = clonedFrom.fileName;
  }

  @Override
  public String toString() {
    return "SourceFileModule:" + getFile().toString();
  }

  @Override
  public boolean isClassFile() {
    return false;
  }

  @Override
  public String getClassName() {
    return FileSuffixes.stripSuffix(fileName).replace(File.separator.charAt(0), '/');
  }

  @Override
  public boolean isSourceFile() {
    return true;
  }

  @Override
  public Reader getInputReader() {
    return new InputStreamReader(getInputStream());
  }

  @Override
  public URL getURL() {
    if (url == null) {
      try {
        url = getFile().toURI().toURL();
      } catch (MalformedURLException e) {
        throw new Error("error making URL for " + getFile(), e);
      }
    }
    return url;
  }
}
