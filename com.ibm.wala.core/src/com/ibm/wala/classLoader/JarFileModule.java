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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.ibm.wala.util.CacheReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 * 
 * A module which is a wrapper around a Jar file
 * 
 * @author sfink
 */
public class JarFileModule implements Module {

  private final JarFile file;

  /**
   * For efficiency, try to cache the byte[] holding each ZipEntries contents;
   * this will help avoid multiple unzipping
   */
  private final HashMap<ZipEntry, Object> cache = HashMapFactory.make();

  public JarFileModule(JarFile f) {
    this.file = f;
  }

  public String getAbsolutePath() {
    return file.getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return "JarFileModule:" + file.getName();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.classLoader.Module#getEntries()
   */
  public Iterator<ModuleEntry> getEntries() {
    HashSet<ModuleEntry> result = HashSetFactory.make();
    for (Enumeration e = file.entries(); e.hasMoreElements();) {
      ZipEntry Z = (ZipEntry) e.nextElement();
      result.add(new JarFileEntry(Z.getName(), this));
    }
    return result.iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return file.hashCode();
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object arg0) {
    if (arg0 == null) {
      return false;
    }
    if (getClass().equals(arg0.getClass())) {
      JarFileModule other = (JarFileModule) arg0;
      return file.equals(other.file);
    } else {
      return false;
    }
  }

  public byte[] getContents(ZipEntry entry) {
    byte[] b = (byte[]) CacheReference.get(cache.get(entry));

    if (b != null) {
      return b;
    }

    try {
      InputStream s = file.getInputStream(entry);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      b = new byte[1024];
      int n = s.read(b);
      while (n != -1) {
        out.write(b, 0, n);
        n = s.read(b);
      }
      byte[] bb = out.toByteArray();
      cache.put(entry, CacheReference.make(bb));
      s.close();
      return bb;
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  public JarFile getJarFile() {
    return file;
  }

}
