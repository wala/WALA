package com.ibm.wala.j2ee;

import com.ibm.wala.classLoader.JarFileEntry;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.util.io.FileSuffixes;
import com.ibm.wala.util.ref.CacheReference;

public class J2EEArchiveFileEntry extends JarFileEntry {
  private Object contents;
  
  protected J2EEArchiveFileEntry(String entryName, JarFileModule jarFile) {
    super(entryName, jarFile);
  }

  private static final String prefix = "WEB-INF/classes/";
  private static final int length = prefix.length();
  
  @Override
  public String getClassName() {
    String name = FileSuffixes.stripSuffix(getName());
    if (FileSuffixes.isWarFile(getJarFile().getName())) {
      if (name.startsWith(prefix)) {
        name = name.substring(length);
      }
    }
    return name;
  }
  
  @Override
  public Module asModule() {
    return new J2EENestedArchiveFileModule(this);
  }
  
  public byte[] getContents() {
    byte[] b = (byte[]) CacheReference.get(contents);
    if (b == null) {
      b = getJarFileModule().getContents(getJarFile().getEntry(getName()));
      contents = CacheReference.make(b);
    }
    return b;
  }
}
