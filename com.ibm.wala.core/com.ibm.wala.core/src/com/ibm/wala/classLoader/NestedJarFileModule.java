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

import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.io.FileSuffixes;

/**
 * 
 * A Jar file nested in a parent jar file
 * 
 * @author sfink
 * 
 */
public class NestedJarFileModule implements Module {

  private static final boolean DEBUG = false;

  private final JarFileModule parent;
  private final ZipEntry entry;

  /**
   * For efficiency, we cache the byte[] holding each ZipEntry's contents; this
   * will help avoid multiple unzipping TODO: use a soft reference?
   */
  private HashMap<String, byte[]> cache = null;

  /**
   *  
   */
  public NestedJarFileModule(JarFileModule parent, ZipEntry entry) {
    this.parent = parent;
    this.entry = entry;
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
    try {
      cache = HashMapFactory.make();
      final byte[] b = parent.getContents(entry);
      final JarInputStream stream = new JarInputStream(new ByteArrayInputStream(b));
      for (ZipEntry z = stream.getNextEntry(); z != null; z = stream.getNextEntry()) {
        if (DEBUG) {
          Trace.println("got entry: " + z.getName());
        }
        if (FileSuffixes.isClassFile(z.getName()) || FileSuffixes.isSourceFile(z.getName())) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          byte[] temp = new byte[1024];
          int n = stream.read(temp);
          while (n != -1) {
            out.write(temp, 0, n);
            n = stream.read(temp);
          }

          byte[] bb = out.toByteArray();
          try {
            if (FileSuffixes.isClassFile(z.getName())) {
              // check that we can read without an InvalidClassFileException
              new ClassReader(bb);
            }
            cache.put(z.getName(), bb);
          } catch (InvalidClassFileException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
            Assertions.UNREACHABLE();
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }

  protected long getEntrySize(String name) {
    populateCache();
    byte[] b = cache.get(name);
    return b.length;
  }

  /*
   * (non-Javadoc)
   * 
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
   * @author sfink
   * an entry in a nested jar file.
   */
  private class Entry implements ModuleEntry {

    private final String name;

    Entry(String name) {
      this.name = name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.classLoader.ModuleEntry#getName()
     */
    public String getName() {
      return name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.classLoader.ModuleEntry#isClassFile()
     */
    public boolean isClassFile() {
      return FileSuffixes.isClassFile(getName());
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#getInputStream()
     */
    public InputStream getInputStream() {
      return NestedJarFileModule.this.getInputStream(name);
    }

    public long getSize() {
      return NestedJarFileModule.this.getEntrySize(name);
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#isModuleFile()
     */
    public boolean isModuleFile() {
      return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#asModule()
     */
    public Module asModule() {
      Assertions.UNREACHABLE();
      return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return "nested entry: " + name;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.classLoader.ModuleEntry#getClassName()
     */
    public String getClassName() {
      return FileSuffixes.stripSuffix(getName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.wala.classLoader.ModuleEntry#isSourceFile()
     */
    public boolean isSourceFile() {
      return FileSuffixes.isSourceFile(getName());
    }

  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "Nested Jar File:" + entry.getName();
  }

}