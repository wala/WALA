package com.ibm.wala.core.java11;

import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import java.io.IOException;

public class Java9AnalysisScopeReader extends AnalysisScopeReader {

  public static final Java9AnalysisScopeReader instance = new Java9AnalysisScopeReader();

  static {
    setScopeReader(instance);
  }

  @Override
  protected boolean handleInSubclass(
      AnalysisScope scope,
      ClassLoaderReference walaLoader,
      String language,
      String entryType,
      String entryPathname) {
    if ("jrt".equals(entryType)) {
      try {
        scope.addToScope(walaLoader, new JrtModule(entryPathname));
        return true;
      } catch (IOException e) {
        return false;
      }
    } else {
      return false;
    }
  }
}
