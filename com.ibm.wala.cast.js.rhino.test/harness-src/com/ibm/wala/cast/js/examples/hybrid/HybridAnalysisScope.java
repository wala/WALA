package com.ibm.wala.cast.js.examples.hybrid;

import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Set;

public class HybridAnalysisScope extends AnalysisScope {

  private static final Set<Language> languages;

  static {
    languages = HashSetFactory.make();

    languages.add(Language.JAVA);
    languages.add(JavaScriptLoader.JS);
  }

  public HybridAnalysisScope() {
    super(languages);
    this.initForJava();

    ClassLoaderReference jsLoader = JavaScriptTypes.jsLoader;
    loadersByName.put(JavaScriptTypes.jsLoaderName, jsLoader);
  }

  public ClassLoaderReference getJavaScriptLoader() {
    return getLoader(JavaScriptTypes.jsLoaderName);
  }
}
