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
  public boolean isSourceFile() {
    return true;
  }

  @Override
  public Reader getInputReader() {
    return new InputStreamReader(getInputStream());
  }
}
