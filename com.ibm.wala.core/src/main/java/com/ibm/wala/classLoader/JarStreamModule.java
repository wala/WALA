/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
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
import com.ibm.wala.core.util.warnings.Warning;
import com.ibm.wala.core.util.warnings.Warnings;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * Read in a jar file from an input stream. Most parts are copied from the NestedJarFileModule class
 * and adapted to work with an input stream.
 *
 * @author Juergen Graf &lt;juergen.graf@gmail.com&gt;
 */
public class JarStreamModule extends JarInputStream implements Module {

  private static final boolean DEBUG = false;

  /**
   * For efficiency, we cache the byte[] holding each ZipEntry's contents; this will help avoid
   * multiple unzipping TODO: use a soft reference?
   */
  private HashMap<String, byte[]> cache = null;

  public JarStreamModule(InputStream stream) throws IOException {
    super(stream);
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
    try {
      for (ZipEntry z = getNextEntry(); z != null; z = getNextEntry()) {
        final String name = z.getName();
        if (DEBUG) {
          System.err.println(("got entry: " + name));
        }
        if (FileSuffixes.isClassFile(name) || FileSuffixes.isSourceFile(name)) {
          ByteArrayOutputStream out = new ByteArrayOutputStream();
          byte[] temp = new byte[1024];
          int n = read(temp);
          while (n != -1) {
            out.write(temp, 0, n);
            n = read(temp);
          }
          byte[] bb = out.toByteArray();
          cache.put(name, bb);
        }
      }
    } catch (IOException e) {
      // just go with what we have
      Warnings.add(
          new Warning() {

            @Override
            public String getMsg() {
              return "could not read contents of jar input stream.";
            }
          });
    }
  }

  protected long getEntrySize(String name) {
    populateCache();
    byte[] b = cache.get(name);
    return b.length;
  }

  @Override
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

      @Override
      public boolean hasNext() {
        return next != null;
      }

      @Override
      public ModuleEntry next() {
        ModuleEntry result = new Entry(next);
        advance();
        return result;
      }

      @Override
      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }

  /** @author sfink an entry in a nested jar file. */
  private class Entry implements ModuleEntry {

    private final String name;

    Entry(String name) {
      this.name = name;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public boolean isClassFile() {
      return FileSuffixes.isClassFile(getName());
    }

    @Override
    public InputStream getInputStream() {
      return JarStreamModule.this.getInputStream(name);
    }

    @Override
    public boolean isModuleFile() {
      return false;
    }

    @Override
    public Module asModule() {
      Assertions.UNREACHABLE();
      return null;
    }

    @Override
    public String toString() {
      return "nested entry: " + name;
    }

    @Override
    public String getClassName() {
      return FileSuffixes.stripSuffix(getName());
    }

    @Override
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
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Entry other = (Entry) obj;
      if (!getOuterType().equals(other.getOuterType())) return false;
      if (name == null) {
        if (other.name != null) return false;
      } else if (!name.equals(other.name)) return false;
      return true;
    }

    private JarStreamModule getOuterType() {
      return JarStreamModule.this;
    }

    @Override
    public Module getContainer() {
      return JarStreamModule.this;
    }
  }

  @Override
  public String toString() {
    return "Jar input stream " + super.toString();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    JarStreamModule other = (JarStreamModule) obj;
    return super.equals(other);
  }
}
