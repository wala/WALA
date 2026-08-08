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
import java.net.URL;

public class SourceURLModule extends AbstractURLModule implements SourceModule {

  public SourceURLModule(URL url) {
    super(url);
  }

  @Override
  public boolean isClassFile() {
    return false;
  }

  @Override
  public String getClassName() {
    // For a JAR resource, the entry name is the logical name of the source file, e.g.
    // `hello/Hello.java`.  For any other URL it is a pathname, possibly absolute.
    return FileSuffixes.stripSuffix(getName()).replace(File.separator.charAt(0), '/');
  }

  @Override
  public boolean isSourceFile() {
    return true;
  }

  @Override
  public Reader getInputReader() {
    return new InputStreamReader(getInputStream());
  }
}
