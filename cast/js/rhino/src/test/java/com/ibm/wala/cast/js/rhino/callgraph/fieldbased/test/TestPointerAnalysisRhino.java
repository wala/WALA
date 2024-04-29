package com.ibm.wala.cast.js.rhino.callgraph.fieldbased.test;

import com.ibm.wala.cast.js.test.TestPointerAnalyses;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;

public class TestPointerAnalysisRhino extends TestPointerAnalyses {

  public TestPointerAnalysisRhino() {
    super(new CAstRhinoTranslatorFactory());
  }
}
