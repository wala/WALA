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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileSuffixes;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 * A Jar file nested in a parent jar file
 */
public class NestedJarFileModule implements Module {

  private static final boolean DEBUG = false;

  private final JarFileModule parent;

  private final ZipEntry entry;

  /**
   * For efficiency, we cache the byte[] holding each ZipEntry's contents; this will help avoid multiple unzipping TODO: use a soft
   * reference?
   */
  private HashMap<String, byte[]> cache = null;

  public NestedJarFileModule(JarFileModule parent, ZipEntry entry) {
    this.parent = parent;
    this.entry = entry;
    if (parent == null) {
      throw new IllegalArgumentException("null parent");
    }
    if (entry == null) {
      throw new IllegalArgumentException("null entry");
    }
  }

  public InputStream getInputStream(String name) {
    populateCache();
    byte[] b = cache.get(name);
    return new ByteArrayInputStream(b);
  }

  private void populateCache() {
    if (cache != null) {
      return;
    }
    cache = HashMapFactory.make();
    final byte[] b = parent.getContents(entry);
    try {
      final JarInputStream stream = new JarInputStream(new ByteArrayInputStream(b));
      for (ZipEntry z = stream.getNextEntry(); z != null; z = stream.getNextEntry()) {
        final String name = z.getName();
        if (DEBUG) {
          System.err.println(("got entry: " + name));
        }
        if (FileSuffixes.isClassFile(name) || FileSuffixes.isSourceFile(name)) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          byte[] temp = new byte[1024];
          int n = stream.read(temp);
          while (n != -1) {
            out.write(temp, 0, n);
            n = stream.read(temp);
          }
          byte[] bb = out.toByteArray();
          cache.put(name, bb);
        }
      }
    } catch (IOException e) {
      // just go with what we have
      Warnings.add(new Warning() {

        @Override
        public String getMsg() {
          return "could not read contents of nested jar file " + entry.getName() + ", parent " + parent.getAbsolutePath();
        }
      });
    }

  }

  protected long getEntrySize(String name) {
    populateCache();
    byte[] b = cache.get(name);
    return b.length;
  }

  /*
   * @see com.ibm.wala.classLoader.Module#getEntries()
   */
  public Iterator<ModuleEntry> getEntries() {
    populateCache();
    final Iterator<String> it = cache.keySet().iterator();
    return new Iterator<ModuleEntry>() {
      String next = null;
      {
        advance();
      }

      private void advance() {
        if (it.hasNext()) {
          next = it.next();
        } else {
          next = null;
        }
      }

      public boolean hasNext() {
        return next != null;
      }

      public ModuleEntry next() {
        ModuleEntry result = new Entry(next);
        advance();
        return result;
      }

      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /**
   * @author sfink an entry in a nested jar file.
   */
  private class Entry implements ModuleEntry {

    private final String name;

    Entry(String name) {
      this.name = name;
    }

    /*
     * @see com.ibm.wala.classLoader.ModuleEntry#getName()
     */
    public String getName() {
      return name;
    }

    /*
     * @see com.ibm.wala.classLoader.ModuleEntry#isClassFile()
     */
    public boolean isClassFile() {
      return FileSuffixes.isClassFile(getName());
    }

    /*
     * @see com.ibm.wala.classLoader.ModuleEntry#getInputStream()
     */
    public InputStream getInputStream() {
      return NestedJarFileModule.this.getInputStream(name);
    }

    /*
     * @see com.ibm.wala.classLoader.ModuleEntry#isModuleFile()
     */
    public boolean isModuleFile() {
      return false;
    }

    /*
     * @see com.ibm.wala.classLoader.ModuleEntry#asModule()
     */
    public Module asModule() {
      Assertions.UNREACHABLE();
      return null;
    }

    @Override
    public String toString() {
      return "nested entry: " + name;
    }

    /*
     * @see com.ibm.wala.classLoader.ModuleEntry#getClassName()
     */
    public String getClassName() {
      return FileSuffixes.stripSuffix(getName());
    }

    /*
     * @see com.ibm.wala.classLoader.ModuleEntry#isSourceFile()
     */
    public boolean isSourceFile() {
      return FileSuffixes.isSourceFile(getName());
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + getOuterType().hashCode();
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      Entry other = (Entry) obj;
      if (!getOuterType().equals(other.getOuterType()))
        return false;
      if (name == null) {
        if (other.name != null)
          return false;
      } else if (!name.equals(other.name))
        return false;
      return true;
    }

    private NestedJarFileModule getOuterType() {
      return NestedJarFileModule.this;
    }

  }

  @Override
  public String toString() {
    return "Nested Jar File:" + entry.getName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((parent == null) ? 0 : parent.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    NestedJarFileModule other = (NestedJarFileModule) obj;
    if (parent == null) {
      if (other.parent != null)
        return false;
    } else if (!parent.equals(other.parent))
      return false;
    return true;
  }

}