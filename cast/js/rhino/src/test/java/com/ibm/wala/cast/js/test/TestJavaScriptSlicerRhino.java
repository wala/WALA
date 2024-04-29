package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import org.junit.jupiter.api.BeforeEach;

public class TestJavaScriptSlicerRhino extends TestJavaScriptSlicer {

  @BeforeEach
  public void setUp() {
    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(
        new CAstRhinoTranslatorFactory());
  }
}
