package com.ibm.wala.j2ee;

import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.ModuleEntry;

public class J2EEArchiveFileModule extends JarFileModule {
  public J2EEArchiveFileModule(JarFile f) {
    super(f);
  }
  
  @Override
  protected ModuleEntry createEntry(ZipEntry z) {
    return new J2EEArchiveFileEntry(z.getName(), this);
  }
}
