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

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;

import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.UnimplementedError;
import com.ibm.wala.util.io.FileProvider;

public abstract class AbstractURLModule implements Module, ModuleEntry {

  private final URL url;

  public AbstractURLModule(URL url) {
    assert url != null; 
    this.url = url;
  }

  public URL getURL() {
    return url;
  }

  @Override
  public String getName() {
    try {
      URLConnection con = url.openConnection();
      if (con instanceof JarURLConnection)
        return ((JarURLConnection) con).getEntryName();
      else
        return (new FileProvider()).filePathFromURL(url);
    } catch (IOException e) {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  @Override
  public InputStream getInputStream() {
    try {
      return url.openConnection().getInputStream();
    } catch (IOException e) {
      Assertions.UNREACHABLE();
      return null;
    }
  }

  @Override
  public boolean isModuleFile() {
    return false;
  }

  @Override
  public Module asModule() throws UnimplementedError {
    Assertions.UNREACHABLE();
    return null;
  }

  @Override
  public String getClassName() throws UnsupportedOperationException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Iterator<ModuleEntry> getEntries() {
    return new NonNullSingletonIterator<>(this);
  }

  @Override
  public Module getContainer() {
    // URLs are freestanding, without containers
    return null;
  }
  
  @Override
  public String toString() {
    return "module:" + url.toString();
  }
  
}
