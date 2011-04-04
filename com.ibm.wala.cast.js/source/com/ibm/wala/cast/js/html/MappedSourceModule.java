package com.ibm.wala.cast.js.html;

import com.ibm.wala.classLoader.SourceModule;

public interface MappedSourceModule extends SourceModule {

  FileMapping getMapping();
  
}
