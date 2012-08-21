package com.ibm.wala.cast.js.test;

import org.junit.Before;

import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;

public class TestJQueryExamplesRhino extends TestJQueryExamples {

	  public static void main(String[] args) {
		    justThisTest(TestJQueryExamplesRhino.class);
		  }

	  @Before
	  public void setUp() {
		  JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
	  }

}
