package com.ibm.wala.cast.java.translator.polyglot;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.ipa.callgraph.impl.SetOfClasses;

public class PolyglotJavaSourceAnalysisEngine extends JavaSourceAnalysisEngine {

  public IRTranslatorExtension getTranslatorExtension() {
    return new JavaIRTranslatorExtension();
  }

  protected ClassLoaderFactory getClassLoaderFactory(SetOfClasses exclusions) {
    return new PolyglotClassLoaderFactory(exclusions, getTranslatorExtension());
  }

}
