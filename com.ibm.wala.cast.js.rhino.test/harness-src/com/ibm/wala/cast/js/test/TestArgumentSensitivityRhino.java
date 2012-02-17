package com.ibm.wala.cast.js.test;

import org.junit.Before;

import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;

public class TestArgumentSensitivityRhino extends TestArgumentSensitivity {

	  @Before
	  public void setUp() {
	    JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
	  }

	  
}
