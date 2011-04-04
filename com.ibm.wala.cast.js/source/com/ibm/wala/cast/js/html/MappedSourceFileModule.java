package com.ibm.wala.cast.js.html;

import java.io.File;

import com.ibm.wala.classLoader.SourceFileModule;

public class MappedSourceFileModule extends SourceFileModule implements MappedSourceModule {
  private final FileMapping fileMapping;
  
  public MappedSourceFileModule(File f, String fileName, FileMapping fileMapping) {
    super(f, fileName);
    this.fileMapping = fileMapping;
  }

  public MappedSourceFileModule(File f, SourceFileModule clonedFrom, FileMapping fileMapping) {
    super(f, clonedFrom);
    this.fileMapping = fileMapping;
  }

  public FileMapping getMapping() {
    return fileMapping;
  }

}
