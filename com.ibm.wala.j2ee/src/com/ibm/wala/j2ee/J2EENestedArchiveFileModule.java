package com.ibm.wala.j2ee;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileSuffixes;
import com.ibm.wala.util.ref.CacheReference;

public class J2EENestedArchiveFileModule implements Module {
  private ModuleEntry parent;

  private Map<String, Object> cache = null;
  
  protected J2EENestedArchiveFileModule(ModuleEntry parent) {
    this.parent = parent;
  }  

  private byte[] readBytes(InputStream stream) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    byte[] temp = new byte[1024];
    int n = stream.read(temp);
    while (n != -1) {
      out.write(temp, 0, n);
      n = stream.read(temp);
    }
    return out.toByteArray();
  }
  
  private byte[] getParentContents() throws IOException {
    if (parent instanceof J2EEArchiveFileEntry) {
      return ((J2EEArchiveFileEntry) parent).getContents();
    } else if (parent instanceof Entry) {
      return ((Entry) parent).getContents();
    } else {
      InputStream stream = parent.getInputStream();
      try {
        return readBytes(stream);
      } finally {
        if (stream != null) {
          stream.close();
        }
      }
    }
  }
  
  private void populateCache() {
    if (cache != null) {
      return;
    }
    
    try {
      cache = HashMapFactory.make();
      byte[] b = getParentContents();
      JarInputStream stream = new JarInputStream(new ByteArrayInputStream(b));
      for (ZipEntry z = stream.getNextEntry(); z != null; z = stream.getNextEntry()) {
        byte[] bb = readBytes(stream);
        try {
          if (FileSuffixes.isClassFile(z.getName())) {
            // check that we can read without an InvalidClassFileException
            new ClassReader(bb);
          }
          cache.put(z.getName(), CacheReference.make(bb));
        } catch (InvalidClassFileException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
          Assertions.UNREACHABLE();
        }
      }
    } catch(IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
  }
  
  public byte[] getContents(String entry) {
    byte[] bytes = (byte[]) CacheReference.get(cache.get(entry));
    
    if (bytes != null) {
      return bytes;
    }
    
    try {
      byte[] parent = getParentContents();
      JarInputStream stream = new JarInputStream(new ByteArrayInputStream(parent));
      for (ZipEntry z = stream.getNextEntry(); z != null; z = stream.getNextEntry()) {
        if (entry.equals(z.getName())) {
          bytes = readBytes(stream);
          try {
            if (FileSuffixes.isClassFile(entry)) {
              // check that we can read without an InvalidClassFileException
              new ClassReader(bytes);
            }
            cache.put(entry, CacheReference.make(bytes));
            break;
          } catch (InvalidClassFileException e1) {
            e1.printStackTrace();
            Assertions.UNREACHABLE();
            return null;
          }
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
    
    return bytes;
  }
  
  public Iterator<ModuleEntry> getEntries() {
    populateCache();
    final Iterator<String> it = cache.keySet().iterator();
    return new Iterator<ModuleEntry>() {
      public boolean hasNext() {
        return it.hasNext();
      }

      public ModuleEntry next() {
        return new Entry(it.next());
      }

      public void remove() {
        Assertions.UNREACHABLE();
      }
    };
  }
  
  private static final String prefix = "WEB-INF/classes/";
  private static final int length = prefix.length();
  
  private class Entry implements ModuleEntry {
    private final String name;
    
    public Entry(String name) {
      this.name = name;
    }
    
    public String getName() {
      return name;
    }
    
    public boolean isClassFile() {
      return FileSuffixes.isClassFile(name);
    }
    
    public boolean isSourceFile() {
      return FileSuffixes.isSourceFile(name);
    }
    
    public boolean isModuleFile() {
      return FileSuffixes.isJarFile(name) ||
        FileSuffixes.isWarFile(name);
    }
    
    public Module asModule() {
      if (isModuleFile()) {
        return new J2EENestedArchiveFileModule(this);
      } else {
        Assertions.UNREACHABLE();
        return null;
      }
    }
    
    public String getClassName() {
      if (isClassFile()) {
        String name = FileSuffixes.stripSuffix(getName());
        if (FileSuffixes.isWarFile(name)) {
          if (name.startsWith(prefix)) {
            name = name.substring(length);
          }
        }
        return name;
      } else {
        Assertions.UNREACHABLE();
        return null;
      }
    }

    public byte[] getContents() {
      return J2EENestedArchiveFileModule.this.getContents(name);
    }
    
    public InputStream getInputStream() {
      return new ByteArrayInputStream(getContents());
    }
  }
}
