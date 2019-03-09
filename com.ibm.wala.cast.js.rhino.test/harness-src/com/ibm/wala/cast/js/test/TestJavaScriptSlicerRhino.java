package com.ibm.wala.cast.js.test;

import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import org.junit.Before;

public class TestJavaScriptSlicerRhino extends TestJavaScriptSlicer {

  public static void main(String[] args) {
    justThisTest(TestJavaScriptSlicerRhino.class);
  }

  @Override
  @Before
  public void setUp() {
    com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil.setTranslatorFactory(
        new CAstRhinoTranslatorFactory());
  }
}
